package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SleepDaySummary
import com.example.ui.theme.*

@Composable
fun SleepSummaryChart(
    sleepDays: List<SleepDaySummary>,
    isHealthConnectConnected: Boolean,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier,
    targetDurationMinutes: Int = 480 // Default 8 hours
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Calculate 7-day average
    val (avgHours, avgRemMins) = remember(sleepDays) {
        val nonZeroDays = sleepDays.filter { it.durationMinutes > 0 }
        val avgMins = if (nonZeroDays.isNotEmpty()) nonZeroDays.map { it.durationMinutes }.average().toInt() else 0
        Pair(avgMins / 60, avgMins % 60)
    }

    val goalMetCount = remember(sleepDays, targetDurationMinutes) {
        sleepDays.count { it.durationMinutes >= targetDurationMinutes }
    }

    val hcCount = remember(sleepDays) {
        sleepDays.count { it.source == "Health Connect" }
    }

    // Selected item or default to today (last item)
    val effectiveSelectedIndex = selectedIndex ?: (sleepDays.size - 1).takeIf { it >= 0 }
    val selectedDay = effectiveSelectedIndex?.let { sleepDays.getOrNull(it) }

    val maxDuration = 600f // 10 hours max scale for the bars

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sleep_summary_chart_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PurpleSleep.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = "Sleep Chart",
                            tint = PurpleSleep
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sleep Duration Trend",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Last 7 Days • Health Connect",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Health Connect Badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isHealthConnectConnected) EmeraldSuccess.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isHealthConnectConnected) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isHealthConnectConnected) EmeraldSuccess else MaterialTheme.colorScheme.outline)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHealthConnectConnected) "Health Sync" else "Local Only",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Stats Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "7-Day Avg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${avgHours}h ${avgRemMins}m",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = PurpleSleep)
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "8h Target Goal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$goalMetCount / 7 Days Met",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = EmeraldSuccess)
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Data Source",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (hcCount > 0) "$hcCount Health Conn" else "Manual/Goal",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Chart Canvas & Interactive Columns
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                // Background Reference Line (Target 8h line)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val targetY = size.height * (1f - (targetDurationMinutes.toFloat() / maxDuration).coerceIn(0f, 1f))
                    drawLine(
                        color = PurpleSleepLight.copy(alpha = 0.4f),
                        start = Offset(0f, targetY),
                        end = Offset(size.width, targetY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                }

                // Target Goal Label
                Text(
                    text = "8h target",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                    color = PurpleSleep,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp)
                )

                // Bar Columns Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    sleepDays.forEachIndexed { index, day ->
                        val isSelected = (index == effectiveSelectedIndex)
                        val fraction = (day.durationMinutes.toFloat() / maxDuration).coerceIn(0.05f, 1f)
                        val animatedFraction by animateFloatAsState(
                            targetValue = fraction,
                            animationSpec = tween(durationMillis = 400),
                            label = "bar_height_$index"
                        )

                        val hoursText = "${day.durationMinutes / 60}h"

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedIndex = index },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Duration text on top of selected/today bar
                            Text(
                                text = hoursText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                ),
                                color = if (isSelected) PurpleSleep else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // The Bar container
                            Box(
                                modifier = Modifier
                                    .width(if (isSelected) 22.dp else 18.dp)
                                    .fillMaxHeight(animatedFraction)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        brush = if (isSelected) {
                                            Brush.verticalGradient(listOf(PurpleSleep, PurpleSleepLight))
                                        } else if (day.source == "Health Connect") {
                                            Brush.verticalGradient(listOf(PurpleSleep.copy(alpha = 0.85f), PurpleSleepLight.copy(alpha = 0.6f)))
                                        } else if (day.durationMinutes >= targetDurationMinutes) {
                                            Brush.verticalGradient(listOf(TealPrimary, TealPrimaryLight))
                                        } else {
                                            Brush.verticalGradient(listOf(PurpleSleepLight.copy(alpha = 0.4f), PurpleSleepLight.copy(alpha = 0.2f)))
                                        }
                                    )
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, PurpleSleep, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        else Modifier
                                    )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Day Label
                            Text(
                                text = day.dayOfWeek,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                ),
                                color = if (isSelected) PurpleSleep else if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Indicator dot for today
                            if (day.isToday) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(PurpleSleep)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected Day Tooltip / Detail Card
            selectedDay?.let { day ->
                val totalMins = day.durationMinutes
                val h = totalMins / 60
                val m = totalMins % 60
                val diffFromGoal = totalMins - targetDurationMinutes

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (day.isToday) "Today's Sleep (${day.date})" else "Sleep on ${day.dayOfWeek} (${day.date})",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = PurpleSleep.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = day.source,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = PurpleSleep, fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${h}h ${m}m duration • Quality: ${day.quality}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Goal Difference Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (diffFromGoal >= 0) EmeraldSuccess.copy(alpha = 0.15f) else AmberWarmth.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (diffFromGoal >= 0) "+${diffFromGoal}m vs goal" else "${diffFromGoal}m vs goal",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (diffFromGoal >= 0) EmeraldSuccess else AmberWarmth
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sync Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Syncs with Health Connect sleep sessions",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = onSyncClick,
                    enabled = !isSyncing,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSyncing) "Syncing..." else "Sync Now",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
