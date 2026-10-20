package com.example.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.local.DebloatDatabase
import com.example.data.local.SystemAppDao
import com.example.data.local.toDomainModel
import com.example.data.local.toEntity
import com.example.data.model.BloatCategory
import com.example.data.model.BloatPackage
import com.example.data.model.DeviceManufacturer
import com.example.data.model.SafetyLevel
import com.example.data.preset.BloatwareCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class DeviceHardwareInfo(
    val manufacturer: String,
    val model: String,
    val deviceCodename: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val detectedOem: DeviceManufacturer,
    val isRootAvailable: Boolean
)

class DeviceScannerRepository(
    private val context: Context,
    private val systemAppDao: SystemAppDao = DebloatDatabase.getInstance(context).systemAppDao()
) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Reactive stream of all system applications stored in the Room database.
     */
    val allSystemAppsFlow: Flow<List<BloatPackage>> =
        systemAppDao.observeAllSystemApps().map { entities ->
            entities.map { it.toDomainModel() }
        }

    fun observeAppsByRiskLevel(riskLevel: SafetyLevel): Flow<List<BloatPackage>> =
        systemAppDao.observeAppsByRiskLevel(riskLevel).map { entities ->
            entities.map { it.toDomainModel() }
        }

    fun observeAppsByEnabledState(isEnabled: Boolean): Flow<List<BloatPackage>> =
        systemAppDao.observeAppsByEnabledState(isEnabled).map { entities ->
            entities.map { it.toDomainModel() }
        }

    fun getDeviceInfo(): DeviceHardwareInfo {
        val manufacturerStr = Build.MANUFACTURER.orEmpty()
        val detectedOem = when {
            manufacturerStr.contains("samsung", ignoreCase = true) -> DeviceManufacturer.SAMSUNG
            manufacturerStr.contains("xiaomi", ignoreCase = true) ||
                manufacturerStr.contains("redmi", ignoreCase = true) ||
                manufacturerStr.contains("poco", ignoreCase = true) -> DeviceManufacturer.XIAOMI
            manufacturerStr.contains("google", ignoreCase = true) -> DeviceManufacturer.GOOGLE
            manufacturerStr.contains("oneplus", ignoreCase = true) ||
                manufacturerStr.contains("oppo", ignoreCase = true) ||
                manufacturerStr.contains("realme", ignoreCase = true) -> DeviceManufacturer.ONEPLUS_OPPO
            manufacturerStr.contains("motorola", ignoreCase = true) -> DeviceManufacturer.MOTOROLA
            else -> DeviceManufacturer.ALL
        }

        val rootPresent = RootExecutor.isRootAvailable()

        return DeviceHardwareInfo(
            manufacturer = Build.MANUFACTURER.capitalizeWords(),
            model = Build.MODEL,
            deviceCodename = Build.DEVICE,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Build.VERSION.SECURITY_PATCH else "N/A",
            detectedOem = detectedOem,
            isRootAvailable = rootPresent
        )
    }

    /**
     * Scans installed packages from PackageManager, merges with curated catalog and existing
     * Room database selection states, and persists the full dataset into Room.
     */
    suspend fun scanPackages(): List<BloatPackage> = withContext(Dispatchers.IO) {
        val existingInDb = runCatching {
            systemAppDao.getAllAppsOneShot().associateBy { it.packageName }
        }.getOrDefault(emptyMap())

        val installedMap = mutableMapOf<String, Pair<Boolean, String?>>() // pkg -> (isEnabled, versionName)

        try {
            val installedPackages = packageManager.getInstalledPackages(0)
            for (pkg in installedPackages) {
                val isEnabled = isPackageEffectivelyEnabled(pkg.packageName, pkg.applicationInfo)
                installedMap[pkg.packageName] = Pair(isEnabled, pkg.versionName)
            }
        } catch (e: Exception) {
            // Fallback
        }

        val now = System.currentTimeMillis()
        val resultList = mutableListOf<BloatPackage>()
        val processedPkgNames = mutableSetOf<String>()

        // 1. Process curated list
        for (curated in BloatwareCatalog.curatedPackages) {
            val installedInfo = installedMap[curated.packageName]
            val isInstalled = installedInfo != null
            val isEnabled = installedInfo?.first ?: true
            val versionName = installedInfo?.second
            val existingEntity = existingInDb[curated.packageName]

            val label = if (isInstalled) {
                try {
                    val appInfo = packageManager.getApplicationInfo(curated.packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    curated.displayName
                }
            } else {
                curated.displayName
            }

            val safetyLevel = existingEntity?.safetyLevel ?: curated.safetyLevel
            val isSelected = existingEntity?.isMarkedForDebloat ?: false

            resultList.add(
                curated.copy(
                    displayName = if (label.isNotBlank()) label else curated.displayName,
                    safetyLevel = safetyLevel,
                    isInstalled = isInstalled,
                    isEnabled = isEnabled,
                    versionName = versionName,
                    isSelectedForDebloat = isSelected
                )
            )
            processedPkgNames.add(curated.packageName)
        }

        // 2. Discover uncatalogued system apps currently on device
        try {
            val allPackages = packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
            for (pkg in allPackages) {
                val appInfo = pkg.applicationInfo ?: continue
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (isSystem && !processedPkgNames.contains(pkg.packageName)) {
                    val pkgName = pkg.packageName
                    if (isCrucialAndroidCore(pkgName)) continue

                    val label = try {
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        pkgName
                    }

                    val existingEntity = existingInDb[pkgName]
                    val isEnabled = isPackageEffectivelyEnabled(pkgName, appInfo)
                    val cat = existingEntity?.category ?: guessCategory(pkgName)
                    val safety = existingEntity?.safetyLevel ?: guessSafety(pkgName)
                    val isSelected = existingEntity?.isMarkedForDebloat ?: false

                    resultList.add(
                        BloatPackage(
                            packageName = pkgName,
                            displayName = label.ifBlank { pkgName },
                            category = cat,
                            safetyLevel = safety,
                            targetManufacturer = DeviceManufacturer.ALL,
                            description = "Detected system package on current device ($pkgName).",
                            ramSavingMb = 30,
                            riskNote = if (safety == SafetyLevel.SAFE) "Safe candidate." else "Check functionality before disabling.",
                            isInstalled = true,
                            isEnabled = isEnabled,
                            versionName = pkg.versionName,
                            isSelectedForDebloat = isSelected
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore scan error
        }

        val sortedList = resultList.sortedWith(
            compareByDescending<BloatPackage> { it.isInstalled }
                .thenBy { it.safetyLevel.ordinal }
                .thenBy { it.displayName }
        )

        // Persist all discovered and catalogued system apps to Room
        runCatching {
            val entities = sortedList.map { pkg ->
                pkg.toEntity(
                    isMarkedForDebloat = pkg.isSelectedForDebloat,
                    lastScannedAt = now
                )
            }
            systemAppDao.upsertAll(entities)
        }

        sortedList
    }

    suspend fun updateSelectionState(packageName: String, isSelected: Boolean) {
        systemAppDao.updateSelectionState(packageName, isSelected)
    }

    suspend fun setSelectedPackages(packageNames: Set<String>) = withContext(Dispatchers.IO) {
        systemAppDao.clearAllSelections()
        if (packageNames.isNotEmpty()) {
            systemAppDao.markPackagesForDebloat(packageNames.toList())
        }
    }

    suspend fun clearAllSelections() {
        systemAppDao.clearAllSelections()
    }

    suspend fun updateAppEnabledState(packageName: String, isEnabled: Boolean) {
        systemAppDao.updateEnabledState(packageName, isEnabled)
    }

    suspend fun updateAppRiskLevel(packageName: String, riskLevel: SafetyLevel) {
        systemAppDao.updateRiskLevel(packageName, riskLevel)
    }

    private fun isPackageEffectivelyEnabled(packageName: String, appInfo: ApplicationInfo?): Boolean {
        try {
            val setting = packageManager.getApplicationEnabledSetting(packageName)
            if (setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
                setting == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
            ) {
                return false
            }
        } catch (e: Exception) {
            // Ignore
        }
        return appInfo?.enabled ?: true
    }

    private fun isCrucialAndroidCore(pkgName: String): Boolean {
        return pkgName == "android" ||
            pkgName == "com.android.keychain" ||
            pkgName == "com.android.permissioncontroller" ||
            pkgName == "com.android.providers.settings"
    }

    private fun guessCategory(pkgName: String): BloatCategory {
        val lower = pkgName.lowercase()
        return when {
            lower.contains("analytic") || lower.contains("telemetry") || lower.contains("metric") || lower.contains("tracker") || lower.contains("diagnostic") -> BloatCategory.TELEMETRY
            lower.contains("vzw") || lower.contains("att") || lower.contains("tmobile") || lower.contains("sprint") || lower.contains("carrier") -> BloatCategory.CARRIER
            lower.contains("game") || lower.contains("video") || lower.contains("music") || lower.contains("store") -> BloatCategory.MEDIA_GAMES
            lower.contains("sync") || lower.contains("cloud") || lower.contains("drive") || lower.contains("backup") -> BloatCategory.CLOUD_SYNC
            lower.contains("daemon") || lower.contains("service") || lower.contains("agent") || lower.contains("sensor") -> BloatCategory.BACKGROUND_DAEMON
            else -> BloatCategory.OEM_EXTRA
        }
    }

    private fun guessSafety(pkgName: String): SafetyLevel {
        val lower = pkgName.lowercase()
        return when {
            lower.contains("systemui") || lower.contains("settings") || lower.contains("packageinstaller") || lower.contains("phone") || lower.contains("dialer") -> SafetyLevel.ESSENTIAL
            lower.contains("telemetry") || lower.contains("analytics") || lower.contains("feedback") || lower.contains("ad") || lower.contains("msa") -> SafetyLevel.SAFE
            else -> SafetyLevel.CAUTION
        }
    }

    private fun String.capitalizeWords(): String = split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
