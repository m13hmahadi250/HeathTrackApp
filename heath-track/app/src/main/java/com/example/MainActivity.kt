package com.example

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.reminder.ReminderScheduler
import com.example.ui.NavDestination
import com.example.ui.SecondaryScreen
import com.example.ui.VitaFlowViewModel
import com.example.ui.screens.*
import com.example.ui.theme.VitaFlowTheme
import com.example.util.FrameRenderProfiler
import com.example.util.HapticsHelper

class MainActivity : ComponentActivity() {

    private val viewModel: VitaFlowViewModel by viewModels()

    override fun onStart() {
        super.onStart()
        FrameRenderProfiler.start(this)
    }

    override fun onStop() {
        super.onStop()
        FrameRenderProfiler.stop(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val currentDest by viewModel.currentDestination.collectAsState()
            val secondaryScreen by viewModel.currentSecondaryScreen.collectAsState()
            val onboardingNeeded by viewModel.isOnboardingNeeded.collectAsState()
            val isHapticEnabled by viewModel.isHapticEnabled.collectAsState()

            val canGoBack by viewModel.canGoBack.collectAsState()

            BackHandler(enabled = canGoBack) {
                viewModel.popBackStack()
            }

            // Initialize notification channels and WorkManager schedules
            LaunchedEffect(Unit) {
                ReminderScheduler.createNotificationChannels(context)
                com.example.reminder.WorkManagerNotificationScheduler.scheduleAllHealthReminders(context)
                viewModel.rescheduleSalatAlarms(context)
            }

            LaunchedEffect(intent) {
                if (intent?.getStringExtra("NAVIGATE_TO") == "SALAT") {
                    viewModel.navigateTo(NavDestination.SALAT)
                }
            }

            // Notification permission request on launch (Android 13+)
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                viewModel.setNotificationsEnabled(isGranted)
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPermission) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            VitaFlowTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        onboardingNeeded || secondaryScreen == SecondaryScreen.ONBOARDING -> {
                            OnboardingScreen(viewModel = viewModel)
                        }

                        secondaryScreen == SecondaryScreen.AUTH -> {
                            AuthScreen(viewModel = viewModel, onBack = { viewModel.closeSecondaryScreen() })
                        }

                        secondaryScreen == SecondaryScreen.WATER_TRACKER -> {
                            WaterTrackerScreen(viewModel = viewModel, onBack = { viewModel.closeSecondaryScreen() })
                        }

                        secondaryScreen == SecondaryScreen.SLEEP_TRACKER -> {
                            SleepTrackerScreen(viewModel = viewModel, onBack = { viewModel.closeSecondaryScreen() })
                        }

                        secondaryScreen == SecondaryScreen.HABITS_TRACKER -> {
                            HabitsTrackerScreen(viewModel = viewModel, onBack = { viewModel.closeSecondaryScreen() })
                        }

                        secondaryScreen == SecondaryScreen.FOOD_HISTORY -> {
                            FoodHistoryScreen(viewModel = viewModel, onBack = { viewModel.closeSecondaryScreen() })
                        }

                        secondaryScreen == SecondaryScreen.REMINDERS_MANAGER -> {
                            RemindersScreen(viewModel = viewModel, onBack = { viewModel.closeSecondaryScreen() })
                        }

                        secondaryScreen == SecondaryScreen.HEALTH_CONNECT -> {
                            HealthConnectScreen(viewModel = viewModel, onBack = { viewModel.closeSecondaryScreen() })
                        }

                        secondaryScreen == SecondaryScreen.NOTIFICATIONS -> {
                            NotificationCenterScreen(viewModel = viewModel, onBack = { viewModel.closeSecondaryScreen() })
                        }

                        secondaryScreen == SecondaryScreen.GLOBAL_SEARCH -> {
                            GlobalSearchScreen(viewModel = viewModel, onBack = { viewModel.closeSecondaryScreen() })
                        }

                        else -> {
                            MainBottomNavigationScaffold(
                                viewModel = viewModel,
                                currentDest = currentDest,
                                isHapticEnabled = isHapticEnabled
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainBottomNavigationScaffold(
    viewModel: VitaFlowViewModel,
    currentDest: NavDestination,
    isHapticEnabled: Boolean = true
) {
    val context = LocalContext.current
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentDest == NavDestination.HOME,
                    onClick = {
                        HapticsHelper.performClick(context, isHapticEnabled)
                        viewModel.navigateTo(NavDestination.HOME)
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentDest == NavDestination.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_item_home")
                )

                NavigationBarItem(
                    selected = currentDest == NavDestination.NUTRITION,
                    onClick = {
                        HapticsHelper.performClick(context, isHapticEnabled)
                        viewModel.navigateTo(NavDestination.NUTRITION)
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentDest == NavDestination.NUTRITION) Icons.Filled.Restaurant else Icons.Outlined.Restaurant,
                            contentDescription = "Nutrition"
                        )
                    },
                    label = { Text("Nutrition", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_item_nutrition")
                )

                NavigationBarItem(
                    selected = currentDest == NavDestination.ACTIVITY,
                    onClick = {
                        HapticsHelper.performClick(context, isHapticEnabled)
                        viewModel.navigateTo(NavDestination.ACTIVITY)
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentDest == NavDestination.ACTIVITY) Icons.Filled.FitnessCenter else Icons.Outlined.FitnessCenter,
                            contentDescription = "Activity"
                        )
                    },
                    label = { Text("Activity", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_item_activity")
                )

                NavigationBarItem(
                    selected = currentDest == NavDestination.SALAT,
                    onClick = {
                        HapticsHelper.performClick(context, isHapticEnabled)
                        viewModel.navigateTo(NavDestination.SALAT)
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentDest == NavDestination.SALAT) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                            contentDescription = "Salat"
                        )
                    },
                    label = { Text("Salat", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_item_salat")
                )

                NavigationBarItem(
                    selected = currentDest == NavDestination.INSIGHTS,
                    onClick = {
                        HapticsHelper.performClick(context, isHapticEnabled)
                        viewModel.navigateTo(NavDestination.INSIGHTS)
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentDest == NavDestination.INSIGHTS) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "Insights"
                        )
                    },
                    label = { Text("Insights", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_item_insights")
                )

                NavigationBarItem(
                    selected = currentDest == NavDestination.PROFILE,
                    onClick = {
                        HapticsHelper.performClick(context, isHapticEnabled)
                        viewModel.navigateTo(NavDestination.PROFILE)
                    },
                    icon = {
                        Icon(
                            imageVector = if (currentDest == NavDestination.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = { Text("Profile", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("nav_item_profile")
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentDest,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_nav_transition"
        ) { destination ->
            when (destination) {
                NavDestination.HOME -> HomeScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                NavDestination.NUTRITION -> NutritionScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                NavDestination.ACTIVITY -> ActivityScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                NavDestination.SALAT -> SalatScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                NavDestination.INSIGHTS -> InsightsScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                NavDestination.PROFILE -> ProfileScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            }
        }
    }
}
