package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.DebloatSnapshot
import com.example.data.model.SnapshotPackageRecord
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class BackupRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("debloat_snapshots", Context.MODE_PRIVATE)

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val snapshotListType = Types.newParameterizedType(List::class.java, DebloatSnapshot::class.java)
    private val listAdapter = moshi.adapter<List<DebloatSnapshot>>(snapshotListType)
    private val singleAdapter = moshi.adapter(DebloatSnapshot::class.java)

    suspend fun getSnapshots(): List<DebloatSnapshot> = withContext(Dispatchers.IO) {
        val json = prefs.getString("saved_snapshots", null) ?: return@withContext emptyList()
        try {
            listAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createSnapshot(
        name: String,
        deviceModel: String,
        androidVersion: String,
        records: List<SnapshotPackageRecord>,
        notes: String = ""
    ): DebloatSnapshot = withContext(Dispatchers.IO) {
        val disabledCount = records.count { !it.isEnabled }
        val snapshot = DebloatSnapshot(
            id = UUID.randomUUID().toString(),
            createdAt = System.currentTimeMillis(),
            name = name.ifBlank { "System Snapshot #${System.currentTimeMillis() % 10000}" },
            deviceModel = deviceModel,
            androidVersion = androidVersion,
            totalCaptured = records.size,
            disabledCount = disabledCount,
            packageRecords = records,
            notes = notes
        )

        val currentList = getSnapshots().toMutableList()
        currentList.add(0, snapshot)
        saveSnapshots(currentList)
        snapshot
    }

    suspend fun deleteSnapshot(id: String): Boolean = withContext(Dispatchers.IO) {
        val currentList = getSnapshots().toMutableList()
        val removed = currentList.removeAll { it.id == id }
        if (removed) {
            saveSnapshots(currentList)
        }
        removed
    }

    suspend fun exportSnapshotJson(snapshot: DebloatSnapshot): String = withContext(Dispatchers.IO) {
        singleAdapter.toJson(snapshot)
    }

    suspend fun importSnapshotJson(jsonStr: String): DebloatSnapshot? = withContext(Dispatchers.IO) {
        try {
            val imported = singleAdapter.fromJson(jsonStr) ?: return@withContext null
            // Security: filter and validate all package records
            val validRecords = imported.packageRecords.filter {
                com.example.data.security.PackageSecurityValidator.isValidPackageName(it.packageName)
            }
            val sanitizedName = com.example.data.security.PackageSecurityValidator.sanitizeText(imported.name, maxLength = 64)
            val sanitizedNotes = com.example.data.security.PackageSecurityValidator.sanitizeText(imported.notes, maxLength = 256)
            val sanitizedModel = com.example.data.security.PackageSecurityValidator.sanitizeText(imported.deviceModel, maxLength = 48)

            val currentList = getSnapshots().toMutableList()
            val safeImported = imported.copy(
                id = UUID.randomUUID().toString(),
                name = if (sanitizedName.isNotBlank()) "$sanitizedName (Imported)" else "Imported Snapshot #${System.currentTimeMillis() % 10000}",
                deviceModel = sanitizedModel.ifBlank { "Unknown Device" },
                notes = sanitizedNotes,
                packageRecords = validRecords,
                totalCaptured = validRecords.size,
                disabledCount = validRecords.count { !it.isEnabled }
            )
            currentList.add(0, safeImported)
            saveSnapshots(currentList)
            safeImported
        } catch (e: Exception) {
            null
        }
    }

    fun generateRestoreScript(snapshot: DebloatSnapshot): String {
        val safeName = com.example.data.security.PackageSecurityValidator.sanitizeText(snapshot.name, 64)
        val safeModel = com.example.data.security.PackageSecurityValidator.sanitizeText(snapshot.deviceModel, 48)
        val safeVersion = com.example.data.security.PackageSecurityValidator.sanitizeText(snapshot.androidVersion, 24)

        val sb = StringBuilder()
        sb.append("#!/bin/bash\n")
        sb.append("# ==================================================================\n")
        sb.append("# DebloatMaster System Restoration Script\n")
        sb.append("# Snapshot: $safeName\n")
        sb.append("# Target Device: $safeModel (Android $safeVersion)\n")
        sb.append("# Created: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(snapshot.createdAt)}\n")
        sb.append("# Total Packages Restored: ${snapshot.packageRecords.count { it.isEnabled && com.example.data.security.PackageSecurityValidator.isValidPackageName(it.packageName) }}\n")
        sb.append("# ==================================================================\n\n")
        sb.append("echo \"[*] Starting DebloatMaster restoration...\"\n\n")

        val enabledInSnapshot = snapshot.packageRecords.filter { 
            it.isEnabled && com.example.data.security.PackageSecurityValidator.isValidPackageName(it.packageName) 
        }
        for (pkg in enabledInSnapshot) {
            val safePkg = pkg.packageName
            sb.append("echo \"Restoring: $safePkg\"\n")
            sb.append("adb shell pm enable $safePkg 2>/dev/null || adb shell cmd package install-existing $safePkg\n")
        }

        sb.append("\necho \"[+] Restoration commands sent successfully!\"\n")
        sb.append("echo \"[!] Restarting device recommended if deep system services were restored.\"\n")
        return sb.toString()
    }

    private fun saveSnapshots(list: List<DebloatSnapshot>) {
        val json = listAdapter.toJson(list)
        prefs.edit().putString("saved_snapshots", json).apply()
    }
}
