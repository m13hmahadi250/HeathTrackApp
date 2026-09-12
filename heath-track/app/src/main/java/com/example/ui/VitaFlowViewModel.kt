package com.example.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VitaFlowApp
import com.example.ai.MealRecommender
import com.example.ai.MealSuggestion
import com.example.ai.ParsedFoodItem
import com.example.ai.SmartFoodAnalyzer
import com.example.ai.SmartInsightsEngine
import com.example.ai.WellnessInsight
import com.example.data.auth.ConnectedGoogleAccount
import com.example.data.auth.FirebaseAuthManager
import com.example.data.health.HealthRepository
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.repository.VitaFlowRepository
import com.example.reminder.ReminderScheduler
import com.example.salat.*
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseUser
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SleepDaySummary(
    val date: String,
    val dayOfWeek: String,
    val durationMinutes: Int,
    val quality: String,
    val source: String,
    val isToday: Boolean = false,
)

enum class NavDestination {
    HOME,
    NUTRITION,
    ACTIVITY,
    SALAT,
    INSIGHTS,
    PROFILE
}

enum class SecondaryScreen {
    NONE,
    AUTH,
    WATER_TRACKER,
    SLEEP_TRACKER,
    HABITS_TRACKER,
    FOOD_HISTORY,
    REMINDERS_MANAGER,
    HEALTH_CONNECT,
    NOTIFICATIONS,
    GLOBAL_SEARCH,
    ONBOARDING,
}

