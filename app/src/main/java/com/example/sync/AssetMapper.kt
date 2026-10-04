package com.example.sync

import com.example.model.AssetCondition
import com.example.model.AssetEntity
import com.example.model.AssetStatus
import com.example.model.AssetType
import com.example.model.SyncState
import com.example.model.VehicleType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

/**
 * Pemetaan AssetEntity (Room) <-> kolom aset di dashboard. Semua field aplikasi punya kolom sendiri di server,
 * jadi tidak ada yang disembunyikan di `details`.
 *
 * Field yang hanya bisa diisi dari dashboard (estimasi pajak tahunan, alamat, jumlah lantai, no. IMB/PBG,
 * departemen) sengaja TIDAK dikirim dari HP: aplikasi hanya menerimanya saat pull, supaya menyimpan aset dari
 * HP tidak pernah menghapus isian milik dashboard. `type` di server = jenis bidang kategori
 * (TANAH | BANGUNAN | KENDARAAN | INVENTARIS), sama persis dengan [AssetType].
 */
object AssetMapper {

    // Nilai status/kondisi di server (form web memakai daftar yang sama).
    private fun serverStatus(s: AssetStatus) = when (s) {
        AssetStatus.DIGUNAKAN -> "Active"
        AssetStatus.STANDBY -> "Standby"
        AssetStatus.DISEWAKAN -> "Rented"
        AssetStatus.DALAM_PERBAIKAN -> "Maintenance"
        AssetStatus.DIHAPUSBUKUKAN -> "Sold"
    }

    private fun localStatus(s: String?) = when (s) {
        "Standby" -> AssetStatus.STANDBY
        "Rented" -> AssetStatus.DISEWAKAN
        "Maintenance" -> AssetStatus.DALAM_PERBAIKAN
        "Sold" -> AssetStatus.DIHAPUSBUKUKAN
        else -> AssetStatus.DIGUNAKAN
    }

    private fun serverCondition(c: AssetCondition) = when (c) {
        AssetCondition.SANGAT_BAIK -> "Excellent"
        AssetCondition.BAIK -> "Good"
        AssetCondition.PERLU_PERBAIKAN -> "Minor Repair"
        AssetCondition.RUSAK -> "Damaged"
    }

    private fun localCondition(c: String?) = when (c) {
        "Excellent" -> AssetCondition.SANGAT_BAIK
        "Minor Repair" -> AssetCondition.PERLU_PERBAIKAN
        "Damaged" -> AssetCondition.RUSAK
        else -> AssetCondition.BAIK
    }

