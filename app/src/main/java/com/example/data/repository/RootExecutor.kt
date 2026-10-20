package com.example.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class CommandResult(
    val command: String,
    val isSuccess: Boolean,
    val output: String,
    val error: String,
    val exitCode: Int
)

object RootExecutor {

    fun isRootAvailable(): Boolean {
        val paths = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/app/Superuser.apk"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    suspend fun executeCommand(command: String, asRoot: Boolean = true): CommandResult = withContext(Dispatchers.IO) {
        if (!com.example.data.security.PackageSecurityValidator.isSafeCommand(command)) {
            return@withContext CommandResult(
                command = command,
                isSuccess = false,
                output = "",
                error = "Security Policy Blocked: Refusing to execute unauthorized or unvalidated shell command.",
                exitCode = -1
            )
        }
        val shellCmd = if (asRoot) arrayOf("su", "-c", command) else arrayOf("sh", "-c", command)
        try {
            val process = Runtime.getRuntime().exec(shellCmd)
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()

            CommandResult(
                command = command,
                isSuccess = exitCode == 0,
                output = output.trim(),
                error = error.trim(),
                exitCode = exitCode
            )
        } catch (e: Exception) {
            CommandResult(
                command = command,
                isSuccess = false,
                output = "",
                error = e.localizedMessage ?: "Execution failed. Root permission denied or command shell unavailable.",
                exitCode = -1
            )
        }
    }
}
