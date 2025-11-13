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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.nadavariel.dietapp.ui.HomeColors.BackgroundGradient
import com.nadavariel.dietapp.ui.HomeColors.TextPrimary
import com.nadavariel.dietapp.ui.HomeColors.TextSecondary
import com.nadavariel.dietapp.ui.HomeColors.PageBackgroundColor
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel

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
    val selectedDate by foodLogViewModel.selectedDateState.collectAsState()
    val currentWeekStartDate by foodLogViewModel.currentWeekStartDateState.collectAsState()
    val mealsForSelectedDate by foodLogViewModel.mealsForSelectedDate.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    // --- FIX 1: Collect the loading and diet plan states ---
    val dietPlan by goalViewModel.currentDietPlan.collectAsStateWithLifecycle()
    val isLoadingPlan by goalViewModel.isLoadingPlan.collectAsStateWithLifecycle()

    val weightHistory by foodLogViewModel.weightHistory.collectAsState()
    val targetWeight by foodLogViewModel.targetWeight.collectAsState()

    var showLogWeightDialog by remember { mutableStateOf(false) }
    var showManageWeightDialog by remember { mutableStateOf(false) }
    var weightEntryToEdit by remember { mutableStateOf<WeightEntry?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<Meal?>(null) }
    var mealWithActionsShownId by remember { mutableStateOf<String?>(null) }

    var showWeightGraphDialog by remember { mutableStateOf(false) }

    val totalCaloriesForSelectedDate = remember(mealsForSelectedDate) {
        mealsForSelectedDate.sumOf { it.calories }
    }
    val goalCalories = remember(goals) {
        goals.firstOrNull()?.value?.toIntOrNull() ?: 2000
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

    if (showWeightGraphDialog) {
        WeightGraphDialog(
            startingWeight = userProfile.startingWeight,
            targetWeight = targetWeight,
            history = weightHistory,
            onDismiss = { showWeightGraphDialog = false },
            onManageClick = {
                showWeightGraphDialog = false
                showManageWeightDialog = true
            },
            onAddClick = {
                showWeightGraphDialog = false
                weightEntryToEdit = null
                showLogWeightDialog = true
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(BackgroundGradient))
    ) {
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

                // --- FIX 2: Alert logic is now based on loading state and diet plan ---
                if (!isLoadingPlan && dietPlan == null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(NavRoutes.QUESTIONS) }, // Navigate to Questions
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Answer the Questionnaire",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "Complete the questionnaire to get your personalized diet plan.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    text = "START",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                // --- END OF CHANGE ---

                item {
                    CompactWeightDateRow(
                        startingWeight = userProfile.startingWeight,
                        currentWeight = weightHistory.lastOrNull()?.weight
                            ?: userProfile.startingWeight,
                        targetWeight = targetWeight,
                        history = weightHistory,
                        onWeightClick = {
                            weightEntryToEdit = null
                            showLogWeightDialog = true
                        },
                        onGraphClick = {
                            showWeightGraphDialog = true
                        }
                    )
                }

                item {
                    DatePickerSection(
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

                item {
                    CalorieSummaryCard(
                        totalCalories = totalCaloriesForSelectedDate,
                        goalCalories = goalCalories
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Meals",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (mealsForSelectedDate.isNotEmpty()) {
                            Text(
                                text = "${mealsForSelectedDate.size} meals",
                                fontSize = 14.sp,
                                color = TextSecondary
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
                                Modifier.background(
                                    PageBackgroundColor
                                )
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