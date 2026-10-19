package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLogEntry
import com.example.data.model.BloatCategory
import com.example.data.model.BloatPackage
import com.example.data.model.DebloatSnapshot
import com.example.data.model.DeviceManufacturer
import com.example.data.model.LogSeverity
import com.example.data.model.PerformanceMetrics
import com.example.data.model.SafetyLevel
import com.example.data.model.SnapshotPackageRecord
import com.example.data.preset.BloatwareCatalog
import com.example.data.repository.AuditLogRepository
import com.example.data.repository.BackupRepository
import com.example.data.repository.DeviceHardwareInfo
import com.example.data.repository.DeviceScannerRepository
import com.example.data.repository.RootExecutor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DebloatMethod(val label: String, val commandPrefix: String, val description: String) {
    DISABLE_USER(
        label = "Safe Disable (Recommended)",
        commandPrefix = "pm disable-user --user 0",
        description = "Freezes package for User 0. 100% reversible anytime without loss of data."
    ),
    UNINSTALL_USER(
        label = "User Uninstall",
        commandPrefix = "pm uninstall -k --user 0",
        description = "Removes app from app drawer while retaining APK in /system for easy re-install."
    )
}

data class DebloatUiState(
    val isLoading: Boolean = true,
    val currentTab: Int = 0,
    val hardwareInfo: DeviceHardwareInfo? = null,
    val packages: List<BloatPackage> = emptyList(),
    val selectedPackageNames: Set<String> = emptySet(),
    val searchQuery: String = "",
    val manufacturerFilter: DeviceManufacturer = DeviceManufacturer.ALL,
    val categoryFilter: BloatCategory? = null,
    val safetyFilter: SafetyLevel? = null,
    val onlyInstalled: Boolean = true,
    val debloatMethod: DebloatMethod = DebloatMethod.DISABLE_USER,
    val activePreset: String? = null,
    val selectedPackageDetails: BloatPackage? = null,
    val snapshots: List<DebloatSnapshot> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val isExecutingCommand: Boolean = false,
    val generatedScript: String = "",
    val performanceMetrics: PerformanceMetrics = PerformanceMetrics()
)

class DebloatViewModel(application: Application) : AndroidViewModel(application) {

    private val scannerRepo = DeviceScannerRepository(application)
    private val backupRepo = BackupRepository(application)
    private val auditRepo = AuditLogRepository(application)

    private val _uiState = MutableStateFlow(DebloatUiState())
    val uiState: StateFlow<DebloatUiState> = _uiState.asStateFlow()

