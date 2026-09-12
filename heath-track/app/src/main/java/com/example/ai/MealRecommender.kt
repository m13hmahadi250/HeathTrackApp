package com.example.ai

import com.example.data.local.UserProfileEntity

data class MealSuggestion(
    val title: String,
    val description: String,
    val balanceTip: String,
    val components: List<String>,
    val estimatedCalories: Int,
    val tags: List<String>
)

object MealRecommender {

    fun getRecommendations(
        mealType: String,
        userProfile: UserProfileEntity?,
        recentFoods: List<String> = emptyList()
    ): List<MealSuggestion> {
        val isVegetarian = userProfile?.dietaryPreference?.contains("Veg", ignoreCase = true) == true
        val allergies = userProfile?.allergies?.lowercase() ?: ""
        val containsEggAllergy = allergies.contains("egg")
        val containsDairyAllergy = allergies.contains("dairy") || allergies.contains("milk")
        val containsNutAllergy = allergies.contains("nut") || allergies.contains("peanut")

        return when (mealType.lowercase()) {
            "breakfast" -> listOfNotNull(
                if (!containsEggAllergy) {
                    MealSuggestion(
                        title = "Whole-grain Roti & Boiled Egg with Veggies",
                        description = "1-2 soft whole-grain rotis served with a farm-fresh egg and sautéed seasonal greens.",
                        balanceTip = "Provides sustained morning energy and essential B-vitamins.",
                        components = listOf("Whole-grain Roti", "Boiled Egg", "Mixed Vegetable Bhaji"),
                        estimatedCalories = 310,
                        tags = listOf("High Fiber", "Sustained Energy")
                    )
                } else null,
                if (!containsDairyAllergy) {
                    MealSuggestion(
                        title = "Rolled Oats Bowl with Banana & Plain Yogurt",
                        description = "Warm or soaked rolled oats paired with fresh ripe banana slices and creamy probiotic yogurt.",
                        balanceTip = "Gentle on digestion and rich in prebiotic gut fiber.",
                        components = listOf("Rolled Oats", "Plain Yogurt", "Fresh Banana"),
                        estimatedCalories = 320,
                        tags = listOf("Gut Friendly", "Heart Healthy")
                    )
                } else null,
                MealSuggestion(
                    title = "Savory Khichuri & Fresh Cucumber Slices",
                    description = "A comforting blend of aromatic basmati rice and lentils with mild turmeric and cumin.",
                    balanceTip = "A complete plant-based amino acid profile that is comforting and easy to digest.",
                    components = listOf("Khichuri", "Fresh Cucumber Salad"),
                    estimatedCalories = 290,
                    tags = listOf("Comfort", "Plant Protein")
                )
            )

            "lunch" -> listOfNotNull(
                if (!isVegetarian) {
                    MealSuggestion(
                        title = "Steamed Rice, Fresh Fish Curry & Lentil Dal",
                        description = "1 cup basmati rice served with tender Rui fish curry, yellow masoor dal, and vegetable bhaji.",
                        balanceTip = "A classic balanced plate with omega-3 fatty acids and clean lean protein.",
                        components = listOf("Steamed Basmati Rice", "Rui Fish Curry", "Red Lentil Dal", "Mixed Vegetable Bhaji"),
                        estimatedCalories = 520,
                        tags = listOf("Traditional Balanced", "Omega-3")
                    )
                } else null,
                if (!isVegetarian) {
                    MealSuggestion(
                        title = "Chicken Curry with Roti & Crisp Garden Salad",
                        description = "Tender lean chicken curry paired with two whole-grain rotis and a fresh tomato-cucumber salad.",
                        balanceTip = "High protein for muscle support and cellular repair.",
                        components = listOf("Chicken Curry", "Whole-grain Roti", "Cucumber & Tomato Salad"),
                        estimatedCalories = 480,
                        tags = listOf("High Protein", "Vibrant Greens")
                    )
                } else null,
                MealSuggestion(
                    title = "Rich Lentil Dal, Mixed Vegetables & Brown Rice",
                    description = "A generous bowl of spiced red lentils, vibrant mixed vegetable curry, and steamed rice.",
                    balanceTip = "High fiber promotes steady post-lunch alertness without energy slumps.",
                    components = listOf("Red Lentil Dal", "Mixed Vegetable Bhaji", "Steamed Basmati Rice"),
                    estimatedCalories = 440,
                    tags = listOf("Plant Powered", "Fiber Rich")
                )
            )

            "dinner" -> listOfNotNull(
                MealSuggestion(
                    title = "Light Whole-grain Roti, Dim Bhaji & Clear Soup",
                    description = "Two light rotis with a herb-infused egg omelette and a warm bowl of vegetable dal.",
                    balanceTip = "Light on evening digestion to support restful and undisturbed sleep.",
                    components = listOf("Whole-grain Roti", "Egg Omelette", "Red Lentil Dal"),
                    estimatedCalories = 360,
                    tags = listOf("Easy Digest", "Sleep Supportive")
                ),
                if (!isVegetarian) {
                    MealSuggestion(
                        title = "Grilled Herb Chicken & Steamed Greens",
                        description = "Lean grilled chicken breast with a colorful side of steamed cauliflower, carrots, and greens.",
                        balanceTip = "Low glycemic load to prevent nocturnal blood sugar fluctuations.",
                        components = listOf("Grilled Chicken Breast", "Mixed Vegetable Bhaji", "Fresh Cucumber Salad"),
                        estimatedCalories = 380,
                        tags = listOf("Clean & Light", "Restorative")
                    )
                } else null,
                MealSuggestion(
                    title = "Lentil Khichuri with Roasted Papad & Greens",
                    description = "Soft, warm rice-lentil stew served with crisp greens and a light seasoning.",
                    balanceTip = "Comforting and grounding after a long active day.",
                    components = listOf("Khichuri", "Mixed Vegetable Bhaji"),
                    estimatedCalories = 340,
                    tags = listOf("Grounding", "Nourishing")
                )
            )

            else -> listOfNotNull(
                if (!containsNutAllergy) {
                    MealSuggestion(
                        title = "Roasted Almonds & Green Tea with Lemon",
                        description = "A handful of crunchy roasted almonds paired with a warm cup of antioxidant green tea.",
                        balanceTip = "Heart-healthy fats help curb hunger between main meals.",
                        components = listOf("Mixed Almonds & Walnuts", "Green Tea with Lemon"),
                        estimatedCalories = 170,
                        tags = listOf("Quick Energy", "Antioxidants")
                    )
                } else null,
                if (!containsDairyAllergy) {
                    MealSuggestion(
                        title = "Fresh Banana with a Dollop of Yogurt",
                        description = "Naturally sweet banana slices dipped in cool, probiotic yogurt.",
                        balanceTip = "Provides natural electrolytes (potassium and magnesium).",
                        components = listOf("Fresh Banana", "Plain Yogurt"),
                        estimatedCalories = 160,
                        tags = listOf("Electrolytes", "Naturally Sweet")
                    )
                } else null,
                MealSuggestion(
                    title = "Crisp Cucumber & Spiced Tomato Slices",
                    description = "Freshly sliced cucumbers and tomatoes with a hint of lemon juice and black pepper.",
                    balanceTip = "Hydrating, crisp, and provides vital micronutrients.",
                    components = listOf("Fresh Cucumber & Tomato Salad"),
                    estimatedCalories = 40,
                    tags = listOf("Hydrating", "Ultra Light")
                )
            )
        }
    }
}
