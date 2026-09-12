package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChecklistCategory
import com.example.data.model.DailyChecklistItem
import com.example.ui.NavDestination
import com.example.ui.SecondaryScreen
import com.example.ui.VitaFlowViewModel
import com.example.ui.components.ConsistencyStreakCard
import com.example.ui.components.DailyWellnessChecklistSection
import com.example.ui.components.DailyWellnessSummaryCard
import com.example.ui.components.HealthTrendsCard
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.components.MinorGrowthBadge
import com.example.ui.components.TaglineBanner
import com.example.ui.theme.*
import java.util.Calendar

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VitaFlowViewModel,
    modifier: Modifier = Modifier,
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val wellnessScore by viewModel.wellnessScore.collectAsStateWithLifecycle()
    val streak by viewModel.streakState.collectAsStateWithLifecycle()
    val weeklyStreakDays by viewModel.weeklyStreakDays.collectAsStateWithLifecycle()
    val weeklyWater by viewModel.weeklyWaterData.collectAsStateWithLifecycle()
    val weeklySteps by viewModel.weeklyStepsData.collectAsStateWithLifecycle()
    val todayWater by viewModel.todayWaterTotalMl.collectAsStateWithLifecycle()
    val todayMeals by viewModel.todayMealLogs.collectAsStateWithLifecycle()
    val healthRecord by viewModel.healthSyncRecord.collectAsStateWithLifecycle()
    val todaySleep by viewModel.todaySleepLog.collectAsStateWithLifecycle()
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val habitLogs by viewModel.todayHabitLogs.collectAsStateWithLifecycle()
    val dashboardConfig by viewModel.dashboardConfig.collectAsStateWithLifecycle()
    val dailyWellnessSummary by viewModel.dailyWellnessSummary.collectAsStateWithLifecycle()
    val dailyChecklist by viewModel.dailyChecklist.collectAsStateWithLifecycle()

    var showCustomizeDialog by remember { mutableStateOf(value = false) }
    var cardsVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cardsVisible = true
    }

    val greeting = remember {
        val hour = Calendar.getInstance()[Calendar.HOUR_OF_DAY]
        when {
            hour < 12 -> "Good Morning"
            hour < 17 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    val waterTargetGlasses = remember(profile?.waterTargetMl) { (profile?.waterTargetMl ?: 2000) / 250 }
    val waterGlasses = remember(todayWater) { todayWater / 250 }
    val stepTarget = profile?.stepTarget ?: 8000
    val currentSteps = healthRecord?.steps ?: 0
    val activeMinutes = healthRecord?.activeCalories ?: 0 // Simplified mapping for prototype
    val activeMinTarget = 60
    
    val sleepDurationStr = remember(todaySleep) {
        if (todaySleep != null) {
            val dur = todaySleep?.durationMinutes ?: 0
            val hours = dur / 60
            val mins = dur % 60
            "${hours}h ${mins}m"
        } else {
            "0h 0m"
        }
    }

    val doneHabitIds = remember(habitLogs) {
        habitLogs.asSequence().filter { it.isCompleted }.map { it.habitId }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(greeting, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(profile?.name ?: "Guest", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { showCustomizeDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Customize Dashboard")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("home_screen_scroll"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Wellness Score
            if (dashboardConfig["WellnessScore"] == true) {
                item(key = "wellness_score", contentType = "score") {
                    WellnessScoreCard(score = wellnessScore, goalProgress = wellnessScore / 100f)
                }
            }

                // Consistency Streak Counter (persisted in Room)
                if (dashboardConfig["ConsistencyStreak"] == true) {
                    item(key = "consistency_streak_card", contentType = "streak") {
                        ConsistencyStreakCard(
                            streak = streak,
                            weeklyDays = weeklyStreakDays
                        )
                    }
                }

                // Daily Wellness Overview (combining Salat, Water, Steps, Habits)
                item(key = "daily_wellness_summary_card", contentType = "summary") {
                    DailyWellnessSummaryCard(summary = dailyWellnessSummary)
                }

                // Health Trends Section (7-Day Room Database History for Hydration & Activity)
                if (dashboardConfig["HealthTrends"] == true) {
                    item(key = "health_trends_card", contentType = "trends") {
                        HealthTrendsCard(
                            weeklyWater = weeklyWater,
                            waterTargetMl = profile?.waterTargetMl ?: 2500,
                            weeklySteps = weeklySteps,
                            stepTarget = stepTarget
                        )
                    }
                }

                // Dual Row for Steps & Active Minutes
                item(key = "steps_active_row", contentType = "metrics") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (dashboardConfig["Steps"] == true) {
                            Box(modifier = Modifier.weight(1f)) {
                                CircularMetricCard(
                                    title = "Steps",
                                    value = currentSteps.toString(),
                                    target = "/$stepTarget",
                                    progress = currentSteps.toFloat() / stepTarget.toFloat().coerceAtLeast(1f),
                                    color = TealPrimary,
                                    icon = Icons.AutoMirrored.Filled.DirectionsWalk
                                )
                            }
                        }
                        if (dashboardConfig["ActiveMinutes"] == true) {
                            Box(modifier = Modifier.weight(1f)) {
                                CircularMetricCard(
                                    title = "Active Mins",
                                    value = "$activeMinutes",
                                    target = "/$activeMinTarget m",
                                    progress = activeMinutes.toFloat() / activeMinTarget.toFloat().coerceAtLeast(1f),
                                    color = CoralAccent,
                                    icon = Icons.Default.LocalFireDepartment
                                )
                            }
                        }
                    }
                }

                // Water & Sleep
                item(key = "water_sleep_row", contentType = "metrics") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (dashboardConfig["Water"] == true) {
                            Box(modifier = Modifier.weight(1f)) {
                                CircularMetricCard(
                                    title = "Water Intake",
                                    value = "$waterGlasses",
                                    target = "/$waterTargetGlasses gl",
                                    progress = waterGlasses.toFloat() / waterTargetGlasses.toFloat().coerceAtLeast(1f),
                                    color = WaterBlue,
                                    icon = Icons.Default.LocalDrink
                                )
                            }
                        }
                        if (dashboardConfig["Sleep"] == true) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.openSecondaryScreen(SecondaryScreen.SLEEP_TRACKER) }
                            ) {
                                CircularMetricCard(
                                    title = "Sleep",
                                    value = sleepDurationStr,
                                    target = "",
                                    progress = todaySleep?.let { it.durationMinutes.toFloat() / 480f } ?: 0f,
                                    color = PurpleSleep,
                                    icon = Icons.Default.Bedtime
                                )
                            }
                        }
                    }
                }

                // Meals Status
                if (dashboardConfig["Meals"] == true) {
                    item(key = "meals_status_card", contentType = "status") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(AmberLight.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = AmberWarmth)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Meal Log Status", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("${todayMeals.size} meals logged today", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Habits 
                if (dashboardConfig["Habits"] == true) {
                    item(key = "habits_title_header", contentType = "header") {
                        Text("Daily Habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(
                        items = habits, 
                        key = { "habit_${it.id}" },
                        contentType = { "habit" }
                    ) { habit ->
                        val isDone = habit.id in doneHabitIds
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                    contentDescription = null,
                                    tint = if (isDone) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(habit.title, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                // Salat / Prayer Glance Card
                item(key = "salat_glance_card", contentType = "salat") {
                    HomeSalatGlanceCard(viewModel = viewModel)
                }

                // Daily Wellness & Salat Checklist
                item(key = "daily_wellness_checklist_header", contentType = "header") {
                    Text(
                        text = "Daily Wellness & Salat Checklist",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(
                    items = dailyChecklist,
                    key = { "checklist_${it.id}" },
                    contentType = { "checklist" }
                ) { item ->
                    val onToggle = remember(item, viewModel) {
                        {
                            when (item.category) {
                                ChecklistCategory.SALAT -> {
                                    item.actionRoute?.let { prayerName ->
                                        viewModel.togglePrayerCompletion(prayerName)
                                    }
                                }
                                ChecklistCategory.HABIT -> {
                                    item.actionRoute?.toLongOrNull()?.let { habitId ->
                                        viewModel.toggleHabit(habitId, item.isCompleted)
                                    }
                                }
                                ChecklistCategory.HYDRATION -> {
                                    viewModel.logWater(250)
                                }
                                ChecklistCategory.NUTRITION -> {
                                    viewModel.openSecondaryScreen(SecondaryScreen.FOOD_HISTORY)
                                }
                                ChecklistCategory.SLEEP -> {
                                    viewModel.openSecondaryScreen(SecondaryScreen.SLEEP_TRACKER)
                                }
                                ChecklistCategory.MINDFULNESS -> {
                                    viewModel.navigateTo(NavDestination.SALAT)
                                }
                            }
                            Unit // Ensure Unit return type
                        }
                    }
                    com.example.ui.components.DailyChecklistRow(
                        item = item,
                        onToggle = onToggle
                    )
                }

                item(key = "medical_disclaimer_item", contentType = "disclaimer") {
                    MedicalDisclaimerCard()
                }
                item(key = "bottom_spacer_item", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }

    if (showCustomizeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomizeDialog = false },
            title = { Text("Customize Dashboard") },
            text = {
                Column {
                    dashboardConfig.forEach { (key, isVisible) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(key.replace(Regex("([a-z])([A-Z]+)"), "$1 $2"))
                            Switch(
                                checked = isVisible,
                                onCheckedChange = { viewModel.toggleDashboardCard(key, it) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomizeDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
fun WellnessScoreCard(score: Int, goalProgress: Float) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val stroke = remember(density) {
        with(density) { Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round) }
    }
    val color = remember(score) {
        when {
            score >= 80 -> EmeraldSuccess
            score >= 50 -> AmberWarmth
            else -> CoralAccent
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                val animatedProgress by animateFloatAsState(targetValue = goalProgress.coerceIn(0f, 1f), animationSpec = tween(400), label = "wellness_progress")
                Canvas(modifier = Modifier.size(100.dp)) {
                    drawArc(
                        color = color.copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = stroke
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        style = stroke
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$score", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = color))
                    Text("Score", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column {
                Text("Daily Wellness", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Great job keeping up with your goals! Keep pushing forward.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CircularMetricCard(
    title: String,
    value: String,
    target: String,
    progress: Float,
    color: Color,
    icon: ImageVector
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val stroke = remember(density) {
        with(density) { Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                val animatedProgress by animateFloatAsState(targetValue = progress.coerceIn(0f, 1f), animationSpec = tween(400), label = "metric_progress")
                Canvas(modifier = Modifier.size(80.dp)) {
                    drawArc(
                        color = color.copy(alpha = 0.2f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = stroke
                    )
                    drawArc(
                        color = color,
                        startAngle = 135f,
                        sweepAngle = animatedProgress * 270f,
                        useCenter = false,
                        style = stroke
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    if (target.isNotEmpty()) {
                        Text(target, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun HomeSalatGlanceCard(
    viewModel: VitaFlowViewModel,
    modifier: Modifier = Modifier,
) {
    val salatConfig by viewModel.salatConfig.collectAsStateWithLifecycle()
    val completions by viewModel.todaySalatCompletions.collectAsStateWithLifecycle()
    val completedToday = remember(completions) { completions.values.count { it } }

    if (salatConfig.isSalatEnabled) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clickable { viewModel.navigateTo(NavDestination.SALAT) }
                .testTag("home_salat_glance_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Salat",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val currentPrayer by viewModel.currentPrayer.collectAsStateWithLifecycle()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Salat • ${currentPrayer?.displayName ?: "Prayer"}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$completedToday/5",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    val prayerStatus by viewModel.prayerStatus.collectAsStateWithLifecycle()
                    Text(
                        text = "Next: ${prayerStatus?.nextPrayer?.displayName ?: "Upcoming"} in ${prayerStatus?.timeRemainingNextFormatted ?: "--"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Salat Schedule",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

