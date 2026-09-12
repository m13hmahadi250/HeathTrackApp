package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfileEntity::class,
        FoodEntity::class,
        MealLogEntity::class,
        WaterLogEntity::class,
        ExerciseEntity::class,
        ExerciseSessionEntity::class,
        SleepLogEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        ReminderEntity::class,
        NotificationEntity::class,
        HealthSyncEntity::class,
        StreakEntity::class,
        SalatConfigEntity::class,
        SalatCompletionEntity::class,
        CachedPrayerTimesEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class VitaFlowDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun foodDao(): FoodDao
    abstract fun mealLogDao(): MealLogDao
    abstract fun waterLogDao(): WaterLogDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun exerciseSessionDao(): ExerciseSessionDao
    abstract fun sleepLogDao(): SleepLogDao
    abstract fun habitDao(): HabitDao
    abstract fun reminderDao(): ReminderDao
    abstract fun notificationDao(): NotificationDao
    abstract fun healthSyncDao(): HealthSyncDao
    abstract fun streakDao(): StreakDao
    abstract fun salatDao(): SalatDao

    companion object {
        @Volatile
        private var INSTANCE: VitaFlowDatabase? = null

        fun getInstance(context: Context): VitaFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VitaFlowDatabase::class.java,
                    "vitaflow_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    populateInitialData(getInstance(context))
                                } catch (_: Exception) {}
                            }
                        }

                        override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                            super.onDestructiveMigration(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    populateInitialData(getInstance(context))
                                } catch (_: Exception) {}
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getAllSeedFoods(): List<FoodEntity> = listOf(
            // --- Bangladeshi & Bengali Specialties ---
            FoodEntity(name = "Steamed Basmati Rice", servingSize = "1 cup (150g)", calories = 190, protein = 4.0f, carbs = 42f, fat = 0.4f, fiber = 0.8f, category = "Grains", isFavorite = true),
            FoodEntity(name = "Whole-grain Roti / Chapati", servingSize = "1 medium (45g)", calories = 110, protein = 3.5f, carbs = 22f, fat = 1.2f, fiber = 3.0f, category = "Grains", isFavorite = true),
            FoodEntity(name = "Shorshe Ilish (Hilsa Mustard Curry)", servingSize = "1 piece with gravy (130g)", calories = 240, protein = 21f, carbs = 3f, fat = 16f, fiber = 1.2f, category = "Protein", isFavorite = true),
            FoodEntity(name = "Rui Fish Curry", servingSize = "1 piece with gravy (140g)", calories = 180, protein = 19f, carbs = 4f, fat = 9.5f, fiber = 1.0f, category = "Protein", isFavorite = true),
            FoodEntity(name = "Chingri Malai Curry (Prawn Coconut)", servingSize = "1 bowl (160g)", calories = 260, protein = 20f, carbs = 6f, fat = 18f, fiber = 1.5f, category = "Protein", isFavorite = true),
            FoodEntity(name = "Beef Bhuna (Bangladeshi Style)", servingSize = "1 bowl (150g)", calories = 290, protein = 26f, carbs = 4f, fat = 19f, fiber = 1.0f, category = "Protein"),
            FoodEntity(name = "Chicken Curry with Gravy", servingSize = "1 cup (180g)", calories = 230, protein = 24f, carbs = 5f, fat = 12f, fiber = 1.2f, category = "Protein", isFavorite = true),
            FoodEntity(name = "Mutton Kacchi Biryani", servingSize = "1 plate (300g)", calories = 480, protein = 22f, carbs = 54f, fat = 20f, fiber = 2.5f, category = "Grains"),
            FoodEntity(name = "Chicken Dum Biryani", servingSize = "1 plate (300g)", calories = 420, protein = 25f, carbs = 52f, fat = 14f, fiber = 2.5f, category = "Grains"),
            FoodEntity(name = "Khichuri (Rice & Lentil Comfort)", servingSize = "1 bowl (220g)", calories = 260, protein = 8.5f, carbs = 44f, fat = 6.0f, fiber = 4.5f, category = "Grains", isFavorite = true),
            FoodEntity(name = "Bhuna Khichuri with Dim (Egg)", servingSize = "1 bowl (240g)", calories = 340, protein = 14f, carbs = 46f, fat = 11f, fiber = 4.5f, category = "Grains"),
            FoodEntity(name = "Alu Bhorta (Spiced Potato Mash)", servingSize = "1 serving (80g)", calories = 95, protein = 2.0f, carbs = 18f, fat = 2.2f, fiber = 2.0f, category = "Vegetables"),
            FoodEntity(name = "Begun Bhorta (Roasted Eggplant Mash)", servingSize = "1 serving (90g)", calories = 75, protein = 1.8f, carbs = 9f, fat = 4.0f, fiber = 3.5f, category = "Vegetables"),
            FoodEntity(name = "Chingri Bhorta (Spiced Shrimp Mash)", servingSize = "1 serving (70g)", calories = 110, protein = 14f, carbs = 2f, fat = 5.5f, fiber = 1.0f, category = "Protein"),
            FoodEntity(name = "Red Lentil Dal (Masoor Dal)", servingSize = "1 bowl (180g)", calories = 140, protein = 9.0f, carbs = 20f, fat = 2.5f, fiber = 5.0f, category = "Curries & Dal", isFavorite = true),
            FoodEntity(name = "Moong Dal with Cumin & Ghee", servingSize = "1 bowl (180g)", calories = 155, protein = 9.5f, carbs = 21f, fat = 4.0f, fiber = 4.5f, category = "Curries & Dal"),
            FoodEntity(name = "Chholar Dal with Coconut Flakes", servingSize = "1 bowl (180g)", calories = 210, protein = 11f, carbs = 28f, fat = 6.5f, fiber = 7.0f, category = "Curries & Dal"),
            FoodEntity(name = "Mixed Vegetable Bhaji / Labra", servingSize = "1 bowl (150g)", calories = 95, protein = 2.5f, carbs = 12f, fat = 4.5f, fiber = 4.2f, category = "Vegetables", isFavorite = true),
            FoodEntity(name = "Shobji Potol & Jheenga Jhol", servingSize = "1 bowl (160g)", calories = 65, protein = 2.0f, carbs = 8f, fat = 2.8f, fiber = 3.0f, category = "Vegetables"),
            FoodEntity(name = "Palong Shak Bhaji (Spinach Fry)", servingSize = "1 bowl (120g)", calories = 80, protein = 3.5f, carbs = 6f, fat = 5.0f, fiber = 3.8f, category = "Vegetables"),
            FoodEntity(name = "Plain Paratha (Tawa Fried)", servingSize = "1 piece (60g)", calories = 180, protein = 4.0f, carbs = 25f, fat = 7.5f, fiber = 2.0f, category = "Grains"),
            FoodEntity(name = "Mughlai Paratha with Egg & Herbs", servingSize = "1 portion (120g)", calories = 320, protein = 12f, carbs = 32f, fat = 16f, fiber = 2.5f, category = "Grains"),
            FoodEntity(name = "Beef Haleem (Lentil & Meat Stew)", servingSize = "1 bowl (250g)", calories = 340, protein = 24f, carbs = 32f, fat = 14f, fiber = 6.0f, category = "Curries & Dal"),
            FoodEntity(name = "Mutton Nihari (Slow Simmered)", servingSize = "1 bowl (250g)", calories = 380, protein = 28f, carbs = 8f, fat = 26f, fiber = 1.2f, category = "Protein"),
            FoodEntity(name = "Mishti Doi (Sweetened Fermented Yogurt)", servingSize = "1 cup (150g)", calories = 180, protein = 6.5f, carbs = 24f, fat = 6.5f, fiber = 0.0f, category = "Dairy"),
            FoodEntity(name = "Roshogolla / Rasgulla", servingSize = "1 piece (50g)", calories = 120, protein = 2.5f, carbs = 24f, fat = 1.8f, fiber = 0.0f, category = "Snacks"),
            FoodEntity(name = "Vegetable Singara / Samosa", servingSize = "1 piece (75g)", calories = 180, protein = 3.0f, carbs = 23f, fat = 9.0f, fiber = 1.8f, category = "Snacks"),

            // --- Indian Specialties ---
            FoodEntity(name = "Palak Paneer (Spinach & Cottage Cheese)", servingSize = "1 bowl (200g)", calories = 240, protein = 14f, carbs = 9f, fat = 17f, fiber = 4.5f, category = "Curries & Dal", isFavorite = true),
            FoodEntity(name = "Butter Chicken / Murgh Makhani", servingSize = "1 cup (200g)", calories = 320, protein = 25f, carbs = 10f, fat = 20f, fiber = 2.0f, category = "Protein"),
            FoodEntity(name = "Chicken Tikka (Tandoori Grilled)", servingSize = "4 pieces (140g)", calories = 195, protein = 28f, carbs = 3f, fat = 8.0f, fiber = 1.0f, category = "Protein", isFavorite = true),
            FoodEntity(name = "Paneer Tikka (Tandoori Grilled)", servingSize = "4 pieces (130g)", calories = 230, protein = 15f, carbs = 6f, fat = 16f, fiber = 1.5f, category = "Protein"),
            FoodEntity(name = "Chana Masala (Chickpea Curry)", servingSize = "1 bowl (180g)", calories = 210, protein = 10f, carbs = 32f, fat = 5.5f, fiber = 8.0f, category = "Curries & Dal"),
            FoodEntity(name = "Rajma Masala (Red Kidney Bean Curry)", servingSize = "1 bowl (200g)", calories = 220, protein = 11f, carbs = 34f, fat = 4.5f, fiber = 9.0f, category = "Curries & Dal"),
            FoodEntity(name = "Plain Dosa with Sambar & Chutney", servingSize = "1 large (180g)", calories = 280, protein = 7.0f, carbs = 46f, fat = 7.5f, fiber = 4.0f, category = "Grains"),
            FoodEntity(name = "Steamed Idli with Sambar", servingSize = "2 pieces (150g)", calories = 160, protein = 6.0f, carbs = 32f, fat = 1.2f, fiber = 3.0f, category = "Grains"),
            FoodEntity(name = "Vegetable Pulao (Spiced Fragrant Rice)", servingSize = "1 bowl (200g)", calories = 230, protein = 4.5f, carbs = 43f, fat = 5.0f, fiber = 3.0f, category = "Grains"),
            FoodEntity(name = "Garlic Naan Bread", servingSize = "1 piece (80g)", calories = 230, protein = 6.0f, carbs = 38f, fat = 6.5f, fiber = 2.0f, category = "Grains"),
            FoodEntity(name = "Puri with Aloo Bhaji", servingSize = "2 puris + sabzi (180g)", calories = 360, protein = 6.0f, carbs = 48f, fat = 16f, fiber = 4.0f, category = "Grains"),
            FoodEntity(name = "Mango Lassi (Chilled Yogurt Drink)", servingSize = "1 glass (240ml)", calories = 190, protein = 6.0f, carbs = 34f, fat = 3.5f, fiber = 1.2f, category = "Dairy"),
            FoodEntity(name = "Masala Chai with Milk", servingSize = "1 cup (150ml)", calories = 75, protein = 2.5f, carbs = 9f, fat = 3.0f, fiber = 0.0f, category = "Drinks"),

            // --- East & Southeast Asian Specialties ---
            FoodEntity(name = "Pad Thai with Tofu & Shrimp", servingSize = "1 plate (250g)", calories = 380, protein = 18f, carbs = 52f, fat = 12f, fiber = 3.5f, category = "Grains"),
            FoodEntity(name = "Vegetable Fried Rice", servingSize = "1 bowl (200g)", calories = 270, protein = 5.5f, carbs = 46f, fat = 7.5f, fiber = 3.0f, category = "Grains"),
            FoodEntity(name = "Chicken Teriyaki with Steamed Rice", servingSize = "1 bowl (260g)", calories = 380, protein = 28f, carbs = 48f, fat = 8.5f, fiber = 2.0f, category = "Protein"),
            FoodEntity(name = "Japanese Miso Soup with Tofu & Wakame", servingSize = "1 bowl (200ml)", calories = 55, protein = 4.5f, carbs = 5f, fat = 1.8f, fiber = 1.5f, category = "Curries & Dal"),
            FoodEntity(name = "Japanese Chicken Ramen Noodle Bowl", servingSize = "1 large bowl (400g)", calories = 450, protein = 26f, carbs = 58f, fat = 13f, fiber = 3.0f, category = "Grains"),
            FoodEntity(name = "Steamed Chicken / Veg Momos (Dumplings)", servingSize = "5 pieces (130g)", calories = 210, protein = 14f, carbs = 26f, fat = 5.5f, fiber = 2.0f, category = "Snacks"),
            FoodEntity(name = "Vietnamese Chicken Pho Noodle Soup", servingSize = "1 large bowl (420g)", calories = 360, protein = 25f, carbs = 50f, fat = 6.0f, fiber = 2.5f, category = "Grains"),
            FoodEntity(name = "Stir-Fried Tofu with Broccoli & Bok Choy", servingSize = "1 bowl (200g)", calories = 165, protein = 14f, carbs = 10f, fat = 8.5f, fiber = 4.5f, category = "Vegetables"),
            FoodEntity(name = "Steamed Edamame with Sea Salt", servingSize = "1 bowl (120g)", calories = 130, protein = 12f, carbs = 9f, fat = 5.0f, fiber = 5.5f, category = "Snacks"),
            FoodEntity(name = "Thai Green Curry with Chicken & Bamboo", servingSize = "1 cup (200g)", calories = 290, protein = 22f, carbs = 8f, fat = 19f, fiber = 2.5f, category = "Curries & Dal"),
            FoodEntity(name = "Kimchi (Spicy Fermented Cabbage)", servingSize = "1 small dish (80g)", calories = 25, protein = 1.5f, carbs = 4f, fat = 0.5f, fiber = 2.0f, category = "Vegetables"),
            FoodEntity(name = "Tom Yum Kung (Spicy Shrimp Soup)", servingSize = "1 bowl (220ml)", calories = 120, protein = 15f, carbs = 7f, fat = 3.5f, fiber = 1.5f, category = "Protein"),
            FoodEntity(name = "Fresh Vietnamese Spring Rolls", servingSize = "2 rolls (120g)", calories = 190, protein = 8.0f, carbs = 26f, fat = 6.5f, fiber = 2.8f, category = "Snacks"),

            // --- Fresh Produce, Dairy & Healthy Snacks ---
            FoodEntity(name = "Boiled Egg", servingSize = "1 large (50g)", calories = 74, protein = 6.3f, carbs = 0.4f, fat = 5.0f, fiber = 0.0f, category = "Protein", isFavorite = true),
            FoodEntity(name = "Egg Omelette with Herbs & Onion", servingSize = "1 egg (65g)", calories = 115, protein = 7.0f, carbs = 2.0f, fat = 8.5f, fiber = 0.5f, category = "Protein"),
            FoodEntity(name = "Grilled Chicken Breast", servingSize = "1 fillet (120g)", calories = 165, protein = 31f, carbs = 0.0f, fat = 3.6f, fiber = 0.0f, category = "Protein"),
            FoodEntity(name = "Fresh Banana", servingSize = "1 medium (118g)", calories = 105, protein = 1.3f, carbs = 27f, fat = 0.3f, fiber = 3.1f, category = "Fruits", isFavorite = true),
            FoodEntity(name = "Fresh Mango Slices", servingSize = "1 cup (165g)", calories = 100, protein = 1.4f, carbs = 25f, fat = 0.6f, fiber = 2.6f, category = "Fruits"),
            FoodEntity(name = "Fresh Papaya Slices", servingSize = "1 cup (145g)", calories = 62, protein = 0.7f, carbs = 16f, fat = 0.4f, fiber = 2.5f, category = "Fruits"),
            FoodEntity(name = "Green Coconut Water (Daab)", servingSize = "1 glass (250ml)", calories = 45, protein = 1.5f, carbs = 9f, fat = 0.2f, fiber = 2.5f, category = "Drinks"),
            FoodEntity(name = "Mixed Almonds & Walnuts", servingSize = "1 handful (28g)", calories = 165, protein = 5.5f, carbs = 6.0f, fat = 14.5f, fiber = 3.5f, category = "Snacks"),
            FoodEntity(name = "Rolled Oats with Milk & Honey", servingSize = "1 bowl (200g)", calories = 210, protein = 8.0f, carbs = 35f, fat = 4.5f, fiber = 4.0f, category = "Grains"),
            FoodEntity(name = "Fresh Cucumber & Tomato Salad", servingSize = "1 bowl (150g)", calories = 35, protein = 1.2f, carbs = 6.5f, fat = 0.3f, fiber = 2.0f, category = "Vegetables"),
            FoodEntity(name = "Plain Greek Yogurt", servingSize = "1 cup (170g)", calories = 100, protein = 17f, carbs = 6f, fat = 0.7f, fiber = 0.0f, category = "Dairy"),
            FoodEntity(name = "Warm Turmeric Milk (Haldi Doodh)", servingSize = "1 glass (240ml)", calories = 150, protein = 8.0f, carbs = 12f, fat = 8.0f, fiber = 0.5f, category = "Dairy"),
            FoodEntity(name = "Green Tea with Lemon", servingSize = "1 mug (250ml)", calories = 2, protein = 0.0f, carbs = 0.5f, fat = 0.0f, fiber = 0.0f, category = "Drinks")
        )

        private suspend fun populateInitialData(db: VitaFlowDatabase) {
            // Static lookup catalogues only (foods and exercises). NO dummy user profile.
            db.foodDao().insertAllFoods(getAllSeedFoods())

            // Pre-populated exercises
            val initialExercises = listOf(
                ExerciseEntity(
                    name = "Brisk Morning Walk",
                    category = "Walking",
                    difficulty = "Beginner",
                    durationMinutes = 25,
                    instructions = "Walk at an active cadence. Keep shoulders relaxed and posture tall.",
                    restTimeSeconds = 0,
                    safetyNotes = "Wear comfortable walking shoes. Stay hydrated."
                ),
                ExerciseEntity(
                    name = "Gentle Morning Yoga Flow",
                    category = "Yoga",
                    difficulty = "Beginner",
                    durationMinutes = 15,
                    instructions = "Child's pose, cat-cow, gentle downward dog, forward fold, deep breathing.",
                    restTimeSeconds = 30,
                    safetyNotes = "Never force joints. Flow at your own breath tempo."
                ),
                ExerciseEntity(
                    name = "Full Body Mobility & Posture",
                    category = "Mobility",
                    difficulty = "Beginner",
                    durationMinutes = 12,
                    instructions = "Arm circles, thoracic spine rotations, hip circles, standing hamstring reach.",
                    restTimeSeconds = 15,
                    safetyNotes = "Keep motions smooth and controlled."
                ),
                ExerciseEntity(
                    name = "Beginner Bodyweight Strength",
                    category = "Bodyweight",
                    difficulty = "Intermediate",
                    durationMinutes = 20,
                    instructions = "3 sets: 10 bodyweight squats, 8 knee/wall push-ups, 20s plank hold, 10 glute bridges.",
                    restTimeSeconds = 45,
                    safetyNotes = "Brace core and maintain steady breathing."
                ),
                ExerciseEntity(
                    name = "Interval Jog & Stride",
                    category = "Running",
                    difficulty = "Intermediate",
                    durationMinutes = 25,
                    instructions = "5 min warm-up walk, alternating 2 min jog with 1 min brisk walk, 5 min cool-down.",
                    restTimeSeconds = 60,
                    safetyNotes = "Listen to your ankles and knees. Maintain light foot strikes."
                ),
                ExerciseEntity(
                    name = "Evening Wind Down & Stretch",
                    category = "Stretching",
                    difficulty = "Beginner",
                    durationMinutes = 10,
                    instructions = "Seated forward fold, supine twist, butterfly stretch, legs-up-the-wall.",
                    restTimeSeconds = 20,
                    safetyNotes = "Focus on calming the nervous system before sleep."
                )
            )
            db.exerciseDao().insertExercises(initialExercises)
            // Note: NO DUMMY USER DATA. All user data (profile, meals, water, sleep, habits, workouts, notifications, health sync) starts completely clean at zero.
        }
    }
}