    private val _snackBarEvent = MutableSharedFlow<String>()
    val snackBarEvent: SharedFlow<String> = _snackBarEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            auditRepo.initLogs()
            loadData()
            auditRepo.logsFlow.collect { logs ->
                _uiState.update { it.copy(auditLogs = logs) }
            }
        }
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(currentTab = index) }
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val devInfo = scannerRepo.getDeviceInfo()
            val pkgs = scannerRepo.scanPackages()
            val snapshots = backupRepo.getSnapshots()

            // Filter initial preset according to OEM
            val initialOemFilter = if (devInfo.detectedOem != DeviceManufacturer.ALL) {
                devInfo.detectedOem
            } else {
                DeviceManufacturer.ALL
            }

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    hardwareInfo = devInfo,
                    packages = pkgs,
                    manufacturerFilter = initialOemFilter,
                    snapshots = snapshots
                )
            }
            updatePerformanceMetrics()
            updateGeneratedScript()
            auditRepo.log(
                tag = "SCAN_COMPLETE",
                message = "Device scan completed. Found ${pkgs.size} candidate packages (${pkgs.count { it.isInstalled }} installed on device).",
                severity = LogSeverity.INFO
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setManufacturerFilter(manufacturer: DeviceManufacturer) {
        _uiState.update { it.copy(manufacturerFilter = manufacturer) }
    }

    fun setCategoryFilter(category: BloatCategory?) {
        _uiState.update { it.copy(categoryFilter = category) }
    }

    fun setSafetyFilter(safety: SafetyLevel?) {
        _uiState.update { it.copy(safetyFilter = safety) }
    }

    fun toggleOnlyInstalled(onlyInstalled: Boolean) {
        _uiState.update { it.copy(onlyInstalled = onlyInstalled) }
    }

    fun setDebloatMethod(method: DebloatMethod) {
        _uiState.update { it.copy(debloatMethod = method) }
        updateGeneratedScript()
    }

    fun togglePackageSelection(pkgName: String) {
        val current = _uiState.value.selectedPackageNames.toMutableSet()
        val pkg = _uiState.value.packages.firstOrNull { it.packageName == pkgName }

        if (pkg?.safetyLevel == SafetyLevel.ESSENTIAL) {
            viewModelScope.launch {
                _snackBarEvent.emit("Cannot select critical system component ($pkgName).")
            }
            return
        }

        if (current.contains(pkgName)) {
            current.remove(pkgName)
        } else {
            current.add(pkgName)
        }

        _uiState.update {
            it.copy(
                selectedPackageNames = current,
                activePreset = null
            )
        }
        updatePerformanceMetrics()
        updateGeneratedScript()
    }

    fun applyPreset(presetName: String) {
        val state = _uiState.value
        val presets = BloatwareCatalog.getRecommendedPresets()
        val targetList = presets[presetName] ?: return

        // Filter packages that are installed and non-essential
        val selectedSet = state.packages
            .filter { pkg ->
                targetList.contains(pkg.packageName) &&
                        pkg.safetyLevel != SafetyLevel.ESSENTIAL &&
                        (!state.onlyInstalled || pkg.isInstalled) &&
                        pkg.isEnabled
            }
            .map { it.packageName }
            .toSet()

        _uiState.update {
            it.copy(
                selectedPackageNames = selectedSet,
                activePreset = presetName
            )
        }
        updatePerformanceMetrics()
        updateGeneratedScript()

        viewModelScope.launch {
            auditRepo.log(
                tag = "PRESET_APPLIED",
                message = "Applied '$presetName' preset: selected ${selectedSet.size} packages.",
                severity = LogSeverity.INFO
            )
            _snackBarEvent.emit("Selected ${selectedSet.size} packages via '$presetName'")
        }
    }

    fun selectAllVisible() {
        val visible = getFilteredPackages()
        val safeToSelect = visible
            .filter { it.safetyLevel != SafetyLevel.ESSENTIAL && it.isEnabled }
            .map { it.packageName }
        val updated = _uiState.value.selectedPackageNames.toMutableSet().apply {
            addAll(safeToSelect)
        }
        _uiState.update { it.copy(selectedPackageNames = updated, activePreset = null) }
        updatePerformanceMetrics()
        updateGeneratedScript()
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPackageNames = emptySet(), activePreset = null) }
        updatePerformanceMetrics()
        updateGeneratedScript()
    }

    fun showPackageDetails(pkg: BloatPackage?) {
        _uiState.update { it.copy(selectedPackageDetails = pkg) }
    }

    private fun updatePerformanceMetrics() {
        val state = _uiState.value
        val selectedPkgs = state.packages.filter { state.selectedPackageNames.contains(it.packageName) }
        val count = selectedPkgs.size
        val ramFreed = selectedPkgs.sumOf { it.ramSavingMb }
        val wakelocks = (count * 1.8).toInt()
        val privacyBoost = (selectedPkgs.count { it.category == BloatCategory.TELEMETRY || it.category == BloatCategory.SOCIAL_PRELOAD } * 12).coerceAtMost(98)

        _uiState.update {
            it.copy(
                performanceMetrics = PerformanceMetrics(
                    selectedCount = count,
                    estimatedRamFreedMb = ramFreed,
                    wakelocksReduced = wakelocks,
                    privacyScoreBoostPercent = privacyBoost
                )
            )
        }
    }

    private fun updateGeneratedScript() {
        val state = _uiState.value
        val selected = state.packages.filter { state.selectedPackageNames.contains(it.packageName) }
        val method = state.debloatMethod

        val sb = StringBuilder()
        sb.append("#!/bin/bash\n")
        sb.append("# =========================================================\n")
        sb.append("# DebloatMaster Batch Debloat Script\n")
        sb.append("# Mode: ${method.label}\n")
        sb.append("# Target Packages: ${selected.size}\n")
        sb.append("# =========================================================\n\n")

        if (selected.isEmpty()) {
            sb.append("# No packages currently selected in checklist.\n")
            sb.append("# Select packages or apply a preset to generate script.\n")
        } else {
            sb.append("echo \"[*] Starting DebloatMaster operation on ${selected.size} packages...\"\n\n")
            for (pkg in selected) {
                sb.append("# [${pkg.displayName}] (${pkg.category.displayName})\n")
                sb.append("adb shell ${method.commandPrefix} ${pkg.packageName}\n\n")
            }
            sb.append("echo \"[+] Debloat script execution completed.\"\n")
            sb.append("echo \"[+] Run 'adb reboot' or restart your device for changes to take full effect.\"\n")
        }

        _uiState.update { it.copy(generatedScript = sb.toString()) }
    }

    fun copyScriptToClipboard() {
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Debloat Script", _uiState.value.generatedScript)
        clipboard.setPrimaryClip(clip)

        viewModelScope.launch {
            auditRepo.log(
                tag = "ADB_COPY",
                message = "Copied ADB script for ${_uiState.value.selectedPackageNames.size} packages to clipboard.",
                severity = LogSeverity.SUCCESS
            )
            _snackBarEvent.emit("ADB commands copied to clipboard!")
        }
    }

    fun shareScript() {
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DebloatMaster Script (${_uiState.value.selectedPackageNames.size} packages)")
            putExtra(Intent.EXTRA_TEXT, _uiState.value.generatedScript)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Share Debloat Script").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })

        viewModelScope.launch {
            auditRepo.log(
                tag = "SCRIPT_SHARED",
                message = "Exported debloat script via Android share sheet.",
                severity = LogSeverity.INFO
            )
        }
    }

    fun openAppSettings(packageName: String) {
        val context = getApplication<Application>()
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)

            viewModelScope.launch {
                auditRepo.log(
                    tag = "SETTINGS_OPEN",
                    message = "Opened system app details for $packageName.",
                    severity = LogSeverity.INFO
                )
            }
        } catch (e: Exception) {
            viewModelScope.launch {
                _snackBarEvent.emit("Could not open system settings for $packageName")
            }
        }
    }

    fun executeRootDebloat() {
        val selected = _uiState.value.selectedPackageNames.toList()
        if (selected.isEmpty()) {
            viewModelScope.launch {
                _snackBarEvent.emit("No packages selected to debloat.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExecutingCommand = true) }
            val method = _uiState.value.debloatMethod
            var successCount = 0
            var failCount = 0

            auditRepo.log(
                tag = "ROOT_START",
                message = "Executing on-device root debloat for ${selected.size} packages...",
                severity = LogSeverity.INFO
            )

            for (pkgName in selected) {
                val cmd = "${method.commandPrefix} $pkgName"
                val res = RootExecutor.executeCommand(cmd, asRoot = true)
                if (res.isSuccess) {
                    successCount++
                } else {
                    failCount++
                }
            }

            _uiState.update { it.copy(isExecutingCommand = false) }

            auditRepo.log(
                tag = "ROOT_FINISH",
                message = "Root execution finished: $successCount succeeded, $failCount failed.",
                severity = if (failCount == 0) LogSeverity.SUCCESS else LogSeverity.WARNING
            )

            _snackBarEvent.emit("Root Execution complete: $successCount disabled, $failCount failed/denied.")
            // Refresh package status
            loadData()
        }
    }

    fun createBackupSnapshot(title: String, notes: String = "") {
        viewModelScope.launch {
            val state = _uiState.value
            val dev = state.hardwareInfo
            val records = state.packages.map {
                SnapshotPackageRecord(
                    packageName = it.packageName,
                    isEnabled = it.isEnabled,
                    isInstalled = it.isInstalled
                )
            }

            val snapshot = backupRepo.createSnapshot(
                name = title,
                deviceModel = "${dev?.manufacturer ?: "Android"} ${dev?.model ?: "Device"}",
                androidVersion = dev?.androidVersion ?: "14",
                records = records,
                notes = notes
            )

            val updatedList = backupRepo.getSnapshots()
            _uiState.update { it.copy(snapshots = updatedList) }

            auditRepo.log(
                tag = "BACKUP_CREATE",
                message = "Created system snapshot '${snapshot.name}' capturing ${records.size} package states.",
                severity = LogSeverity.SUCCESS
            )
            _snackBarEvent.emit("Backup snapshot '${snapshot.name}' saved!")
        }
    }

    fun deleteBackupSnapshot(id: String) {
        viewModelScope.launch {
            val removed = backupRepo.deleteSnapshot(id)
            if (removed) {
                val updatedList = backupRepo.getSnapshots()
                _uiState.update { it.copy(snapshots = updatedList) }
                auditRepo.log(
                    tag = "BACKUP_DELETE",
                    message = "Deleted backup snapshot ($id).",
                    severity = LogSeverity.INFO
                )
                _snackBarEvent.emit("Snapshot deleted.")
            }
        }
    }

    fun restoreSnapshot(snapshot: DebloatSnapshot) {
        val script = backupRepo.generateRestoreScript(snapshot)
        val context = getApplication<Application>()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Debloat Restore Script", script)
        clipboard.setPrimaryClip(clip)

        viewModelScope.launch {
            auditRepo.log(
                tag = "RESTORE_SCRIPT",
                message = "Generated and copied rollback script for snapshot '${snapshot.name}'.",
                severity = LogSeverity.SUCCESS
            )
            _snackBarEvent.emit("Rollback script for '${snapshot.name}' copied! Paste in ADB or terminal.")
        }
    }

    fun exportSnapshotJson(snapshot: DebloatSnapshot) {
        viewModelScope.launch {
            val json = backupRepo.exportSnapshotJson(snapshot)
            val context = getApplication<Application>()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "DebloatMaster Backup - ${snapshot.name}")
                putExtra(Intent.EXTRA_TEXT, json)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(intent, "Export Snapshot JSON").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
            auditRepo.log(
                tag = "BACKUP_EXPORT",
                message = "Exported snapshot '${snapshot.name}' as JSON.",
                severity = LogSeverity.INFO
            )
        }
    }

    fun importSnapshotFromJson(jsonStr: String) {
        viewModelScope.launch {
            val snapshot = backupRepo.importSnapshotJson(jsonStr)
            if (snapshot != null) {
                val updatedList = backupRepo.getSnapshots()
                _uiState.update { it.copy(snapshots = updatedList) }
                auditRepo.log(
                    tag = "BACKUP_IMPORT",
                    message = "Imported snapshot '${snapshot.name}' (${snapshot.packageRecords.size} packages).",
                    severity = LogSeverity.SUCCESS
                )
                _snackBarEvent.emit("Successfully imported '${snapshot.name}'!")
            } else {
                _snackBarEvent.emit("Failed to import snapshot. Invalid JSON format.")
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            auditRepo.clearLogs()
            _snackBarEvent.emit("Audit logs cleared.")
        }
    }

    fun exportLogs() {
        val text = auditRepo.exportLogsAsText()
        val context = getApplication<Application>()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DebloatMaster Audit Log History")
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Export Audit Logs").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    fun getFilteredPackages(): List<BloatPackage> {
        val state = _uiState.value
        return state.packages.filter { pkg ->
            // Search query
            val matchesQuery = state.searchQuery.isBlank() ||
                    pkg.displayName.contains(state.searchQuery, ignoreCase = true) ||
                    pkg.packageName.contains(state.searchQuery, ignoreCase = true) ||
                    pkg.description.contains(state.searchQuery, ignoreCase = true)

            // Manufacturer filter
            val matchesOem = state.manufacturerFilter == DeviceManufacturer.ALL ||
                    pkg.targetManufacturer == state.manufacturerFilter ||
                    pkg.targetManufacturer == DeviceManufacturer.CARRIER_UNIVERSAL

            // Category filter
            val matchesCategory = state.categoryFilter == null || pkg.category == state.categoryFilter

            // Safety filter
            val matchesSafety = state.safetyFilter == null || pkg.safetyLevel == state.safetyFilter

            // Installed filter
            val matchesInstalled = !state.onlyInstalled || pkg.isInstalled

            matchesQuery && matchesOem && matchesCategory && matchesSafety && matchesInstalled
        }
    }
}
