package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
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
import com.example.ui.NavDestination
import com.example.ui.SecondaryScreen
import com.example.ui.VitaFlowViewModel
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.components.TaglineBanner
import com.example.ui.theme.*

@Composable
fun InsightsScreen(
    viewModel: VitaFlowViewModel,
    modifier: Modifier = Modifier
) {
    val insights by viewModel.smartInsights.collectAsState()
    val wellnessScore by viewModel.wellnessScore.collectAsState()
    val weeklyWater by viewModel.weeklyWaterData.collectAsState()
    val weeklySteps by viewModel.weeklyStepsData.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    val waterTarget = profile?.waterTargetMl ?: 2500
    val stepTarget = profile?.stepTarget ?: 8000

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("insights_screen_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item(key = "insights_header") {
            Column {
                Text(
                    text = "Personal Insights",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Gentle pattern recognition • Consistency Over Restriction",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item(key = "insights_tagline") {
            TaglineBanner()
        }

        // Weekly Habit Consistency Chart Card
        item(key = "insights_weekly_water_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_trends_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Weekly Hydration Consistency",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Daily intake vs ${waterTarget}ml target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val hasAnyWater = weeklyWater.any { it.second > 0 }
                    if (!hasAnyWater) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No water intake logged this week yet. Your real 7-day consistency will appear here as you log.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            weeklyWater.forEachIndexed { idx, (day, amount) ->
                                val ratio = if (amount > 0) (amount.toFloat() / waterTarget).coerceIn(0.08f, 1f) else 0.04f
                                val isToday = idx == weeklyWater.lastIndex
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .height((ratio * 80).dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                if (amount >= waterTarget) EmeraldSuccess
                                                else if (amount > 0) (if (isToday) TealPrimary else OceanSecondary.copy(alpha = 0.7f))
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isToday) TealPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Weekly Steps & Movement Rhythm
        item(key = "insights_weekly_steps_card") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("steps_trends_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "7-Day Movement Cadence",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Daily steps relative to $stepTarget step target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val hasAnySteps = weeklySteps.any { it.second > 0 }
                    if (!hasAnySteps) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No steps recorded this week yet. Track your walks or sync with Health Connect to build your movement trend.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            weeklySteps.forEachIndexed { idx, (day, count) ->
                                val ratio = if (count > 0) (count.toFloat() / stepTarget).coerceIn(0.08f, 1f) else 0.04f
                                val isToday = idx == weeklySteps.lastIndex
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(22.dp)
                                            .height((ratio * 80).dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(
                                                if (count >= stepTarget) EmeraldSuccess
                                                else if (count > 0) AmberWarmth
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Insights List
        item(key = "insights_list_header") {
            Text(
                text = "Today's Wellness Observations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(insights, key = { "insight_${it.id}" }) { insight ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insight_card_${insight.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (insight.isPositive) EmeraldSuccess.copy(alpha = 0.08f) else AmberWarmth.copy(alpha = 0.08f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (insight.isPositive) EmeraldSuccess.copy(alpha = 0.2f) else AmberWarmth.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (insight.category) {
                                "Hydration" -> Icons.Default.WaterDrop
                                "Nutrition" -> Icons.Default.Restaurant
                                "Activity" -> Icons.Default.FitnessCenter
                                "Sleep" -> Icons.Default.NightlightRound
                                else -> Icons.Default.AutoAwesome
                            },
                            contentDescription = insight.category,
                            tint = if (insight.isPositive) EmeraldSuccess else AmberWarmth,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = insight.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        if (insight.actionLabel != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            FilledTonalButton(
                                onClick = {
                                    val label = insight.actionLabel.lowercase()
                                    val cat = insight.category.lowercase()
                                    when {
                                        label.contains("water") || cat == "hydration" ->
                                            viewModel.openSecondaryScreen(SecondaryScreen.WATER_TRACKER)
                                        label.contains("sleep") || cat == "sleep" ->
                                            viewModel.openSecondaryScreen(SecondaryScreen.SLEEP_TRACKER)
                                        label.contains("habit") || cat == "habits" ->
                                            viewModel.openSecondaryScreen(SecondaryScreen.HABITS_TRACKER)
                                        label.contains("walk") || label.contains("move") || label.contains("activity") || cat == "activity" ->
                                            viewModel.navigateTo(NavDestination.ACTIVITY)
                                        label.contains("meal") || label.contains("food") || cat == "nutrition" ->
                                            viewModel.navigateTo(NavDestination.NUTRITION)
                                        insight.id == "clean_start" || cat == "welcome" || label.contains("start tracking") ->
                                            viewModel.navigateTo(NavDestination.NUTRITION)
                                        else ->
                                            viewModel.navigateTo(NavDestination.NUTRITION)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("insight_action_button_${insight.id}")
                            ) {
                                Text(insight.actionLabel, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        }

        // Philosophy & Craft Note
        item(key = "insights_philosophy_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "The HeathTrack Philosophy 🌿",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Healthy habits thrive when approached with curiosity rather than punishment. By noticing your hydration, sleep rhythm, balanced meals, and regular movement, you build sustainable vitality that lasts a lifetime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item(key = "insights_disclaimer_card") {
            MedicalDisclaimerCard()
        }

        item(key = "insights_bottom_spacer") {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
