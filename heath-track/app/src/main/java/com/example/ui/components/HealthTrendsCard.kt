package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class HealthTrendTab {
    HYDRATION,
    ACTIVITY
}

@Composable
fun HealthTrendsCard(
    weeklyWater: List<Pair<String, Int>>,
    waterTargetMl: Int,
    weeklySteps: List<Pair<String, Int>>,
    stepTarget: Int,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(HealthTrendTab.HYDRATION) }
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("health_trends_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Title & Tab Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TealPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = "Health Trends",
                            tint = TealPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Health Trends",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "7-Day Room Database History",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tab Switcher
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(2.dp)
                ) {
                    TabPill(
                        text = "Water",
                        icon = Icons.Default.LocalDrink,
                        isSelected = selectedTab == HealthTrendTab.HYDRATION,
                        onClick = {
                            selectedTab = HealthTrendTab.HYDRATION
                            selectedDayIndex = null
                        }
                    )
                    TabPill(
                        text = "Steps",
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        isSelected = selectedTab == HealthTrendTab.ACTIVITY,
                        onClick = {
                            selectedTab = HealthTrendTab.ACTIVITY
                            selectedDayIndex = null
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                HealthTrendTab.HYDRATION -> {
                    HydrationTrendView(
                        weeklyWater = weeklyWater,
                        waterTargetMl = waterTargetMl,
                        selectedDayIndex = selectedDayIndex,
                        onSelectDay = { selectedDayIndex = if (selectedDayIndex == it) null else it }
                    )
                }
                HealthTrendTab.ACTIVITY -> {
                    ActivityTrendView(
                        weeklySteps = weeklySteps,
                        stepTarget = stepTarget,
                        selectedDayIndex = selectedDayIndex,
                        onSelectDay = { selectedDayIndex = if (selectedDayIndex == it) null else it }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
private fun HydrationTrendView(
    weeklyWater: List<Pair<String, Int>>,
    waterTargetMl: Int,
    selectedDayIndex: Int?,
    onSelectDay: (Int) -> Unit
) {
    val nonZeroEntries = weeklyWater.filter { it.second > 0 }
    val avgWater = if (nonZeroEntries.isNotEmpty()) nonZeroEntries.map { it.second }.average().toInt() else 0
    val daysReachedTarget = weeklyWater.count { it.second >= waterTargetMl }

    // Summary Stat Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Target: ${waterTargetMl}ml / day",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (avgWater > 0) "Avg ${avgWater}ml • $daysReachedTarget/7 days met" else "Log water to see consistency",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (daysReachedTarget > 0) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
            )
        }

        if (selectedDayIndex != null && selectedDayIndex in weeklyWater.indices) {
            val (day, amount) = weeklyWater[selectedDayIndex]
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = WaterBlue.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$day: ${amount}ml (${(amount * 100f / waterTargetMl).toInt()}%)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = WaterBlue,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 7-Day Interactive Columns
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .testTag("hydration_trend_bars"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        weeklyWater.forEachIndexed { idx, (day, amount) ->
            val ratio = if (amount > 0) (amount.toFloat() / waterTargetMl.toFloat()).coerceIn(0.08f, 1.2f) else 0.04f
            val isToday = idx == weeklyWater.lastIndex
            val isSelected = selectedDayIndex == idx
            val isTargetMet = amount >= waterTargetMl

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectDay(idx) }
            ) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height((ratio * 85).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            when {
                                isSelected -> CoralAccent
                                isTargetMet -> EmeraldSuccess
                                amount > 0 -> if (isToday) TealPrimary else WaterBlue.copy(alpha = 0.75f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) CoralAccent else if (isToday) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActivityTrendView(
    weeklySteps: List<Pair<String, Int>>,
    stepTarget: Int,
    selectedDayIndex: Int?,
    onSelectDay: (Int) -> Unit
) {
    val nonZeroSteps = weeklySteps.filter { it.second > 0 }
    val avgSteps = if (nonZeroSteps.isNotEmpty()) nonZeroSteps.map { it.second }.average().toInt() else 0
    val daysReachedTarget = weeklySteps.count { it.second >= stepTarget }

    // Summary Stat Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Daily Target: $stepTarget steps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (avgSteps > 0) "Avg ${avgSteps} steps • $daysReachedTarget/7 days met" else "Sync with Health Connect or log walks",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (daysReachedTarget > 0) EmeraldSuccess else MaterialTheme.colorScheme.onSurface
            )
        }

        if (selectedDayIndex != null && selectedDayIndex in weeklySteps.indices) {
            val (day, count) = weeklySteps[selectedDayIndex]
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AmberWarmth.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$day: $count steps",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AmberWarmth,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 7-Day Interactive Columns
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .testTag("steps_trend_bars"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        weeklySteps.forEachIndexed { idx, (day, count) ->
            val ratio = if (count > 0) (count.toFloat() / stepTarget.toFloat()).coerceIn(0.08f, 1.2f) else 0.04f
            val isToday = idx == weeklySteps.lastIndex
            val isSelected = selectedDayIndex == idx
            val isTargetMet = count >= stepTarget

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelectDay(idx) }
            ) {
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .height((ratio * 85).dp)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            when {
                                isSelected -> CoralAccent
                                isTargetMet -> EmeraldSuccess
                                count > 0 -> AmberWarmth
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isSelected) CoralAccent else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
