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
import com.nadavariel.dietapp.model.WeightEntry
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
    navController: NavController,
    openWeightLog: Boolean = false
) {
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val isLoadingProfile by authViewModel.isLoadingProfile.collectAsStateWithLifecycle()

    val selectedDate by foodLogViewModel.selectedDateState.collectAsState()
    val currentWeekStartDate by foodLogViewModel.currentWeekStartDateState.collectAsState()
    val mealsForSelectedDate by foodLogViewModel.mealsForSelectedDate.collectAsState()
    val weightHistory by foodLogViewModel.weightHistory.collectAsState()
    val targetWeight by foodLogViewModel.targetWeight.collectAsState()
    val isLoadingLogs by foodLogViewModel.isLoadingLogs.collectAsStateWithLifecycle()

    val goals by goalViewModel.goals.collectAsState()
    val dietPlan by goalViewModel.currentDietPlan.collectAsStateWithLifecycle()
    val isLoadingPlan by goalViewModel.isLoadingPlan.collectAsStateWithLifecycle()

    val isScreenLoading = isLoadingProfile || isLoadingLogs || isLoadingPlan

    var showLogWeightDialog by remember { mutableStateOf(false) }
    var showManageWeightDialog by remember { mutableStateOf(false) }
    var weightEntryToEdit by remember { mutableStateOf<WeightEntry?>(null) }
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

    LaunchedEffect(openWeightLog) {
        if (openWeightLog) {
            showLogWeightDialog = true
        }
    }

    val groupedMeals = remember(mealsForSelectedDate) {
        mealsForSelectedDate
            .groupBy { meal -> MealSection.getMealSection(meal.timestamp.toDate()) }
            .toSortedMap(compareBy { it.ordinal })
    }

    if (showLogWeightDialog) {
        LogWeightDialog(
            entryToEdit = weightEntryToEdit,
            onDismiss = {
                showLogWeightDialog = false
                weightEntryToEdit = null
            },
            onSave = { weight, date ->
                foodLogViewModel.addWeightEntry(weight, date)
                showLogWeightDialog = false
                weightEntryToEdit = null
            },
            onUpdate = { id, weight, date ->
                foodLogViewModel.updateWeightEntry(id, weight, date)
                showLogWeightDialog = false
                weightEntryToEdit = null
            }
        )
    }

    if (showManageWeightDialog) {
        ManageWeightHistoryDialog(
            history = weightHistory,
            onDismiss = { showManageWeightDialog = false },
            onEdit = { entry ->
                weightEntryToEdit = entry
                showManageWeightDialog = false
                showLogWeightDialog = true
            },
            onDelete = { entry ->
                foodLogViewModel.deleteWeightEntry(entry.id)
            }
        )
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

                    // --- SECTION 1: WEIGHT (Top Hierarchy) ---
                    item {
                        val currentWeight = weightHistory.lastOrNull()?.weight ?: userProfile.startingWeight
                        val startWeight = userProfile.startingWeight

                        WeightStatusCard(
                            currentWeight = currentWeight,
                            startWeight = startWeight,
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

// --- NEW REDESIGNED COMPONENT ---
@Composable
fun WeightStatusCard(
    currentWeight: Float,
    startWeight: Float,
    onClick: () -> Unit
) {
    // Calculate logic
    val difference = currentWeight - startWeight
    val isLoss = difference < 0
    val isGain = difference > 0
    val absoluteDiff = abs(difference)

    // Visual configurations
    val trendIcon = when {
        isLoss -> Icons.Rounded.TrendingDown
        isGain -> Icons.Rounded.TrendingUp
        else -> Icons.Rounded.TrendingFlat
    }

    // In diet apps, weight loss is usually "Green" (Success)
    val trendColor = when {
        isLoss -> AppTheme.colors.primaryGreen
        isGain -> AppTheme.colors.warmOrange // Or a warning color
        else -> AppTheme.colors.textSecondary
    }

    val formattedDiff = "%.1f".format(absoluteDiff)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            AppTheme.colors.primaryGreen.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Current Status
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CURRENT WEIGHT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currentWeight kg",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AppTheme.colors.textPrimary
                        )
                    )
                }

                // Right Side: Progress Pill & Action
                Column(horizontalAlignment = Alignment.End) {
                    // Trend Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = trendColor.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = trendIcon,
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (difference.toDouble() == 0.0) "No change" else "$formattedDiff kg",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = trendColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Call to Action
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "View Graph",
                            style = MaterialTheme.typography.labelSmall,
                            color = AppTheme.colors.textSecondary.copy(alpha = 0.7f)
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = AppTheme.colors.textSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}