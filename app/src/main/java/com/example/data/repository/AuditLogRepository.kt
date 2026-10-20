package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AuditLogEntry
import com.example.data.model.LogSeverity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class AuditLogRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("debloat_audit_logs", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, AuditLogEntry::class.java)
    private val adapter = moshi.adapter<List<AuditLogEntry>>(listType)

    private val _logsFlow = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val logsFlow: StateFlow<List<AuditLogEntry>> = _logsFlow.asStateFlow()

    suspend fun initLogs() = withContext(Dispatchers.IO) {
        val json = prefs.getString("logs_history", null)
        val loaded = if (json != null) {
            try {
                adapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            // Initial seed log
            listOf(
                AuditLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    tag = "INIT",
                    message = "DebloatMaster engine initialized. Ready to inspect device state and generate safe restoration scripts.",
                    severity = LogSeverity.INFO
                )
            )
        }
        _logsFlow.value = loaded
    }

    suspend fun log(
        tag: String,
        message: String,
        severity: LogSeverity = LogSeverity.INFO,
        commandExecuted: String? = null
    ) = withContext(Dispatchers.IO) {
        val safeTag = com.example.data.security.PackageSecurityValidator.sanitizeText(tag, 32)
        val safeMessage = com.example.data.security.PackageSecurityValidator.sanitizeText(message, 500)
        val safeCommand = commandExecuted?.let { com.example.data.security.PackageSecurityValidator.sanitizeText(it, 250) }
        val entry = AuditLogEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            tag = safeTag,
            message = safeMessage,
            severity = severity,
            commandExecuted = safeCommand
        )

        val updated = mutableListOf(entry).apply {
            addAll(_logsFlow.value.take(250)) // Keep recent 250 records
        }
        _logsFlow.value = updated
        saveLogs(updated)
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        _logsFlow.value = emptyList()
        prefs.edit().remove("logs_history").apply()
        log("LOG_CLEARED", "Audit log history cleared by user.", LogSeverity.INFO)
    }

    fun exportLogsAsText(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()
        sb.append("=== DEBLOATMASTER AUDIT LOG EXPORT ===\n")
        sb.append("Generated: ${dateFormat.format(System.currentTimeMillis())}\n")
        sb.append("Total Events: ${_logsFlow.value.size}\n")
        sb.append("======================================\n\n")

        for (log in _logsFlow.value) {
            sb.append("[${dateFormat.format(log.timestamp)}] [${log.severity}] [${log.tag}]\n")
            sb.append("  ${log.message}\n")
            if (log.commandExecuted != null) {
                sb.append("  Command: ${log.commandExecuted}\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun saveLogs(list: List<AuditLogEntry>) {
        try {
            val json = adapter.toJson(list)
            prefs.edit().putString("logs_history", json).apply()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
