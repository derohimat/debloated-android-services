package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.BloatCategory
import com.example.data.model.DeviceManufacturer
import com.example.data.model.SafetyLevel

class RoomConverters {

    @TypeConverter
    fun fromSafetyLevel(level: SafetyLevel): String = level.name

    @TypeConverter
    fun toSafetyLevel(value: String): SafetyLevel =
        runCatching { SafetyLevel.valueOf(value) }.getOrDefault(SafetyLevel.CAUTION)

    @TypeConverter
    fun fromBloatCategory(category: BloatCategory): String = category.name

    @TypeConverter
    fun toBloatCategory(value: String): BloatCategory =
        runCatching { BloatCategory.valueOf(value) }.getOrDefault(BloatCategory.OEM_EXTRA)

    @TypeConverter
    fun fromDeviceManufacturer(manufacturer: DeviceManufacturer): String = manufacturer.name

    @TypeConverter
    fun toDeviceManufacturer(value: String): DeviceManufacturer =
        runCatching { DeviceManufacturer.valueOf(value) }.getOrDefault(DeviceManufacturer.ALL)
}
