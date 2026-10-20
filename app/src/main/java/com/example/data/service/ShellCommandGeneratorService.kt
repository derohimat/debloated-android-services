package com.example.data.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.data.local.SystemAppDao
import com.example.data.local.SystemAppEntity
import com.example.data.model.SafetyLevel
import com.example.data.repository.CommandResult
import com.example.data.repository.RootExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * Defines the shell operation to generate or execute for selected system applications.
 */
enum class ShellActionType(
    val label: String,
    val description: String
) {
    DISABLE_USER(
        label = "Safe Disable",
        description = "Freezes package for User 0 (pm disable-user --user 0). 100% reversible without APK loss."
    ),
    UNINSTALL_USER(
        label = "User Uninstall",
        description = "Uninstalls for User 0 while keeping data/APK in /system (pm uninstall -k --user 0)."
    ),
    RESTORE_APP(
        label = "Restore & Re-Enable",
        description = "Re-enables frozen packages and re-links uninstalled system APKs (pm enable + install-existing)."
    )
}

/**
 * Controls how generated shell commands are formatted for copying or exporting.
 */
enum class ShellTargetFormat(
    val label: String,
    val hint: String
) {
    PC_ADB(
        label = "PC ADB Shell",
        hint = "Prefixed with 'adb shell' for Windows PowerShell, macOS, or Linux terminals."
    ),
    LOCAL_SHELL(
        label = "On-Device Shell",
        hint = "Raw 'pm' / 'cmd' commands for interactive adb shell, Termux, Shizuku, or Root."
    ),
    BASH_SCRIPT(
        label = "Full .sh Script",
        hint = "Complete executable shell script with progress output and comments."
    )
}

/**
 * Holds generated disable and restore commands for a single system application from Room.
 */
data class PackageShellCommand(
    val packageName: String,
    val displayName: String,
    val safetyLevel: SafetyLevel,
    val isCurrentlyEnabled: Boolean,
    val activeCommand: String,
    val disableCommand: String,
    val restoreCommand: String
)

/**
 * Complete command generation result derived from the Room database.
 */
data class GeneratedCommandPayload(
    val actionType: ShellActionType = ShellActionType.DISABLE_USER,
    val targetFormat: ShellTargetFormat = ShellTargetFormat.PC_ADB,
    val targetCount: Int = 0,
    val skippedEssentialCount: Int = 0,
    val packageCommands: List<PackageShellCommand> = emptyList(),
    val formattedOutput: String = "",
    val singleLineCommand: String = ""
)

/**
 * Summary report after executing generated commands on-device.
 */
data class BatchExecutionReport(
    val actionType: ShellActionType,
    val totalAttempted: Int,
    val succeededPackages: List<String>,
    val failedPackages: List<String>,
    val commandResults: List<CommandResult>
)

/**
 * Service and utility that queries selected system applications from the Room database
 * and generates or executes ADB / shell commands to disable or restore them.
 */
