package com.example.data

import androidx.room.TypeConverter
import com.example.model.AssetCondition
import com.example.model.AssetStatus
import com.example.model.AssetType
import com.example.model.VehicleType

class Converters {
    @TypeConverter
    fun fromAssetType(value: AssetType?): String? = value?.name

    @TypeConverter
    fun toAssetType(value: String?): AssetType? = value?.let { AssetType.valueOf(it) }

    @TypeConverter
    fun fromVehicleType(value: VehicleType?): String? = value?.name

    @TypeConverter
    fun toVehicleType(value: String?): VehicleType? = value?.let { VehicleType.valueOf(it) }

    @TypeConverter
    fun fromAssetStatus(value: AssetStatus?): String? = value?.name

    @TypeConverter
    fun toAssetStatus(value: String?): AssetStatus? = value?.let { AssetStatus.valueOf(it) }

    @TypeConverter
    fun fromAssetCondition(value: AssetCondition?): String? = value?.name

    @TypeConverter
    fun toAssetCondition(value: String?): AssetCondition? = value?.let { AssetCondition.valueOf(it) }
}
