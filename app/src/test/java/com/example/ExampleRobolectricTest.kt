package com.example

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DebloatDatabase
import com.example.data.local.SystemAppEntity
import com.example.data.local.toDomainModel
import com.example.data.model.BloatCategory
import com.example.data.model.BloatPackage
import com.example.data.model.DebloatSnapshot
import com.example.data.model.DeviceManufacturer
import com.example.data.model.SafetyLevel
import com.example.data.model.SnapshotPackageRecord
import com.example.data.preset.BloatwareCatalog
import com.example.data.repository.BackupRepository
import com.example.data.repository.CommandResult
import com.example.data.service.ShellActionType
import com.example.data.service.ShellCommandGeneratorService
import com.example.data.service.ShellTargetFormat
import com.example.ui.DebloatUiState
import com.example.ui.screens.ChecklistScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: DebloatDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            DebloatDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

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
    fun `room database stores and queries system apps by state and risk level`() = runBlocking {
        val dao = database.systemAppDao()

        val sampleApps = listOf(
            SystemAppEntity(
                packageName = "com.miui.msa.global",
                displayName = "MIUI System Ads",
                isEnabled = true,
                isInstalled = true,
                safetyLevel = SafetyLevel.SAFE,
                category = BloatCategory.TELEMETRY,
                targetManufacturer = DeviceManufacturer.XIAOMI,
                description = "Ad daemon"
            ),
            SystemAppEntity(
                packageName = "com.samsung.android.scloud",
                displayName = "Samsung Cloud",
                isEnabled = false,
                isInstalled = true,
                safetyLevel = SafetyLevel.CAUTION,
                category = BloatCategory.CLOUD_SYNC,
                targetManufacturer = DeviceManufacturer.SAMSUNG,
                description = "Cloud sync"
            ),
            SystemAppEntity(
                packageName = "com.android.systemui",
                displayName = "System UI",
                isEnabled = true,
                isInstalled = true,
                safetyLevel = SafetyLevel.ESSENTIAL,
                category = BloatCategory.SYSTEM_CORE,
                targetManufacturer = DeviceManufacturer.GOOGLE,
                description = "Core UI"
            )
        )

        dao.upsertAll(sampleApps)

        val allApps = dao.observeAllSystemApps().first()
        assertEquals(3, allApps.size)

        val safeApps = dao.observeAppsByRiskLevel(SafetyLevel.SAFE).first()
        assertEquals(1, safeApps.size)
        assertEquals("com.miui.msa.global", safeApps.first().packageName)

        val disabledApps = dao.observeAppsByEnabledState(isEnabled = false).first()
        assertEquals(1, disabledApps.size)
        assertEquals("com.samsung.android.scloud", disabledApps.first().packageName)

        dao.updateEnabledState("com.miui.msa.global", isEnabled = false)
        dao.updateRiskLevel("com.samsung.android.scloud", SafetyLevel.SAFE)

        val updatedMsa = dao.getAppByPackageName("com.miui.msa.global")
        assertNotNull(updatedMsa)
        assertFalse(updatedMsa!!.isEnabled)

        val updatedCloud = dao.getAppByPackageName("com.samsung.android.scloud")
        assertEquals(SafetyLevel.SAFE, updatedCloud?.safetyLevel)

        dao.markPackagesForDebloat(listOf("com.miui.msa.global", "com.android.systemui"))
        val selected = dao.observeSelectedForDebloat().first()
        assertEquals(1, selected.size)
        assertEquals("com.miui.msa.global", selected.first().packageName)
    }

    @Test
    fun `application list renders visual risk indicator chips from room entities`(): Unit = runBlocking {
        val dao = database.systemAppDao()
        val entities = listOf(
            SystemAppEntity(
                packageName = "com.miui.msa.global",
                displayName = "MIUI System Ads",
                isEnabled = true,
                isInstalled = true,
                safetyLevel = SafetyLevel.SAFE,
                category = BloatCategory.TELEMETRY,
                targetManufacturer = DeviceManufacturer.XIAOMI,
                description = "Ad daemon"
            ),
            SystemAppEntity(
                packageName = "com.samsung.android.scloud",
                displayName = "Samsung Cloud",
                isEnabled = true,
                isInstalled = true,
                safetyLevel = SafetyLevel.CAUTION,
                category = BloatCategory.CLOUD_SYNC,
                targetManufacturer = DeviceManufacturer.SAMSUNG,
                description = "Cloud sync"
            ),
            SystemAppEntity(
                packageName = "com.android.systemui",
                displayName = "System UI",
                isEnabled = true,
                isInstalled = true,
                safetyLevel = SafetyLevel.ESSENTIAL,
                category = BloatCategory.SYSTEM_CORE,
                targetManufacturer = DeviceManufacturer.GOOGLE,
                description = "Core UI"
            )
        )
        dao.upsertAll(entities)

        val domainPackages = dao.observeAllSystemApps().first().map { it.toDomainModel() }

        composeTestRule.setContent {
            MyApplicationTheme {
                ChecklistScreen(
                    state = DebloatUiState(
                        isLoading = false,
                        packages = domainPackages,
                        onlyInstalled = true
                    ),
                    filteredPackages = domainPackages,
                    onToggleSelect = {},
                    onApplyPreset = {},
                    onClearSelection = {},
                    onSelectAllVisible = {},
                    onSearchChange = {},
                    onManufacturerFilterChange = {},
                    onSafetyFilterChange = {},
                    onToggleOnlyInstalled = {},
                    onShowDetails = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Verify color-coded risk chips distinguish 'Safe to remove', 'Caution', and 'Critical system'
        composeTestRule
            .onNodeWithTag("checklist_lazy_column")
            .performScrollToNode(hasTestTag("package_card_com.miui.msa.global"))
        composeTestRule
            .onNodeWithTag("risk_indicator_chip_com.miui.msa.global", useUnmergedTree = true)
            .assertIsDisplayed()
            .onChildren()
            .filterToOne(hasText("Safe to remove"))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("checklist_lazy_column")
            .performScrollToNode(hasTestTag("package_card_com.samsung.android.scloud"))
        composeTestRule
            .onNodeWithTag("risk_indicator_chip_com.samsung.android.scloud", useUnmergedTree = true)
            .assertIsDisplayed()
            .onChildren()
            .filterToOne(hasText("Caution"))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("checklist_lazy_column")
            .performScrollToNode(hasTestTag("package_card_com.android.systemui"))
        composeTestRule
            .onNodeWithTag("risk_indicator_chip_com.android.systemui", useUnmergedTree = true)
            .assertIsDisplayed()
            .onChildren()
            .filterToOne(hasText("Critical system"))
            .assertIsDisplayed()
    }

    @Test
    fun `bulk select and deselect toggle feature allows quick bulk actions on candidate apps`() = runBlocking {
        var selectAllCalled = false
        var deselectAllCalled = false

        val testPackages = listOf(
            BloatPackage(
                packageName = "com.miui.analytics",
                displayName = "Analytics",
                isEnabled = true,
                isInstalled = true,
                safetyLevel = SafetyLevel.SAFE,
                category = BloatCategory.TELEMETRY,
                targetManufacturer = DeviceManufacturer.XIAOMI,
                description = "Xiaomi tracking"
            ),
            BloatPackage(
                packageName = "com.facebook.services",
                displayName = "Facebook Services",
                isEnabled = true,
                isInstalled = true,
                safetyLevel = SafetyLevel.SAFE,
                category = BloatCategory.SOCIAL_PRELOAD,
                targetManufacturer = DeviceManufacturer.CARRIER_UNIVERSAL,
                description = "Facebook background service"
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                ChecklistScreen(
                    state = DebloatUiState(
                        isLoading = false,
                        packages = testPackages,
                        selectedPackageNames = setOf("com.miui.analytics"),
                        onlyInstalled = true
                    ),
                    filteredPackages = testPackages,
                    onToggleSelect = {},
                    onApplyPreset = {},
                    onClearSelection = {},
                    onSelectAllVisible = { selectAllCalled = true },
                    onDeselectAllVisible = { deselectAllCalled = true },
                    onSearchChange = {},
                    onManufacturerFilterChange = {},
                    onSafetyFilterChange = {},
                    onToggleOnlyInstalled = {},
                    onShowDetails = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("checklist_lazy_column")
            .performScrollToNode(hasTestTag("bulk_selection_bar"))

        composeTestRule.onNodeWithTag("bulk_selection_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("select_all_toggle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("select_all_button").assertIsDisplayed().performClick()
        assertTrue(selectAllCalled)

        composeTestRule.onNodeWithTag("deselect_all_button").assertIsDisplayed().performClick()
        assertTrue(deselectAllCalled)
    }

    @Test
    fun `shell command generator service generates, copies, and executes disable and restore commands from room`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dao = database.systemAppDao()
        val service = ShellCommandGeneratorService(context, dao)

        val apps = listOf(
            SystemAppEntity(
                packageName = "com.miui.msa.global",
                displayName = "MIUI System Ads",
                isEnabled = true,
                isInstalled = true,
                safetyLevel = SafetyLevel.SAFE,
                category = BloatCategory.TELEMETRY,
                targetManufacturer = DeviceManufacturer.XIAOMI,
                description = "Ad daemon",
                isMarkedForDebloat = true
            ),
            SystemAppEntity(
                packageName = "com.facebook.services",
                displayName = "Facebook Services",
                isEnabled = true,
                isInstalled = true,
                safetyLevel = SafetyLevel.SAFE,
                category = BloatCategory.SOCIAL_PRELOAD,
                targetManufacturer = DeviceManufacturer.CARRIER_UNIVERSAL,
                description = "Facebook daemon",
                isMarkedForDebloat = true
            )
        )
        dao.upsertAll(apps)

        val disablePayload = service.generateFromRoomSelection(
            actionType = ShellActionType.DISABLE_USER,
            targetFormat = ShellTargetFormat.PC_ADB
        )
        assertEquals(2, disablePayload.targetCount)
        assertTrue(disablePayload.formattedOutput.contains("adb shell pm disable-user --user 0 com.miui.msa.global"))
        assertTrue(disablePayload.singleLineCommand.contains("&&"))

        val copied = service.copyCommandToClipboard(disablePayload.singleLineCommand)
        assertTrue(copied)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals(disablePayload.singleLineCommand, clipboard.primaryClip?.getItemAt(0)?.text?.toString())

        val disableReport = service.executeSelectedCommands(
            actionType = ShellActionType.DISABLE_USER,
            asRoot = true,
            customExecutor = { cmd, _ ->
                CommandResult(command = cmd, isSuccess = true, output = "Package disabled", error = "", exitCode = 0)
            }
        )
        assertEquals(2, disableReport.succeededPackages.size)
        assertFalse(dao.getAppByPackageName("com.miui.msa.global")!!.isEnabled)
        assertFalse(dao.getAppByPackageName("com.facebook.services")!!.isEnabled)

        val restorePayload = service.generateFromRoomSelection(
            actionType = ShellActionType.RESTORE_APP,
            targetFormat = ShellTargetFormat.LOCAL_SHELL
        )
        assertTrue(restorePayload.formattedOutput.contains("pm enable com.miui.msa.global || cmd package install-existing com.miui.msa.global"))

        val restoreReport = service.executeSelectedCommands(
            actionType = ShellActionType.RESTORE_APP,
            asRoot = true,
            customExecutor = { cmd, _ ->
                CommandResult(command = cmd, isSuccess = true, output = "Package enabled", error = "", exitCode = 0)
            }
        )
        assertEquals(2, restoreReport.succeededPackages.size)
        assertTrue(dao.getAppByPackageName("com.miui.msa.global")!!.isEnabled)
        assertTrue(dao.getAppByPackageName("com.facebook.services")!!.isEnabled)
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
