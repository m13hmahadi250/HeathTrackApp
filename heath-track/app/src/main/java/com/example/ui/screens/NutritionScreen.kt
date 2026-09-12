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
import com.example.data.local.FoodEntity
import com.example.data.local.MealLogEntity
import com.example.ui.SecondaryScreen
import com.example.ui.VitaFlowViewModel
import com.example.ui.components.MedicalDisclaimerCard
import com.example.ui.theme.*

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: VitaFlowViewModel,
    modifier: Modifier = Modifier,
) {
    val todayMeals by viewModel.todayMealLogs.collectAsStateWithLifecycle()
    // val allFoods by viewModel.allFoods.collectAsStateWithLifecycle()
    // val isAnalyzing by viewModel.isAnalyzingFood.collectAsStateWithLifecycle()
    val parsedItems by viewModel.parsedFoodItems.collectAsStateWithLifecycle()

    val searchQuery by viewModel.foodSearchQuery.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.foodCategoryFilter.collectAsStateWithLifecycle()
    val displayedFoods by viewModel.filteredFoods.collectAsStateWithLifecycle()

    var showAnalyzerDialog by remember { mutableStateOf(value = false) }
    var showRecommenderDialog by remember { mutableStateOf(false) }
    var showAddCustomFoodDialog by remember { mutableStateOf(false) }
    var selectedFoodToLog by remember { mutableStateOf<FoodEntity?>(null) }

    val categories = remember { listOf("All", "Grains", "Protein", "Curries & Dal", "Vegetables", "Fruits", "Dairy", "Snacks") }

    val totalCalories = remember(todayMeals) { todayMeals.sumOf { it.calories } }
    val totalProtein = remember(todayMeals) { todayMeals.sumOf { it.protein.toDouble() }.toFloat() }
    val totalCarbs = remember(todayMeals) { todayMeals.sumOf { it.carbs.toDouble() }.toFloat() }
    val totalFat = remember(todayMeals) { todayMeals.sumOf { it.fat.toDouble() }.toFloat() }
    val totalFiber = remember(todayMeals) { todayMeals.sumOf { it.fiber.toDouble() }.toFloat() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("nutrition_screen_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Date Header
        item(key = "nutrition_header", contentType = "header") {
            Column {
                Text(
                    text = "Nutrition & Meal Planner",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Mindful, balanced eating • Consistency Over Restriction",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Nutrient Balance Summary Card
        item(key = "nutrition_energy_card", contentType = "summary") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nutrition_summary_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Energy Balance",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$totalCalories kcal",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${todayMeals.size} meal item(s) logged",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AmberWarmth.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Estimated",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = AmberWarmth
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Macro Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MacroColumn(title = "Protein", value = "${String.format(java.util.Locale.getDefault(), "%.1f", totalProtein)}g", color = EmeraldSuccess)
                        MacroColumn(title = "Carbs", value = "${String.format(java.util.Locale.getDefault(), "%.1f", totalCarbs)}g", color = AmberWarmth)
                        MacroColumn(title = "Fat", value = "${String.format(java.util.Locale.getDefault(), "%.1f", totalFat)}g", color = CoralAccent)
                        MacroColumn(title = "Fiber", value = "${String.format(java.util.Locale.getDefault(), "%.1f", totalFiber)}g", color = TealPrimary)
                    }
                }
            }
        }

        // Smart Feature Quick Actions
        item(key = "nutrition_quick_actions_1", contentType = "actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showAnalyzerDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_smart_analyzer_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Analyzer", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Analyzer", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }

                FilledTonalButton(
                    onClick = { showRecommenderDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("open_meal_ideas_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = "Ideas", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Meal Ideas", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        item(key = "nutrition_quick_actions_2", contentType = "actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { showAddCustomFoodDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("add_custom_food_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Food", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Custom Food", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = { viewModel.openSecondaryScreen(SecondaryScreen.FOOD_HISTORY) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("food_history_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = "History", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("History & Variety", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Today's Logged Meals Section
        item(key = "nutrition_logged_meals_header", contentType = "header") {
            Text(
                text = "Today's Meals (${todayMeals.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (todayMeals.isEmpty()) {
            item(key = "nutrition_empty_meals", contentType = "empty") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = "No meals",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No meals logged yet today",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Search foods below or try typing 'I ate 2 rotis with dal and salad'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                items = todayMeals, 
                key = { it.id },
                contentType = { "meal" }
            ) { meal ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logged_meal_${meal.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TealPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = meal.mealType.take(1),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = TealPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = meal.mealType,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (meal.isEstimated) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• Estimated",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = meal.foodName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${meal.portion} • ${meal.calories} kcal • P: ${meal.protein}g • C: ${meal.carbs}g • F: ${meal.fat}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteMeal(meal) },
                            modifier = Modifier.testTag("delete_meal_${meal.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Food Database & Search Section
        item(key = "nutrition_search_section", contentType = "search") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Food Database & Quick Log",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setFoodSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("food_search_input"),
                    placeholder = { Text("Search rice, roti, fish curry, dal, fruits...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setFoodSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories, key = { it }) { category ->
                        FilterChip(
                            selected = selectedCategoryFilter == category,
                            onClick = { viewModel.setFoodCategoryFilter(category) },
                            label = { Text(category) }
                        )
                    }
                }
            }
        }

        items(
            items = displayedFoods, 
            key = { it.id },
            contentType = { "food" }
        ) { food ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("food_item_${food.id}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.toggleFoodFavorite(food) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (food.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (food.isFavorite) AmberWarmth else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = food.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${food.servingSize} • ${food.calories} kcal • P: ${food.protein}g • C: ${food.carbs}g • F: ${food.fat}g",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { selectedFoodToLog = food },
                        modifier = Modifier.testTag("log_food_button_${food.id}"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Log", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        item(key = "nutrition_disclaimer", contentType = "disclaimer") {
            MedicalDisclaimerCard()
        }

        item(key = "nutrition_bottom_spacer", contentType = "spacer") {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Dialog: Log Food with Meal Type
    if (selectedFoodToLog != null) {
        val food = selectedFoodToLog!!
        var selectedMealType by remember { mutableStateOf("Lunch") }
        var portionText by remember { mutableStateOf(food.servingSize) }

        AlertDialog(
            onDismissRequest = { selectedFoodToLog = null },
            title = { Text("Log to Today's Meals") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text("Select meal time:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { type ->
                            FilterChip(
                                selected = selectedMealType == type,
                                onClick = { selectedMealType = type },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = portionText,
                        onValueChange = { portionText = it },
                        label = { Text("Portion") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logMeal(
                            MealLogEntity(
                                date = viewModel.todayDate,
                                mealType = selectedMealType,
                                foodName = food.name,
                                portion = portionText,
                                calories = food.calories,
                                protein = food.protein,
                                carbs = food.carbs,
                                fat = food.fat,
                                fiber = food.fiber,
                                isEstimated = food.isEstimated
                            )
                        )
                        selectedFoodToLog = null
                    }
                ) {
                    Text("Add Meal")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFoodToLog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Smart Food Analyzer (Natural Language)
    if (showAnalyzerDialog) {
        var naturalInput by remember { mutableStateOf("I ate 2 cups of rice, rui fish curry and salad") }
        var analyzerMealType by remember { mutableStateOf("Lunch") }

        AlertDialog(
            onDismissRequest = {
                showAnalyzerDialog = false
                viewModel.clearParsedFoodItems()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TealPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Smart Food Analyzer")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Type or speak naturally what you ate:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = naturalInput,
                        onValueChange = { naturalInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("natural_food_input"),
                        placeholder = { Text("e.g. 2 rotis with chicken curry and dal") },
                        maxLines = 3
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { type ->
                            FilterChip(
                                selected = analyzerMealType == type,
                                onClick = { analyzerMealType = type },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.parseNaturalLanguageMeal(naturalInput, analyzerMealType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parse_meal_button")
                    ) {
                        Text("Detect Meal Items")
                    }

                    if (parsedItems.isNotEmpty()) {
                        Text(
                            text = "Detected Items (${parsedItems.size}):",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            parsedItems.forEachIndexed { index, item ->
                                var isEditing by remember { mutableStateOf(false) }
                                var editPortion by remember(item) { mutableStateOf(item.portion) }
                                var editCals by remember(item) { mutableStateOf(item.estimatedCalories.toString()) }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth().clickable { isEditing = !isEditing }
                                ) {
                                    if (isEditing) {
                                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(item.matchedFoodName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                            OutlinedTextField(
                                                value = editPortion,
                                                onValueChange = { editPortion = it },
                                                label = { Text("Portion") },
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.fillMaxWidth().height(50.dp)
                                            )
                                            OutlinedTextField(
                                                value = editCals,
                                                onValueChange = { editCals = it },
                                                label = { Text("Calories") },
                                                textStyle = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.fillMaxWidth().height(50.dp)
                                            )
                                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                                TextButton(
                                                    onClick = { isEditing = false }
                                                ) { Text("Cancel") }
                                                TextButton(
                                                    onClick = { 
                                                        isEditing = false
                                                        viewModel.updateParsedItem(
                                                            index, 
                                                            item.copy(
                                                                portion = editPortion, 
                                                                estimatedCalories = editCals.toIntOrNull() ?: item.estimatedCalories
                                                            )
                                                        ) 
                                                    }
                                                ) {
                                                    Text("Save")
                                                }
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(item.matchedFoodName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                                                Text(item.portion, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("${item.estimatedCalories} kcal", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (parsedItems.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.saveParsedItemsAsMeals(parsedItems, analyzerMealType)
                            showAnalyzerDialog = false
                        },
                        modifier = Modifier.testTag("confirm_save_parsed_meals")
                    ) {
                        Text("Save to Meal Log")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAnalyzerDialog = false
                    viewModel.clearParsedFoodItems()
                }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: "What Should I Eat?" intelligent recommender
    if (showRecommenderDialog) {
        val selectedMealType by viewModel.selectedRecommendationMealType.collectAsState()
        val recommendations by viewModel.mealRecommendations.collectAsState()

        AlertDialog(
            onDismissRequest = { showRecommenderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = AmberWarmth)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("What Should I Eat?")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "I want ideas for:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { type ->
                            FilterChip(
                                selected = selectedMealType.equals(type, ignoreCase = true),
                                onClick = { viewModel.setRecommendationMealType(type) },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }

                    recommendations.forEach { suggestion ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = suggestion.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = suggestion.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "💡 ${suggestion.balanceTip}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "~${suggestion.estimatedCalories} kcal",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecommenderDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    // Dialog: Add Custom Food
    if (showAddCustomFoodDialog) {
        var name by remember { mutableStateOf("") }
        var serving by remember { mutableStateOf("1 serving") }
        var calories by remember { mutableStateOf("150") }
        var protein by remember { mutableStateOf("5") }
        var carbs by remember { mutableStateOf("20") }
        var fat by remember { mutableStateOf("4") }
        var fiber by remember { mutableStateOf("2") }

        AlertDialog(
            onDismissRequest = { showAddCustomFoodDialog = false },
            title = { Text("Create Custom Food") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Food Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = serving,
                        onValueChange = { serving = it },
                        label = { Text("Serving Size") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it },
                            label = { Text("Calories") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = protein,
                            onValueChange = { protein = it },
                            label = { Text("Protein (g)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = carbs,
                            onValueChange = { carbs = it },
                            label = { Text("Carbs (g)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = fat,
                            onValueChange = { fat = it },
                            label = { Text("Fat (g)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addCustomFood(
                                FoodEntity(
                                    name = name.trim(),
                                    servingSize = serving.trim(),
                                    calories = calories.toIntOrNull() ?: 150,
                                    protein = protein.toFloatOrNull() ?: 5f,
                                    carbs = carbs.toFloatOrNull() ?: 20f,
                                    fat = fat.toFloatOrNull() ?: 4f,
                                    fiber = fiber.toFloatOrNull() ?: 2f,
                                    category = "Custom",
                                    isCustom = true
                                )
                            )
                            showAddCustomFoodDialog = false
                        }
                    }
                ) {
                    Text("Save Food")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomFoodDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MacroColumn(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = color)
    }
}
