package com.example.ai

import com.example.data.local.FoodEntity
import com.example.data.local.MealLogEntity
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ParsedFoodItem(
    val originalQuery: String,
    val matchedFoodName: String,
    val portion: String,
    val estimatedCalories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float
)

object SmartFoodAnalyzer {
    
    private val geminiService = GeminiService()

    suspend fun parseNaturalLanguageMeal(
        input: String,
        availableFoods: List<FoodEntity>,
        mealType: String = "Lunch"
    ): List<ParsedFoodItem> {
        val result = geminiService.analyzeFood(input)
        if (result.isNotEmpty()) {
            return result.map { item ->
                ParsedFoodItem(
                    originalQuery = input,
                    matchedFoodName = item.foodName,
                    portion = item.portion,
                    estimatedCalories = item.calories,
                    protein = item.protein,
                    carbs = item.carbs,
                    fat = item.fat,
                    fiber = item.fiber
                )
            }
        }
        
        // Fallback to simplistic parsing if Gemini fails or API key is missing
        val cleanInput = input.lowercase()
            .replace("i ate", "")
            .replace("i had", "")
            .replace("for breakfast", "")
            .replace("for lunch", "")
            .replace("for dinner", "")
            .replace("for snack", "")
            .replace("a cup of", "1 cup")
            .trim()

        val rawTokens = cleanInput.split(Regex("[,&+]|\\band\\b"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val parsedItems = mutableListOf<ParsedFoodItem>()

        for (token in rawTokens) {
            val genericName = token.replace(Regex("\\d+\\s*(cup|cups|plate|bowl|piece|roti|rotis|gram|g|ml)"), "").trim().capitalizeWords()
            parsedItems.add(
                ParsedFoodItem(
                    originalQuery = token,
                    matchedFoodName = if (genericName.isNotBlank()) genericName else "Mixed Meal Item",
                    portion = "1 standard serving",
                    estimatedCalories = 180,
                    protein = 6.0f,
                    carbs = 25.0f,
                    fat = 6.0f,
                    fiber = 2.0f
                )
            )
        }

        return parsedItems
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
