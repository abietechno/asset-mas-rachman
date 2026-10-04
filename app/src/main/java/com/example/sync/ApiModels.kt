package com.example.sync

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Kontrak dengan dashboard (dash/app/Http/Controllers/Api). Waktu "version" = updated_at server dalam detik epoch.

@JsonClass(generateAdapter = true)
data class LoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class LoginResponse(val success: Boolean = false, val message: String? = null, val data: LoginData? = null)

@JsonClass(generateAdapter = true)
data class LoginData(val user: UserDto, val token: String)

@JsonClass(generateAdapter = true)
data class UserDto(val id: Long, val name: String, val email: String, val role: String? = null)

/** Aset sebagaimana dikirim server (SyncController::payload). */
@JsonClass(generateAdapter = true)
data class RemoteAsset(
    val id: Long,
    @Json(name = "client_uuid") val clientUuid: String? = null,
    val version: Long,
    val deleted: Boolean = false,
    val name: String = "",
    val type: String = "", // jenis bidang kategori: TANAH | BANGUNAN | KENDARAAN | INVENTARIS
    @Json(name = "category_id") val categoryId: Long? = null,
    val code: String? = null,
    @Json(name = "useful_life_years") val usefulLifeYears: Int? = null,
    @Json(name = "salvage_value") val salvageValue: Double? = null,
    val pic: String? = null,
    val department: String? = null,
    @Json(name = "vehicle_type") val vehicleType: String? = null,
    @Json(name = "bpkb_number") val bpkbNumber: String? = null,
    @Json(name = "stnk_number") val stnkNumber: String? = null,
    @Json(name = "five_year_plate_due_date") val fiveYearPlateDueDate: String? = null,
    @Json(name = "annual_tax_amount") val annualTaxAmount: Double? = null,
    @Json(name = "last_service_date") val lastServiceDate: String? = null,
    @Json(name = "number_of_floors") val numberOfFloors: Int? = null,
    @Json(name = "pbg_number") val pbgNumber: String? = null,
    @Json(name = "serial_number") val serialNumber: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "location_accuracy") val locationAccuracy: Double? = null,
    @Json(name = "location_captured_at") val locationCapturedAt: String? = null,
    val price: Double? = null,
    @Json(name = "purchase_date") val purchaseDate: String? = null,
    val condition: String? = null,
    val description: String? = null,
    val location: String? = null,
    val status: String? = null,
    @Json(name = "license_plate") val licensePlate: String? = null,
    val brand: String? = null,
    val model: String? = null,
    @Json(name = "year_manufacture") val yearManufacture: Int? = null,
    val vin: String? = null,
    @Json(name = "engine_number") val engineNumber: String? = null,
    @Json(name = "tax_due_date") val taxDueDate: String? = null,
    val details: Map<String, Any?>? = null,
    @Json(name = "certificate_type") val certificateType: String? = null,
    @Json(name = "certificate_number") val certificateNumber: String? = null,
    @Json(name = "land_area") val landArea: Double? = null,
    @Json(name = "building_area") val buildingArea: Double? = null,
    val address: String? = null,
    @Json(name = "pbb_number") val pbbNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class RemoteCategory(
    val id: Long,
    val name: String = "",
    val kind: String = "",
    @Json(name = "useful_life_years") val usefulLifeYears: Int? = null,
    @Json(name = "is_system") val isSystem: Boolean = false,
    val deleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PullResponse(
    @Json(name = "server_time") val serverTime: Long,
    val data: List<RemoteAsset> = emptyList(),
    /** Daftar kategori lengkap (kecil), bukan delta. */
    val categories: List<RemoteCategory> = emptyList()
)

/** Field yang dikirim ke server. Null ikut dikirim (lihat ApiClient) supaya field yang dikosongkan di HP ikut terhapus. */
@JsonClass(generateAdapter = true)
data class PushAsset(
    val name: String,
    val type: String,
    @Json(name = "category_id") val categoryId: Long?,
    val code: String?,
    val price: Double,
    @Json(name = "purchase_date") val purchaseDate: String?,
    @Json(name = "useful_life_years") val usefulLifeYears: Int?,
    @Json(name = "salvage_value") val salvageValue: Double?,
    val pic: String?,
    val department: String?,
    val condition: String?,
    val description: String?,
    val location: String?,
    val status: String?,
    @Json(name = "vehicle_type") val vehicleType: String?,
    @Json(name = "license_plate") val licensePlate: String?,
    val brand: String?,
    val model: String?,
    @Json(name = "year_manufacture") val yearManufacture: Int?,
    @Json(name = "engine_number") val engineNumber: String?,
    val vin: String?,
    @Json(name = "bpkb_number") val bpkbNumber: String?,
    @Json(name = "stnk_number") val stnkNumber: String?,
    @Json(name = "tax_due_date") val taxDueDate: String?,
    @Json(name = "five_year_plate_due_date") val fiveYearPlateDueDate: String?,
    @Json(name = "last_service_date") val lastServiceDate: String?,
    @Json(name = "certificate_type") val certificateType: String?,
    @Json(name = "certificate_number") val certificateNumber: String?,
    @Json(name = "land_area") val landArea: Double?,
    @Json(name = "building_area") val buildingArea: Double?,
    @Json(name = "number_of_floors") val numberOfFloors: Int?,
    @Json(name = "pbg_number") val pbgNumber: String?,
    @Json(name = "pbb_number") val pbbNumber: String?,
    @Json(name = "serial_number") val serialNumber: String?,
    val latitude: Double?,
    val longitude: Double?,
    @Json(name = "location_accuracy") val locationAccuracy: Double?,
    @Json(name = "location_captured_at") val locationCapturedAt: String?
)

@JsonClass(generateAdapter = true)
data class PushChange(
    @Json(name = "client_uuid") val clientUuid: String,
    val id: Long?,
    @Json(name = "base_version") val baseVersion: Long?,
    val deleted: Boolean,
    val data: PushAsset?
)

@JsonClass(generateAdapter = true)
data class PushRequest(val changes: List<PushChange>)

@JsonClass(generateAdapter = true)
data class PushResult(
    @Json(name = "client_uuid") val clientUuid: String? = null,
    val status: String,
    val message: String? = null,
    val asset: RemoteAsset? = null
)

@JsonClass(generateAdapter = true)
data class PushResponse(
    @Json(name = "server_time") val serverTime: Long,
    val results: List<PushResult> = emptyList()
)
