package com.example.data.model

enum class SafetyLevel(val label: String, val badgeColorHex: Long) {
    SAFE("Safe to Disable", 0xFF10B981),
    CAUTION("Caution / Optional", 0xFFF59E0B),
    ESSENTIAL("Critical System", 0xFFEF4444)
}

enum class BloatCategory(val displayName: String, val iconName: String) {
    TELEMETRY("Telemetry & Analytics", "analytics"),
    CARRIER("Carrier Bloatware", "sim_card"),
    OEM_EXTRA("Manufacturer Duplicate", "business"),
    SOCIAL_PRELOAD("Preloaded & Sponsored", "apps"),
    MEDIA_GAMES("Media, Stores & Games", "games"),
    BACKGROUND_DAEMON("Heavy Background Daemons", "memory"),
    CLOUD_SYNC("Cloud Sync & Services", "cloud"),
    SYSTEM_CORE("Core Android Framework", "shield")
}

enum class DeviceManufacturer(val label: String) {
    ALL("All Manufacturers"),
    SAMSUNG("Samsung (One UI)"),
    XIAOMI("Xiaomi (MIUI / HyperOS)"),
    GOOGLE("Google (Pixel / AOSP)"),
    ONEPLUS_OPPO("OnePlus / Oppo"),
    MOTOROLA("Motorola"),
    CARRIER_UNIVERSAL("Carrier & Partner Bloat")
}

data class BloatPackage(
    val packageName: String,
    val displayName: String,
    val category: BloatCategory,
    val safetyLevel: SafetyLevel,
    val targetManufacturer: DeviceManufacturer,
    val description: String,
    val ramSavingMb: Int = 35,
    val riskNote: String = "No critical side-effects observed.",
    val isInstalled: Boolean = false,
    val isEnabled: Boolean = true,
    val versionName: String? = null,
    val isSelectedForDebloat: Boolean = false
)

data class SnapshotPackageRecord(
    val packageName: String,
    val isEnabled: Boolean,
    val isInstalled: Boolean
)

data class DebloatSnapshot(
    val id: String,
    val createdAt: Long,
    val name: String,
    val deviceModel: String,
    val androidVersion: String,
    val totalCaptured: Int,
    val disabledCount: Int,
    val packageRecords: List<SnapshotPackageRecord>,
    val notes: String = ""
)

enum class LogSeverity {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

data class AuditLogEntry(
    val id: String,
    val timestamp: Long,
    val tag: String,
    val message: String,
    val severity: LogSeverity = LogSeverity.INFO,
    val commandExecuted: String? = null
)

data class PerformanceMetrics(
    val selectedCount: Int = 0,
    val estimatedRamFreedMb: Int = 0,
    val wakelocksReduced: Int = 0,
    val privacyScoreBoostPercent: Int = 0
)