    // Tanggal di server berupa "yyyy-MM-dd". Formatter dibuat per pemanggilan (SimpleDateFormat tidak thread-safe).
    private fun formatDate(ms: Long?): String? =
        ms?.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it) }

    private fun parseDate(s: String?): Long? =
        s?.take(10)?.let { runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)?.time }.getOrNull() }

    /** Jika hari yang sama, pertahankan nilai lokal (yang menyimpan jam) agar tidak bergeser ke tengah malam. */
    private fun keepIfSameDay(local: Long?, remote: String?): Long? =
        if (local != null && formatDate(local) == remote?.take(10)) local else parseDate(remote)

    // Waktu pengambilan pin: ISO-8601 UTC saat dikirim; server membalas dengan offset (mis. +00:00).
    private fun formatInstant(ms: Long?): String? = ms?.let {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(it)
    }

    private fun parseInstant(s: String?): Long? =
        s?.let { runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(it)?.time }.getOrNull() }

    fun kindOf(s: String?): AssetType? = AssetType.values().firstOrNull { it.name == s }

    fun toChange(e: AssetEntity): PushChange {
        val deleted = e.syncState == SyncState.DELETED
        return PushChange(
            clientUuid = e.clientUuid,
            id = e.serverId,
            baseVersion = e.serverVersion,
            deleted = deleted,
            data = if (deleted) null else toPushAsset(e)
        )
    }

    private fun toPushAsset(e: AssetEntity) = PushAsset(
        name = e.name,
        type = e.type.name,
        categoryId = e.categoryId,
        code = e.code.ifBlank { null },
        price = e.acquisitionCost,
        purchaseDate = formatDate(e.acquisitionDate),
        usefulLifeYears = e.usefulLifeYears,
        salvageValue = e.salvageValue,
        pic = e.pic.ifBlank { null },
        department = e.department,
        condition = serverCondition(e.condition),
        description = e.notes.ifBlank { null },
        location = e.location.ifBlank { null },
        status = serverStatus(e.status),
        vehicleType = e.vehicleType?.name,
        licensePlate = e.licensePlate,
        brand = if (e.type == AssetType.INVENTARIS) e.brandModel else e.brand,
        model = e.vehicleModel,
        yearManufacture = e.yearManufacture,
        engineNumber = e.engineNumber,
        vin = e.chassisNumber,
        bpkbNumber = e.bpkbNumber,
        stnkNumber = e.stnkNumber,
        taxDueDate = formatDate(e.annualTaxDueDate),
        fiveYearPlateDueDate = formatDate(e.fiveYearPlateDueDate),
        lastServiceDate = formatDate(e.lastServiceDate),
        certificateType = e.certificateType,
        certificateNumber = e.certificateNumber,
        landArea = e.landAreaM2,
        buildingArea = e.buildingAreaM2,
        numberOfFloors = e.numberOfFloors,
        pbgNumber = e.pbgNumber,
        pbbNumber = e.pbbNop,
        serialNumber = e.serialNumber,
        latitude = e.latitude,
        longitude = e.longitude,
        locationAccuracy = e.locationAccuracyM,
        locationCapturedAt = formatInstant(e.locationCapturedAt)
    )

    /** Membangun entity SYNCED dari data server; [existing] (jika ada) menyumbang id lokal, kode & jam tanggal. */
    fun toEntity(r: RemoteAsset, existing: AssetEntity?): AssetEntity {
        val type = kindOf(r.type) ?: AssetType.INVENTARIS
        val now = System.currentTimeMillis()
        val defaultLife = when (type) {
            AssetType.TANAH -> 0
            AssetType.BANGUNAN -> 20
            AssetType.KENDARAAN -> 8
            AssetType.INVENTARIS -> 4
        }

        return AssetEntity(
            id = existing?.id ?: 0L,
            name = r.name,
            code = r.code ?: existing?.code ?: "WEB-${r.id}",
            type = type,
            categoryId = r.categoryId,
            acquisitionCost = r.price ?: 0.0,
            acquisitionDate = keepIfSameDay(existing?.acquisitionDate, r.purchaseDate) ?: existing?.acquisitionDate ?: now,
            usefulLifeYears = r.usefulLifeYears ?: defaultLife,
            salvageValue = r.salvageValue ?: 0.0,
            location = r.location.orEmpty(),
            pic = r.pic.orEmpty(),
            status = localStatus(r.status),
            condition = localCondition(r.condition),
            notes = r.description.orEmpty(),
            vehicleType = VehicleType.values().firstOrNull { it.name == r.vehicleType },
            licensePlate = r.licensePlate,
            engineNumber = r.engineNumber,
            chassisNumber = r.vin,
            bpkbNumber = r.bpkbNumber,
            stnkNumber = r.stnkNumber,
            annualTaxDueDate = keepIfSameDay(existing?.annualTaxDueDate, r.taxDueDate),
            fiveYearPlateDueDate = keepIfSameDay(existing?.fiveYearPlateDueDate, r.fiveYearPlateDueDate),
            annualTaxAmount = r.annualTaxAmount,
            lastServiceDate = keepIfSameDay(existing?.lastServiceDate, r.lastServiceDate),
            certificateType = r.certificateType,
            certificateNumber = r.certificateNumber,
            pbbNop = r.pbbNumber,
            landAreaM2 = r.landArea,
            buildingAreaM2 = r.buildingArea,
            numberOfFloors = r.numberOfFloors,
            pbgNumber = r.pbgNumber,
            latitude = r.latitude,
            longitude = r.longitude,
            locationAccuracyM = r.locationAccuracy,
            locationCapturedAt = parseInstant(r.locationCapturedAt),
            brand = r.brand,
            vehicleModel = r.model,
            yearManufacture = r.yearManufacture,
            address = r.address,
            brandModel = r.brand,
            serialNumber = r.serialNumber,
            department = r.department,
            createdAt = existing?.createdAt ?: now,
            updatedAt = r.version * 1000,
            serverId = r.id,
            clientUuid = existing?.clientUuid ?: r.clientUuid ?: UUID.randomUUID().toString(),
            syncState = SyncState.SYNCED,
            serverVersion = r.version
        )
    }
}
