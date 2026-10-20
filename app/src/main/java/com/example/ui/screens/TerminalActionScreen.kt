package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.service.PackageShellCommand
import com.example.data.service.ShellActionType
import com.example.data.service.ShellTargetFormat
import com.example.ui.DebloatUiState
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.SafeGreenBg
import com.example.ui.theme.SafeGreenText
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun TerminalActionScreen(
    state: DebloatUiState,
    onSetActionType: (ShellActionType) -> Unit,
    onSetTargetFormat: (ShellTargetFormat) -> Unit,
    onCopyScript: () -> Unit,
    onCopySingleLine: () -> Unit,
    onCopyIndividualCommand: (packageName: String, commandText: String, label: String) -> Unit,
    onShareScript: () -> Unit,
    onExecuteBatch: () -> Unit,
    onExecuteSinglePackage: (packageName: String, actionType: ShellActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRoot = state.hardwareInfo?.isRootAvailable ?: false
    val payload = state.commandPayload

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Action Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ROOM-BACKED SHELL SERVICE",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ADB & Command Suite",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Root status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isRoot) SafeGreenBg else Color(0xFF1E293B))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isRoot) NeonEmerald else CautionAmber)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRoot) "ROOT ACTIVE" else "ADB / SHELL MODE",
                            color = if (isRoot) SafeGreenText else TextSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Operation Type & Format Configuration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "1. SELECT OPERATION MODE (DISABLE OR RESTORE)",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShellActionType.values().forEach { action ->
                            val isSelected = state.shellActionType == action
                            val activeColor = if (action == ShellActionType.RESTORE_APP) NeonEmerald else CyberCyan
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetActionType(action) },
                                label = {
                                    Text(
                                        text = action.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = activeColor.copy(alpha = 0.2f),
                                    selectedLabelColor = activeColor,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = TextMuted
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("action_mode_${action.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = state.shellActionType.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "2. TARGET SHELL ENVIRONMENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShellTargetFormat.values().forEach { format ->
                            val isSelected = state.shellTargetFormat == format
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSetTargetFormat(format) },
                                label = { Text(format.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF1E3A5F),
                                    selectedLabelColor = CyberCyan,
                                    containerColor = Color(0xFF0F172A),
                                    labelColor = TextMuted
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("format_mode_${format.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        // Script Terminal Box
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GENERATED SHELL BUFFER (${payload.targetCount} ROOM APPS)",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = state.shellTargetFormat.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xFF060911),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = state.generatedScript,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = if (payload.targetCount == 0) TextMuted else if (state.shellActionType == ShellActionType.RESTORE_APP) NeonEmerald else CyberCyan,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Batch Copy & Execution Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onCopyScript,
                    enabled = payload.targetCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.shellActionType == ShellActionType.RESTORE_APP) NeonEmerald else CyberCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("copy_all_adb_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copy ${state.shellActionType.label} Commands (${payload.targetCount})",
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCopySingleLine,
                        enabled = payload.targetCount > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_oneliner_button")
                    ) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Copy 1-Liner", color = TextPrimary, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onShareScript,
                        enabled = payload.targetCount > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_script_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Export .sh", color = TextPrimary, fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onExecuteBatch,
                    enabled = payload.targetCount > 0 && !state.isExecutingCommand,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRoot) NeonEmerald else Color(0xFF1E293B),
                        contentColor = if (isRoot) Color.Black else TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("execute_batch_button")
                ) {
                    if (state.isExecutingCommand) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = if (isRoot) Color.Black else CyberCyan,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRoot) {
                                "Execute ${state.shellActionType.label} on Device (Root)"
                            } else {
                                "Attempt Direct Shell Execution (${payload.targetCount} Apps)"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Individual Package Command Cards (for granular copy/execute of disable or restore)
        if (payload.packageCommands.isNotEmpty()) {
            item {
                Text(
                    text = "INDIVIDUAL PACKAGE SHELL COMMANDS (${payload.packageCommands.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            items(payload.packageCommands, key = { it.packageName }) { itemCmd ->
                IndividualCommandCard(
                    item = itemCmd,
                    onCopyDisable = {
                        onCopyIndividualCommand(itemCmd.packageName, itemCmd.disableCommand, "Disable")
                    },
                    onCopyRestore = {
                        onCopyIndividualCommand(itemCmd.packageName, itemCmd.restoreCommand, "Restore")
                    },
                    onExecuteDisable = {
                        onExecuteSinglePackage(itemCmd.packageName, ShellActionType.DISABLE_USER)
                    },
                    onExecuteRestore = {
                        onExecuteSinglePackage(itemCmd.packageName, ShellActionType.RESTORE_APP)
                    }
                )
            }
        }

        // Interactive ADB Guide for Non-Root Users
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HOW TO EXECUTE VIA ADB (NO ROOT REQUIRED)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AdbGuideStep(
                        step = "1",
                        title = "Enable Developer Options",
                        description = "Go to Phone Settings -> About Phone. Tap 'Build Number' 7 times until Developer Mode is unlocked."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AdbGuideStep(
                        step = "2",
                        title = "Enable USB Debugging",
                        description = "Open Developer Options and toggle on 'USB Debugging'. (On Xiaomi, also enable 'USB debugging (Security settings)')."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AdbGuideStep(
                        step = "3",
                        title = "Connect & Run",
                        description = "Connect phone to PC, open Terminal / PowerShell, type 'adb devices' to allow access, and paste the copied commands!"
                    )
                }
            }
        }
    }
}

@Composable
private fun IndividualCommandCard(
    item: PackageShellCommand,
    onCopyDisable: () -> Unit,
    onCopyRestore: () -> Unit,
    onExecuteDisable: () -> Unit,
    onExecuteRestore: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp)),
        color = SurfaceCard
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberCyan,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCopyDisable,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("copy_disable_${item.packageName}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Disable Command",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onCopyRestore,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("copy_restore_${item.packageName}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Copy Restore Command",
                            tint = NeonEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0A0F1D))
                    .padding(8.dp)
            ) {
                Text(
                    text = "$ ${item.activeCommand}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExecuteDisable,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Run Disable", fontSize = 11.sp, color = CyberCyan)
                }
                OutlinedButton(
                    onClick = onExecuteRestore,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Run Restore", fontSize = 11.sp, color = NeonEmerald)
                }
            }
        }
    }
}

@Composable
private fun AdbGuideStep(step: String, title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = step, color = CyberCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
    }
}
