package com.nadavariel.dietapp.screens

import android.app.DatePickerDialog
import android.os.Build
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.model.MealSection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.nadavariel.dietapp.util.AvatarConstants
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    foodLogViewModel: FoodLogViewModel,
    navController: NavController,
) {
    // Get data from the viewmodel
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val userName = userProfile.name
    val selectedDate by foodLogViewModel.selectedDateState.collectAsState()
    val currentWeekStartDate by foodLogViewModel.currentWeekStartDateState.collectAsState()
    val mealsForSelectedDate by foodLogViewModel.mealsForSelectedDate.collectAsState()

    val totalCaloriesForSelectedDate = remember(mealsForSelectedDate) {
        mealsForSelectedDate.sumOf { it.calories }
    }

    // State variables
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<Meal?>(null) }
    var mealWithActionsShownId by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Ensure the log starts on the current day
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            foodLogViewModel.selectDate(LocalDate.now())
        }
    }

    // Groups the meals by their respective MealSection
    val groupedMeals = remember(mealsForSelectedDate) {
        mealsForSelectedDate
            .groupBy { meal -> MealSection.getMealSection(meal.timestamp.toDate()) }
            .toSortedMap(compareBy { it.ordinal })
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    mealWithActionsShownId = null
                }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(0.dp))

                // Welcome
                Text(
                    text = "Welcome, ${userName.ifBlank { "Guest" }}!",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                // Avatar Display
                Image(
                    painter = painterResource(id = AvatarConstants.getAvatarResId(userProfile.avatarId)),
                    contentDescription = "User Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(128.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { navController.navigate(NavRoutes.MY_PROFILE) }
                )

                // Week selection row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { foodLogViewModel.previousWeek() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous Week", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    // Month/Year and Today button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val startMonth = currentWeekStartDate.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
                        val endMonth = currentWeekStartDate.plusDays(6).format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
                        val year = currentWeekStartDate.format(DateTimeFormatter.ofPattern("yyyy", Locale.getDefault()))

                        val monthDisplay = if (startMonth == endMonth) {
                            "$startMonth $year"
                        } else {
                            "$startMonth - $endMonth $year"
                        }

                        Text(
                            text = monthDisplay,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable {
                                val calendar = Calendar.getInstance()
                                calendar.set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)

                                DatePickerDialog(
                                    context,
                                    { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
                                        val newDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDayOfMonth)
                                        foodLogViewModel.selectDate(newDate)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                        )
                    }

                    IconButton(onClick = { foodLogViewModel.nextWeek() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next Week", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Week dates
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val weekDays = remember(currentWeekStartDate) {
                        (0..6).map { currentWeekStartDate.plusDays(it.toLong()) }
                    }
                    weekDays.forEach { date ->
                        val isSelected = date == selectedDate
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    foodLogViewModel.selectDate(date)
                                    mealWithActionsShownId = null
                                }
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Today Button
                TextButton(
                    onClick = { foodLogViewModel.goToToday() },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text("Today")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Total Calories for the selected day
                Text(
                    text = "Total Calories: $totalCaloriesForSelectedDate kcal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // The meals for the selected day
                if (mealsForSelectedDate.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No meals logged for this day.\n Click 'Add Meal' to log one!",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        var isFirstSection = true

                        MealSection.entries.forEach { section ->
                            val mealsInSection = groupedMeals[section] ?: emptyList()
                            if (mealsInSection.isNotEmpty()) {
                                item {
                                    if (!isFirstSection) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    Text(
                                        text = section.sectionName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = section.color,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    )
                                    isFirstSection = false
                                }
                                items(mealsInSection) { meal ->
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
            }
        }
    }

    // Confirmation dialog for meal deletion
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

// Composable for a single meal item in the list
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleActions(meal.id) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // This is the new change: The meal item content is now a column
        // to accommodate the collapsible sub-table.
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // First Row: Meal Name, Time, and Actions/Calories
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    val servingInfo = if (!meal.servingAmount.isNullOrBlank() && !meal.servingUnit.isNullOrBlank()) {
                        " (${meal.servingAmount} ${meal.servingUnit})"
                    } else {
                        ""
                    }
                    Text(
                        text = "${meal.foodName}$servingInfo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = sectionColor
                    )
                    Text(
                        text = meal.timestamp.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = MaterialTheme.typography.bodySmall,
                        color = sectionColor.copy(alpha = 0.7f)
                    )
                }

                if (showActions) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        IconButton(onClick = { onEdit(meal) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Meal", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { onDelete(meal) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Meal", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Text(
                        text = "${meal.calories} kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = sectionColor,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // This is the new collapsible sub-table for nutritional information.
            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    NutritionDetailsTable(meal)
                }
            }
        }
    }
}

// New composable function for displaying the nutritional table.
@Composable
fun NutritionDetailsTable(meal: Meal) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NutritionDetailItem("Protein", meal.protein)
            NutritionDetailItem("Carbs", meal.carbohydrates)
            NutritionDetailItem("Fat", meal.fat)
        }
    }
}

// New composable function for each individual nutrition detail.
@Composable
fun NutritionDetailItem(label: String, value: Double?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (value != null) "${String.format("%.1f", value)}g" else "N/A",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}