package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.DebloatSnapshot
import com.example.data.model.SafetyLevel
import com.example.data.model.SnapshotPackageRecord
import com.example.data.preset.BloatwareCatalog
import com.example.data.repository.BackupRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DebloatMaster", appName)
    }

    @Test
    fun `bloatware catalog contains safe and essential packages`() {
        val packages = BloatwareCatalog.curatedPackages
        assertTrue(packages.isNotEmpty())

        val safePackages = packages.filter { it.safetyLevel == SafetyLevel.SAFE }
        assertTrue(safePackages.isNotEmpty())

        val essentialPackages = packages.filter { it.safetyLevel == SafetyLevel.ESSENTIAL }
        assertTrue(essentialPackages.any { it.packageName == "com.android.systemui" })
    }

    @Test
    fun `backup repository generates valid restore script`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = BackupRepository(context)

        val snapshot = DebloatSnapshot(
            id = "test-uuid",
            createdAt = System.currentTimeMillis(),
            name = "Test Snapshot",
            deviceModel = "Pixel 8",
            androidVersion = "14",
            totalCaptured = 2,
            disabledCount = 1,
            packageRecords = listOf(
                SnapshotPackageRecord("com.miui.msa.global", isEnabled = true, isInstalled = true),
                SnapshotPackageRecord("com.facebook.services", isEnabled = false, isInstalled = true)
            )
        )

        val script = repo.generateRestoreScript(snapshot)
        assertTrue(script.contains("pm enable com.miui.msa.global"))
    }
}
