package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChecklistCategory
import com.example.data.model.DailyChecklistItem
import com.example.data.model.DailyWellnessSummary
import com.example.util.HapticsHelper

@Composable
fun DailyWellnessSummaryCard(
    summary: DailyWellnessSummary,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_wellness_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Wellness Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = summary.formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${summary.wellnessScore}/100",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick stats row: Salat, Water, Steps, Habits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WellnessPillMetric(
                    title = "Salat",
                    value = "${summary.salatCompletedCount}/5",
                    icon = Icons.Default.Mosque,
                    color = Color(0xFF0D9488),
                    modifier = Modifier.weight(1f)
                )

                WellnessPillMetric(
                    title = "Water",
                    value = "${summary.waterMl}ml",
                    icon = Icons.Default.WaterDrop,
                    color = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )

                WellnessPillMetric(
                    title = "Steps",
                    value = summary.steps.toString(),
                    icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )

                WellnessPillMetric(
                    title = "Habits",
                    value = "${summary.habitsCompletedCount}/${summary.habitsTotalCount.coerceAtLeast(summary.habitsCompletedCount)}",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WellnessPillMetric(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun DailyWellnessChecklistSection(
    modifier: Modifier = Modifier,
    items: List<DailyChecklistItem>,
    onToggleItem: (DailyChecklistItem) -> Unit,
    onCategoryAction: ((ChecklistCategory) -> Unit)? = null,
    hapticEnabled: Boolean = true,
) {
    val context = LocalContext.current
    val completedCount = items.count { it.isCompleted }
    val totalCount = items.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_wellness_checklist_section"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Wellness & Salat Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$completedCount of $totalCount daily tasks completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.5.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            items.forEach { item ->
                DailyChecklistRow(
                    item = item,
                    onToggle = {
                        HapticsHelper.performClick(context, hapticEnabled)
                        onToggleItem(item)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DailyChecklistRow(
    item: DailyChecklistItem,
    onToggle: () -> Unit
) {
    val categoryColor = when (item.category) {
        ChecklistCategory.SALAT -> Color(0xFF0D9488)
        ChecklistCategory.HYDRATION -> Color(0xFF0284C7)
        ChecklistCategory.NUTRITION -> Color(0xFFD97706)
        ChecklistCategory.HABIT -> Color(0xFF8B5CF6)
        ChecklistCategory.SLEEP -> Color(0xFF6366F1)
        ChecklistCategory.MINDFULNESS -> Color(0xFF10B981)
    }

    val icon = when (item.category) {
        ChecklistCategory.SALAT -> Icons.Default.Mosque
        ChecklistCategory.HYDRATION -> Icons.Default.WaterDrop
        ChecklistCategory.NUTRITION -> Icons.Default.Restaurant
        ChecklistCategory.HABIT -> Icons.Default.Star
        ChecklistCategory.SLEEP -> Icons.Default.Bedtime
        ChecklistCategory.MINDFULNESS -> Icons.Default.SelfImprovement
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("checklist_row_${item.id}"),
        shape = RoundedCornerShape(12.dp),
        color = if (item.isCompleted)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isCompleted,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(24.dp),
                colors = CheckboxDefaults.colors(checkedColor = categoryColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(categoryColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = categoryColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (item.subtitle.isNotBlank()) {
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Surface(
                color = categoryColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = item.category.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
