package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExerciseEntity
import com.example.ui.ActiveWorkoutState
import com.example.ui.SecondaryScreen
import com.example.ui.VitaFlowViewModel
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ActivityScreen(
    viewModel: VitaFlowViewModel,
    modifier: Modifier = Modifier,
) {
    val healthRecord by viewModel.healthSyncRecord.collectAsStateWithLifecycle()
    val allExercises by viewModel.allExercises.collectAsStateWithLifecycle()
    val todaySessions by viewModel.todayExerciseSessions.collectAsStateWithLifecycle()
    val workoutState by viewModel.workoutState.collectAsStateWithLifecycle()
    val isWorkoutActive by remember { derivedStateOf { workoutState.isActive } }
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf("All") }
    var selectedTimeFrame by remember { mutableStateOf("Today") }

    val categories = remember { listOf("All", "Walking", "Yoga", "Mobility", "Bodyweight", "Running", "Stretching") }

    val stepTarget = profile?.stepTarget ?: 8000
    val currentSteps = healthRecord?.steps ?: 0
    val distanceKm = remember(healthRecord?.distanceMeters) { (healthRecord?.distanceMeters ?: 0f) / 1000f }
    val activeCalories = healthRecord?.activeCalories ?: 0

    val filteredExercises = remember(allExercises, selectedCategory) {
        if (selectedCategory == "All") allExercises
        else allExercises.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    // Workout timer tick
    LaunchedEffect(Unit) {
        // We only care if it's active and not paused. 
        // Using a side effect in the ViewModel is better, but keeping local logic for now.
        // But we must NOT use workoutState from top level here to avoid recomposition loop.
        while (true) {
            val state = viewModel.workoutState.value
            if (state.isActive && !state.isPaused) {
                viewModel.tickWorkoutTimer()
            }
            delay(1.seconds)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("activity_screen_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        item(key = "activity_header", contentType = "header") {
            Column {
                Text(
                    text = "Activity & Movement",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Daily active movement • Consistency Over Restriction",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Time Frame Selector
        item(key = "activity_timeframe_selector", contentType = "filter") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Today", "This Week", "This Month").forEach { frame ->
                    FilterChip(
                        selected = selectedTimeFrame == frame,
                        onClick = { selectedTimeFrame = frame },
                        label = { Text(frame) }
                    )
                }
            }
        }

        // Steps & Movement Hero Card
        item(key = "activity_hero_card", contentType = "hero") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_hero_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Steps",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%,d".format(currentSteps),
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Target: %,d steps".format(stepTarget),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(TealPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                                contentDescription = "Steps",
                                tint = TealPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { (currentSteps.toFloat() / stepTarget).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = TealPrimary,
                        trackColor = TealPrimary.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ActivityMetricColumn(title = "Distance", value = "${String.format(java.util.Locale.getDefault(), "%.2f", distanceKm)} km")
                        ActivityMetricColumn(title = "Active Energy", value = "$activeCalories kcal")
                        val hr = healthRecord?.heartRateBpm
                        ActivityMetricColumn(title = "Heart Rate", value = if ((hr != null) && (hr > 0)) "$hr bpm" else "-- bpm")
                    }
                }
            }
        }

        // Health Connect Status Card
        item(key = "activity_health_connect_banner", contentType = "banner") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openSecondaryScreen(SecondaryScreen.HEALTH_CONNECT) }
                    .testTag("health_connect_banner"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = OceanSecondary.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        tint = OceanSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Google Health Connect",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sync steps, distance & active workouts smoothly",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { viewModel.syncHealthData() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("sync_now_button")
                    ) {
                        Text("Sync Now", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Active Workout Floating Banner (if workout in progress)
        if (isWorkoutActive && workoutState.exercise != null) {
            item(key = "active_workout_banner", contentType = "workout") {
                // The banner itself needs the full workoutState for the timer.
                // We keep the state collection here or pass it in.
                ActiveWorkoutBanner(
                    workoutState = workoutState,
                    onPauseResume = { viewModel.toggleWorkoutPause() }
                ) { viewModel.finishWorkout() }
            }
        }

        // Today's Completed Sessions
        if (todaySessions.isNotEmpty()) {
            item(key = "today_sessions_header", contentType = "header") {
                Text(
                    text = "Today's Completed Workouts (${todaySessions.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(
                items = todaySessions, 
                key = { "session_${it.id}" },
                contentType = { "session" }
            ) { session ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Done", tint = EmeraldSuccess)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session.exerciseName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                text = "${session.durationMinutes} min • ~${session.caloriesBurned} kcal burned",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Exercise Library
        item(key = "exercise_library_header", contentType = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Movement & Exercise Library",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories, key = { it }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
        }

        items(
            items = filteredExercises, 
            key = { "exercise_${it.id}" },
            contentType = { "exercise" }
        ) { exercise ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("exercise_card_${exercise.id}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${exercise.category} • ${exercise.difficulty} • ${exercise.durationMinutes} min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = { viewModel.startWorkout(exercise) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("start_exercise_button_${exercise.id}")
                        ) {
                            Text("Start", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exercise.instructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Safety: ${exercise.safetyNotes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }

        item(key = "activity_disclaimer_card", contentType = "disclaimer") {
            MedicalDisclaimerCard()
        }

        item(key = "activity_bottom_spacer", contentType = "spacer") {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun ActiveWorkoutBanner(
    workoutState: ActiveWorkoutState,
    onPauseResume: () -> Unit,
    onFinish: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_workout_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = TealPrimary.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Session In Progress ⏱️",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TealPrimary
                    )
                    workoutState.exercise?.let { exercise ->
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                val minutes = workoutState.elapsedSeconds / 60
                val seconds = workoutState.elapsedSeconds % 60
                Text(
                    text = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = TealPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            workoutState.exercise?.let { exercise ->
                Text(
                    text = exercise.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Safety: ${exercise.safetyNotes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberWarmth
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onPauseResume,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (workoutState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "Pause/Resume"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (workoutState.isPaused) "Resume" else "Pause")
                }

                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("finish_workout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Finish")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Finish")
                }
            }
        }
    }
}

@Composable
fun ActivityMetricColumn(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
    }
}