class ShellCommandGeneratorService(
    private val context: Context?,
    private val systemAppDao: SystemAppDao
) {

    /**
     * Reactively observes selected (and disabled) apps in the Room database and emits
     * an up-to-date [GeneratedCommandPayload] whenever Room state or formatting options change.
     */
    fun observeCommandPayload(
        actionTypeFlow: Flow<ShellActionType>,
        targetFormatFlow: Flow<ShellTargetFormat>
    ): Flow<GeneratedCommandPayload> {
        return combine(
            systemAppDao.observeSelectedForDebloat(),
            systemAppDao.observeAppsByEnabledState(isEnabled = false),
            actionTypeFlow,
            targetFormatFlow
        ) { selectedEntities, disabledEntities, actionType, targetFormat ->
            val candidateEntities = if (actionType == ShellActionType.RESTORE_APP && selectedEntities.isEmpty()) {
                disabledEntities
            } else {
                selectedEntities
            }
            buildPayload(
                entities = candidateEntities,
                actionType = actionType,
                targetFormat = targetFormat
            )
        }
    }

    /**
     * Generates a [GeneratedCommandPayload] on-demand from the currently selected apps in Room.
     */
    suspend fun generateFromRoomSelection(
        actionType: ShellActionType = ShellActionType.DISABLE_USER,
        targetFormat: ShellTargetFormat = ShellTargetFormat.PC_ADB
    ): GeneratedCommandPayload = withContext(Dispatchers.IO) {
        val allApps = systemAppDao.getAllAppsOneShot()
        val selected = allApps.filter { it.isMarkedForDebloat }
        val candidates = if (actionType == ShellActionType.RESTORE_APP && selected.isEmpty()) {
            allApps.filter { !it.isEnabled }
        } else {
            selected
        }
        buildPayload(
            entities = candidates,
            actionType = actionType,
            targetFormat = targetFormat
        )
    }

    /**
     * Pure builder that transforms a list of [SystemAppEntity] rows into shell commands,
     * automatically safeguarding [SafetyLevel.ESSENTIAL] packages from disable/uninstall actions.
     */
    fun buildPayload(
        entities: List<SystemAppEntity>,
        actionType: ShellActionType,
        targetFormat: ShellTargetFormat
    ): GeneratedCommandPayload {
        val skippedEssentials = if (actionType != ShellActionType.RESTORE_APP) {
            entities.count { it.safetyLevel == SafetyLevel.ESSENTIAL }
        } else {
            0
        }

        val safeEntities = if (actionType != ShellActionType.RESTORE_APP) {
            entities.filter { it.safetyLevel != SafetyLevel.ESSENTIAL }
        } else {
            entities
        }

        val useAdbPrefix = targetFormat != ShellTargetFormat.LOCAL_SHELL

        val packageCommands = safeEntities.map { entity ->
            val pkg = entity.packageName
            val disableCmd = formatSingleCommand(
                rawShellCmd = "pm disable-user --user 0 $pkg",
                useAdbPrefix = useAdbPrefix
            )
            val uninstallCmd = formatSingleCommand(
                rawShellCmd = "pm uninstall -k --user 0 $pkg",
                useAdbPrefix = useAdbPrefix
            )
            val restoreCmd = if (useAdbPrefix) {
                "adb shell pm enable $pkg || adb shell cmd package install-existing $pkg"
            } else {
                "pm enable $pkg || cmd package install-existing $pkg"
            }

            val activeCmd = when (actionType) {
                ShellActionType.DISABLE_USER -> disableCmd
                ShellActionType.UNINSTALL_USER -> uninstallCmd
                ShellActionType.RESTORE_APP -> restoreCmd
            }

            PackageShellCommand(
                packageName = pkg,
                displayName = entity.displayName,
                safetyLevel = entity.safetyLevel,
                isCurrentlyEnabled = entity.isEnabled,
                activeCommand = activeCmd,
                disableCommand = disableCmd,
                restoreCommand = restoreCmd
            )
        }

        val singleLine = packageCommands.joinToString(separator = " && ") { it.activeCommand }

        val formattedOutput = when (targetFormat) {
            ShellTargetFormat.PC_ADB -> buildConsoleOutput(packageCommands, actionType, skippedEssentials, isAdb = true)
            ShellTargetFormat.LOCAL_SHELL -> buildConsoleOutput(packageCommands, actionType, skippedEssentials, isAdb = false)
            ShellTargetFormat.BASH_SCRIPT -> buildFullBashScript(packageCommands, actionType, skippedEssentials)
        }

        return GeneratedCommandPayload(
            actionType = actionType,
            targetFormat = targetFormat,
            targetCount = packageCommands.size,
            skippedEssentialCount = skippedEssentials,
            packageCommands = packageCommands,
            formattedOutput = formattedOutput,
            singleLineCommand = singleLine
        )
    }

    /**
     * Copies any generated command or script string directly to the Android system clipboard.
     */
    fun copyCommandToClipboard(commandText: String, label: String = "DebloatMaster ADB Command"): Boolean {
        val ctx = context ?: return false
        return try {
            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, commandText)
            clipboard.setPrimaryClip(clip)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Executes the generated commands for the currently selected Room apps (or specific package)
     * using on-device shell / root and updates the Room database `is_enabled` state accordingly.
     */
    suspend fun executeSelectedCommands(
        actionType: ShellActionType,
        asRoot: Boolean = true,
        customExecutor: (suspend (String, Boolean) -> CommandResult)? = null
    ): BatchExecutionReport = withContext(Dispatchers.IO) {
        val payload = generateFromRoomSelection(
            actionType = actionType,
            targetFormat = ShellTargetFormat.LOCAL_SHELL
        )

        val succeeded = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val results = mutableListOf<CommandResult>()

        for (item in payload.packageCommands) {
            val rawCmd = when (actionType) {
                ShellActionType.DISABLE_USER -> "pm disable-user --user 0 ${item.packageName}"
                ShellActionType.UNINSTALL_USER -> "pm uninstall -k --user 0 ${item.packageName}"
                ShellActionType.RESTORE_APP -> "pm enable ${item.packageName}; cmd package install-existing ${item.packageName}"
            }

            val result = if (customExecutor != null) {
                customExecutor(rawCmd, asRoot)
            } else {
                RootExecutor.executeCommand(rawCmd, asRoot = asRoot)
            }

            results.add(result)
            if (result.isSuccess) {
                succeeded.add(item.packageName)
                val newEnabledState = (actionType == ShellActionType.RESTORE_APP)
                systemAppDao.updateEnabledState(
                    packageName = item.packageName,
                    isEnabled = newEnabledState
                )
            } else {
                failed.add(item.packageName)
            }
        }

        BatchExecutionReport(
            actionType = actionType,
            totalAttempted = payload.packageCommands.size,
            succeededPackages = succeeded,
            failedPackages = failed,
            commandResults = results
        )
    }

    /**
     * Executes a single package disable or restore command and updates its Room state.
     */
    suspend fun executeSinglePackageCommand(
        packageName: String,
        actionType: ShellActionType,
        asRoot: Boolean = true,
        customExecutor: (suspend (String, Boolean) -> CommandResult)? = null
    ): CommandResult = withContext(Dispatchers.IO) {
        val entity = systemAppDao.getAppByPackageName(packageName)
        if (entity?.safetyLevel == SafetyLevel.ESSENTIAL && actionType != ShellActionType.RESTORE_APP) {
            return@withContext CommandResult(
                command = "",
                isSuccess = false,
                output = "",
                error = "Blocked: Cannot disable critical system package ($packageName).",
                exitCode = -1
            )
        }

        val rawCmd = when (actionType) {
            ShellActionType.DISABLE_USER -> "pm disable-user --user 0 $packageName"
            ShellActionType.UNINSTALL_USER -> "pm uninstall -k --user 0 $packageName"
            ShellActionType.RESTORE_APP -> "pm enable $packageName; cmd package install-existing $packageName"
        }

        val result = if (customExecutor != null) {
            customExecutor(rawCmd, asRoot)
        } else {
            RootExecutor.executeCommand(rawCmd, asRoot = asRoot)
        }

        if (result.isSuccess) {
            val newEnabledState = (actionType == ShellActionType.RESTORE_APP)
            systemAppDao.updateEnabledState(packageName = packageName, isEnabled = newEnabledState)
        }
        result
    }

    private fun formatSingleCommand(rawShellCmd: String, useAdbPrefix: Boolean): String {
        return if (useAdbPrefix) "adb shell $rawShellCmd" else rawShellCmd
    }

    private fun buildConsoleOutput(
        commands: List<PackageShellCommand>,
        actionType: ShellActionType,
        skippedEssentials: Int,
        isAdb: Boolean
    ): String {
        val sb = StringBuilder()
        sb.append("# Action: ${actionType.label} | Mode: ${if (isAdb) "PC ADB" else "Local Shell"}\n")
        sb.append("# Targets from Room DB: ${commands.size} package(s)\n")
        if (skippedEssentials > 0) {
            sb.append("# [SAFETY GUARD] Skipped $skippedEssentials CRITICAL SYSTEM package(s)\n")
        }
        sb.append("\n")

        if (commands.isEmpty()) {
            if (actionType == ShellActionType.RESTORE_APP) {
                sb.append("# Select packages in the Checklist or freeze packages to generate restore commands.\n")
            } else {
                sb.append("# No packages currently selected in Room database.\n")
                sb.append("# Select packages in the Checklist tab or apply a preset.\n")
            }
        } else {
            for (cmd in commands) {
                sb.append("# ${cmd.displayName} [${cmd.safetyLevel.label}]\n")
                sb.append("${cmd.activeCommand}\n\n")
            }
        }
        return sb.toString().trimEnd()
    }

    private fun buildFullBashScript(
        commands: List<PackageShellCommand>,
        actionType: ShellActionType,
        skippedEssentials: Int
    ): String {
        val sb = StringBuilder()
        sb.append("#!/bin/bash\n")
        sb.append("# =========================================================\n")
        sb.append("# DebloatMaster Room-Backed Shell Script\n")
        sb.append("# Operation : ${actionType.label}\n")
        sb.append("# Packages  : ${commands.size}\n")
        if (skippedEssentials > 0) {
            sb.append("# Protected : $skippedEssentials critical package(s) excluded\n")
        }
        sb.append("# =========================================================\n\n")

        if (commands.isEmpty()) {
            sb.append("echo \"[!] No packages selected in Room database.\"\n")
            return sb.toString()
        }

        sb.append("echo \"[*] Starting DebloatMaster (${actionType.label}) for ${commands.size} package(s)...\"\n\n")
        commands.forEachIndexed { index, cmd ->
            sb.append("echo \"[${index + 1}/${commands.size}] ${cmd.displayName} (${cmd.packageName})\"\n")
            sb.append("${cmd.activeCommand}\n\n")
        }
        sb.append("echo \"[+] Completed ${actionType.label} on ${commands.size} package(s).\"\n")
        return sb.toString()
    }
}
