package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.model.BloatPackage
import com.example.data.model.DeviceManufacturer
import com.example.data.model.SafetyLevel
import com.example.ui.DebloatUiState
import com.example.ui.components.HeaderMetricsCard
import com.example.ui.components.RiskDistributionBar
import com.example.ui.components.RiskLevelChip
import com.example.ui.components.toVisualStyle
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionAmberBg
import com.example.ui.theme.CautionAmberText
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberCyanDim
import com.example.ui.theme.DangerRedBg
import com.example.ui.theme.DangerRedText
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.SafeGreenBg
import com.example.ui.theme.SafeGreenText
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ChecklistScreen(
    state: DebloatUiState,
    filteredPackages: List<BloatPackage>,
    onToggleSelect: (String) -> Unit,
    onApplyPreset: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAllVisible: () -> Unit,
    onSearchChange: (String) -> Unit,
    onManufacturerFilterChange: (DeviceManufacturer) -> Unit,
    onSafetyFilterChange: (SafetyLevel?) -> Unit,
    onToggleOnlyInstalled: (Boolean) -> Unit,
    onShowDetails: (BloatPackage) -> Unit,
    modifier: Modifier = Modifier
) {
    // Compute risk counts from the Room-backed package list (respecting installed filter)
    val basePool = if (state.onlyInstalled) {
        state.packages.filter { it.isInstalled }
    } else {
        state.packages
    }
    val safeCount = basePool.count { it.safetyLevel == SafetyLevel.SAFE }
    val cautionCount = basePool.count { it.safetyLevel == SafetyLevel.CAUTION }
    val criticalCount = basePool.count { it.safetyLevel == SafetyLevel.ESSENTIAL }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceDark)
            .testTag("checklist_lazy_column"),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Header Performance Boost Dashboard
        item {
            HeaderMetricsCard(
                metrics = state.performanceMetrics,
                activePreset = state.activePreset,
                onApplyPreset = onApplyPreset,
                onClearSelection = onClearSelection,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Visual Risk Distribution Bar from Room Database
        item {
            RiskDistributionBar(
                safeCount = safeCount,
                cautionCount = cautionCount,
                criticalCount = criticalCount,
                selectedFilter = state.safetyFilter,
                onSelectFilter = onSafetyFilterChange,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Search and Filters Section
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Search Input
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_packages_input"),
                    placeholder = { Text("Search package, app name, or service...", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberCyan)
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMuted)
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Color-Coded Risk Level Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.safetyFilter == null,
                        onClick = { onSafetyFilterChange(null) },
                        label = { Text("All Risks (${basePool.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("filter_chip_all_risks")
                    )

                    FilterChip(
                        selected = state.safetyFilter == SafetyLevel.SAFE,
                        onClick = {
                            onSafetyFilterChange(if (state.safetyFilter == SafetyLevel.SAFE) null else SafetyLevel.SAFE)
                        },
                        label = { Text("${SafetyLevel.SAFE.label} ($safeCount)") },
                        leadingIcon = {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SafeGreenBg,
                            selectedLabelColor = SafeGreenText,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("filter_chip_safe")
                    )

                    FilterChip(
                        selected = state.safetyFilter == SafetyLevel.CAUTION,
                        onClick = {
                            onSafetyFilterChange(if (state.safetyFilter == SafetyLevel.CAUTION) null else SafetyLevel.CAUTION)
                        },
                        label = { Text("${SafetyLevel.CAUTION.label} ($cautionCount)") },
                        leadingIcon = {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = CautionAmber, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CautionAmberBg,
                            selectedLabelColor = CautionAmberText,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("filter_chip_caution")
                    )

                    FilterChip(
                        selected = state.safetyFilter == SafetyLevel.ESSENTIAL,
                        onClick = {
                            onSafetyFilterChange(if (state.safetyFilter == SafetyLevel.ESSENTIAL) null else SafetyLevel.ESSENTIAL)
                        },
                        label = { Text("${SafetyLevel.ESSENTIAL.label} ($criticalCount)") },
                        leadingIcon = {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = CriticalRed, modifier = Modifier.size(14.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DangerRedBg,
                            selectedLabelColor = DangerRedText,
                            containerColor = SurfaceCard,
                            labelColor = TextSecondary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("filter_chip_essential")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // OEM Filter row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DeviceManufacturer.values().forEach { mfg ->
                        val isSelected = state.manufacturerFilter == mfg
                        FilterChip(
                            selected = isSelected,
                            onClick = { onManufacturerFilterChange(mfg) },
                            label = { Text(mfg.label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1E3A5F),
                                selectedLabelColor = CyberCyan,
                                containerColor = Color(0xFF0F172A),
                                labelColor = TextMuted
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Installed Switch + Select All Visible
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = state.onlyInstalled,
                            onCheckedChange = onToggleOnlyInstalled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CyberCyan,
                                checkedTrackColor = Color(0xFF0E3A52),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.testTag("toggle_only_installed_switch")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Installed on this phone only",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                    }

                    Text(
                        text = "Select Visible (${filteredPackages.count { it.safetyLevel != SafetyLevel.ESSENTIAL && it.isEnabled }})",
                        color = CyberCyan,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .testTag("select_all_visible_btn")
                            .clip(RoundedCornerShape(6.dp))
                            .background(CyberCyan.copy(alpha = 0.12f))
                            .clickable { onSelectAllVisible() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CANDIDATE APPLICATIONS (${filteredPackages.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${state.selectedPackageNames.size} marked for debloat",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (state.selectedPackageNames.isNotEmpty()) NeonEmerald else TextMuted,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Empty State
        if (filteredPackages.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No packages match your active filters",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try toggling off 'Installed on this phone only' or clearing the search query.",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else {
            items(filteredPackages, key = { it.packageName }) { pkg ->
                val isSelected = state.selectedPackageNames.contains(pkg.packageName)
                PackageItemCard(
                    packageItem = pkg,
                    isSelected = isSelected,
                    onToggleSelect = { onToggleSelect(pkg.packageName) },
                    onOpenDetails = { onShowDetails(pkg) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
fun PackageItemCard(
    packageItem: BloatPackage,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCritical = packageItem.safetyLevel == SafetyLevel.ESSENTIAL
    val riskStyle = packageItem.safetyLevel.toVisualStyle()

    val cardBorder = when {
        isSelected -> CyberCyan
        isCritical -> CriticalRed.copy(alpha = 0.45f)
        else -> Color(0xFF1E293B)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = !isCritical) { onToggleSelect() }
            .testTag("package_card_${packageItem.packageName}"),
        color = if (isSelected) Color(0xFF132238) else SurfaceCard,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color-coded left edge risk indicator bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(riskStyle.accentBarColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox or Lock Icon
                if (isCritical) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DangerRedBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Protected Critical System Component",
                            tint = DangerRedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelect() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = CyberCyan,
                            uncheckedColor = TextMuted,
                            checkmarkColor = Color.Black
                        ),
                        modifier = Modifier.testTag("checkbox_${packageItem.packageName}")
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Info Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = packageItem.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // Disabled / Frozen badge
                        if (packageItem.isInstalled && !packageItem.isEnabled) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF334155))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "FROZEN",
                                    color = TextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Monospace Package Name
                    Text(
                        text = packageItem.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberCyanDim,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Color-Coded Risk Chip + Category & RAM Benefit Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RiskLevelChip(
                            safetyLevel = packageItem.safetyLevel,
                            compact = true,
                            modifier = Modifier.testTag("risk_indicator_chip_${packageItem.packageName}")
                        )

                        Text(
                            text = packageItem.category.displayName,
                            color = TextMuted,
                            fontSize = 10.sp,
                            maxLines = 1
                        )

                        if (packageItem.ramSavingMb > 0) {
                            Text(
                                text = "• +${packageItem.ramSavingMb}MB",
                                color = NeonEmerald,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Info / Details icon button
                IconButton(
                    onClick = onOpenDetails,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("details_button_${packageItem.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Package Details",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
