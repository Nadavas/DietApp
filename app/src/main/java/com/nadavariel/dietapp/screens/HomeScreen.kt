package com.nadavariel.dietapp.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import com.nadavariel.dietapp.AuthViewModel
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.model.MealSection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.interaction.MutableInteractionSource

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    foodLogViewModel: FoodLogViewModel,
    navController: NavController,
    onSignOut: () -> Unit
) {
    val userProfile = authViewModel.userProfile
    val userName = userProfile.name

    val selectedDate = foodLogViewModel.selectedDate
    val currentWeekStartDate = foodLogViewModel.currentWeekStartDate
    val mealsForSelectedDate = foodLogViewModel.mealsForSelectedDate
    Log.d("HomeScreen", "Composable Recomposition: selectedDate=$selectedDate, currentWeekStartDate=$currentWeekStartDate")

    val totalCaloriesForSelectedDate = remember(mealsForSelectedDate) {
        mealsForSelectedDate.sumOf { it.calories }
    }

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showMealsList by remember { mutableStateOf(false) }

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<Meal?>(null) }

    var mealWithActionsShownId by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            Log.d("HomeScreen", "LaunchedEffect: App RESUMED. Current selectedDate in VM: ${foodLogViewModel.selectedDate}. Current system date: ${LocalDate.now()}")
            foodLogViewModel.selectDate(LocalDate.now())
            Log.d("HomeScreen", "LaunchedEffect: Called selectDate(LocalDate.now()) to ensure refresh.")
        }
    }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Spacer(Modifier.weight(1f))

                    IconButton(onClick = {
                        showSignOutDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(0.dp))

                Text(
                    text = "Welcome, ${userName.ifBlank { "Guest" }}!",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { foodLogViewModel.nextWeek() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next Week", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

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
                Spacer(modifier = Modifier.height(16.dp))

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
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        showMealsList = !showMealsList
                        mealWithActionsShownId = null
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = if (showMealsList) "Hide Meals" else "Show Meals (${mealsForSelectedDate.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                if (showMealsList) {
                    if (mealsForSelectedDate.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No meals logged for this day. Click 'Add Meal' to log one!",
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
            } // ⭐ THIS IS THE FIX: The closing brace for the Column was misplaced after this.
        } // Closing brace for the Box
    } // Closing brace for the Scaffold's content lambda

    // Sign-out confirmation dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = {
                showSignOutDialog = false
            },
            title = {
                Text(text = "Confirm Sign Out")
            },
            text = {
                Text(text = "Are you sure you want to sign out?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        authViewModel.signOut()
                        onSignOut()
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

    // Delete Confirmation Dialog
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

// --- MealItem Composable (This would typically be in a separate file, but included here for "one section") ---
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = meal.foodName,
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
                    IconButton(
                        onClick = {
                            onEdit(meal)
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Meal", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            onDelete(meal)
                        }
                    ) {
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
    }
}