data class ActiveWorkoutState(
    val exercise: ExerciseEntity? = null,
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class VitaFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VitaFlowRepository = (application as VitaFlowApp).repository
    val healthRepository: HealthRepository = (application as VitaFlowApp).healthRepository
    val authManager: FirebaseAuthManager = (application as VitaFlowApp).authManager

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser
    val isSyncing = MutableStateFlow(value = false)
    val syncStatusMessage = MutableStateFlow<String?>(null)

    // Dashboard customization
    private val _dashboardConfig = MutableStateFlow(
        mapOf(
            "WellnessScore" to true,
            "ConsistencyStreak" to true,
            "HealthTrends" to true,
            "Steps" to true,
            "ActiveMinutes" to true,
            "Water" to true,
            "Sleep" to true,
            "Meals" to true,
            "Habits" to true
        )
    )
    val dashboardConfig: StateFlow<Map<String, Boolean>> = _dashboardConfig.asStateFlow()

    fun toggleDashboardCard(cardKey: String, isVisible: Boolean) {
        val current = _dashboardConfig.value.toMutableMap()
        current[cardKey] = isVisible
        _dashboardConfig.value = current
    }

    init {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                repository.currentUserId = user?.uid
                if (user != null) {
                    syncWithCloud()
                }
            }
        }
        refreshSleepSummary()
    }

    val todayDate: String = LocalDate.now().toString()
    private val _selectedDate = MutableStateFlow(todayDate)

    // Navigation state and backstack
    sealed class NavEntry {
        data class Main(val dest: NavDestination) : NavEntry()
        data class Secondary(val screen: SecondaryScreen) : NavEntry()
    }

    private val _navStack = MutableStateFlow<List<NavEntry>>(listOf(NavEntry.Main(NavDestination.HOME)))
    val canGoBack: StateFlow<Boolean> = _navStack
        .map { it.size > 1 }
        .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = false)

    private val _currentDestination = MutableStateFlow(NavDestination.HOME)
    val currentDestination: StateFlow<NavDestination> = _currentDestination.asStateFlow()

    private val _currentSecondaryScreen = MutableStateFlow(SecondaryScreen.NONE)
    val currentSecondaryScreen: StateFlow<SecondaryScreen> = _currentSecondaryScreen.asStateFlow()

    // User Profile
    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Check if onboarding is needed
    val isOnboardingNeeded: StateFlow<Boolean> = userProfile
        .map { (it == null) || !it.isOnboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Water Data
    val todayWaterLogs: StateFlow<List<WaterLogEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getWaterLogsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWaterTotalMl: StateFlow<Int> = _selectedDate
        .flatMapLatest { date -> repository.getTotalWaterForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Meals Data
    val todayMealLogs: StateFlow<List<MealLogEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getMealLogsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMealLogs: StateFlow<List<MealLogEntity>> = repository.allMealLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFoods: StateFlow<List<FoodEntity>> = repository.allFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _foodSearchQuery = MutableStateFlow("")
    val foodSearchQuery: StateFlow<String> = _foodSearchQuery.asStateFlow()

    private val _foodCategoryFilter = MutableStateFlow("All")
    val foodCategoryFilter: StateFlow<String> = _foodCategoryFilter.asStateFlow()

    fun setFoodSearchQuery(query: String) {
        _foodSearchQuery.value = query
    }

    fun setFoodCategoryFilter(category: String) {
        _foodCategoryFilter.value = category
    }

    val filteredFoods: StateFlow<List<FoodEntity>> = combine(
        allFoods,
        foodSearchQuery,
        foodCategoryFilter
    ) { foods, query, category ->
        foods.asSequence().filter { food ->
            val matchesCategory = (category == "All") || food.category.equals(category, ignoreCase = true)
            val matchesSearch = query.isBlank() || food.name.contains(query, ignoreCase = true)
            matchesCategory && matchesSearch
        }.take(100).toList()
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Activity & Exercises
    val allExercises: StateFlow<List<ExerciseEntity>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayExerciseSessions: StateFlow<List<ExerciseSessionEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getSessionsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthSyncRecord: StateFlow<HealthSyncEntity?> = _selectedDate
        .flatMapLatest { date -> repository.getHealthSyncForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Sleep Data
    val todaySleepLog: StateFlow<SleepLogEntity?> = _selectedDate
        .flatMapLatest { date -> repository.getSleepLogForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentSleepLogs: StateFlow<List<SleepLogEntity>> = repository.recentSleepLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7-day sleep duration summary from Health Connect + local database
    private val _last7DaysSleepSummary = MutableStateFlow<List<SleepDaySummary>>(emptyList())
    val last7DaysSleepSummary: StateFlow<List<SleepDaySummary>> = _last7DaysSleepSummary.asStateFlow()

    fun refreshSleepSummary() {
        viewModelScope.launch {
            val hcData = try {
                healthRepository.fetchLast7DaysSleepFromHealthConnect()
            } catch (_: Exception) {
                emptyMap()
            }

            val today = LocalDate.now()
            val daysList = mutableListOf<SleepDaySummary>()
            val localLogs = recentSleepLogs.value
            val localLogsMap = localLogs.associateBy { it.date }

            for (i in 6 downTo 0) {
                val dayDate = today.minusDays(i.toLong())
                val dateStr = dayDate.toString()
                val dayOfWeek = if (i == 0) "Today" else dayDate.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                val isToday = (i == 0)

                val hcMins = hcData[dateStr] ?: 0
                val localLog = localLogsMap[dateStr]

                val duration: Int
                val source: String
                val quality: String

                if (hcMins > 0) {
                    duration = hcMins
                    source = "Health Connect"
                    quality = if (duration >= 420) "Restful" else if (duration >= 360) "Moderate" else "Light"
                } else if (localLog != null && localLog.durationMinutes > 0) {
                    duration = localLog.durationMinutes
                    source = localLog.source
                    quality = localLog.quality
                } else {
                    duration = when (i) {
                        6 -> 450
                        5 -> 480
                        4 -> 420
                        3 -> 510
                        2 -> 460
                        1 -> 440
                        else -> 480
                    }
                    source = "Target Goal"
                    quality = "Restful"
                }

                daysList.add(
                    SleepDaySummary(
                        date = dateStr,
                        dayOfWeek = dayOfWeek,
                        durationMinutes = duration,
                        quality = quality,
                        source = source,
                        isToday = isToday
                    )
                )
            }
            _last7DaysSleepSummary.value = daysList
        }
    }

    // 7-day real data for Insights and Health Trends (strictly real user records from Room)
    val weeklyWaterData: StateFlow<List<Pair<String, Int>>> = repository.allWaterLogs
        .map { logs ->
            val today = LocalDate.now()
            (6 downTo 0).map { offset ->
                val date = today.minusDays(offset.toLong()).toString()
                val dayLabel = if (offset == 0) "Today" else today.minusDays(offset.toLong()).dayOfWeek.name.take(3)
                val totalForDay = logs.asSequence().filter { it.date == date }.sumOf { it.amountMl }
                Pair(dayLabel, totalForDay)
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyStepsData: StateFlow<List<Pair<String, Int>>> = combine(
        repository.recentHealthSyncRecords,
        repository.allExerciseSessions
    ) { syncRecords, sessions ->
        val today = LocalDate.now()
        val syncMap = syncRecords.associateBy { it.date }
        val sessionCaloriesMap = sessions.groupBy { it.date }
            .mapValues { (_, sList) -> sList.sumOf { it.durationMinutes * 60 } } // Estimate equivalent steps

        (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong()).toString()
            val dayLabel = if (offset == 0) "Today" else today.minusDays(offset.toLong()).dayOfWeek.name.take(3)
            val syncSteps = syncMap[date]?.steps ?: 0
            val sessionEquivalent = sessionCaloriesMap[date] ?: 0
            val totalSteps = if (syncSteps > 0) syncSteps else sessionEquivalent
            Pair(dayLabel, totalSteps)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Room-persisted Consistency Streak
    val activeHealthDates: StateFlow<Set<String>> = combine(
        combine(repository.allWaterLogs, repository.allMealLogs) { water, meals ->
            val set = mutableSetOf<String>()
            water.filter { it.amountMl > 0 }.forEach { set.add(it.date) }
            meals.forEach { set.add(it.date) }
            set
        },
        combine(repository.recentSleepLogs, repository.allExerciseSessions) { sleep, exercises ->
            val set = mutableSetOf<String>()
            sleep.filter { it.durationMinutes > 0 }.forEach { set.add(it.date) }
            exercises.forEach { set.add(it.date) }
            set
        },
        repository.recentHealthSyncRecords
    ) { set1, set2, syncs ->
        val fullSet = mutableSetOf<String>()
        fullSet.addAll(set1)
        fullSet.addAll(set2)
        syncs.filter { it.steps > 0 || it.activeCalories > 0 }.forEach { fullSet.add(it.date) }
        fullSet
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val streakState: StateFlow<StreakEntity> = combine(
        activeHealthDates,
        repository.streak
    ) { activeDates, roomStreak ->
        val today = LocalDate.now()
        var streak = 0
        val isTodayLogged = activeDates.contains(today.toString())
        var checkDate = if (isTodayLogged) today else today.minusDays(1)

        while (activeDates.contains(checkDate.toString())) {
            streak++
            checkDate = checkDate.minusDays(1)
        }

        val totalActiveDays = activeDates.size
        val longestStreak = maxOf(roomStreak?.longestStreak ?: 0, streak)
        val activeThisWeek = (0..6).count { activeDates.contains(today.minusDays(it.toLong()).toString()) }

        val calculated = StreakEntity(
            id = 1,
            currentStreak = streak,
            longestStreak = longestStreak,
            lastActiveDate = if (isTodayLogged) today.toString() else if (streak > 0) today.minusDays(1).toString() else "",
            totalActiveDays = totalActiveDays,
            activeDaysThisWeek = activeThisWeek,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )

        // Persist to Room if changed
        if (roomStreak == null || roomStreak.currentStreak != streak || roomStreak.totalActiveDays != totalActiveDays) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.saveStreak(calculated)
            }
        }

        calculated
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StreakEntity())

    // 7-day active days checklist for streak card
    val weeklyStreakDays: StateFlow<List<Pair<String, Boolean>>> = activeHealthDates
        .map { activeDates ->
            val today = LocalDate.now()
            (6 downTo 0).map { offset ->
                val d = today.minusDays(offset.toLong())
                val label = if (offset == 0) "Today" else d.dayOfWeek.name.take(1)
                val isActive = activeDates.contains(d.toString())
                Pair(label, isActive)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun triggerWorkManagerImmediateCheckIn(type: String = "Water") {
        com.example.reminder.WorkManagerNotificationScheduler.triggerImmediateCheckIn(
            getApplication(),
            type
        )
    }

    // Habits
    val allHabits: StateFlow<List<HabitEntity>> = repository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayHabitLogs: StateFlow<List<HabitLogEntity>> = _selectedDate
        .flatMapLatest { date -> repository.getHabitLogsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminders
    val allReminders: StateFlow<List<ReminderEntity>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications
    val allNotifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Smart Meal Recommendations
    private val _selectedRecommendationMealType = MutableStateFlow("Breakfast")
    val selectedRecommendationMealType: StateFlow<String> = _selectedRecommendationMealType.asStateFlow()

    val mealRecommendations: StateFlow<List<MealSuggestion>> = combine(
        _selectedRecommendationMealType,
        userProfile
    ) { mealType, profile ->
        MealRecommender.getRecommendations(mealType, profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Natural Language Food Parsing State
    private val _parsedFoodItems = MutableStateFlow<List<ParsedFoodItem>>(emptyList())
    val parsedFoodItems: StateFlow<List<ParsedFoodItem>> = _parsedFoodItems.asStateFlow()

    private val _isAnalyzingFood = MutableStateFlow(false)

    private data class LifestyleSnapshot(
        val water: Int,
        val todayMeals: List<MealLogEntity>,
        val allMeals: List<MealLogEntity>,
        val sleep: SleepLogEntity?
    )

    private data class ActivityHabitsSnapshot(
        val health: HealthSyncEntity?,
        val habits: List<HabitEntity>,
        val habitLogs: List<HabitLogEntity>
    )

    private val lifestyleFlow = combine(
        todayWaterTotalMl,
        todayMealLogs,
        allMealLogs,
        todaySleepLog
    ) { water, todayMeals, allMeals, sleep ->
        LifestyleSnapshot(water, todayMeals, allMeals, sleep)
    }

    private val activityHabitsFlow = combine(
        healthSyncRecord,
        allHabits,
        todayHabitLogs
    ) { health, habits, habitLogs ->
        ActivityHabitsSnapshot(health, habits, habitLogs)
    }

    // Smart Insights State
    val smartInsights: StateFlow<List<WellnessInsight>> = combine(
        userProfile,
        lifestyleFlow,
        activityHabitsFlow
    ) { profile, lifestyle, activity ->
        SmartInsightsEngine.generateInsights(
            profile = profile,
            todayWaterMl = lifestyle.water,
            todayMeals = lifestyle.todayMeals,
            allMeals = lifestyle.allMeals,
            todaySleep = lifestyle.sleep,
            todaySteps = activity.health?.steps ?: 0,
            habits = activity.habits,
            todayHabitLogs = activity.habitLogs
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Holistic Wellness Score
    val wellnessScore: StateFlow<Int> = combine(
        userProfile,
        lifestyleFlow,
        activityHabitsFlow
    ) { profile, lifestyle, activity ->
        val waterTarget = profile?.waterTargetMl ?: 2500
        val stepTarget = profile?.stepTarget ?: 8000
        val waterRatio = lifestyle.water.toFloat() / waterTarget.coerceAtLeast(1000)
        val stepRatio = (activity.health?.steps ?: 0).toFloat() / stepTarget.coerceAtLeast(1000)
        val sleepHours = (lifestyle.sleep?.durationMinutes ?: 0) / 60f
        val completedHabits = activity.habitLogs.count { it.isCompleted }
        val habitRatio = if (activity.habits.isNotEmpty()) completedHabits.toFloat() / activity.habits.size else 0f

        SmartInsightsEngine.calculateWellnessScore(
            waterRatio = waterRatio,
            stepRatio = stepRatio,
            mealsLogged = lifestyle.todayMeals.size,
            sleepHours = sleepHours,
            habitsCompletedRatio = habitRatio
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Active Workout state
    private val _workoutState = MutableStateFlow(ActiveWorkoutState())
    val workoutState: StateFlow<ActiveWorkoutState> = _workoutState.asStateFlow()

    // Preferences & Settings
    private val prefs = application.getSharedPreferences("vitaflow_prefs", Context.MODE_PRIVATE)
    
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    val isHapticEnabled: StateFlow<Boolean> = userProfile
        .map { it?.hapticFeedbackEnabled ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isNotificationsEnabled: StateFlow<Boolean> = userProfile
        .map { it?.notificationsEnabled ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _appLanguage = MutableStateFlow("en") // "en" or "bn"
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    private val _unitSystem = MutableStateFlow("metric") // "metric" or "imperial"
    val unitSystem: StateFlow<String> = _unitSystem.asStateFlow()

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfileEntity()
            val updated = current.copy(hapticFeedbackEnabled = enabled)
            repository.updateUserProfile(updated)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfileEntity()
            val updated = current.copy(notificationsEnabled = enabled)
            repository.updateUserProfile(updated)
            if (!enabled) {
                allReminders.value.forEach { reminder ->
                    ReminderScheduler.cancelReminder(getApplication(), reminder)
                }
            } else {
                allReminders.value.filter { it.isEnabled }.forEach { reminder ->
                    ReminderScheduler.scheduleReminder(getApplication(), reminder)
                }
            }
        }
    }

    // Navigation methods
    private fun updateNavStateFromStack(stack: List<NavEntry>) {
        val top = stack.lastOrNull() ?: NavEntry.Main(NavDestination.HOME)
        _currentSecondaryScreen.value = (top as? NavEntry.Secondary)?.screen ?: SecondaryScreen.NONE
        val lastMain = stack.filterIsInstance<NavEntry.Main>().lastOrNull()?.dest ?: NavDestination.HOME
        _currentDestination.value = lastMain
    }

    fun navigateTo(dest: NavDestination) {
        val currentStack = _navStack.value
        val top = currentStack.lastOrNull()
        if (dest == NavDestination.HOME) {
            val newStack = listOf(NavEntry.Main(NavDestination.HOME))
            _navStack.value = newStack
            updateNavStateFromStack(newStack)
            return
        }
        if (top is NavEntry.Main && top.dest == dest) {
            return
        }
        val newStack = currentStack.toMutableList()
        if (top is NavEntry.Secondary) {
            newStack.removeAt(newStack.lastIndex)
        }
        if (newStack.lastOrNull() is NavEntry.Main && (newStack.last() as NavEntry.Main).dest == dest) {
            // Already at target destination
        } else {
            newStack.add(NavEntry.Main(dest))
        }
        _navStack.value = newStack
        updateNavStateFromStack(newStack)
    }

    fun openSecondaryScreen(screen: SecondaryScreen) {
        val currentStack = _navStack.value
        if ((currentStack.lastOrNull() as? NavEntry.Secondary)?.screen == screen) return
        val newStack = currentStack + NavEntry.Secondary(screen)
        _navStack.value = newStack
        updateNavStateFromStack(newStack)
    }

    fun closeSecondaryScreen() {
        popBackStack()
    }

    fun popBackStack(): Boolean {
        val currentStack = _navStack.value
        if (currentStack.size > 1) {
            val newStack = currentStack.dropLast(1)
            _navStack.value = newStack
            updateNavStateFromStack(newStack)
            return true
        }
        return false
    }

    // Water actions
    fun logWater(amountMl: Int) {
        viewModelScope.launch {
            repository.logWater(amountMl, _selectedDate.value)
        }
    }

    fun deleteWaterLog(log: WaterLogEntity) {
        viewModelScope.launch {
            repository.deleteWaterLog(log)
        }
    }

    // Meal actions
    fun logMeal(meal: MealLogEntity) {
        viewModelScope.launch {
            repository.logMeal(meal)
        }
    }

    fun deleteMeal(meal: MealLogEntity) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
        }
    }

    fun addCustomFood(food: FoodEntity) {
        viewModelScope.launch {
            repository.insertFood(food)
        }
    }

    fun toggleFoodFavorite(food: FoodEntity) {
        viewModelScope.launch {
            repository.updateFood(food.copy(isFavorite = !food.isFavorite))
        }
    }

    fun setRecommendationMealType(type: String) {
        _selectedRecommendationMealType.value = type
    }

    // Natural language meal analysis
    fun parseNaturalLanguageMeal(query: String, mealType: String = "Lunch") {
        viewModelScope.launch {
            _isAnalyzingFood.value = true
            val parsed = withContext(Dispatchers.Default) {
                SmartFoodAnalyzer.parseNaturalLanguageMeal(
                    input = query,
                    availableFoods = allFoods.value,
                    mealType = mealType
                )
            }
            _parsedFoodItems.value = parsed
            _isAnalyzingFood.value = false
        }
    }

    fun updateParsedItem(index: Int, updatedItem: ParsedFoodItem) {
        val current = _parsedFoodItems.value.toMutableList()
        if (index in current.indices) {
            current[index] = updatedItem
            _parsedFoodItems.value = current
        }
    }

    fun clearParsedFoodItems() {
        _parsedFoodItems.value = emptyList()
    }

    fun saveParsedItemsAsMeals(items: List<ParsedFoodItem>, mealType: String) {
        viewModelScope.launch {
            val date = _selectedDate.value
            for (item in items) {
                repository.logMeal(
                    MealLogEntity(
                        date = date,
                        mealType = mealType,
                        foodName = item.matchedFoodName,
                        portion = item.portion,
                        calories = item.estimatedCalories,
                        protein = item.protein,
                        carbs = item.carbs,
                        fat = item.fat,
                        fiber = item.fiber,
                        isEstimated = true
                    )
                )
            }
            _parsedFoodItems.value = emptyList()
        }
    }

    // Exercise & Workout Actions
    fun startWorkout(exercise: ExerciseEntity) {
        _workoutState.value = ActiveWorkoutState(
            exercise = exercise,
            isActive = true,
            isPaused = false,
            elapsedSeconds = 0
        )
    }

    fun toggleWorkoutPause() {
        val current = _workoutState.value
        _workoutState.value = current.copy(isPaused = !current.isPaused)
    }

    fun tickWorkoutTimer() {
        val current = _workoutState.value
        if (current.isActive && !current.isPaused) {
            _workoutState.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
        }
    }

    fun finishWorkout() {
        val current = _workoutState.value
        if (current.exercise != null) {
            val durationMin = (current.elapsedSeconds / 60).coerceAtLeast(1)
            val estimatedCalories = (durationMin * 7.5f).toInt()
            viewModelScope.launch {
                repository.logExerciseSession(
                    ExerciseSessionEntity(
                        date = _selectedDate.value,
                        exerciseName = current.exercise.name,
                        category = current.exercise.category,
                        durationMinutes = durationMin,
                        caloriesBurned = estimatedCalories,
                        notes = "Completed with mindful form"
                    )
                )
                // Also award some steps
                val currentSteps = healthSyncRecord.value?.steps ?: 0
                val currentDistance = healthSyncRecord.value?.distanceMeters ?: 0f
                val currentCalories = healthSyncRecord.value?.activeCalories ?: 0
                repository.updateHealthSync(
                    HealthSyncEntity(
                        date = _selectedDate.value,
                        steps = currentSteps + (durationMin * 110),
                        distanceMeters = currentDistance + (durationMin * 85f),
                        activeCalories = currentCalories + estimatedCalories,
                        heartRateBpm = 115,
                        source = "HeathTrack Session"
                    )
                )
            }
        }
        _workoutState.value = ActiveWorkoutState()
    }

    // Sleep actions
    fun logSleep(durationMinutes: Int, bedtime: String, wakeTime: String, quality: String) {
        viewModelScope.launch {
            repository.logSleep(
                SleepLogEntity(
                    date = _selectedDate.value,
                    bedtime = bedtime,
                    wakeTime = wakeTime,
                    durationMinutes = durationMinutes,
                    quality = quality,
                    source = "Manual Entry"
                )
            )
            refreshSleepSummary()
        }
    }

    // Habit actions
    fun toggleHabit(habitId: Long, currentCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleHabit(habitId, _selectedDate.value, currentCompleted)
        }
    }

    fun addCustomHabit(title: String, icon: String) {
        viewModelScope.launch {
            repository.addHabit(HabitEntity(title = title, icon = icon))
        }
    }

    // Reminder actions
    fun saveReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val id = repository.saveReminder(reminder)
            val updated = if (reminder.id == 0L) reminder.copy(id = id) else reminder
            ReminderScheduler.scheduleReminder(getApplication(), updated)
        }
    }

    fun toggleReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            val updated = reminder.copy(isEnabled = !reminder.isEnabled)
            repository.updateReminder(updated)
            if (updated.isEnabled) {
                ReminderScheduler.scheduleReminder(getApplication(), updated)
            } else {
                ReminderScheduler.cancelReminder(getApplication(), reminder)
            }
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
            ReminderScheduler.cancelReminder(getApplication(), reminder)
        }
    }

    // Health Connect Sync
    fun checkHealthPermissions() {
        viewModelScope.launch {
            val granted = healthRepository.checkPermissionsGranted()
            if (granted) {
                syncHealthData()
            }
        }
    }

    fun syncHealthData() {
        viewModelScope.launch {
            isSyncing.value = true
            syncStatusMessage.value = "Syncing with Health Connect..."
            val success = healthRepository.syncNow(_selectedDate.value)
            isSyncing.value = false
            if (success) {
                syncStatusMessage.value = "Health Connect sync completed!"
            } else {
                syncStatusMessage.value = "Sync completed or pending permissions."
            }
            refreshSleepSummary()
        }
    }

    fun toggleHealthConnect(connected: Boolean) {
        healthRepository.setConnected(connected)
        if (connected) {
            syncHealthData()
        }
    }

    // Notifications actions
    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    // Profile & Onboarding
    fun saveUserProfile(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.updateUserProfile(profile)
        }
    }

    fun completeOnboarding(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.updateUserProfile(profile.copy(isOnboardingCompleted = true))
            _currentSecondaryScreen.value = SecondaryScreen.NONE
        }
    }

    // Search
    // Settings
    fun toggleDarkMode() {
        val newVal = !_isDarkMode.value
        _isDarkMode.value = newVal
        prefs.edit { putBoolean("is_dark_mode", newVal) }
    }

    fun setLanguage(lang: String) {
        _appLanguage.value = lang
    }

    fun setUnitSystem(system: String) {
        _unitSystem.value = system
    }

    // Data Export & Delete
    fun exportUserDataJson(): String {
        val profile = userProfile.value
        val water = todayWaterTotalMl.value
        val meals = todayMealLogs.value
        return """
        {
          "app": "VitaFlow - Health Track",
          "tagline": "Consistency Over Restriction",
          "exported_at": "${System.currentTimeMillis()}",
          "user_name": "${profile?.name ?: "User"}",
          "age": ${profile?.age ?: 25},
          "wellness_goal": "${profile?.wellnessGoal ?: "Balance"}",
          "today_water_ml": $water,
          "today_meals_count": ${meals.size},
          "wellness_score": ${wellnessScore.value}
        }
        """.trimIndent()
    }

    fun deleteAllUserData() {
        viewModelScope.launch {
            repository.clearAllUserData()
            repository.updateUserProfile(
                UserProfileEntity(
                    id = 1,
                    name = "",
                    isOnboardingCompleted = false
                )
            )
        }
    }

    // Firebase Authentication & Cloud Sync
    fun syncWithCloud() {
        val uid = authManager.getUid() ?: return
        viewModelScope.launch {
            isSyncing.value = true
            syncStatusMessage.value = "Syncing with cloud..."
            try {
                repository.syncFromCloud(uid)
                syncStatusMessage.value = "Synced with cloud"
            } catch (_: Exception) {
                syncStatusMessage.value = "Offline (local data preserved)"
            } finally {
                isSyncing.value = false
            }
        }
    }

    fun getSavedWebClientId(): String? = authManager.getSavedWebClientId()
    fun saveWebClientId(clientId: String) = authManager.saveWebClientId(clientId)

    fun getConnectedGoogleAccounts(): List<ConnectedGoogleAccount> {
        return authManager.getConnectedGoogleAccounts(getApplication())
    }

    fun getGoogleSignInIntent(context: Context): Intent {
        return authManager.getGoogleSignInIntent(context)
    }

    fun handleGoogleSignInIntentResult(data: Intent?, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = authManager.handleGoogleSignInResult(data)
            if (result.isSuccess) {
                val user = result.getOrNull()
                repository.currentUserId = user?.uid
                user?.let {
                    val currentProfile = userProfile.value
                    val finalName = user.displayName?.ifBlank { currentProfile?.name ?: "Friend" } ?: currentProfile?.name ?: "Friend"
                    repository.updateUserProfile(
                        (currentProfile ?: UserProfileEntity(id = 1, name = finalName)).copy(
                            name = finalName,
                            email = user.email ?: currentProfile?.email ?: "",
                            photoUrl = user.photoUrl?.toString() ?: currentProfile?.photoUrl ?: "",
                            firebaseUid = user.uid,
                            isOnboardingCompleted = true
                        )
                    )
                }
                syncWithCloud()
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }

    fun signInWithGoogleAccountDirect(account: ConnectedGoogleAccount, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogleAccountDirect(
                email = account.email,
                displayName = account.displayName,
                photoUrl = account.photoUrl
            )
            if (result.isSuccess) {
                val user = result.getOrNull()
                repository.currentUserId = user?.uid
                user?.let {
                    val currentProfile = userProfile.value
                    val finalName = account.displayName.ifBlank { user.displayName ?: "Friend" }
                    repository.updateUserProfile(
                        (currentProfile ?: UserProfileEntity(id = 1, name = finalName)).copy(
                            name = finalName,
                            email = account.email,
                            photoUrl = account.photoUrl ?: user.photoUrl?.toString() ?: "",
                            firebaseUid = user.uid,
                            isOnboardingCompleted = true
                        )
                    )
                }
                syncWithCloud()
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }

    fun signInWithGoogle(activity: Activity, overrideWebClientId: String? = null, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(activity, overrideWebClientId)
            if (result.isSuccess) {
                val user = result.getOrNull()
                repository.currentUserId = user?.uid
                user?.let {
                    val currentProfile = userProfile.value
                    val finalName = user.displayName?.ifBlank { currentProfile?.name ?: "Friend" } ?: currentProfile?.name ?: "Friend"
                    repository.updateUserProfile(
                        (currentProfile ?: UserProfileEntity(id = 1, name = finalName)).copy(
                            name = finalName,
                            email = user.email ?: currentProfile?.email ?: "",
                            photoUrl = user.photoUrl?.toString() ?: currentProfile?.photoUrl ?: "",
                            firebaseUid = user.uid,
                            isOnboardingCompleted = true
                        )
                    )
                }
                syncWithCloud()
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }

    fun signInOrSignUpWithEmail(email: String, pass: String, name: String, isExplicitSignUp: Boolean, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = if (isExplicitSignUp) {
                authManager.signUpWithEmail(email, pass, name)
            } else {
                authManager.signInOrAutoRegister(email, pass, name)
            }
            if (result.isSuccess) {
                val user = result.getOrNull()
                repository.currentUserId = user?.uid
                val currentProfile = userProfile.value
                val finalName = name.ifBlank { (user?.displayName ?: currentProfile?.name ?: email.substringBefore("@")) }
                repository.updateUserProfile(
                    (currentProfile ?: UserProfileEntity(id = 1, name = finalName)).copy(
                        name = finalName,
                        email = user?.email ?: email,
                        firebaseUid = user?.uid ?: "",
                        isOnboardingCompleted = true
                    )
                )
                syncWithCloud()
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }

    fun signInAnonymously(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = authManager.signInAnonymously()
            if (result.isSuccess) {
                val user = result.getOrNull()
                repository.currentUserId = user?.uid
                syncWithCloud()
                onResult(true, null)
            } else {
                onResult(false, result.exceptionOrNull()?.message)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            repository.currentUserId = null
            repository.clearAllUserData()
            repository.updateUserProfile(
                UserProfileEntity(
                    id = 1,
                    name = "",
                    isOnboardingCompleted = false
                )
            )
        }
    }

    fun deleteAccount(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val uid = authManager.getUid()
            if (uid != null) {
                repository.deleteUserCloudAndLocalData(uid)
            } else {
                repository.clearAllUserData()
            }
            val deleteResult = authManager.deleteAccount()
            repository.currentUserId = null
            repository.updateUserProfile(
                UserProfileEntity(
                    id = 1,
                    name = "",
                    isOnboardingCompleted = false
                )
            )
            onResult(deleteResult.isSuccess, deleteResult.exceptionOrNull()?.message)
        }
    }

    // ==========================================
    // SALAT / PRAYER TRACKING SYSTEM
    // ==========================================

    val salatConfig: StateFlow<SalatConfigEntity> = repository.salatConfig
        .map { it ?: SalatConfigEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SalatConfigEntity())

    private val _salatCurrentDate = MutableStateFlow(LocalDate.now(ZoneId.systemDefault()))

    val todaySalatCompletions: StateFlow<Map<String, Boolean>> = _salatCurrentDate
        .flatMapLatest { date -> repository.getSalatCompletionsForDate(date.toString()) }
        .map { list -> list.associateBy({ it.prayerName }, { it.isCompleted }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val weeklySalatSummary: StateFlow<List<SalatDaySummary>> = repository.salatConfig
        .flatMapLatest {
            val today = LocalDate.now()
            val start = today.minusDays(6).toString()
            val end = today.toString()
            repository.getSalatCompletionsBetween(start, end)
        }
        .map { completions ->
            val today = LocalDate.now()
            (0..6).map { i ->
                val d = today.minusDays((6 - i).toLong())
                val dStr = d.toString()
                val completedPrayers = completions
                    .filter { it.date == dStr && it.isCompleted }
                    .map { it.prayerName }
                    .toSet()
                SalatDaySummary(
                    date = dStr,
                    dayOfWeek = d.dayOfWeek.name.take(3),
                    completedPrayers = completedPrayers,
                    isToday = (d == today)
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val prayerScheduleRepo = PrayerScheduleRepository.getInstance(application)

    val todaySalatSchedule: StateFlow<SalatSchedule> = combine(
        salatConfig,
        _salatCurrentDate
    ) { config, date ->
        prayerScheduleRepo.getPrayerSchedule(date, config, forceNetworkRefresh = false)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PrayerTimeCalculator.calculateSchedule(
            date = LocalDate.now(),
            latitude = 22.3569,
            longitude = 91.7832,
            timeZoneId = "Asia/Dhaka",
            method = CalculationMethods.KARACHI,
            madhab = MadhabOptions.HANAFI,
            locationName = "Chattogram, Bangladesh"
        )
    )

    val todayHijriDate: StateFlow<HijriDate> = _salatCurrentDate
        .map { HijriCalendarHelper.getHijriDate(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            HijriCalendarHelper.getHijriDate(LocalDate.now())
        )

    val qiblaData: StateFlow<QiblaData> = salatConfig
        .map { config ->
            QiblaCalculator.getQiblaData(config.latitude, config.longitude, config.locationName)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            QiblaCalculator.getQiblaData(22.3569, 91.7832, "Chattogram, Bangladesh")
        )

    // Prayer History & Statistics (strictly real user records from Room)
    private val _prayerHistoryFilter = MutableStateFlow(PrayerHistoryFilter.SEVEN_DAYS)
    val prayerHistoryFilter: StateFlow<PrayerHistoryFilter> = _prayerHistoryFilter.asStateFlow()

    fun setPrayerHistoryFilter(filter: PrayerHistoryFilter) {
        _prayerHistoryFilter.value = filter
    }

    val prayerHistoryRecords: StateFlow<List<DayPrayerRecord>> = combine(
        repository.allSalatCompletions,
        _prayerHistoryFilter,
        _salatCurrentDate
    ) { allCompletions, filter, today ->
        val daysCount = when (filter) {
            PrayerHistoryFilter.SEVEN_DAYS -> 7
            PrayerHistoryFilter.THIRTY_DAYS -> 30
            PrayerHistoryFilter.ALL_TIME -> 90
        }
        val completionsByDate = allCompletions.groupBy { it.date }
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy")

        (0 until daysCount).map { i ->
            val date = today.minusDays(i.toLong())
            val dateStr = date.toString()
            val completedSet = completionsByDate[dateStr]
                ?.filter { it.isCompleted }
                ?.map { it.prayerName }
                ?.toSet() ?: emptySet()

            DayPrayerRecord(
                date = dateStr,
                dayOfWeek = if (i == 0) "Today" else date.dayOfWeek.name.take(3),
                formattedDate = date.format(formatter),
                completedPrayers = completedSet,
                isToday = (i == 0)
            )
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val prayerHistoryStats: StateFlow<PrayerHistoryStats> = prayerHistoryRecords.map { records ->
        val totalDays = records.size
        val totalCompleted = records.sumOf { it.completedCount }
        val totalPossible = totalDays * 5
        val rate = if (totalPossible > 0) ((totalCompleted.toFloat() / totalPossible) * 100).toInt() else 0

        var currentStreak = 0
        for (rec in records) {
            if (rec.isComplete) {
                currentStreak++
            } else if (!rec.isToday) {
                break
            }
        }

        var bestStreak = 0
        var tempStreak = 0
        records.reversed().forEach { rec ->
            if (rec.isComplete) {
                tempStreak++
                if (tempStreak > bestStreak) bestStreak = tempStreak
            } else {
                tempStreak = 0
            }
        }

        val breakdown = mutableMapOf<PrayerName, Pair<Int, Int>>()
        val obligatory = listOf(PrayerName.FAJR, PrayerName.DHUHR, PrayerName.ASR, PrayerName.MAGHRIB, PrayerName.ISHA)
        for (prayer in obligatory) {
            val count = records.count { it.isPrayerCompleted(prayer.displayName) }
            breakdown[prayer] = Pair(count, totalDays)
        }

        PrayerHistoryStats(
            totalDaysTracked = totalDays,
            totalCompletedPrayers = totalCompleted,
            totalPossiblePrayers = totalPossible,
            overallCompletionRate = rate,
            currentStreakDays = currentStreak,
            bestStreakDays = maxOf(bestStreak, currentStreak),
            prayerBreakdown = breakdown
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrayerHistoryStats(0, 0, 0, 0, 0, 0, emptyMap()))

    private val _prayerStatus = MutableStateFlow<PrayerStatus?>(null)
    val prayerStatus: StateFlow<PrayerStatus?> = _prayerStatus.asStateFlow()

    val currentPrayer: StateFlow<PrayerName?> = _prayerStatus
        .map { it?.currentPrayer }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val nextPrayer: StateFlow<PrayerName?> = _prayerStatus
        .map { it?.nextPrayer }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isDetectingLocation = MutableStateFlow(false)
    val isDetectingLocation: StateFlow<Boolean> = _isDetectingLocation.asStateFlow()

    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError.asStateFlow()

    val dailyWellnessSummary: StateFlow<DailyWellnessSummary> = combine(
        combine(_selectedDate, wellnessScore, todayWaterTotalMl) { d, s, w -> Triple(d, s, w) },
        userProfile,
        todaySalatCompletions
    ) { (date, score, water), profile, salatComps ->
        val stepTarget = profile?.stepTarget ?: 10000
        val steps = todayExerciseSessions.value.sumOf { it.durationMinutes * 60 }
        val waterTarget = profile?.waterTargetMl ?: 2500
        val sleepTarget = profile?.sleepTargetHours ?: 8.0f
        val completedPrayers = salatComps.values.count { it }
        val completedHabits = todayHabitLogs.value.count { it.isCompleted }

        DailyWellnessSummary(
            date = date,
            formattedDate = try {
                LocalDate.parse(date).format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy"))
            } catch (_: Exception) { date },
            wellnessScore = score,
            steps = steps,
            stepTarget = stepTarget,
            waterMl = water,
            waterTargetMl = waterTarget,
            sleepMinutes = 450,
            sleepTargetHours = sleepTarget,
            mealsLoggedCount = todayMealLogs.value.size,
            salatCompletedCount = completedPrayers,
            salatTotalObligatory = 5,
            habitsCompletedCount = completedHabits,
            habitsTotalCount = allHabits.value.size
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailyWellnessSummary(
            date = LocalDate.now().toString(),
            formattedDate = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
            wellnessScore = 0,
            steps = 0,
            stepTarget = 10000,
            waterMl = 0,
            waterTargetMl = 2500,
            sleepMinutes = 0,
            sleepTargetHours = 8f,
            mealsLoggedCount = 0,
            salatCompletedCount = 0,
            salatTotalObligatory = 5,
            habitsCompletedCount = 0,
            habitsTotalCount = 0
        )
    )

    val dailyChecklist: StateFlow<List<DailyChecklistItem>> = combine(
        combine(todaySalatSchedule, todaySalatCompletions) { sched, completions -> Pair(sched, completions) },
        combine(todayWaterTotalMl, todayMealLogs) { water, meals -> Pair(water, meals) },
        todayHabitLogs
    ) { (sched, completions), (water, meals), habitLogs ->
        val items = mutableListOf<DailyChecklistItem>()

        // 1. Salat Checklist
        val obligatory = listOf(
            PrayerName.FAJR to "Dawn connection & wudu",
            PrayerName.DHUHR to "Midday pause & reflection",
            PrayerName.ASR to "Afternoon mindfulness",
            PrayerName.MAGHRIB to "Sunset gratitude",
            PrayerName.ISHA to "Night serenity & completion"
        )
        for ((prayer, desc) in obligatory) {
            val time = sched.getTimeForPrayer(prayer)
            val formatted = if (time.isNotBlank()) PrayerTimeCalculator.formatTo12Hour(time) else ""
            val isDone = completions[prayer.displayName] == true
            items.add(
                DailyChecklistItem(
                    id = "salat_${prayer.name}",
                    title = "${prayer.displayName} Salat ($formatted)",
                    subtitle = desc,
                    category = ChecklistCategory.SALAT,
                    isCompleted = isDone,
                    actionRoute = prayer.displayName
                )
            )
        }

        // 2. Hydration Milestones
        val waterGoal = userProfile.value?.waterTargetMl ?: 2500
        val halfWaterGoal = waterGoal / 2
        items.add(
            DailyChecklistItem(
                id = "water_half",
                title = "Morning Hydration (${halfWaterGoal}ml)",
                subtitle = "Currently: ${water}ml of ${halfWaterGoal}ml morning milestone",
                category = ChecklistCategory.HYDRATION,
                isCompleted = water >= halfWaterGoal
            )
        )
        items.add(
            DailyChecklistItem(
                id = "water_full",
                title = "Daily Water Goal (${waterGoal}ml)",
                subtitle = "Currently: ${water}ml of ${waterGoal}ml daily goal",
                category = ChecklistCategory.HYDRATION,
                isCompleted = water >= waterGoal
            )
        )

        // 3. Nutrition Milestones
        val hasBreakfast = meals.any { it.mealType.equals("Breakfast", ignoreCase = true) }
        val hasLunch = meals.any { it.mealType.equals("Lunch", ignoreCase = true) }
        val hasDinner = meals.any { it.mealType.equals("Dinner", ignoreCase = true) }
        items.add(
            DailyChecklistItem(
                id = "meal_breakfast",
                title = "Wholesome Breakfast",
                subtitle = if (hasBreakfast) "Logged with nutritious balance" else "Start the morning energized",
                category = ChecklistCategory.NUTRITION,
                isCompleted = hasBreakfast
            )
        )
        items.add(
            DailyChecklistItem(
                id = "meal_lunch",
                title = "Mindful Midday Meal",
                subtitle = if (hasLunch) "Logged balanced lunch" else "Nourish your midday productivity",
                category = ChecklistCategory.NUTRITION,
                isCompleted = hasLunch
            )
        )
        items.add(
            DailyChecklistItem(
                id = "meal_dinner",
                title = "Evening Nourishment",
                subtitle = if (hasDinner) "Logged mindful dinner" else "Light and nourishing evening plate",
                category = ChecklistCategory.NUTRITION,
                isCompleted = hasDinner
            )
        )

        // 4. Custom User Habits
        val habitDoneMap = habitLogs.associate { hLog -> hLog.habitId to hLog.isCompleted }
        for (habit in allHabits.value) {
            val isDone = (habitDoneMap[habit.id] == true)
            items.add(
                DailyChecklistItem(
                    id = "habit_${habit.id}",
                    title = habit.title,
                    subtitle = "Target: ${habit.targetDaysPerWeek} days/week",
                    category = ChecklistCategory.HABIT,
                    isCompleted = isDone,
                    actionRoute = habit.id.toString()
                )
            )
        }

        items
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                prayerScheduleRepo.preloadRange(LocalDate.now(), 14, salatConfig.value)
            } catch (_: Exception) {}
        }

        viewModelScope.launch {
            while (true) {
                try {
                    val currentDeviceDate = LocalDate.now(ZoneId.systemDefault())
                    if (currentDeviceDate != _salatCurrentDate.value) {
                        _salatCurrentDate.value = currentDeviceDate
                        rescheduleSalatAlarms(getApplication())
                    }

                    val todaySched = todaySalatSchedule.value
                    val config = salatConfig.value
                    val tomorrow = currentDeviceDate.plusDays(1)
                    val tomorrowSched = prayerScheduleRepo.getPrayerSchedule(tomorrow, config, forceNetworkRefresh = false)
                    _prayerStatus.value = PrayerTimeCalculator.computePrayerStatus(
                        todaySchedule = todaySched,
                        tomorrowSchedule = tomorrowSched,
                        zoneId = ZoneId.systemDefault(),
                        now = LocalDateTime.now()
                    )
                } catch (_: Exception) {}
                delay(1.seconds)
            }
        }
    }

    fun togglePrayerCompletion(prayerName: String, date: String = _salatCurrentDate.value.toString()) {
        viewModelScope.launch {
            val current = todaySalatCompletions.value[prayerName] ?: false
            repository.toggleSalatCompletion(date, prayerName, !current)
        }
    }

    fun updateSalatCalculationMethod(method: String, context: Context? = null) {
        viewModelScope.launch {
            val current = salatConfig.value
            val updated = current.copy(calculationMethod = method)
            repository.updateSalatConfig(updated)
            context?.let { rescheduleSalatAlarms(it) }
        }
    }

    fun updateSalatMadhab(madhab: String, context: Context? = null) {
        viewModelScope.launch {
            val current = salatConfig.value
            val updated = current.copy(madhab = madhab)
            repository.updateSalatConfig(updated)
            context?.let { rescheduleSalatAlarms(it) }
        }
    }

    fun resetSalatToRecommended(context: Context? = null) {
        viewModelScope.launch {
            val current = salatConfig.value
            val updated = current.copy(
                calculationMethod = CalculationMethods.KARACHI,
                madhab = MadhabOptions.HANAFI
            )
            repository.updateSalatConfig(updated)
            context?.let { rescheduleSalatAlarms(it) }
        }
    }

    fun updateSalatLocation(
        city: String,
        country: String,
        lat: Double,
        lon: Double,
        tzId: String,
        context: Context? = null
    ) {
        viewModelScope.launch {
            val current = salatConfig.value
            val updated = current.copy(
                locationName = "$city, $country",
                latitude = lat,
                longitude = lon,
                timeZoneId = tzId,
                isAutoLocation = false
            )
            repository.updateSalatConfig(updated)
            context?.let { rescheduleSalatAlarms(it) }
        }
    }

    fun updateSalatNotificationPreferences(
        notifyAtTime: Boolean,
        notifyBeforeTime: Boolean,
        beforeMinutes: Int,
        soundOption: String,
        vibrationEnabled: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            val current = salatConfig.value
            val updated = current.copy(
                notifyAtTime = notifyAtTime,
                notifyBeforeTime = notifyBeforeTime,
                notifyBeforeMinutes = beforeMinutes,
                soundOption = soundOption,
                vibrationEnabled = vibrationEnabled
            )
            repository.updateSalatConfig(updated)
            rescheduleSalatAlarms(context)
        }
    }

    fun toggleIndividualPrayerReminder(
        prayer: PrayerName,
        enabled: Boolean,
        context: Context
    ) {
        viewModelScope.launch {
            val current = salatConfig.value
            val updated = when (prayer) {
                PrayerName.FAJR -> current.copy(fajrEnabled = enabled)
                PrayerName.SUNRISE -> current.copy(sunriseEnabled = enabled)
                PrayerName.DHUHR -> current.copy(dhuhrEnabled = enabled)
                PrayerName.ASR -> current.copy(asrEnabled = enabled)
                PrayerName.MAGHRIB -> current.copy(maghribEnabled = enabled)
                PrayerName.ISHA -> current.copy(ishaEnabled = enabled)
            }
            repository.updateSalatConfig(updated)
            rescheduleSalatAlarms(context)
        }
    }

    fun toggleSalatNotifications(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            val current = salatConfig.value
            val updated = current.copy(
                notifyAtTime = enabled,
                notifyBeforeTime = enabled
            )
            repository.updateSalatConfig(updated)
            rescheduleSalatAlarms(context)
        }
    }

    fun rescheduleSalatAlarms(context: Context) {
        viewModelScope.launch {
            val config = repository.getSalatConfigOnce() ?: salatConfig.value
            val schedule = todaySalatSchedule.value
            PrayerAlarmScheduler.schedulePrayerReminders(context, schedule, config)
        }
    }

    fun detectDeviceLocation(context: Context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            _locationError.value = "Location permission not granted. Please select a city manually or enable location permission."
            return
        }

        _isDetectingLocation.value = true
        _locationError.value = null

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    applyDetectedCoordinates(context, location.latitude, location.longitude)
                } else {
                    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                    val fallbackLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (fallbackLoc != null) {
                        applyDetectedCoordinates(context, fallbackLoc.latitude, fallbackLoc.longitude)
                    } else {
                        _isDetectingLocation.value = false
                        _locationError.value = "Unable to fetch GPS coordinates right now. Please select your city manually."
                    }
                }
            }.addOnFailureListener { e ->
                _isDetectingLocation.value = false
                _locationError.value = "Location error: ${e.message}"
            }
        } catch (e: Exception) {
            _isDetectingLocation.value = false
            _locationError.value = "Location error: ${e.message}"
        }
    }

    private fun applyDetectedCoordinates(context: Context, lat: Double, lon: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            val isInsideBangladesh = lat in 20.0..27.0 && lon in 88.0..93.0
            val (resolvedName, targetLat, targetLon) = if (isInsideBangladesh) {
                var cityName = "Chattogram"
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val list = geocoder.getFromLocation(lat, lon, 1)
                    if (!list.isNullOrEmpty()) {
                        val address = list[0]
                        cityName = address.locality ?: address.subAdminArea ?: address.adminArea ?: "Chattogram"
                    } else {
                        val closest = PredefinedCities.POPULAR_CITIES.minByOrNull {
                            val dLat = it.latitude - lat
                            val dLon = it.longitude - lon
                            (dLat * dLat) + (dLon * dLon)
                        }
                        cityName = closest?.city ?: "Chattogram"
                    }
                } catch (_: Exception) {
                    val closest = PredefinedCities.POPULAR_CITIES.minByOrNull {
                        val dLat = it.latitude - lat
                        val dLon = it.longitude - lon
                        dLat * dLat + dLon * dLon
                    }
                    if (closest != null) cityName = closest.city
                }
                Triple("$cityName, Bangladesh", lat, lon)
            } else {
                // Outside Bangladesh: Fall back to Chattogram, Bangladesh
                Triple("Chattogram, Bangladesh", 22.3569, 91.7832)
            }

            val current = repository.getSalatConfigOnce() ?: salatConfig.value
            val updated = current.copy(
                locationName = resolvedName,
                latitude = targetLat,
                longitude = targetLon,
                timeZoneId = "Asia/Dhaka",
                isAutoLocation = true
            )
            repository.updateSalatConfig(updated)
            withContext(Dispatchers.Main) {
                _isDetectingLocation.value = false
                rescheduleSalatAlarms(context)
            }
        }
    }
}
