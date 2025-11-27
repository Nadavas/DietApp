package com.nadavariel.dietapp.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.model.MealSection
import com.nadavariel.dietapp.ui.account.StyledAlertDialog
import com.nadavariel.dietapp.ui.home.*
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import kotlin.math.abs

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    foodLogViewModel: FoodLogViewModel,
    goalViewModel: GoalsViewModel = viewModel(),
    navController: NavController
) {
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val isLoadingProfile by authViewModel.isLoadingProfile.collectAsStateWithLifecycle()

    val selectedDate by foodLogViewModel.selectedDateState.collectAsState()
    val currentWeekStartDate by foodLogViewModel.currentWeekStartDateState.collectAsState()
    val mealsForSelectedDate by foodLogViewModel.mealsForSelectedDate.collectAsState()
    val weightHistory by foodLogViewModel.weightHistory.collectAsState()
    val isLoadingLogs by foodLogViewModel.isLoadingLogs.collectAsStateWithLifecycle()

    val goals by goalViewModel.goals.collectAsState()
    val dietPlan by goalViewModel.currentDietPlan.collectAsStateWithLifecycle()
    val isLoadingPlan by goalViewModel.isLoadingPlan.collectAsStateWithLifecycle()

    val isScreenLoading = isLoadingProfile || isLoadingLogs || isLoadingPlan

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<Meal?>(null) }
    var mealWithActionsShownId by remember { mutableStateOf<String?>(null) }

    // State for the nutrition summary dialog
    var showDailyTotalsDialog by remember { mutableStateOf(false) }

    val totalCaloriesForSelectedDate = remember(mealsForSelectedDate) {
        mealsForSelectedDate.sumOf { it.calories }
    }
    val goalCalories = remember(goals) {
        goals.firstOrNull()?.value?.toIntOrNull() ?: 0
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            foodLogViewModel.resetDateToTodayIfNeeded()
        }
    }

    val groupedMeals = remember(mealsForSelectedDate) {
        mealsForSelectedDate
            .groupBy { meal -> MealSection.getMealSection(meal.timestamp.toDate()) }
            .toSortedMap(compareBy { it.ordinal })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(AppTheme.colors.homeGradient))
    ) {
        if (isScreenLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppTheme.colors.primaryGreen)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                ModernHomeHeader(
                    userName = userProfile.name,
                    avatarId = userProfile.avatarId,
                    onAvatarClick = { navController.navigate(NavRoutes.MY_PROFILE) }
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { mealWithActionsShownId = null },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    if (dietPlan == null) {
                        item { /* Placeholder */ }
                    }

                    // --- SECTION 1: WEIGHT ---
                    item {
                        val currentWeight = weightHistory.lastOrNull()?.weight ?: userProfile.startingWeight
                        val startWeight = userProfile.startingWeight
                        val targetWeight by foodLogViewModel.targetWeight.collectAsState()

                        WeightStatusCard(
                            currentWeight = currentWeight,
                            startWeight = startWeight,
                            targetWeight = targetWeight,
                            onClick = { navController.navigate(NavRoutes.WEIGHT_TRACKER) }
                        )
                    }

                    // --- SECTION 2: CALORIES ---
                    item {
                        CardSectionHeader("Calorie Tracker")
                    }

                    item {
                        CalorieSummaryCard(
                            totalCalories = totalCaloriesForSelectedDate,
                            goalCalories = goalCalories,
                            currentWeekStartDate = currentWeekStartDate,
                            selectedDate = selectedDate,
                            onPreviousWeek = { foodLogViewModel.previousWeek() },
                            onNextWeek = { foodLogViewModel.nextWeek() },
                            onDateSelected = { date ->
                                foodLogViewModel.selectDate(date)
                                mealWithActionsShownId = null
                            },
                            onGoToToday = { foodLogViewModel.goToToday() }
                        )
                    }

                    // --- SECTION 3: MEALS ---
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: Title + Summary Button
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Today's Meals",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.textPrimary
                                )

                                // FIXED: Only show button if there are meals
                                if (mealsForSelectedDate.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { showDailyTotalsDialog = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Assessment,
                                            contentDescription = "Calculate Daily Totals",
                                            tint = AppTheme.colors.primaryGreen
                                        )
                                    }
                                }
                            }

                            // Right side: Meal count
                            if (mealsForSelectedDate.isNotEmpty()) {
                                Text(
                                    text = "${mealsForSelectedDate.size} meals",
                                    fontSize = 14.sp,
                                    color = AppTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    if (mealsForSelectedDate.isEmpty()) {
                        item { EmptyState() }
                    } else {
                        groupedMeals.forEach { (section, mealsInSection) ->
                            stickyHeader {
                                MealSectionHeader(
                                    section,
                                    Modifier.background(AppTheme.colors.screenBackground)
                                )
                            }
                            items(mealsInSection, key = { it.id }) { meal ->
                                MealItem(
                                    meal = meal,
                                    sectionColor = section.color,
                                    showActions = mealWithActionsShownId == meal.id,
                                    onToggleActions = { clickedMealId ->
                                        mealWithActionsShownId =
                                            if (mealWithActionsShownId == clickedMealId) null else clickedMealId
                                    },
                                    onDelete = {
                                        mealToDelete = it
                                        showDeleteConfirmationDialog = true
                                        mealWithActionsShownId = null
                                    },
                                    onEdit = {
                                        navController.navigate("${NavRoutes.ADD_EDIT_MEAL}/${it.id}")
                                        mealWithActionsShownId = null
                                    }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showDeleteConfirmationDialog && mealToDelete != null) {
        StyledAlertDialog(
            onDismissRequest = {
                showDeleteConfirmationDialog = false; mealToDelete = null
            },
            title = "Confirm Deletion",
            text = "Are you sure you want to delete this meal: ${mealToDelete?.foodName}?",
            confirmButtonText = "Delete",
            dismissButtonText = "Cancel",
            onConfirm = {
                mealToDelete?.let { foodLogViewModel.deleteMeal(it.id) }
                showDeleteConfirmationDialog = false
                mealToDelete = null
            }
        )
    }

    // Daily Nutrition Summary Dialog
    if (showDailyTotalsDialog) {
        val totalProtein = mealsForSelectedDate.sumOf { it.protein ?: 0.0 }
        val totalCarbs = mealsForSelectedDate.sumOf { it.carbohydrates ?: 0.0 }
        val totalFat = mealsForSelectedDate.sumOf { it.fat ?: 0.0 }
        val totalFiber = mealsForSelectedDate.sumOf { it.fiber ?: 0.0 }
        val totalSugar = mealsForSelectedDate.sumOf { it.sugar ?: 0.0 }
        val totalSodium = mealsForSelectedDate.sumOf { it.sodium ?: 0.0 }
        val totalPotassium = mealsForSelectedDate.sumOf { it.potassium ?: 0.0 }
        val totalCalcium = mealsForSelectedDate.sumOf { it.calcium ?: 0.0 }
        val totalIron = mealsForSelectedDate.sumOf { it.iron ?: 0.0 }
        val totalVitC = mealsForSelectedDate.sumOf { it.vitaminC ?: 0.0 }
        val totalVitA = mealsForSelectedDate.sumOf { it.vitaminA ?: 0.0 }
        val totalVitB12 = mealsForSelectedDate.sumOf { it.vitaminB12 ?: 0.0 }

        AlertDialog(
            onDismissRequest = { showDailyTotalsDialog = false },
            title = {
                Text("Daily Nutrition Summary", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Macronutrients
                    NutritionRow("Protein", totalProtein, "g", AppTheme.colors.primaryGreen)
                    // FIXED: Switched colors for Carbs and Fat
                    NutritionRow("Carbohydrates", totalCarbs, "g", AppTheme.colors.activeLifestyle)
                    NutritionRow("Fat", totalFat, "g", AppTheme.colors.disclaimerIcon)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = AppTheme.colors.textSecondary.copy(alpha = 0.2f))

                    // Other Nutrients
                    NutritionRow("Fiber", totalFiber, "g", AppTheme.colors.textPrimary)
                    NutritionRow("Sugar", totalSugar, "g", AppTheme.colors.textPrimary)
                    NutritionRow("Sodium", totalSodium, "mg", AppTheme.colors.textPrimary)
                    NutritionRow("Potassium", totalPotassium, "mg", AppTheme.colors.textPrimary)
                    NutritionRow("Calcium", totalCalcium, "mg", AppTheme.colors.textPrimary)
                    NutritionRow("Iron", totalIron, "mg", AppTheme.colors.textPrimary)
                    NutritionRow("Vitamin C", totalVitC, "mg", AppTheme.colors.textPrimary)
                    NutritionRow("Vitamin A", totalVitA, "mcg", AppTheme.colors.textPrimary)
                    NutritionRow("Vitamin B12", totalVitB12, "mcg", AppTheme.colors.textPrimary)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDailyTotalsDialog = false }) {
                    Text("Close", color = AppTheme.colors.primaryGreen)
                }
            },
            containerColor = androidx.compose.ui.graphics.Color.White
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun NutritionRow(label: String, value: Double, unit: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label:", color = AppTheme.colors.textPrimary)
        Text(
            text = String.format("%.1f %s", value, unit),
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun CardSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.textPrimary,
        modifier = modifier.padding(start = 4.dp)
    )
}

@Composable
fun WeightStatusCard(
    currentWeight: Float,
    startWeight: Float,
    targetWeight: Float,
    onClick: () -> Unit
) {
    // 1. Math Logic (Same as WeightScreen)
    val totalGoalDiff = abs(targetWeight - startWeight)
    val isWeightLossGoal = targetWeight < startWeight

    val progressAmount = if (isWeightLossGoal) {
        startWeight - currentWeight
    } else {
        currentWeight - startWeight
    }

    // Clamp percentage between 0-100
    val progressPercentage = if (totalGoalDiff > 0) {
        (progressAmount / totalGoalDiff * 100f).coerceIn(0f, 100f)
    } else 0f

    // 2. Text & Status Logic
    val rawDiff = currentWeight - startWeight
    val actuallyGained = rawDiff > 0
    val progressText = if (actuallyGained) "gained" else "lost"
    val formattedProgress = "%.1f".format(abs(rawDiff))

    // Check for Setback (Moved wrong way) to highlight text
    val isSetback = progressPercentage == 0f && abs(rawDiff) > 0.1f

    // 3. Updated Badges (Matches WeightScreen)
    val badge = when {
        progressPercentage >= 100f -> "🏆"
        progressPercentage >= 75f -> "🥇"
        progressPercentage >= 50f -> "🥈"
        progressPercentage >= 25f -> "🥉"
        progressPercentage >= 10f -> "⚡" // Added "The Spark"
        else -> null
    }

    val badgeTitle = when {
        progressPercentage >= 100f -> "Champion"
        progressPercentage >= 75f -> "Almost There"
        progressPercentage >= 50f -> "Halfway"
        progressPercentage >= 25f -> "First Steps"
        progressPercentage >= 10f -> "The Spark"
        else -> null
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WEIGHT PROGRESS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${progressPercentage.toInt()}%",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            // Use Orange if setback, Green if progress, otherwise default
                            color = if (isSetback) AppTheme.colors.warmOrange else AppTheme.colors.primaryGreen
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "complete",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                // Status text (Orange if setback)
                Text(
                    text = if (isSetback) "⚠️ $formattedProgress kg $progressText" else "$formattedProgress kg $progressText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSetback) AppTheme.colors.warmOrange else AppTheme.colors.textSecondary,
                    fontWeight = if (isSetback) FontWeight.Medium else FontWeight.Normal
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (badge != null && badgeTitle != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(
                                        AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                                        AppTheme.colors.primaryGreen.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badge,
                            fontSize = 28.sp
                        )
                    }
                    Text(
                        text = badgeTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.primaryGreen,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "View details",
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}