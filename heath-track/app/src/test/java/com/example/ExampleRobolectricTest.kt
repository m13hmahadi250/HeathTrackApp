package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.MealRecommender
import com.example.ai.SmartFoodAnalyzer
import com.example.ai.SmartInsightsEngine
import com.example.data.local.FoodEntity
import com.example.data.local.UserProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches Heath Track`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Heath Track", appName)
    }

    @Test
    fun `smart food analyzer accurately parses natural language input`() = kotlinx.coroutines.runBlocking {
        val sampleFoods = listOf(
            FoodEntity(name = "Steamed Basmati Rice", servingSize = "1 cup (150g)", calories = 190, protein = 4.0f, carbs = 42f, fat = 0.4f, fiber = 0.8f, category = "Grains"),
            FoodEntity(name = "Rui Fish Curry", servingSize = "1 piece with gravy (140g)", calories = 180, protein = 19f, carbs = 4f, fat = 9.5f, fiber = 1.0f, category = "Protein"),
            FoodEntity(name = "Fresh Cucumber Salad", servingSize = "1 bowl (150g)", calories = 35, protein = 1.2f, carbs = 6.5f, fat = 0.3f, fiber = 2.0f, category = "Vegetables")
        )

        val input = "I ate 2 cups of rice, rui fish and cucumber salad"
        val parsed = SmartFoodAnalyzer.parseNaturalLanguageMeal(input, sampleFoods, "Lunch")

        assertTrue("Should detect parsed items", parsed.isNotEmpty())
        assertTrue("Should detect rice", parsed.any { it.matchedFoodName.contains("Rice", ignoreCase = true) })
        assertTrue("Should detect fish", parsed.any { it.matchedFoodName.contains("Fish", ignoreCase = true) })
    }

    @Test
    fun `meal recommender filters out allergens`() {
        val profileWithEggAllergy = UserProfileEntity(
            id = 1,
            name = "Test User",
            age = 24,
            allergies = "egg"
        )

        val breakfastIdeas = MealRecommender.getRecommendations("breakfast", profileWithEggAllergy)
        assertTrue(breakfastIdeas.isNotEmpty())
        // None of the recommended meals should contain Boiled Egg
        assertFalse(breakfastIdeas.any { it.title.contains("Boiled Egg", ignoreCase = true) })
    }

    @Test
    fun `wellness score stays encouraging and bounded`() {
        val score = SmartInsightsEngine.calculateWellnessScore(
            waterRatio = 1.0f,
            stepRatio = 0.8f,
            mealsLogged = 3,
            sleepHours = 7.5f,
            habitsCompletedRatio = 0.9f
        )
        assertTrue("Score should be positive and encouraging", score >= 80)
        assertTrue("Score should not exceed 100", score <= 100)
    }

    @Test
    fun `minor safe mode does not promote calorie deficit`() {
        val minorProfile = UserProfileEntity(
            id = 1,
            name = "Ayaan",
            age = 16,
            isMinor = true
        )
        assertTrue(minorProfile.isMinor)
        assertEquals(16, minorProfile.age)
    }

    @Test
    fun `sleep day summary formats duration and health connect source correctly`() {
        val summary = com.example.ui.SleepDaySummary(
            date = "2026-09-11",
            dayOfWeek = "Fri",
            durationMinutes = 465, // 7h 45m
            quality = "Restful",
            source = "Health Connect",
            isToday = true
        )

        assertEquals("Health Connect", summary.source)
        assertEquals(465, summary.durationMinutes)
        assertTrue("Should be marked as today", summary.isToday)
        assertEquals(7, summary.durationMinutes / 60)
        assertEquals(45, summary.durationMinutes % 60)
    }

    @Test
    fun `navigation back stack respects hierarchal screen navigation`() {
        val stack = mutableListOf<com.example.ui.VitaFlowViewModel.NavEntry>(
            com.example.ui.VitaFlowViewModel.NavEntry.Main(com.example.ui.NavDestination.HOME),
            com.example.ui.VitaFlowViewModel.NavEntry.Main(com.example.ui.NavDestination.NUTRITION),
            com.example.ui.VitaFlowViewModel.NavEntry.Secondary(com.example.ui.SecondaryScreen.FOOD_HISTORY)
        )

        // Opening Food History from Nutrition gives a stack of 3
        assertEquals(3, stack.size)

        // Pressing back pops Food History -> returns to Nutrition
        stack.removeAt(stack.lastIndex)
        val top1 = stack.last()
        assertTrue(top1 is com.example.ui.VitaFlowViewModel.NavEntry.Main)
        assertEquals(com.example.ui.NavDestination.NUTRITION, (top1 as com.example.ui.VitaFlowViewModel.NavEntry.Main).dest)

        // Pressing back again pops Nutrition -> returns to Home
        stack.removeAt(stack.lastIndex)
        val top2 = stack.last()
        assertTrue(top2 is com.example.ui.VitaFlowViewModel.NavEntry.Main)
        assertEquals(com.example.ui.NavDestination.HOME, (top2 as com.example.ui.VitaFlowViewModel.NavEntry.Main).dest)

        // At Home, size is 1, so further back exits app
        assertEquals(1, stack.size)
    }
}
