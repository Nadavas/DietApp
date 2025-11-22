package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    foodLogViewModel: FoodLogViewModel,
    goalViewModel: GoalsViewModel = viewModel(),
    navController: NavController
    // REMOVED: openWeightLog parameter is no longer needed here
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

    // REMOVED: Weight dialog states (showLogWeightDialog, showManageWeightDialog, weightEntryToEdit)

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<Meal?>(null) }
    var mealWithActionsShownId by remember { mutableStateOf<String?>(null) }

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

    // REMOVED: LaunchedEffect(openWeightLog) - Moved to WeightScreen logic

    val groupedMeals = remember(mealsForSelectedDate) {
        mealsForSelectedDate
            .groupBy { meal -> MealSection.getMealSection(meal.timestamp.toDate()) }
            .toSortedMap(compareBy { it.ordinal })
    }

    // REMOVED: LogWeightDialog and ManageWeightHistoryDialog composables

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

                    // --- SECTION 1: WEIGHT (Top Hierarchy) ---
                    item {
                        val currentWeight = weightHistory.lastOrNull()?.weight ?: userProfile.startingWeight
                        val startWeight = userProfile.startingWeight
                        val targetWeight by foodLogViewModel.targetWeight.collectAsState() // Add this line

                        WeightStatusCard(
                            currentWeight = currentWeight,
                            startWeight = startWeight,
                            targetWeight = targetWeight, // Add this parameter
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
                            Text(
                                text = "Today's Meals",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary
                            )
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
    val totalGoal = abs(targetWeight - startWeight)
    val currentProgress = abs(currentWeight - startWeight)
    val progressPercentage = if (totalGoal > 0) (currentProgress / totalGoal * 100f).coerceIn(0f, 100f) else 0f

    val isGaining = targetWeight > startWeight
    val progressText = if (isGaining) "gained" else "lost"
    val formattedProgress = "%.1f".format(abs(currentWeight - startWeight))

    // Determine badge
    val badge = when {
        progressPercentage >= 75f -> "🥇"
        progressPercentage >= 50f -> "🥈"
        progressPercentage >= 25f -> "🥉"
        else -> null
    }

    val badgeTitle = when {
        progressPercentage >= 75f -> "Almost There!"
        progressPercentage >= 50f -> "Halfway Hero"
        progressPercentage >= 25f -> "First Steps"
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
            // Left side - Progress info
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
                            color = AppTheme.colors.primaryGreen
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
                Text(
                    text = "$formattedProgress kg $progressText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary
                )
            }

            // Right side - Badge or arrow
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