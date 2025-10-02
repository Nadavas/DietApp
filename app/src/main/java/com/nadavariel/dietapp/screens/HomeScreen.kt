package com.nadavariel.dietapp.screens

import android.app.DatePickerDialog
import android.os.Build
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.nadavariel.dietapp.util.AvatarConstants
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class) // Needed for stickyHeader
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

    // New State: Collect missing goals from ViewModel
    val missingGoals by goalViewModel.missingGoals.collectAsState()

    val totalCaloriesForSelectedDate = remember(mealsForSelectedDate) {
        mealsForSelectedDate.sumOf { it.calories }
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
        // The main screen is now a LazyColumn for better performance and to allow for sticky headers.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    mealWithActionsShownId = null // Dismiss actions when clicking background
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

            // New UI: Display missing goals warning if needed
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
                    // A goal is assumed for the progress bar.
                    goalCalories = goals.firstOrNull()?.value?.toIntOrNull() ?: 2000
                )
            }

            // --- MEALS LIST ---
            if (mealsForSelectedDate.isEmpty()) {
                item {
                    EmptyState()
                }
            } else {
                groupedMeals.forEach { (section, mealsInSection) ->
                    // Key UI Change: Sticky headers provide better context while scrolling.
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

    // --- DELETE CONFIRMATION DIALOG (LOGIC UNCHANGED) ---
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


// --------------------------------------------------------------------------------
// |                           NEW & IMPROVED COMPOSABLES                         |
// --------------------------------------------------------------------------------

@Composable
private fun HeaderSection(userName: String, avatarId: String?, onAvatarClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Welcome,",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = userName.ifBlank { "Guest" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Image(
            painter = painterResource(id = AvatarConstants.getAvatarResId(avatarId)),
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .clickable(onClick = onAvatarClick)
                .shadow(elevation = 4.dp, shape = CircleShape)
        )
    }
}

// New Composable: Missing Goals Warning
@Composable
private fun MissingGoalsWarning(missingGoals: List<String>, onSetGoalsClick: () -> Unit) {
    val missingListText = missingGoals.joinToString(" and ")
    val message = "Your ${missingListText} goal${if (missingGoals.size > 1) "s" else ""} are missing."

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(onClick = onSetGoalsClick) {
                Text("SET GOALS")
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DatePickerSection(
    currentWeekStartDate: LocalDate,
    selectedDate: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onGoToToday: () -> Unit
) {
    val context = LocalContext.current
    val weekDays = remember(currentWeekStartDate) {
        (0..6).map { currentWeekStartDate.plusDays(it.toLong()) }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Month/Year and navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPreviousWeek) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous Week")
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        val calendar = Calendar.getInstance()
                        calendar.set(
                            selectedDate.year,
                            selectedDate.monthValue - 1,
                            selectedDate.dayOfMonth
                        )
                        DatePickerDialog(
                            context,
                            { _: DatePicker, year, month, day ->
                                onDateSelected(LocalDate.of(year, month + 1, day))
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Select Date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = selectedDate.format(formatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onNextWeek) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next Week")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days of the week selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekDays.forEach { date ->
                DayOfWeekItem(
                    modifier = Modifier.weight(1f),
                    date = date,
                    isSelected = date == selectedDate,
                    onClick = { onDateSelected(date) }
                )
            }
        }

        // Today button
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(
                onClick = onGoToToday,
                enabled = selectedDate != LocalDate.now()
            ) {
                Text("Go to Today")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DayOfWeekItem(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = spring(), label = "day_background_color"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(), label = "day_content_color"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
private fun CalorieSummaryCard(totalCalories: Int, goalCalories: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Daily Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$totalCalories",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "/ $goalCalories kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            val progress = (totalCalories.toFloat() / goalCalories.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
fun MealSectionHeader(section: MealSection) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(section.color, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = section.sectionName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "No meals logged for this day.\nTime to add one! 🥗",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MealItem(
    meal: Meal,
    sectionColor: Color,
    showActions: Boolean,
    onToggleActions: (String) -> Unit,
    onDelete: (Meal) -> Unit,
    onEdit: (Meal) -> Unit
) {
    // Key UI Change: A gradient background makes the card pop.
    val cardBrush = Brush.horizontalGradient(
        colors = listOf(
            sectionColor.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBrush)
            .clickable { onToggleActions(meal.id) }
    ) {
        // Key UI Change: smooth expansion animation
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(animationSpec = spring())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Meal Info
                Column(modifier = Modifier.weight(1f)) {
                    val servingInfo = if (!meal.servingAmount.isNullOrBlank() && !meal.servingUnit.isNullOrBlank()) {
                        "(${meal.servingAmount} ${meal.servingUnit})"
                    } else ""
                    Text(
                        text = meal.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = servingInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Calories or Actions
                AnimatedContent(
                    targetState = showActions,
                    transitionSpec = {
                        (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                    }, label = "actions_calories_switch"
                ) { targetState ->
                    if (targetState) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { onEdit(meal) }) {
                                Icon(Icons.Default.Edit, "Edit Meal", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDelete(meal) }) {
                                Icon(Icons.Default.Delete, "Delete Meal", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${meal.calories} kcal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = sectionColor
                            )
                            val formattedTime = meal.timestamp.toDate().toInstant()
                                .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Collapsible nutritional information
            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Column {
                    Divider(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))
                    // 🌟 KEY CHANGE 1: Update to new NutritionDetailsTable
                    NutritionDetailsTable(meal)
                }
            }
        }
    }
}

// 🌟 KEY CHANGE 2: Updated NutritionDetailsTable to include all 7 new nutrients
@Composable
fun NutritionDetailsTable(meal: Meal) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Macronutrients (g)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Row 1: Protein, Carbs, Fat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NutritionDetailItem("Protein", meal.protein, "g")
            NutritionDetailItem("Carbs", meal.carbohydrates, "g")
            NutritionDetailItem("Fat", meal.fat, "g")
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "Micronutrients & Fiber",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Row 2: Fiber, Sugar, Sodium (g/mg)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NutritionDetailItem("Fiber", meal.fiber, "g")
            NutritionDetailItem("Sugar", meal.sugar, "g")
            NutritionDetailItem("Sodium", meal.sodium, "mg")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 3: Potassium, Calcium, Iron, Vitamin C (mg)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NutritionDetailItem("Potassium", meal.potassium, "mg")
            NutritionDetailItem("Calcium", meal.calcium, "mg")
            NutritionDetailItem("Iron", meal.iron, "mg")
            NutritionDetailItem("Vit C", meal.vitaminC, "mg")
        }
    }
}

// 🌟 KEY CHANGE 3: Updated NutritionDetailItem to accept a unit
@Composable
fun RowScope.NutritionDetailItem(label: String, value: Double?, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value?.let { "${String.format("%.1f", it)}${unit}" } ?: "–",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}