package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.BloatCategory
import com.example.data.model.BloatPackage
import com.example.data.model.DeviceManufacturer
import com.example.data.model.SafetyLevel

/**
 * Room Entity representing an installed or catalogued Android system application,
 * including its unique package name, enabled/disabled state, and risk level for debloating.
 */
@Entity(
    tableName = "system_applications",
    indices = [
        Index(value = ["safety_level"]),
        Index(value = ["is_enabled"]),
        Index(value = ["is_installed"]),
        Index(value = ["category"])
    ]
)
data class SystemAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean,

    @ColumnInfo(name = "is_installed")
    val isInstalled: Boolean,

    @ColumnInfo(name = "safety_level")
    val safetyLevel: SafetyLevel,

    @ColumnInfo(name = "category")
    val category: BloatCategory,

    @ColumnInfo(name = "target_manufacturer")
    val targetManufacturer: DeviceManufacturer,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "ram_saving_mb")
    val ramSavingMb: Int = 35,

    @ColumnInfo(name = "risk_note")
    val riskNote: String = "No critical side-effects observed.",

    @ColumnInfo(name = "version_name")
    val versionName: String? = null,

    @ColumnInfo(name = "is_marked_for_debloat")
    val isMarkedForDebloat: Boolean = false,

    @ColumnInfo(name = "last_scanned_at")
    val lastScannedAt: Long = System.currentTimeMillis()
)

fun SystemAppEntity.toDomainModel(): BloatPackage {
    return BloatPackage(
        packageName = packageName,
        displayName = displayName,
        category = category,
        safetyLevel = safetyLevel,
        targetManufacturer = targetManufacturer,
        description = description,
        ramSavingMb = ramSavingMb,
        riskNote = riskNote,
        isInstalled = isInstalled,
        isEnabled = isEnabled,
        versionName = versionName,
        isSelectedForDebloat = isMarkedForDebloat
    )
}

fun BloatPackage.toEntity(
    isMarkedForDebloat: Boolean = this.isSelectedForDebloat,
    lastScannedAt: Long = System.currentTimeMillis()
): SystemAppEntity {
    return SystemAppEntity(
        packageName = packageName,
        displayName = displayName,
        isEnabled = isEnabled,
        isInstalled = isInstalled,
        safetyLevel = safetyLevel,
        category = category,
        targetManufacturer = targetManufacturer,
        description = description,
        ramSavingMb = ramSavingMb,
        riskNote = riskNote,
        versionName = versionName,
        isMarkedForDebloat = isMarkedForDebloat,
        lastScannedAt = lastScannedAt
    )
}
