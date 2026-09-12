package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.SalatConfigEntity
import com.example.salat.*
import com.example.ui.VitaFlowViewModel
import com.example.ui.components.SalatQiblaCompass
import com.example.util.HapticsHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalatScreen(
    viewModel: VitaFlowViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val salatConfig by viewModel.salatConfig.collectAsStateWithLifecycle()
    val schedule by viewModel.todaySalatSchedule.collectAsStateWithLifecycle()
    // val prayerStatus by viewModel.prayerStatus.collectAsStateWithLifecycle() // Removed from top level to prevent per-second recomposition
    val completions by viewModel.todaySalatCompletions.collectAsStateWithLifecycle()
    val weeklySummary by viewModel.weeklySalatSummary.collectAsStateWithLifecycle()
    val isDetectingLocation by viewModel.isDetectingLocation.collectAsStateWithLifecycle()
    val locationError by viewModel.locationError.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isHaptic = remember(userProfile) { userProfile?.hapticFeedbackEnabled ?: true }

    val hijriDate by viewModel.todayHijriDate.collectAsStateWithLifecycle()
    val qiblaData by viewModel.qiblaData.collectAsStateWithLifecycle()
    val historyRecords by viewModel.prayerHistoryRecords.collectAsStateWithLifecycle()
    val historyStats by viewModel.prayerHistoryStats.collectAsStateWithLifecycle()
    val historyFilter by viewModel.prayerHistoryFilter.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Schedule, 1: Qibla, 2: History
    var showSettingsDialog by remember { mutableStateOf(value = false) }
    var showLocationPicker by remember { mutableStateOf(value = false) }

    val prayerItems = remember(schedule.fajr, schedule.sunrise, schedule.dhuhr, schedule.asr, schedule.maghrib, schedule.isha) {
        listOf(
            PrayerScheduleItem(
                prayer = PrayerName.FAJR,
                startTime24 = schedule.fajr,
                endTime24 = PrayerTimeCalculator.subtractMinutes(schedule.sunrise, 1),
                description = "Dawn to Sunrise",
            ),
            PrayerScheduleItem(
                prayer = PrayerName.DHUHR,
                startTime24 = schedule.dhuhr,
                endTime24 = PrayerTimeCalculator.subtractMinutes(schedule.asr, 1),
                description = "Noon to Asr",
            ),
            PrayerScheduleItem(
                prayer = PrayerName.ASR,
                startTime24 = schedule.asr,
                endTime24 = PrayerTimeCalculator.subtractMinutes(schedule.maghrib, 1),
                description = "Afternoon to Sunset",
            ),
            PrayerScheduleItem(
                prayer = PrayerName.MAGHRIB,
                startTime24 = schedule.maghrib,
                endTime24 = PrayerTimeCalculator.subtractMinutes(schedule.isha, 1),
                description = "Sunset to Twilight",
            ),
            PrayerScheduleItem(
                prayer = PrayerName.ISHA,
                startTime24 = schedule.isha,
                endTime24 = schedule.fajr,
                description = "Night to Dawn",
            ),
        )
    }
    val makruhZawalStart = remember(schedule.dhuhr) { PrayerTimeCalculator.subtractMinutes(schedule.dhuhr, 40) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val granted = (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        if (granted) {
            viewModel.detectDeviceLocation(context)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Salat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${salatConfig.locationName} • ${hijriDate.formatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            HapticsHelper.performClick(context, isHaptic)
                            showLocationPicker = true
                        },
                        modifier = Modifier.testTag("salat_location_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = "Change Location",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            HapticsHelper.performClick(context, isHaptic)
                            showSettingsDialog = true
                        },
                        modifier = Modifier.testTag("salat_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Salat Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            // Section Switcher Tabs: Schedule | Qibla Compass | Prayer History
            item(key = "salat_section_tabs", contentType = "tabs") {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("salat_section_selector")
                ) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = {
                            HapticsHelper.performClick(context, isHaptic)
                            selectedTab = 0
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("salat_tab_schedule")
                    ) {
                        Text("Schedule")
                    }

                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = {
                            HapticsHelper.performClick(context, isHaptic)
                            selectedTab = 1
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        icon = { Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("salat_tab_qibla")
                    ) {
                        Text("Qibla")
                    }

                    SegmentedButton(
                        selected = selectedTab == 2,
                        onClick = {
                            HapticsHelper.performClick(context, isHaptic)
                            selectedTab = 2
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("salat_tab_history")
                    ) {
                        Text("History")
                    }
                }
            }

            // Islamic Date & Gregorian Date Header Card
            item(key = "islamic_date_banner", contentType = "banner") {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = hijriDate.formatted,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Bangladesh",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Location Error Alert if any
            if (locationError != null) {
                item(key = "location_error", contentType = "error") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = locationError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // TAB 0: SCHEDULE
                    // Offline calculation & method banner
                    item(key = "status_banner", contentType = "banner") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Offline Astronomical Calculator Active",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = salatConfig.madhab,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Hero Card: Current Prayer & Live Countdown
                    item(key = "hero_active_prayer", contentType = "hero") {
                        ActivePrayerHeroCard(
                            viewModel = viewModel,
                            schedule = schedule
                        ) { prayer ->
                            HapticsHelper.performClick(context, isHaptic)
                            viewModel.togglePrayerCompletion(prayer.displayName)
                        }
                    }

                    // Sunrise & Sunset Header Card (Sunrise & Sunset in 12-hour format)
                    item(key = "sunrise_sunset_banner", contentType = "banner") {
                        val currentPrayer by viewModel.currentPrayer.collectAsStateWithLifecycle()
                        val isActive = currentPrayer == PrayerName.SUNRISE
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sunrise_sunset_card"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) Color(0xFF006D44) else Color(0xFF008955)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sun Icon with soft glow halo
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WbSunny,
                                        contentDescription = "Sun",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                // Two columns: Sunrise & Sunset
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Sunrise Column
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = PrayerTimeCalculator.formatTo12Hour(schedule.sunrise),
                                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Sunrise",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.95f)
                                        )
                                    }

                                    // Vertical Divider
                                    Box(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .width(1.dp)
                                            .background(Color.White.copy(alpha = 0.35f))
                                    )

                                    // Sunset Column
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = PrayerTimeCalculator.formatTo12Hour(schedule.maghrib),
                                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Sunset",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.95f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Today's Complete Prayer Schedule Card Header
                    item(key = "schedule_header", contentType = "header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Daily Salat Times",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Start – End windows for all daily prayers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val completedCount = completions.values.count { it }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "$completedCount of 5 Done",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Prayer items with Start Time - End Time ranges in 12-hour format
                    items(
                        items = prayerItems,
                        key = { it.prayer.name },
                        contentType = { "prayer" }
                    ) { item ->
                        val currentPrayer by viewModel.currentPrayer.collectAsStateWithLifecycle()
                        val nextPrayer by viewModel.nextPrayer.collectAsStateWithLifecycle()
                        val isActive = currentPrayer == item.prayer
                        val isNext = nextPrayer == item.prayer
                        PrayerScheduleRow(
                            item = item,
                            isActive = isActive,
                            isNext = isNext,
                            isCompleted = completions[item.prayer.displayName] == true
                        ) {
                            if (item.isObligatory) {
                                HapticsHelper.performClick(context, isHaptic)
                                viewModel.togglePrayerCompletion(item.prayer.displayName)
                            }
                        }
                    }

                    // Makruh / Prohibited Times Note (English, 12-hour format)
                    item(key = "makruh_time_note", contentType = "note") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("makruh_times_card"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Makruh (Prohibited) Time",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Zawal Makruh: ${PrayerTimeCalculator.formatTo12Hour(makruhZawalStart)} – ${PrayerTimeCalculator.formatTo12Hour(schedule.dhuhr)} (Until Dhuhr)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "Makruh: ${PrayerTimeCalculator.formatTo12Hour(makruhZawalStart)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Weekly Completion Tracker Card
                    item(key = "weekly_history", contentType = "weekly") {
                        WeeklyPrayerTrackerCard(
                            summaries = weeklySummary
                        ) {
                            HapticsHelper.performClick(context, isHaptic)
                        }
                    }

                    // Settings & Customization Card
                    item(key = "quick_actions_card", contentType = "actions") {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Preferences & Reminders",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Prayer Notifications",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (salatConfig.notifyAtTime || salatConfig.notifyBeforeTime)
                                                "Active • Sound: ${salatConfig.soundOption} • ${if (salatConfig.vibrationEnabled) "Vibration on" else "Vibration off"}"
                                            else "Disabled",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Switch(
                                        checked = salatConfig.notifyAtTime || salatConfig.notifyBeforeTime,
                                        onCheckedChange = { enabled ->
                                            HapticsHelper.performClick(context, isHaptic)
                                            viewModel.toggleSalatNotifications(enabled, context)
                                        },
                                        modifier = Modifier.testTag("salat_notifications_toggle")
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            HapticsHelper.performClick(context, isHaptic)
                                            showLocationPicker = true
                                        },
                                        modifier = Modifier.testTag("change_city_button")
                                    ) {
                                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Change City")
                                    }

                                    Button(
                                        onClick = {
                                            HapticsHelper.performClick(context, isHaptic)
                                            showSettingsDialog = true
                                        },
                                        modifier = Modifier.testTag("open_salat_settings_button")
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Settings & Reminders")
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: QIBLA DIRECTION COMPASS
                    item(key = "qibla_compass_tab_view", contentType = "qibla") {
                        SalatQiblaCompass(
                            qiblaData = qiblaData,
                            hapticEnabled = isHaptic,
                            onOpenLocationPicker = { showLocationPicker = true }
                        )
                    }
                }
                2 -> {
                    // TAB 2: PRAYER HISTORY & STATISTICS (Unrolled for performance)
                    item(key = "prayer_history_filter", contentType = "filter") {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SegmentedButton(
                                selected = historyFilter == PrayerHistoryFilter.SEVEN_DAYS,
                                onClick = {
                                    HapticsHelper.performClick(context, isHaptic)
                                    viewModel.setPrayerHistoryFilter(PrayerHistoryFilter.SEVEN_DAYS)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                            ) { Text("7 Days") }
                            SegmentedButton(
                                selected = historyFilter == PrayerHistoryFilter.THIRTY_DAYS,
                                onClick = {
                                    HapticsHelper.performClick(context, isHaptic)
                                    viewModel.setPrayerHistoryFilter(PrayerHistoryFilter.THIRTY_DAYS)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                            ) { Text("30 Days") }
                            SegmentedButton(
                                selected = historyFilter == PrayerHistoryFilter.ALL_TIME,
                                onClick = {
                                    HapticsHelper.performClick(context, isHaptic)
                                    viewModel.setPrayerHistoryFilter(PrayerHistoryFilter.ALL_TIME)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                            ) { Text("All Time") }
                        }
                    }

                    item(key = "prayer_history_stats", contentType = "stats") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Prayer Consistency",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${historyStats.totalCompletedPrayers} of ${historyStats.totalPossiblePrayers} completed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "${historyStats.overallCompletionRate}%",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { (historyStats.overallCompletionRate / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    item(key = "prayer_history_title", contentType = "header") {
                        Text(
                            text = "Daily Prayer Records",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (historyRecords.isEmpty()) {
                        item(key = "history_empty") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "No prayer history recorded yet.\nMark prayers as completed on the Today tab to build your real record!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(
                            items = historyRecords,
                            key = { "history_${it.date}" },
                            contentType = { "history" }
                        ) { record ->
                            com.example.ui.components.DayPrayerRecordRow(
                                record = record
                            ) { prayerName ->
                                HapticsHelper.performClick(context, isHaptic)
                                viewModel.togglePrayerCompletion(prayerName, record.date)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showSettingsDialog) {
        SalatSettingsDialog(
            config = salatConfig,
            onDismiss = { showSettingsDialog = false },
            onUpdateMethod = { method ->
                viewModel.updateSalatCalculationMethod(method, context)
            },
            onUpdateMadhab = { madhab ->
                viewModel.updateSalatMadhab(madhab, context)
            },
            onResetRecommended = {
                viewModel.resetSalatToRecommended(context)
            },
            onUpdateNotificationPreferences = { atTime, beforeTime, minutes, sound, vibration ->
                viewModel.updateSalatNotificationPreferences(atTime, beforeTime, minutes, sound, vibration, context)
            }
        ) { prayer, enabled ->
            viewModel.toggleIndividualPrayerReminder(prayer, enabled, context)
        }
    }

    if (showLocationPicker) {
        SalatLocationDialog(
            currentCity = salatConfig.locationName,
            isDetecting = isDetectingLocation,
            onDismiss = { showLocationPicker = false },
            onDetectLocation = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        ) { city ->
            viewModel.updateSalatLocation(
                city = city.city,
                country = city.country,
                lat = city.latitude,
                lon = city.longitude,
                tzId = city.timeZoneId,
                context = context,
            )
            showLocationPicker = false
        }
    }
}

data class PrayerScheduleItem(
    val prayer: PrayerName,
    val startTime24: String,
    val endTime24: String,
    val description: String,
    val isObligatory: Boolean = true,
) {
    val startTime12: String get() = PrayerTimeCalculator.formatTo12Hour(startTime24)
    val endTime12: String get() = PrayerTimeCalculator.formatTo12Hour(endTime24)
}

@Composable
fun ActivePrayerHeroCard(
    viewModel: VitaFlowViewModel,
    schedule: SalatSchedule,
    onToggleCompletion: (PrayerName) -> Unit,
) {
    val status by viewModel.prayerStatus.collectAsStateWithLifecycle()
    val completions by viewModel.todaySalatCompletions.collectAsStateWithLifecycle()
    
    val currentPrayer = status?.currentPrayer ?: PrayerName.FAJR
    val nextPrayer = status?.nextPrayer ?: PrayerName.DHUHR
    val isCompleted = completions[currentPrayer.displayName] == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_prayer_hero_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "CURRENT PRAYER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentPrayer.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = currentPrayer.arabicName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }

                // Next Prayer Countdown Pill
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Next: ${nextPrayer.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // This text updates every second
                        val timeRemaining = status?.timeRemainingNextFormatted ?: "--"
                        Text(
                            text = timeRemaining,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = PrayerTimeCalculator.formatTo12Hour(status?.nextPrayerTime ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current prayer window ending countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prayer Window Progress",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                // This text updates every second
                val endsIn = status?.currentPrayerEndsInFormatted ?: "--"
                Text(
                    text = "Period ends in $endsIn",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // This progress indicator updates every second
            val progress = status?.progress ?: 0.5f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Completion Button (for obligatory prayers) or Next Prayer Info
            if (currentPrayer.isObligatory) {
                Button(
                    onClick = { onToggleCompletion(currentPrayer) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_toggle_completion_button"),
                    colors = if (isCompleted) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCompleted) "✓ ${currentPrayer.displayName} Completed" else "Mark ${currentPrayer.displayName} Completed",
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Next Obligatory: Dhuhr at ${PrayerTimeCalculator.formatTo12Hour(schedule.dhuhr)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PrayerScheduleRow(
    item: PrayerScheduleItem,
    isActive: Boolean,
    isNext: Boolean,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    val containerColor = when {
        isActive -> Color(0xFF008955) // Emerald Green highlight matching Muslims Day
        isNext -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }

    val contentColor = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isActive) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.isObligatory) { onToggle() }
            .testTag("prayer_row_${item.prayer.name}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActive) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val icon = getPrayerIcon(item.prayer)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.prayer.displayName,
                    tint = if (isActive) Color.White else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Prayer names (English + NOW/NEXT badge on same single line)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.prayer.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1
                    )

                    if (isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isNext) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "NEXT",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${item.prayer.arabicName} • ${item.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = subTextColor,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 12-Hour Time Range: Start – End (e.g. 4:23 AM – 5:37 AM)
            Text(
                text = "${item.startTime12} – ${item.endTime12}",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Checkbox for obligatory prayers
            if (item.isObligatory) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("prayer_checkbox_${item.prayer.name}"),
                    colors = CheckboxDefaults.colors(
                        checkedColor = if (isActive) Color.White else MaterialTheme.colorScheme.primary,
                        checkmarkColor = if (isActive) Color(0xFF008955) else MaterialTheme.colorScheme.onPrimary,
                        uncheckedColor = if (isActive) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline
                    )
                )
            } else {
                Spacer(modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun WeeklyPrayerTrackerCard(
    summaries: List<SalatDaySummary>,
    onDayClick: (SalatDaySummary) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDayClick(summaries.last()) }
            .testTag("weekly_salat_tracker_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Prayer Consistency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                val totalCompleted = summaries.sumOf { it.completedCount }
                val totalPossible = summaries.size * 5
                val percent = if (totalPossible > 0) ((totalCompleted * 100) / totalPossible) else 0

                Text(
                    text = "$totalCompleted / $totalPossible ($percent%)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                summaries.forEach { summary ->
                    val isToday = summary.isToday
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = summary.dayOfWeek,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Completion dots (5 daily prayers)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        summary.completedCount == 5 -> MaterialTheme.colorScheme.primary
                                        summary.completedCount > 0 -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = summary.completedCount.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    summary.completedCount == 5 -> MaterialTheme.colorScheme.onPrimary
                                    summary.completedCount > 0 -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "/5",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalatSettingsDialog(
    config: SalatConfigEntity,
    onDismiss: () -> Unit,
    onUpdateMethod: (String) -> Unit,
    onUpdateMadhab: (String) -> Unit,
    onResetRecommended: () -> Unit,
    onUpdateNotificationPreferences: (Boolean, Boolean, Int, String, Boolean) -> Unit,
    onToggleIndividualPrayer: (PrayerName, Boolean) -> Unit
) {
    var selectedMethod by remember { mutableStateOf(config.calculationMethod) }
    var selectedMadhab by remember { mutableStateOf(config.madhab) }
    var notifyAtTime by remember { mutableStateOf(config.notifyAtTime) }
    var notifyBeforeTime by remember { mutableStateOf(config.notifyBeforeTime) }
    var beforeMinutes by remember { mutableIntStateOf(config.notifyBeforeMinutes) }
    var soundOption by remember { mutableStateOf(config.soundOption) }
    var vibrationEnabled by remember { mutableStateOf(config.vibrationEnabled) }

    var fajrReminder by remember { mutableStateOf(config.fajrEnabled) }
    var dhuhrReminder by remember { mutableStateOf(config.dhuhrEnabled) }
    var asrReminder by remember { mutableStateOf(config.asrEnabled) }
    var maghribReminder by remember { mutableStateOf(config.maghribEnabled) }
    var ishaReminder by remember { mutableStateOf(config.ishaEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Salat Settings & Notifications",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Calculation Method Section
                Text(
                    text = "Calculation Method",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                CalculationMethods.ALL_METHODS.forEach { method ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMethod = method }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = method,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedMethod == method) FontWeight.Bold else FontWeight.Normal
                            )
                            if (method == CalculationMethods.KARACHI) {
                                Text(
                                    text = "Recommended for Bangladesh, Pakistan, India",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Madhab Section
                Text(
                    text = "Juridical Method (Asr Calculation)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                MadhabOptions.ALL_MADHABS.forEach { madhab ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMadhab = madhab }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedMadhab == madhab,
                            onClick = { selectedMadhab = madhab }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = madhab,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedMadhab == madhab) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = if (madhab == MadhabOptions.HANAFI)
                                    "Shadow factor = 2 (Standard in Bangladesh)"
                                else "Shadow factor = 1 (Earlier Asr)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Notifications Section
                Text(
                    text = "Prayer Notification Timing",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify when prayer begins", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = notifyAtTime, onCheckedChange = { notifyAtTime = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify before prayer time", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = notifyBeforeTime, onCheckedChange = { notifyBeforeTime = it })
                }

                if (notifyBeforeTime) {
                    Text(
                        text = "Alert $beforeMinutes minutes before each prayer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20).forEach { mins ->
                            FilterChip(
                                selected = beforeMinutes == mins,
                                onClick = { beforeMinutes = mins },
                                label = { Text("${mins}m") }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Notification Sound & Vibration
                Text(
                    text = "Alert Sound & Vibration",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Sound Option",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("Adhan", "Default", "Soft Chime", "Silent").forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { soundOption = option }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = soundOption == option,
                                onClick = { soundOption = option }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (option) {
                                    "Adhan" -> "Adhan (Call to Prayer)"
                                    "Default" -> "Default Alarm Tone"
                                    "Soft Chime" -> "Soft Chime"
                                    "Silent" -> "Silent (No Audio)"
                                    else -> option
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (soundOption == option) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibration", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = vibrationEnabled, onCheckedChange = { vibrationEnabled = it })
                }

                HorizontalDivider()

                // Individual Prayer Reminders
                Text(
                    text = "Individual Prayer Reminders",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                PrayerReminderToggleRow(name = "Fajr", isChecked = fajrReminder) { fajrReminder = it }
                PrayerReminderToggleRow(name = "Dhuhr", isChecked = dhuhrReminder) { dhuhrReminder = it }
                PrayerReminderToggleRow(name = "Asr", isChecked = asrReminder) { asrReminder = it }
                PrayerReminderToggleRow(name = "Maghrib", isChecked = maghribReminder) { maghribReminder = it }
                PrayerReminderToggleRow(name = "Isha", isChecked = ishaReminder) { ishaReminder = it }

                HorizontalDivider()

                // Disclaimer note
                Text(
                    text = "Note: Calculated prayer times can vary slightly from local mosques depending on calculation method and horizon observations. Adjust if necessary.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Reset to recommended button
                OutlinedButton(
                    onClick = {
                        selectedMethod = CalculationMethods.KARACHI
                        selectedMadhab = MadhabOptions.HANAFI
                        soundOption = "Adhan"
                        vibrationEnabled = true
                        fajrReminder = true
                        dhuhrReminder = true
                        asrReminder = true
                        maghribReminder = true
                        ishaReminder = true
                        onResetRecommended()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_recommended_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset to Recommended (Bangladesh/Karachi/Hanafi)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onUpdateMethod(selectedMethod)
                    onUpdateMadhab(selectedMadhab)
                    onUpdateNotificationPreferences(notifyAtTime, notifyBeforeTime, beforeMinutes, soundOption, vibrationEnabled)
                    onToggleIndividualPrayer(PrayerName.FAJR, fajrReminder)
                    onToggleIndividualPrayer(PrayerName.DHUHR, dhuhrReminder)
                    onToggleIndividualPrayer(PrayerName.ASR, asrReminder)
                    onToggleIndividualPrayer(PrayerName.MAGHRIB, maghribReminder)
                    onToggleIndividualPrayer(PrayerName.ISHA, ishaReminder)
                    onDismiss()
                },
                modifier = Modifier.testTag("save_salat_settings_button")
            ) {
                Text("Save Settings")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalatLocationDialog(
    currentCity: String,
    isDetecting: Boolean,
    onDismiss: () -> Unit,
    onDetectLocation: () -> Unit,
    onSelectCity: (CityLocation) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            PredefinedCities.POPULAR_CITIES
        } else {
            PredefinedCities.POPULAR_CITIES.filter {
                it.city.contains(searchQuery, ignoreCase = true) ||
                        it.country.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Bangladesh Location",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Auto Detect Button
                Button(
                    onClick = onDetectLocation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detect_gps_location_button"),
                    enabled = !isDetecting
                ) {
                    if (isDetecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detecting GPS Location...")
                    } else {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use Device Current Location")
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search city (e.g. Chattogram, Dhaka, Sylhet)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_city_input")
                )

                Text(
                    text = "Bangladesh Cities & Districts",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCities, key = { it.displayName }) { city ->
                        val isSelected = currentCity.startsWith(city.city, ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectCity(city) }
                                .testTag("city_item_${city.city}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = city.city,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                    Text(
                                        text = city.country,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun getPrayerIcon(prayer: PrayerName): ImageVector = when (prayer) {
    PrayerName.FAJR -> Icons.Default.WbTwilight
    PrayerName.SUNRISE -> Icons.Default.WbSunny
    PrayerName.DHUHR -> Icons.Default.LightMode
    PrayerName.ASR -> Icons.Default.BrightnessMedium
    PrayerName.MAGHRIB -> Icons.Default.NightsStay
    PrayerName.ISHA -> Icons.Default.Bedtime
}

@Composable
fun PrayerReminderToggleRow(
    name: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$name Reminder",
            style = MaterialTheme.typography.bodyMedium
        )
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("prayer_toggle_${name.lowercase()}")
        )
    }
}
