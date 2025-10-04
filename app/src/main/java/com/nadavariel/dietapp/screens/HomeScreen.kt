package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.model.MealSection
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import com.nadavariel.dietapp.ui.home.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    foodLogViewModel: FoodLogViewModel,
    goalViewModel: GoalsViewModel = viewModel(),
    navController: NavController,
) {
    // --- STATE AND DATA ---
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val selectedDate by foodLogViewModel.selectedDateState.collectAsState()
    val currentWeekStartDate by foodLogViewModel.currentWeekStartDateState.collectAsState()
    val mealsForSelectedDate by foodLogViewModel.mealsForSelectedDate.collectAsState()
    val goals by goalViewModel.goals.collectAsState()
    val missingGoals by goalViewModel.missingGoals.collectAsState()

    val totalCaloriesForSelectedDate = remember(mealsForSelectedDate) {
        mealsForSelectedDate.sumOf { it.calories }
    }
    val goalCalories = remember(goals) {
        goals.firstOrNull()?.value?.toIntOrNull() ?: 2000
    }

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<Meal?>(null) }
    var mealWithActionsShownId by remember { mutableStateOf<String?>(null) }

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

    // --- UI ---
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        androidx.compose.foundation.lazy.LazyColumn( // Full import to distinguish from local imports
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // Dismiss actions when clicking background
                    mealWithActionsShownId = null
                },
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // --- HEADER SECTION ---
            item {
                HeaderSection(
                    userName = userProfile.name,
                    avatarId = userProfile.avatarId,
                    onAvatarClick = { navController.navigate(NavRoutes.MY_PROFILE) }
                )
            }

            // Missing goals warning
            item {
                if (missingGoals.isNotEmpty()) {
                    MissingGoalsWarning(
                        missingGoals = missingGoals,
                        onSetGoalsClick = { navController.navigate(NavRoutes.GOALS) }
                    )
                }
            }

            // --- DATE PICKER SECTION ---
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

            // --- CALORIE SUMMARY CARD ---
            item {
                CalorieSummaryCard(
                    totalCalories = totalCaloriesForSelectedDate,
                    goalCalories = goalCalories
                )
            }

            // --- MEALS LIST ---
            if (mealsForSelectedDate.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                groupedMeals.forEach { (section, mealsInSection) ->
                    stickyHeader {
                        MealSectionHeader(section)
                    }
                    items(mealsInSection, key = { it.id }) { meal ->
                        MealItem(
                            meal = meal,
                            sectionColor = section.color,
                            showActions = mealWithActionsShownId == meal.id,
                            onToggleActions = { clickedMealId ->
                                mealWithActionsShownId = if (mealWithActionsShownId == clickedMealId) null else clickedMealId
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
        }
    }

    // --- DELETE CONFIRMATION DIALOG ---
    if (showDeleteConfirmationDialog && mealToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this meal: ${mealToDelete?.foodName}?") },
            confirmButton = {
                TextButton(onClick = {
                    mealToDelete?.let { foodLogViewModel.deleteMeal(it.id) }
                    showDeleteConfirmationDialog = false
                    mealToDelete = null
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirmationDialog = false
                    mealToDelete = null
                }) { Text("No") }
            }
        )
    }
}