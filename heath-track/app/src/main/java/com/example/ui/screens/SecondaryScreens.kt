package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.data.local.HabitEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.UserProfileEntity
import com.example.ui.SecondaryScreen
import com.example.ui.VitaFlowViewModel
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.components.MinorGrowthBadge
import com.example.ui.components.SleepSummaryChart
import com.example.ui.components.VitaFlowTopBar
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ----------------------------------------------------------------------------
// 1. ONBOARDING SCREEN
// ----------------------------------------------------------------------------
@Composable
fun OnboardingScreen(
    viewModel: VitaFlowViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var step by remember { mutableIntStateOf(1) }
    var name by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }
    var ageText by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf("Moderate") }
    var goal by remember { mutableStateOf("Consistency & Natural Energy") }
    var dietaryPref by remember { mutableStateOf("Balanced Whole Foods") }
    var allergies by remember { mutableStateOf("") }
    var waterTargetText by remember { mutableStateOf("2500") }

    val age = ageText.toIntOrNull() ?: 0
    val isMinor = age in 1..17

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("onboarding_container"),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Indicator
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HeathTrack",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Step $step of 4",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { step / 4f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = TealPrimary
                )
            }

            // Step Content
            when (step) {
                1 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Consistency\nOver Restriction 🌿",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Welcome to a different kind of health tracker. HeathTrack helps you build mindful routines around nutrition, hydration, daily movement, and sleep — without guilt or extreme dieting.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 22.sp
                        )
                        MedicalDisclaimerCard()

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.openSecondaryScreen(SecondaryScreen.AUTH) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign In with Google / Email to Sync Data")
                        }

                        TextButton(
                            onClick = {
                                viewModel.completeOnboarding(
                                    UserProfileEntity(
                                        id = 1,
                                        name = "Friend",
                                        age = 25,
                                        heightCm = 172f,
                                        activityLevel = "Moderate",
                                        wellnessGoal = "Consistency & Natural Energy",
                                        dietaryPreference = "Balanced Whole Foods",
                                        allergies = "None",
                                        waterTargetMl = 2500,
                                        stepTarget = 8000,
                                        isOnboardingCompleted = true
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Explore Dashboard as Guest (Set Up Later)")
                        }
                    }
                }

                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Tell us about yourself",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("What should we call you?") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { ageText = it },
                            label = { Text("Your Age") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isMinor) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = AmberWarmth.copy(alpha = 0.15f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("🌱 Healthy Growth Mode Activated", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AmberWarmth)
                                    Text("During adolescent growth years, nutrient restriction or weight-loss deficits can harm healthy development. HeathTrack focuses exclusively on energy, hydration, sports, and balanced whole foods.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                3 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Daily Rhythm & Goals",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text("Select your primary wellness goal:", style = MaterialTheme.typography.bodySmall)
                        listOf("Consistency & Natural Energy", "Steady Hydration & Vitality", "Mindful Nutrition & Balanced Plate", "Daily Active Movement").forEach { g ->
                            FilterChip(
                                selected = goal == g,
                                onClick = { goal = g },
                                label = { Text(g) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Activity level:", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Light", "Moderate", "Active").forEach { lvl ->
                                FilterChip(
                                    selected = activityLevel.contains(lvl),
                                    onClick = { activityLevel = "$lvl Active" },
                                    label = { Text(lvl) }
                                )
                            }
                        }
                    }
                }

                4 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Dietary Preferences",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text("Choose your preferred style of eating:", style = MaterialTheme.typography.bodySmall)
                        listOf("Balanced Local Diet", "Vegetarian", "Halal Traditional", "High Fiber & Whole Foods").forEach { pref ->
                            FilterChip(
                                selected = dietaryPref == pref,
                                onClick = { dietaryPref = pref },
                                label = { Text(pref) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        OutlinedTextField(
                            value = allergies,
                            onValueChange = { allergies = it },
                            label = { Text("Allergies or Intolerances (e.g. Peanuts, Dairy, None)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = waterTargetText,
                            onValueChange = { waterTargetText = it },
                            label = { Text("Daily Hydration Target (ml)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (step > 1) {
                    OutlinedButton(onClick = { step-- }) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (step < 4) {
                            step++
                        } else {
                            viewModel.completeOnboarding(
                                UserProfileEntity(
                                    id = 1,
                                    name = name.ifBlank { "Friend" },
                                    age = age,
                                    heightCm = heightText.toFloatOrNull() ?: 172f,
                                    activityLevel = activityLevel,
                                    wellnessGoal = goal,
                                    dietaryPreference = dietaryPref,
                                    allergies = allergies,
                                    waterTargetMl = waterTargetText.toIntOrNull() ?: 2500,
                                    isMinor = isMinor,
                                    isOnboardingCompleted = true
                                )
                            )
                        }
                    },
                    modifier = Modifier.testTag("onboarding_next_button")
                ) {
                    Text(if (step == 4) "Get Started 🚀" else "Continue")
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// 2. WATER TRACKER SCREEN
// ----------------------------------------------------------------------------
@Composable
fun WaterTrackerScreen(
    viewModel: VitaFlowViewModel,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsStateWithLifecycle()
    val todayWater by viewModel.todayWaterTotalMl.collectAsStateWithLifecycle()
    val waterLogs by viewModel.todayWaterLogs.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val targetMl = profile?.waterTargetMl ?: 2500
    val progressRatio = (todayWater.toFloat() / targetMl).coerceIn(0f, 1f)
    val reversedLogs = remember(waterLogs) { waterLogs.reversed() }

    Scaffold(
        topBar = {
            VitaFlowTopBar(
                title = "Hydration Tracker",
                subtitle = "Mindful fluid balance",
                onBackClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Water Display Hero
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { progressRatio },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 14.dp,
                                color = WaterBlue,
                                trackColor = WaterBlue.copy(alpha = 0.2f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = WaterBlue, modifier = Modifier.size(32.dp))
                                Text(
                                    text = "${(progressRatio * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "${todayWater} / ${targetMl} ml",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (todayWater >= targetMl) "Daily hydration goal achieved! 💧" else "Keep a steady water flow throughout your day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Add Buttons
            item {
                Text(
                    text = "Quick Add Water",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(100, 250, 500, 750).forEach { ml ->
                        Button(
                            onClick = {
                                if (todayWater + ml >= targetMl && todayWater < targetMl) {
                                    com.example.util.HapticsHelper.performSuccess(context, isHapticEnabled)
                                } else {
                                    com.example.util.HapticsHelper.performClick(context, isHapticEnabled)
                                }
                                viewModel.logWater(ml)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("log_water_${ml}ml"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("+${ml}ml", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Hydration Tips Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = OceanSecondary.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("💧 Smart Hydration Tips", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = OceanSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Drink a glass of water first thing in the morning.\n• Keep a bottle at your workspace to sip gradually.\n• Mild thirst can sometimes mimic hunger cues.\n• Infuse with mint or lemon for natural flavor.", style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
                    }
                }
            }

            // Today's Logs Timeline
            item {
                Text(
                    text = "Today's Hydration Logs (${waterLogs.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            if (waterLogs.isEmpty()) {
                item {
                    Text("No water logged today yet. Tap a button above to record your first glass!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(reversedLogs, key = { it.id }) { log ->
                    val timeStr = remember(log.timestamp) {
                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.WaterDrop, contentDescription = null, tint = WaterBlue)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("+${log.amountMl} ml", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = {
                                com.example.util.HapticsHelper.performTick(context, isHapticEnabled)
                                viewModel.deleteWaterLog(log)
                            }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// 3. SLEEP TRACKER SCREEN
// ----------------------------------------------------------------------------
@Composable
fun SleepTrackerScreen(
    viewModel: VitaFlowViewModel,
    onBack: () -> Unit
) {
    val todaySleep by viewModel.todaySleepLog.collectAsStateWithLifecycle()
    val recentSleep by viewModel.recentSleepLogs.collectAsStateWithLifecycle()
    val sleepDays by viewModel.last7DaysSleepSummary.collectAsStateWithLifecycle()
    val isHealthConnectConnected by viewModel.healthRepository.isConnected.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    var bedtime by remember { mutableStateOf("23:15") }
    var wakeTime by remember { mutableStateOf("07:05") }
    var quality by remember { mutableStateOf("Restful") }

    Scaffold(
        topBar = {
            VitaFlowTopBar(
                title = "Sleep & Rest",
                subtitle = "Circadian rhythm & restoration",
                onBackClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SleepSummaryChart(
                    sleepDays = sleepDays,
                    isHealthConnectConnected = isHealthConnectConnected,
                    isSyncing = isSyncing,
                    onSyncClick = { viewModel.syncHealthData() }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(PurpleSleep.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NightlightRound, contentDescription = null, tint = PurpleSleep)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Last Night's Sleep", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                val sleep = todaySleep
                                if (sleep != null) {
                                    val dur = sleep.durationMinutes
                                    Text("${dur / 60}h ${dur % 60}m • ${sleep.quality}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                } else {
                                    Text("No sleep logged today yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Log or update your sleep schedule:", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = bedtime,
                                onValueChange = { bedtime = it },
                                label = { Text("Bedtime (e.g. 23:00)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = wakeTime,
                                onValueChange = { wakeTime = it },
                                label = { Text("Wake Time (e.g. 07:00)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Restful", "Moderate", "Light").forEach { q ->
                                FilterChip(
                                    selected = quality == q,
                                    onClick = { quality = q },
                                    label = { Text(q) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                val bedParts = bedtime.split(":")
                                val wakeParts = wakeTime.split(":")
                                val bedMins = if (bedParts.size == 2) (bedParts[0].toIntOrNull() ?: 23) * 60 + (bedParts[1].toIntOrNull() ?: 0) else 23 * 60
                                val wakeMins = if (wakeParts.size == 2) (wakeParts[0].toIntOrNull() ?: 7) * 60 + (wakeParts[1].toIntOrNull() ?: 0) else 7 * 60
                                val diff = if (wakeMins >= bedMins) wakeMins - bedMins else (1440 - bedMins) + wakeMins
                                val durationMins = if (diff in 60..900) diff else 480

                                viewModel.logSleep(
                                    durationMinutes = durationMins,
                                    bedtime = bedtime.ifBlank { "23:00" },
                                    wakeTime = wakeTime.ifBlank { "07:00" },
                                    quality = quality
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Sleep Record")
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🌙 Sleep Hygiene Essentials", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Keep a consistent wake-up time 7 days a week.\n• Dim screens 30-45 minutes before turning off lights.\n• Maintain a cool, well-ventilated sleeping room.\n• Avoid large heavy dinners within 2 hours of bed.", style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// 4. HABITS TRACKER SCREEN
// ----------------------------------------------------------------------------
@Composable
fun HabitsTrackerScreen(
    viewModel: VitaFlowViewModel,
    onBack: () -> Unit
) {
    val habits by viewModel.allHabits.collectAsStateWithLifecycle()
    val habitLogs by viewModel.todayHabitLogs.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    val doneHabitIds = remember(habitLogs) {
        habitLogs.filter { it.isCompleted }.map { it.habitId }.toSet()
    }

    Scaffold(
        topBar = {
            VitaFlowTopBar(
                title = "Healthy Habits",
                subtitle = "Consistency Over Restriction",
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Habit")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TealPrimary.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TealPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Daily micro-habits anchor your energy. Check off actions mindfully as you complete them.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (habits.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No habits added yet. Tap the '+' button below to create your first micro-habit!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(habits, key = { it.id }) { habit ->
                    val isDone = habit.id in doneHabitIds
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleHabit(habit.id, isDone) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDone) EmeraldSuccess.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledIconToggleButton(
                                checked = isDone,
                                onCheckedChange = { viewModel.toggleHabit(habit.id, isDone) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                if (isDone) {
                                    Icon(Icons.Default.Check, contentDescription = "Done", tint = Color.White)
                                } else {
                                    Icon(Icons.Outlined.Circle, contentDescription = "Pending")
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = habit.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (isDone) "Completed for today! 🌟" else "Target: ${habit.targetDaysPerWeek} days/week",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDone) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Habit") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Habit Title (e.g. 5 min stretching)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        viewModel.addCustomHabit(title.trim(), "habit")
                        showAddDialog = false
                    }
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ----------------------------------------------------------------------------
// 5. FOOD HISTORY SCREEN
// ----------------------------------------------------------------------------
@Composable
fun FoodHistoryScreen(
    viewModel: VitaFlowViewModel,
    onBack: () -> Unit
) {
    val allMeals by viewModel.allMealLogs.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            VitaFlowTopBar(
                title = "Food History & Variety",
                subtitle = "Mindful dietary patterns",
                onBackClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Variety & Frequency Insights", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Total Logged Meals: ${allMeals.size}", style = MaterialTheme.typography.bodyMedium)
                        val vegCount = allMeals.count { it.foodName.contains("Veg", ignoreCase = true) || it.foodName.contains("Salad", ignoreCase = true) }
                        Text("Vegetable / Salad Frequency: $vegCount times", style = MaterialTheme.typography.bodyMedium, color = EmeraldSuccess)
                        val fishChickenCount = allMeals.count { it.foodName.contains("Fish", ignoreCase = true) || it.foodName.contains("Chicken", ignoreCase = true) || it.foodName.contains("Egg", ignoreCase = true) }
                        Text("Quality Protein Sources: $fishChickenCount times", style = MaterialTheme.typography.bodyMedium, color = TealPrimary)
                    }
                }
            }

            item {
                Text("Recent Meals Logged", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }

            if (allMeals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No meals logged in your food history yet. Track your meals in the Nutrition tab to see variety insights.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(allMeals, key = { it.id }) { meal ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${meal.date} • ${meal.mealType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(meal.foodName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("${meal.portion} • ${meal.calories} kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// 6. REMINDERS SCREEN
// ----------------------------------------------------------------------------
@Composable
fun RemindersScreen(
    viewModel: VitaFlowViewModel,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsStateWithLifecycle()
    val reminders by viewModel.allReminders.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            VitaFlowTopBar(
                title = "Reminders & Alarms",
                subtitle = "Mindful scheduled prompts",
                onBackClick = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                com.example.util.HapticsHelper.performClick(context, isHapticEnabled)
                showAddDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "WorkManager Push Notifications",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Background schedules run automatically for daily hydration prompts (every 2.5 hrs) and mindful meal windows (08:30, 12:45, 19:30).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    com.example.util.HapticsHelper.performSuccess(context, isHapticEnabled)
                                    viewModel.triggerWorkManagerImmediateCheckIn("Water")
                                    android.widget.Toast.makeText(context, "Hydration notification scheduled via WorkManager!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("💧 Water Push", style = MaterialTheme.typography.labelSmall)
                            }

                            FilledTonalButton(
                                onClick = {
                                    com.example.util.HapticsHelper.performSuccess(context, isHapticEnabled)
                                    viewModel.triggerWorkManagerImmediateCheckIn("Meal")
                                    android.widget.Toast.makeText(context, "Meal notification scheduled via WorkManager!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🥗 Meal Push", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            items(reminders, key = { it.id }) { reminder ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(reminder.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            Text("${reminder.time} • ${reminder.repeatType} • ${reminder.type}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Switch(
                            checked = reminder.isEnabled,
                            onCheckedChange = {
                                com.example.util.HapticsHelper.performClick(context, isHapticEnabled)
                                viewModel.toggleReminder(reminder)
                            }
                        )
                        IconButton(onClick = {
                            com.example.util.HapticsHelper.performTick(context, isHapticEnabled)
                            viewModel.deleteReminder(reminder)
                        }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("09:00") }
        var type by remember { mutableStateOf("Water") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("New Reminder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time (HH:mm)") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Water", "Meal", "Exercise", "Sleep").forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        com.example.util.HapticsHelper.performSuccess(context, isHapticEnabled)
                        viewModel.saveReminder(
                            ReminderEntity(title = title.trim(), time = time.trim(), type = type, repeatType = "Daily")
                        )
                        showAddDialog = false
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ----------------------------------------------------------------------------
// 7. HEALTH CONNECT SCREEN
// ----------------------------------------------------------------------------
@Composable
fun HealthConnectScreen(
    viewModel: VitaFlowViewModel,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isHapticEnabled by viewModel.isHapticEnabled.collectAsStateWithLifecycle()
    val isConnected by viewModel.healthRepository.isConnected.collectAsStateWithLifecycle()
    val availabilityStatus = remember { viewModel.healthRepository.getAvailabilityStatus() }
    val isAvailable = remember { viewModel.healthRepository.isAvailable() }
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkHealthPermissions()
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.isNotEmpty()) {
            viewModel.toggleHealthConnect(true)
            viewModel.syncHealthData()
            android.widget.Toast.makeText(context, "Health Connect Permissions Granted! Syncing...", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.widget.Toast.makeText(context, "Permissions not granted in Health Connect.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    val launchPermissions = {
        try {
            permissionLauncher.launch(viewModel.healthRepository.getPermissionsToRequest())
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "Health Connect app is not installed or requires update.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            try {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
                ).apply { setPackage("com.android.vending") }
                context.startActivity(intent)
            } catch (_: Exception) {
                val intent = android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                )
                context.startActivity(intent)
            }
        }
    }

    Scaffold(
        topBar = {
            VitaFlowTopBar(
                title = "Health Connect & Google Fit",
                subtitle = "Google Health Data Hub",
                onBackClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sync Status", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    if (isConnected) "Connected to Health Connect" else "Sync Disconnected",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isConnected) EmeraldSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Device Status: $availabilityStatus",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAvailable) TealPrimary else MaterialTheme.colorScheme.error
                                )
                            }
                            Switch(
                                checked = isConnected,
                                onCheckedChange = { checked ->
                                    com.example.util.HapticsHelper.performClick(context, isHapticEnabled)
                                    if (checked) {
                                        if (isAvailable) {
                                            launchPermissions()
                                        } else {
                                            launchPermissions()
                                        }
                                    } else {
                                        viewModel.toggleHealthConnect(false)
                                    }
                                }
                            )
                        }

                        if (!isConnected || !isAvailable) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    com.example.util.HapticsHelper.performClick(context, isHapticEnabled)
                                    launchPermissions()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Connect & Grant Health Permissions")
                            }
                        }

                        if (!isAvailable) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Install Health Connect App from Play Store")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                com.example.util.HapticsHelper.performClick(context, isHapticEnabled)
                                viewModel.syncHealthData()
                                android.widget.Toast.makeText(context, "Syncing data...", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSyncing) "Syncing..." else "Sync All Today's Data Now")
                        }

                        syncStatusMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("About Health Connect & Google Health", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Health Connect is Android's unified, on-device health platform replacing the legacy Google Fit APIs. All synchronized health data (steps, distance, active calories, sleep, exercise sessions, heart rate) is handled strictly through official Health Connect APIs with zero fake data.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// 8. NOTIFICATION CENTER SCREEN
// ----------------------------------------------------------------------------
@Composable
fun NotificationCenterScreen(
    viewModel: VitaFlowViewModel,
    onBack: () -> Unit
) {
    val notifications by viewModel.allNotifications.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            VitaFlowTopBar(
                title = "Notification Center",
                subtitle = "In-app alerts & reminders",
                onBackClick = onBack,
                actions = {
                    TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                        Text("Read All")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (notifications.isEmpty()) {
                item {
                    Text("No notifications yet. Reminders and milestone updates will appear here.", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                items(notifications, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.isRead) MaterialTheme.colorScheme.surface else TealPrimary.copy(alpha = 0.08f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                Text(item.message, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.deleteNotification(item.id) }) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// 9. GLOBAL SEARCH SCREEN
// ----------------------------------------------------------------------------
@Composable
fun GlobalSearchScreen(
    viewModel: VitaFlowViewModel,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val allFoods by viewModel.allFoods.collectAsStateWithLifecycle()
    val allExercises by viewModel.allExercises.collectAsStateWithLifecycle()

    val matchedFoods = remember(query, allFoods) {
        if (query.isBlank()) emptyList() else allFoods.filter { it.name.contains(query, ignoreCase = true) }
    }
    val matchedExercises = remember(query, allExercises) {
        if (query.isBlank()) emptyList() else allExercises.filter { it.name.contains(query, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            VitaFlowTopBar(
                title = "Search HeathTrack",
                subtitle = "Foods, workouts & habits",
                onBackClick = onBack
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search foods, exercises, routines...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (matchedFoods.isNotEmpty()) {
                item { Text("Foods (${matchedFoods.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) }
                items(matchedFoods, key = { it.id }) { food ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(food.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("${food.calories} kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (matchedExercises.isNotEmpty()) {
                item { Text("Exercises (${matchedExercises.size})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)) }
                items(matchedExercises, key = { it.id }) { ex ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(ex.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("${ex.durationMinutes} min", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
