package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.DebloatViewModel
import com.example.ui.components.PackageDetailsBottomSheet
import com.example.ui.screens.AuditLogScreen
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.ChecklistScreen
import com.example.ui.screens.DeviceInfoScreen
import com.example.ui.screens.TerminalActionScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    private val viewModel: DebloatViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    viewModel.snackBarEvent.collect { message ->
                        snackbarHostState.showSnackbar(message)
                    }
                }

                // Handle back press if inside a sub-tab
                if (state.currentTab != 0) {
                    BackHandler {
                        viewModel.setTab(0)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = SurfaceDark,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(NeonEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "DebloatMaster",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1E293B))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = state.hardwareInfo?.manufacturer?.take(8) ?: "PRO",
                                            color = CyberCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { viewModel.loadData() },
                                    modifier = Modifier.testTag("rescan_packages_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Rescan Packages",
                                        tint = CyberCyan
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = SurfaceDark
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF0D1424),
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = state.currentTab == 0,
                                onClick = { viewModel.setTab(0) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (state.selectedPackageNames.isNotEmpty()) {
                                                Badge(
                                                    containerColor = NeonEmerald,
                                                    contentColor = Color.Black
                                                ) {
                                                    Text(text = state.selectedPackageNames.size.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Checklist, contentDescription = "Checklist")
                                    }
                                },
                                label = { Text("Checklist", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_checklist")
                            )

                            NavigationBarItem(
                                selected = state.currentTab == 1,
                                onClick = { viewModel.setTab(1) },
                                icon = {
                                    Icon(Icons.Default.Terminal, contentDescription = "ADB Shell")
                                },
                                label = { Text("ADB Shell", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_terminal")
                            )

                            NavigationBarItem(
                                selected = state.currentTab == 2,
                                onClick = { viewModel.setTab(2) },
                                icon = {
                                    Icon(Icons.Default.Restore, contentDescription = "Backups")
                                },
                                label = { Text("Vault", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_backup")
                            )

                            NavigationBarItem(
                                selected = state.currentTab == 3,
                                onClick = { viewModel.setTab(3) },
                                icon = {
                                    Icon(Icons.Default.History, contentDescription = "Audit Logs")
                                },
                                label = { Text("Logs", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_logs")
                            )

                            NavigationBarItem(
                                selected = state.currentTab == 4,
                                onClick = { viewModel.setTab(4) },
                                icon = {
                                    Icon(Icons.Default.Tune, contentDescription = "Device Info")
                                },
                                label = { Text("Device", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                ),
                                modifier = Modifier.testTag("nav_device")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (state.currentTab) {
                            0 -> ChecklistScreen(
                                state = state,
                                filteredPackages = viewModel.getFilteredPackages(),
                                onToggleSelect = { viewModel.togglePackageSelection(it) },
                                onApplyPreset = { viewModel.applyPreset(it) },
                                onClearSelection = { viewModel.clearSelection() },
                                onSelectAllVisible = { viewModel.selectAllVisible() },
                                onDeselectAllVisible = { viewModel.deselectAllVisible() },
                                onSearchChange = { viewModel.setSearchQuery(it) },
                                onManufacturerFilterChange = { viewModel.setManufacturerFilter(it) },
                                onSafetyFilterChange = { viewModel.setSafetyFilter(it) },
                                onToggleOnlyInstalled = { viewModel.toggleOnlyInstalled(it) },
                                onShowDetails = { viewModel.showPackageDetails(it) }
                            )

                            1 -> TerminalActionScreen(
                                state = state,
                                onSetActionType = { viewModel.setShellActionType(it) },
                                onSetTargetFormat = { viewModel.setShellTargetFormat(it) },
                                onCopyScript = { viewModel.copyScriptToClipboard() },
                                onCopySingleLine = { viewModel.copySingleLineCommand() },
                                onCopyIndividualCommand = { pkgName, cmd, label ->
                                    viewModel.copyIndividualCommand(pkgName, cmd, label)
                                },
                                onShareScript = { viewModel.shareScript() },
                                onExecuteBatch = { viewModel.executeBatchShellCommands() },
                                onExecuteSinglePackage = { pkgName, actionType ->
                                    viewModel.executeSinglePackageShellCommand(pkgName, actionType)
                                }
                            )

                            2 -> BackupRestoreScreen(
                                state = state,
                                onCreateSnapshot = { title, notes -> viewModel.createBackupSnapshot(title, notes) },
                                onDeleteSnapshot = { viewModel.deleteBackupSnapshot(it) },
                                onRestoreSnapshot = { viewModel.restoreSnapshot(it) },
                                onExportSnapshot = { viewModel.exportSnapshotJson(it) },
                                onImportSnapshot = { viewModel.importSnapshotFromJson(it) }
                            )

                            3 -> AuditLogScreen(
                                state = state,
                                onClearLogs = { viewModel.clearLogs() },
                                onExportLogs = { viewModel.exportLogs() }
                            )

                            4 -> DeviceInfoScreen(
                                state = state
                            )
                        }
                    }

                    // Package Details Bottom Sheet
                    state.selectedPackageDetails?.let { pkg ->
                        PackageDetailsBottomSheet(
                            packageItem = pkg,
                            onDismiss = { viewModel.showPackageDetails(null) },
                            onOpenAppSettings = { viewModel.openAppSettings(it) },
                            onCopyCommand = { /* handled inside bottom sheet */ },
                            onUpdateRiskLevel = { pkgName, level ->
                                viewModel.updatePackageRiskLevel(pkgName, level)
                            }
                        )
                    }
                }
            }
        }
    }
}
