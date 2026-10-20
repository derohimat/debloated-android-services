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
import com.example.data.local.DebloatDatabase
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
import com.example.data.service.GeneratedCommandPayload
import com.example.data.service.ShellActionType
import com.example.data.service.ShellCommandGeneratorService
import com.example.data.service.ShellTargetFormat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val shellActionType: ShellActionType = ShellActionType.DISABLE_USER,
    val shellTargetFormat: ShellTargetFormat = ShellTargetFormat.PC_ADB,
    val commandPayload: GeneratedCommandPayload = GeneratedCommandPayload(),
    val activePreset: String? = null,
    val selectedPackageDetails: BloatPackage? = null,
    val snapshots: List<DebloatSnapshot> = emptyList(),
    val auditLogs: List<AuditLogEntry> = emptyList(),
    val isExecutingCommand: Boolean = false,
    val generatedScript: String = "",
    val performanceMetrics: PerformanceMetrics = PerformanceMetrics()
)

class DebloatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DebloatDatabase.getInstance(application)
    private val systemAppDao = database.systemAppDao()
    private val scannerRepo = DeviceScannerRepository(application, systemAppDao)
    private val backupRepo = BackupRepository(application)
    private val auditRepo = AuditLogRepository(application)
    val shellCommandService = ShellCommandGeneratorService(application, systemAppDao)

    private val actionTypeFlow = MutableStateFlow(ShellActionType.DISABLE_USER)
    private val targetFormatFlow = MutableStateFlow(ShellTargetFormat.PC_ADB)

    private val _uiState = MutableStateFlow(DebloatUiState())
    val uiState: StateFlow<DebloatUiState> = _uiState.asStateFlow()

    private val _snackBarEvent = MutableSharedFlow<String>()
    val snackBarEvent: SharedFlow<String> = _snackBarEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            auditRepo.initLogs()
            loadData()
        }

        // Observe Room Database system_applications table reactively
        viewModelScope.launch {
            scannerRepo.allSystemAppsFlow.collect { roomPackages ->
                if (roomPackages.isNotEmpty()) {
                    val selectedFromRoom = roomPackages
                        .filter { it.isSelectedForDebloat && it.safetyLevel != SafetyLevel.ESSENTIAL }
                        .map { it.packageName }
                        .toSet()

                    _uiState.update { current ->
                        val updatedDetails = current.selectedPackageDetails?.let { openPkg ->
                            roomPackages.firstOrNull { it.packageName == openPkg.packageName }
                        }
                        current.copy(
                            packages = roomPackages,
                            selectedPackageNames = selectedFromRoom,
                            selectedPackageDetails = updatedDetails
                        )
                    }
                    updatePerformanceMetrics()
                }
            }
        }

        // Observe reactive shell command generation from Room selections
        viewModelScope.launch {
            shellCommandService.observeCommandPayload(
                actionTypeFlow = actionTypeFlow,
                targetFormatFlow = targetFormatFlow
            ).collect { payload ->
                _uiState.update {
                    it.copy(
                        shellActionType = payload.actionType,
                        shellTargetFormat = payload.targetFormat,
                        commandPayload = payload,
                        generatedScript = payload.formattedOutput
                    )
                }
            }
        }

        // Observe audit logs
        viewModelScope.launch {
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

            val initialOemFilter = if (devInfo.detectedOem != DeviceManufacturer.ALL) {
                devInfo.detectedOem
            } else {
                DeviceManufacturer.ALL
            }

            val selectedSet = pkgs.filter { it.isSelectedForDebloat }.map { it.packageName }.toSet()

            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    hardwareInfo = devInfo,
                    packages = pkgs,
                    selectedPackageNames = selectedSet,
                    manufacturerFilter = initialOemFilter,
                    snapshots = snapshots
                )
            }
            updatePerformanceMetrics()
            auditRepo.log(
                tag = "ROOM_SYNC",
                message = "Device scan & Room DB sync complete. Stored ${pkgs.size} system packages (${pkgs.count { it.isInstalled }} installed).",
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

    fun setShellActionType(actionType: ShellActionType) {
        actionTypeFlow.value = actionType
    }

    fun setShellTargetFormat(targetFormat: ShellTargetFormat) {
        targetFormatFlow.value = targetFormat
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

        val willBeSelected = !current.contains(pkgName)
        if (willBeSelected) {
            current.add(pkgName)
        } else {
            current.remove(pkgName)
        }

        _uiState.update {
            it.copy(
                selectedPackageNames = current,
                activePreset = null
            )
        }
        updatePerformanceMetrics()

        viewModelScope.launch {
            scannerRepo.updateSelectionState(pkgName, willBeSelected)
        }
    }

    fun updatePackageRiskLevel(packageName: String, newRiskLevel: SafetyLevel) {
        viewModelScope.launch {
            scannerRepo.updateAppRiskLevel(packageName, newRiskLevel)
            if (newRiskLevel == SafetyLevel.ESSENTIAL) {
                scannerRepo.updateSelectionState(packageName, false)
            }
            auditRepo.log(
                tag = "RISK_UPDATED",
                message = "Updated risk level of $packageName to ${newRiskLevel.label} in Room database.",
                severity = LogSeverity.INFO
            )
            _snackBarEvent.emit("Updated $packageName risk level to ${newRiskLevel.label}")
        }
    }

    fun applyPreset(presetName: String) {
        val state = _uiState.value
        val presets = BloatwareCatalog.getRecommendedPresets()
        val targetList = presets[presetName] ?: return

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

        viewModelScope.launch {
            scannerRepo.setSelectedPackages(selectedSet)
            auditRepo.log(
                tag = "PRESET_APPLIED",
                message = "Applied '$presetName' preset: persisted ${selectedSet.size} selections in Room DB.",
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

        viewModelScope.launch {
            scannerRepo.setSelectedPackages(updated)
            auditRepo.log(
                tag = "BULK_SELECT",
                message = "Selected all visible candidates (${safeToSelect.size} packages) for debloating.",
                severity = LogSeverity.INFO
            )
            _snackBarEvent.emit("Selected ${safeToSelect.size} candidate application(s)")
        }
    }

    fun deselectAllVisible() {
        val visible = getFilteredPackages().map { it.packageName }.toSet()
        val current = _uiState.value.selectedPackageNames
        val countDeselected = current.count { visible.contains(it) }
        val updated = if (countDeselected > 0) {
            current.filter { !visible.contains(it) }.toSet()
        } else {
            emptySet()
        }
        _uiState.update { it.copy(selectedPackageNames = updated, activePreset = null) }
        updatePerformanceMetrics()

        viewModelScope.launch {
            scannerRepo.setSelectedPackages(updated)
            auditRepo.log(
                tag = "BULK_DESELECT",
                message = "Deselected $countDeselected candidate application(s).",
                severity = LogSeverity.INFO
            )
            _snackBarEvent.emit(if (countDeselected > 0) "Deselected $countDeselected application(s)" else "All applications deselected")
        }
    }

    fun toggleSelectAllVisible(selectAll: Boolean) {
        if (selectAll) {
            selectAllVisible()
        } else {
            deselectAllVisible()
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPackageNames = emptySet(), activePreset = null) }
        updatePerformanceMetrics()

        viewModelScope.launch {
            scannerRepo.clearAllSelections()
        }
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
        val privacyBoost = (selectedPkgs.count {
            it.category == BloatCategory.TELEMETRY || it.category == BloatCategory.SOCIAL_PRELOAD
        } * 12).coerceAtMost(98)

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

    fun copyScriptToClipboard() {
        val payload = _uiState.value.commandPayload
        shellCommandService.copyCommandToClipboard(
            commandText = payload.formattedOutput,
            label = "DebloatMaster ${payload.actionType.label}"
        )

        viewModelScope.launch {
            auditRepo.log(
                tag = "ADB_COPY",
                message = "Copied ${payload.actionType.label} commands (${payload.targetCount} apps from Room DB) to clipboard.",
                severity = LogSeverity.SUCCESS,
                commandExecuted = payload.packageCommands.firstOrNull()?.activeCommand
            )
            _snackBarEvent.emit("Copied ${payload.targetCount} ${payload.actionType.label} command(s) to clipboard!")
        }
    }

    fun copySingleLineCommand() {
        val payload = _uiState.value.commandPayload
        if (payload.singleLineCommand.isBlank()) return
        shellCommandService.copyCommandToClipboard(
            commandText = payload.singleLineCommand,
            label = "DebloatMaster Chained Command"
        )

        viewModelScope.launch {
            auditRepo.log(
                tag = "CHAIN_COPY",
                message = "Copied chained one-liner for ${payload.targetCount} apps to clipboard.",
                severity = LogSeverity.SUCCESS,
                commandExecuted = payload.singleLineCommand.take(120)
            )
            _snackBarEvent.emit("Chained one-line command copied!")
        }
    }

    fun copyIndividualCommand(packageName: String, commandText: String, actionLabel: String) {
        shellCommandService.copyCommandToClipboard(commandText, "Command for $packageName")
        viewModelScope.launch {
            auditRepo.log(
                tag = "CMD_COPY",
                message = "Copied $actionLabel command for $packageName.",
                severity = LogSeverity.INFO,
                commandExecuted = commandText
            )
            _snackBarEvent.emit("Copied $actionLabel command for $packageName")
        }
    }

    fun shareScript() {
        val context = getApplication<Application>()
        val payload = _uiState.value.commandPayload
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "DebloatMaster ${payload.actionType.label} (${payload.targetCount} packages)")
            putExtra(Intent.EXTRA_TEXT, payload.formattedOutput)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Share Debloat Script").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })

        viewModelScope.launch {
            auditRepo.log(
                tag = "SCRIPT_SHARED",
                message = "Exported ${payload.actionType.label} script via Android share sheet.",
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

    fun executeBatchShellCommands(
        actionType: ShellActionType = _uiState.value.shellActionType,
        asRoot: Boolean = _uiState.value.hardwareInfo?.isRootAvailable == true
    ) {
        val targetCount = _uiState.value.commandPayload.targetCount
        if (targetCount == 0) {
            viewModelScope.launch {
                _snackBarEvent.emit("No target packages in Room selection to execute.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isExecutingCommand = true) }

            auditRepo.log(
                tag = "EXEC_START",
                message = "Executing ${actionType.label} for $targetCount Room-selected package(s) (Root: $asRoot)...",
                severity = LogSeverity.INFO
            )

            val report = shellCommandService.executeSelectedCommands(
                actionType = actionType,
                asRoot = asRoot
            )

            _uiState.update { it.copy(isExecutingCommand = false) }

            val successCount = report.succeededPackages.size
            val failCount = report.failedPackages.size

            auditRepo.log(
                tag = "EXEC_FINISH",
                message = "${actionType.label} finished: $successCount succeeded, $failCount failed/denied.",
                severity = if (failCount == 0) LogSeverity.SUCCESS else LogSeverity.WARNING,
                commandExecuted = report.commandResults.firstOrNull()?.command
            )

            if (failCount > 0 && !asRoot) {
                _snackBarEvent.emit("Direct shell requires Root or ADB. Copied commands ready for PC ADB!")
            } else {
                _snackBarEvent.emit("${actionType.label}: $successCount succeeded, $failCount failed.")
            }
            loadData()
        }
    }

    fun executeSinglePackageShellCommand(packageName: String, actionType: ShellActionType) {
        viewModelScope.launch {
            val asRoot = _uiState.value.hardwareInfo?.isRootAvailable == true
            val result = shellCommandService.executeSinglePackageCommand(
                packageName = packageName,
                actionType = actionType,
                asRoot = asRoot
            )
            if (result.isSuccess) {
                auditRepo.log(
                    tag = "SINGLE_EXEC",
                    message = "Executed ${actionType.label} on $packageName and updated Room DB state.",
                    severity = LogSeverity.SUCCESS,
                    commandExecuted = result.command
                )
                _snackBarEvent.emit("${actionType.label} succeeded for $packageName")
            } else {
                // Copy fallback command to clipboard automatically for convenience
                val fallbackCmd = if (actionType == ShellActionType.RESTORE_APP) {
                    "adb shell pm enable $packageName || adb shell cmd package install-existing $packageName"
                } else {
                    "adb shell pm disable-user --user 0 $packageName"
                }
                shellCommandService.copyCommandToClipboard(fallbackCmd)
                auditRepo.log(
                    tag = "SINGLE_EXEC_FALLBACK",
                    message = "Direct execution blocked without Root; copied ADB command for $packageName to clipboard.",
                    severity = LogSeverity.WARNING,
                    commandExecuted = fallbackCmd
                )
                _snackBarEvent.emit("Root unavailable—copied ADB command for $packageName to clipboard!")
            }
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
            val matchesQuery = state.searchQuery.isBlank() ||
                pkg.displayName.contains(state.searchQuery, ignoreCase = true) ||
                pkg.packageName.contains(state.searchQuery, ignoreCase = true) ||
                pkg.description.contains(state.searchQuery, ignoreCase = true)

            val matchesOem = state.manufacturerFilter == DeviceManufacturer.ALL ||
                pkg.targetManufacturer == state.manufacturerFilter ||
                pkg.targetManufacturer == DeviceManufacturer.CARRIER_UNIVERSAL

            val matchesCategory = state.categoryFilter == null || pkg.category == state.categoryFilter

            val matchesSafety = state.safetyFilter == null || pkg.safetyLevel == state.safetyFilter

            val matchesInstalled = !state.onlyInstalled || pkg.isInstalled

            matchesQuery && matchesOem && matchesCategory && matchesSafety && matchesInstalled
        }
    }
}
