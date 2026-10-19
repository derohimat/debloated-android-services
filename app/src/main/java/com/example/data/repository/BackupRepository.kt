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
            val currentList = getSnapshots().toMutableList()
            // Avoid duplicate ID
            val safeImported = if (currentList.any { it.id == imported.id }) {
                imported.copy(id = UUID.randomUUID().toString(), name = "${imported.name} (Imported)")
            } else {
                imported
            }
            currentList.add(0, safeImported)
            saveSnapshots(currentList)
            safeImported
        } catch (e: Exception) {
            null
        }
    }

    fun generateRestoreScript(snapshot: DebloatSnapshot): String {
        val sb = StringBuilder()
        sb.append("#!/bin/bash\n")
        sb.append("# ==================================================================\n")
        sb.append("# DebloatMaster System Restoration Script\n")
        sb.append("# Snapshot: ${snapshot.name}\n")
        sb.append("# Target Device: ${snapshot.deviceModel} (Android ${snapshot.androidVersion})\n")
        sb.append("# Created: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(snapshot.createdAt)}\n")
        sb.append("# Total Packages Restored: ${snapshot.packageRecords.count { it.isEnabled }}\n")
        sb.append("# ==================================================================\n\n")
        sb.append("echo \"[*] Starting DebloatMaster restoration...\"\n\n")

        val enabledInSnapshot = snapshot.packageRecords.filter { it.isEnabled }
        for (pkg in enabledInSnapshot) {
            sb.append("echo \"Restoring: ${pkg.packageName}\"\n")
            sb.append("adb shell pm enable ${pkg.packageName} 2>/dev/null || adb shell cmd package install-existing ${pkg.packageName}\n")
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
