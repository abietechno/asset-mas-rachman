package com.example.sync

import com.example.model.AssetCondition
import com.example.model.AssetEntity
import com.example.model.AssetStatus
import com.example.model.AssetType
import com.example.model.SyncState
import com.example.model.VehicleType
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class AssetMapperTest {

    private val day = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun vehicle() = AssetEntity(
        id = 7,
        name = "Innova Zenix",
        code = "AST-KND-001",
        type = AssetType.KENDARAAN,
        categoryId = 3,
        acquisitionCost = 500_000_000.0,
        acquisitionDate = day.parse("2025-01-10")!!.time + 3_600_000, // ada jam-nya
        usefulLifeYears = 8,
        salvageValue = 50_000_000.0,
        location = "Jakarta",
        pic = "Budi",
        status = AssetStatus.STANDBY,
        condition = AssetCondition.SANGAT_BAIK,
        notes = "Mobil direksi",
        vehicleType = VehicleType.MOBIL,
        licensePlate = "B 1024 SPO",
        engineNumber = "ENG1",
        chassisNumber = "VIN1",
        bpkbNumber = "BPKB1",
        stnkNumber = "STNK1",
        annualTaxDueDate = day.parse("2026-03-01")!!.time,
        fiveYearPlateDueDate = day.parse("2030-03-01")!!.time,
        annualTaxAmount = 4_500_000.0,
        lastServiceDate = day.parse("2026-06-15")!!.time,
        clientUuid = "uuid-1",
        syncState = SyncState.DIRTY
    )

    /** Meniru respons server untuk data yang baru dikirim. */
    private fun serverEcho(e: AssetEntity, version: Long = 100, override: (RemoteAsset) -> RemoteAsset = { it }): RemoteAsset {
        val d = AssetMapper.toChange(e).data!!
        return override(
            RemoteAsset(
                id = 55, clientUuid = e.clientUuid, version = version, name = d.name, type = d.type,
                categoryId = d.categoryId, code = d.code, usefulLifeYears = d.usefulLifeYears,
                salvageValue = d.salvageValue, pic = d.pic, department = d.department,
                vehicleType = d.vehicleType, bpkbNumber = d.bpkbNumber, stnkNumber = d.stnkNumber,
                fiveYearPlateDueDate = d.fiveYearPlateDueDate, annualTaxAmount = d.annualTaxAmount,
                lastServiceDate = d.lastServiceDate, numberOfFloors = d.numberOfFloors, pbgNumber = d.pbgNumber,
                serialNumber = d.serialNumber, price = d.price, purchaseDate = d.purchaseDate,
                condition = d.condition, description = d.description, location = d.location, status = d.status,
                licensePlate = d.licensePlate, brand = d.brand, vin = d.vin, engineNumber = d.engineNumber,
                taxDueDate = d.taxDueDate, certificateType = d.certificateType,
                certificateNumber = d.certificateNumber, landArea = d.landArea, buildingArea = d.buildingArea,
                pbbNumber = d.pbbNumber,
                latitude = d.latitude, longitude = d.longitude, locationAccuracy = d.locationAccuracy,
                locationCapturedAt = d.locationCapturedAt
            )
        )
    }

    @Test
    fun roundTripKeepsEveryField() {
        val original = vehicle()
        val back = AssetMapper.toEntity(serverEcho(original), original)

        assertEquals(original.id, back.id)
        assertEquals(AssetType.KENDARAAN, back.type)
        assertEquals(3L, back.categoryId)
        assertEquals(AssetStatus.STANDBY, back.status)
        assertEquals(AssetCondition.SANGAT_BAIK, back.condition)
        assertEquals("AST-KND-001", back.code)
        assertEquals(VehicleType.MOBIL, back.vehicleType)
        assertEquals(8, back.usefulLifeYears)
        assertEquals(50_000_000.0, back.salvageValue, 0.0)
        assertEquals("Budi", back.pic)
        assertEquals("BPKB1", back.bpkbNumber)
        assertEquals("STNK1", back.stnkNumber)
        assertEquals(original.fiveYearPlateDueDate, back.fiveYearPlateDueDate)
        assertEquals(original.lastServiceDate, back.lastServiceDate)
        assertEquals(4_500_000.0, back.annualTaxAmount!!, 0.0)
        assertEquals(original.acquisitionDate, back.acquisitionDate) // jam tidak bergeser ke tengah malam
        assertEquals(55L, back.serverId)
        assertEquals(100L, back.serverVersion)
        assertEquals(SyncState.SYNCED, back.syncState)
        assertEquals("uuid-1", back.clientUuid)
    }

    @Test
    fun locationPinRoundTripsWithAccuracyAndCaptureTime() {
        val captured = 1_790_000_000_000L // detik bulat: format ISO tidak membawa milidetik
        val land = vehicle().copy(
            type = AssetType.TANAH, latitude = -6.2088001, longitude = 106.8456002,
            locationAccuracyM = 4.5, locationCapturedAt = captured - captured % 1000
        )
        val back = AssetMapper.toEntity(serverEcho(land), land)

        assertEquals(-6.2088001, back.latitude!!, 1e-9)
        assertEquals(106.8456002, back.longitude!!, 1e-9)
        assertEquals(4.5, back.locationAccuracyM!!, 0.0)
        assertEquals(land.locationCapturedAt, back.locationCapturedAt)
    }

    @Test
    fun serverTimestampWithOffsetIsParsed() {
        val remote = RemoteAsset(id = 1, version = 1, name = "X", type = "TANAH", latitude = 1.0, longitude = 2.0,
            locationCapturedAt = "2026-10-02T03:00:00+00:00")
        val e = AssetMapper.toEntity(remote, null)
        assertEquals(1_790_910_000_000L, e.locationCapturedAt)
    }

    @Test
    fun manualPinHasNoAccuracyAndNullsAreSentToClearIt() {
        val manual = vehicle().copy(type = AssetType.TANAH, latitude = -6.2, longitude = 106.8, locationAccuracyM = null)
        val json = Moshi.Builder().build().adapter(PushChange::class.java).serializeNulls().toJson(AssetMapper.toChange(manual))
        assertTrue(json, json.contains("\"location_accuracy\":null"))
        assertTrue(json, json.contains("\"latitude\":-6.2"))

        val cleared = AssetMapper.toChange(manual.copy(latitude = null, longitude = null))
        assertNull(cleared.data!!.latitude)
    }

    @Test
    fun everyStatusAndConditionSurvivesTheServerValues() {
        for (s in AssetStatus.values()) {
            val e = vehicle().copy(status = s)
            assertEquals(s, AssetMapper.toEntity(serverEcho(e), e).status)
        }
        for (c in AssetCondition.values()) {
            val e = vehicle().copy(condition = c)
            assertEquals(c, AssetMapper.toEntity(serverEcho(e), e).condition)
        }
    }

    @Test
    fun webEditIsAppliedOnPull() {
        val original = vehicle()
        val edited = serverEcho(original) { it.copy(status = "Maintenance", condition = "Damaged", categoryId = 9) }
        val back = AssetMapper.toEntity(edited, original)

        assertEquals(AssetStatus.DALAM_PERBAIKAN, back.status)
        assertEquals(AssetCondition.RUSAK, back.condition)
        assertEquals(9L, back.categoryId)
    }

    @Test
    fun assetCreatedOnWebGetsSensibleDefaults() {
        val web = RemoteAsset(
            id = 9, version = 5, name = "Tanah Bekasi", type = "TANAH", price = 1e9,
            landArea = 500.0, purchaseDate = "2024-06-01", status = "Active", condition = "Good"
        )
        val e = AssetMapper.toEntity(web, null)

        assertEquals(AssetType.TANAH, e.type)
        assertEquals("WEB-9", e.code)
        assertEquals(0, e.usefulLifeYears) // tanah tidak menyusut
        assertEquals(AssetStatus.DIGUNAKAN, e.status)
        assertTrue(e.clientUuid.isNotBlank())
        assertEquals(SyncState.SYNCED, e.syncState)
    }

    @Test
    fun unknownKindFromServerFallsBackToInventory() {
        val e = AssetMapper.toEntity(RemoteAsset(id = 1, version = 1, name = "X", type = "PRECIOUS_METAL"), null)
        assertEquals(AssetType.INVENTARIS, e.type)
    }

    @Test
    fun deletedEntityBecomesTombstoneWithoutData() {
        val change = AssetMapper.toChange(vehicle().copy(serverId = 55, syncState = SyncState.DELETED))
        assertTrue(change.deleted)
        assertNull(change.data)
        assertEquals(55L, change.id)
    }

    @Test
    fun clearedFieldsAreSentAsExplicitNullSoServerClearsThem() {
        val change = AssetMapper.toChange(vehicle().copy(licensePlate = null))
        val json = Moshi.Builder().build().adapter(PushChange::class.java).serializeNulls().toJson(change)

        assertTrue(json, json.contains("\"license_plate\":null"))
        assertTrue(json, json.contains("\"client_uuid\":\"uuid-1\""))
        assertTrue(json, json.contains("\"category_id\":3"))
        assertFalse(json, json.contains("\"base_version\":0"))
    }

    @Test
    fun serverResponseWithCategoriesAndUnknownFieldsParses() {
        val json = """{"server_time":1,"data":[{"id":1,"version":2,"name":"X","type":"KENDARAAN","details":null,"extra":"ignored"}],
            "categories":[{"id":3,"name":"Kendaraan","kind":"KENDARAAN","useful_life_years":8,"is_system":true,"deleted":false}]}"""
        val parsed = Moshi.Builder().build().adapter(PullResponse::class.java).fromJson(json)!!

        assertEquals("X", parsed.data.single().name)
        assertNull(parsed.data.single().details)
        assertEquals("Kendaraan", parsed.categories.single().name)
        assertTrue(parsed.categories.single().isSystem)
    }
}
