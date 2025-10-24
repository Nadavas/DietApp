package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications // REQUIRED: Import Notifications Icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
) {
    // --- STATE AND DATA ---
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val selectedDate by foodLogViewModel.selectedDateState.collectAsState()
    val currentWeekStartDate by foodLogViewModel.currentWeekStartDateState.collectAsState()
    val mealsForSelectedDate by foodLogViewModel.mealsForSelectedDate.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    val totalCaloriesForSelectedDate = remember(mealsForSelectedDate) {
        mealsForSelectedDate.sumOf { it.calories }
    }
    val goalCalories = remember(goals) {
        goals.firstOrNull()?.value?.toIntOrNull() ?: 2000
    }

    val missingGoals = remember(goals) {
        goals.filter { goal ->
            goal.value.isNullOrBlank() || goal.value == "0"
        }.map { goal ->
            when {
                goal.text.contains("calorie", ignoreCase = true) -> "Calorie"
                goal.text.contains("protein", ignoreCase = true) -> "Protein"
                else -> "Goal"
            }
        }.distinct()
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

    // --- THEME ---
    val screenBackgroundColor = Color(0xFFF7F9FC)
    val primaryActionColor = Color(0xFF4CAF50)

    // --- UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // RESTORED: Original HeaderSection logic
                    HeaderSection(
                        userName = userProfile.name ?: "User",
                        avatarId = userProfile.avatarId,
                        onAvatarClick = { navController.navigate(NavRoutes.MY_PROFILE) }
                    )
                },
                // NEW: Use actions slot to place a button on the right, left of the avatar logic.
                actions = {
                    IconButton(
                        onClick = { navController.navigate(NavRoutes.NOTIFICATIONS) }
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBackgroundColor,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        containerColor = screenBackgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { mealWithActionsShownId = null },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- MISSING GOALS WARNING ---
            if (missingGoals.isNotEmpty()) {
                item {
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

            // --- CALORIE SUMMARY DASHBOARD ---
            item {
                CalorieSummaryCard(
                    totalCalories = totalCaloriesForSelectedDate,
                    goalCalories = goalCalories
                )
            }

            // --- MEALS LIST ---
            if (mealsForSelectedDate.isEmpty()) {
                item { EmptyState() }
            } else {
                groupedMeals.forEach { (section, mealsInSection) ->
                    stickyHeader {
                        MealSectionHeader(section, Modifier.background(screenBackgroundColor))
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
            // Add space at the bottom so FAB doesn't hide content
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // --- DELETE CONFIRMATION DIALOG (No changes needed) ---
    if (showDeleteConfirmationDialog && mealToDelete != null) {
        StyledAlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false; mealToDelete = null },
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