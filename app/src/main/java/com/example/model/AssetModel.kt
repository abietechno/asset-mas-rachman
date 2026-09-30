package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AssetType(val displayName: String) {
    TANAH("Tanah"),
    BANGUNAN("Rumah & Bangunan"),
    KENDARAAN("Kendaraan"),
    INVENTARIS("Inventaris Kantor")
}

enum class VehicleType(val displayName: String) {
    MOBIL("Mobil"),
    MOTOR("Motor"),
    TRUK("Truk / Pickup"),
    LAINNYA("Lainnya")
}

enum class AssetStatus(val displayName: String) {
    DIGUNAKAN("Digunakan"),
    STANDBY("Siap Pakai / Standby"),
    DISEWAKAN("Disewakan"),
    DALAM_PERBAIKAN("Perbaikan / Bengkel"),
    DIHAPUSBUKUKAN("Dihapusbukukan")
}

enum class AssetCondition(val displayName: String) {
    SANGAT_BAIK("Sangat Baik"),
    BAIK("Baik"),
    PERLU_PERBAIKAN("Perlu Perbaikan"),
    RUSAK("Rusak")
}

enum class TaxStatus(val label: String) {
    EXPIRED("Kadaluarsa"),
    CRITICAL("Kritis (< 14 Hari)"),
    WARNING("Mendekati (< 30 Hari)"),
    SAFE("Masa Berlaku Aman")
}

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String, // e.g., AST-KND-001, AST-TNH-002
    val type: AssetType,
    val acquisitionCost: Double, // Nilai Perolehan (IDR)
    val acquisitionDate: Long, // Epoch ms
    val usefulLifeYears: Int, // Masa manfaat (tahun). 0 untuk tanah (tidak susut)
    val salvageValue: Double = 0.0, // Nilai residu
    val location: String, // Lokasi fisik / alamat / cabang
    val pic: String, // Penanggung Jawab / User / Departemen
    val status: AssetStatus = AssetStatus.DIGUNAKAN,
    val condition: AssetCondition = AssetCondition.BAIK,
    val notes: String = "",

    // --- Vehicle specific fields ---
    val vehicleType: VehicleType? = null,
    val licensePlate: String? = null, // Nomor Plat Polisi (e.g. B 1024 SPO)
    val engineNumber: String? = null, // Nomor Mesin
    val chassisNumber: String? = null, // Nomor Rangka (VIN)
    val bpkbNumber: String? = null, // Nomor BPKB
    val stnkNumber: String? = null, // Nomor STNK
    val annualTaxDueDate: Long? = null, // Tanggal Habis Pajak Tahunan
    val fiveYearPlateDueDate: Long? = null, // Tanggal Habis Pajak 5 Tahunan (Plat Kaleng)
    val annualTaxAmount: Double? = null, // Estimasi nominal PKB / Pajak
    val lastServiceDate: Long? = null,

    // --- Property / Real Estate specific fields (Tanah / Rumah / Bangunan) ---
    val certificateType: String? = null, // SHM, HGB, HGU, Girik, dll
    val certificateNumber: String? = null, // No. Sertifikat
    val pbbNop: String? = null, // NOP PBB
    val landAreaM2: Double? = null, // Luas Tanah (m²)
    val buildingAreaM2: Double? = null, // Luas Bangunan (m²)
    val numberOfFloors: Int? = null, // Jumlah Lantai
    val pbgNumber: String? = null, // Nomor IMB / PBG

    // --- Office Inventory specific fields ---
    val brandModel: String? = null, // e.g. "Apple MacBook Pro M3 Max", "Dell Server R750"
    val serialNumber: String? = null, // Serial Number hardware / QR tag
    val department: String? = null, // e.g. "IT Operations", "Finance"

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
