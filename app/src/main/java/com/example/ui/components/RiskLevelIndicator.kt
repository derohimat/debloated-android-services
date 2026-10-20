package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SafetyLevel
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CautionAmberBg
import com.example.ui.theme.CautionAmberText
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.DangerRedBg
import com.example.ui.theme.DangerRedText
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.SafeGreenBg
import com.example.ui.theme.SafeGreenText
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

/**
 * Styling palette for each Room [SafetyLevel] tier.
 */
data class RiskVisualStyle(
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val accentBarColor: Color,
    val icon: ImageVector,
    val badgeText: String
)

fun SafetyLevel.toVisualStyle(): RiskVisualStyle {
    return when (this) {
        SafetyLevel.SAFE -> RiskVisualStyle(
            containerColor = SafeGreenBg,
            contentColor = SafeGreenText,
            borderColor = NeonEmerald.copy(alpha = 0.55f),
            accentBarColor = NeonEmerald,
            icon = Icons.Default.CheckCircle,
            badgeText = this.label // "Safe to remove"
        )
        SafetyLevel.CAUTION -> RiskVisualStyle(
            containerColor = CautionAmberBg,
            contentColor = CautionAmberText,
            borderColor = CautionAmber.copy(alpha = 0.55f),
            accentBarColor = CautionAmber,
            icon = Icons.Default.Warning,
            badgeText = this.label // "Caution"
        )
        SafetyLevel.ESSENTIAL -> RiskVisualStyle(
            containerColor = DangerRedBg,
            contentColor = DangerRedText,
            borderColor = CriticalRed.copy(alpha = 0.65f),
            accentBarColor = CriticalRed,
            icon = Icons.Default.Shield,
            badgeText = this.label // "Critical system"
        )
    }
}

/**
 * Color-coded visual indicator chip that distinguishes between
 * 'Safe to remove', 'Caution', and 'Critical system' apps from the Room database.
 */
@Composable
fun RiskLevelChip(
    safetyLevel: SafetyLevel,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val style = safetyLevel.toVisualStyle()
    val cornerRadius = if (compact) 6.dp else 8.dp

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(style.containerColor)
            .border(
                width = 1.dp,
                color = style.borderColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 3.dp else 5.dp
            )
    ) {
        Icon(
            imageVector = style.icon,
            contentDescription = style.badgeText,
            tint = style.contentColor,
            modifier = Modifier.size(if (compact) 12.dp else 14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = style.badgeText,
            color = style.contentColor,
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
        )
    }
}

/**
 * Interactive color-coded Room risk distribution bar and legend allowing users
 * to visually inspect and filter 'Safe to remove', 'Caution', and 'Critical system' apps.
 */
@Composable
fun RiskDistributionBar(
    safeCount: Int,
    cautionCount: Int,
    criticalCount: Int,
    selectedFilter: SafetyLevel?,
    onSelectFilter: (SafetyLevel?) -> Unit,
    modifier: Modifier = Modifier
) {
    val total = (safeCount + cautionCount + criticalCount).coerceAtLeast(1)
    val safeWeight = (safeCount.toFloat() / total).coerceAtLeast(0.05f)
    val cautionWeight = (cautionCount.toFloat() / total).coerceAtLeast(0.05f)
    val criticalWeight = (criticalCount.toFloat() / total).coerceAtLeast(0.05f)

    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ROOM DB RISK BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.Bold
                )
                if (selectedFilter != null) {
                    Text(
                        text = "Show All",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonEmerald,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onSelectFilter(null) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Multi-segment color-coded proportion bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0F172A)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (safeCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(safeWeight)
                            .fillMaxHeight()
                            .background(NeonEmerald)
                    )
                }
                if (cautionCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(cautionWeight)
                            .fillMaxHeight()
                            .background(CautionAmber)
                    )
                }
                if (criticalCount > 0) {
                    Box(
                        modifier = Modifier
                            .weight(criticalWeight)
                            .fillMaxHeight()
                            .background(CriticalRed)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Interactive Color-Coded Legend Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RiskLegendFilterPill(
                    level = SafetyLevel.SAFE,
                    count = safeCount,
                    isSelected = selectedFilter == SafetyLevel.SAFE,
                    onClick = {
                        onSelectFilter(if (selectedFilter == SafetyLevel.SAFE) null else SafetyLevel.SAFE)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("risk_filter_pill_safe")
                )
                RiskLegendFilterPill(
                    level = SafetyLevel.CAUTION,
                    count = cautionCount,
                    isSelected = selectedFilter == SafetyLevel.CAUTION,
                    onClick = {
                        onSelectFilter(if (selectedFilter == SafetyLevel.CAUTION) null else SafetyLevel.CAUTION)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("risk_filter_pill_caution")
                )
                RiskLegendFilterPill(
                    level = SafetyLevel.ESSENTIAL,
                    count = criticalCount,
                    isSelected = selectedFilter == SafetyLevel.ESSENTIAL,
                    onClick = {
                        onSelectFilter(if (selectedFilter == SafetyLevel.ESSENTIAL) null else SafetyLevel.ESSENTIAL)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("risk_filter_pill_essential")
                )
            }
        }
    }
}

@Composable
private fun RiskLegendFilterPill(
    level: SafetyLevel,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = level.toVisualStyle()
    val bgColor = if (isSelected) style.containerColor else Color(0xFF0F172A)
    val borderColor = if (isSelected) style.accentBarColor else Color(0xFF1E293B)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(style.accentBarColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = count.toString(),
                    color = if (isSelected) style.contentColor else Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = style.badgeText,
                color = if (isSelected) style.contentColor else TextMuted,
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
