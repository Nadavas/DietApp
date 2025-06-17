package com.nadavariel.dietapp.screens

import android.os.Build
import android.util.Log // Import for Log
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
import androidx.compose.material3.*
import androidx.compose.runtime.* // This import is needed for 'remember' and 'mutableStateOf'
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nadavariel.dietapp.AuthViewModel
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.model.Meal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.navigation.NavController


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

    // ⭐ Logging: Observing ViewModel states
    val selectedDate = foodLogViewModel.selectedDate
    val currentWeekStartDate = foodLogViewModel.currentWeekStartDate
    val mealsForSelectedDate = foodLogViewModel.mealsForSelectedDate
    Log.d("HomeScreen", "Composable Recomposition: selectedDate=$selectedDate, currentWeekStartDate=$currentWeekStartDate")


    val totalCaloriesForSelectedDate = remember(mealsForSelectedDate) {
        mealsForSelectedDate.sumOf { it.calories }
    }

    var showSignOutDialog by remember { mutableStateOf(false) }

    // ⭐ START OF ONLY CHANGE FOR THIS REQUEST: New state for meal list visibility
    var showMealsList by remember { mutableStateOf(false) } // Default to not shown
    // ⭐ END OF ONLY CHANGE FOR THIS REQUEST

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            // ⭐ Logging: Lifecycle RESUMED event
            Log.d("HomeScreen", "LaunchedEffect: App RESUMED. Current selectedDate in VM: ${foodLogViewModel.selectedDate}. Current system date: ${LocalDate.now()}")
            if (foodLogViewModel.selectedDate != LocalDate.now()) {
                foodLogViewModel.selectDate(LocalDate.now())
                Log.d("HomeScreen", "LaunchedEffect: Called selectDate(LocalDate.now()) to reset.")
            } else {
                Log.d("HomeScreen", "LaunchedEffect: selectedDate already today. No reset needed.")
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
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

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Welcome, ${userName.ifBlank { "Guest" }}!",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Weekly Calendar Header (Month and Year with week navigation) ---
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

            // --- Weekly Day Selection Row ---
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
                            .clickable { foodLogViewModel.selectDate(date) }
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

            // --- Total Calories for Selected Date ---
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
            Spacer(modifier = Modifier.height(32.dp))

            // ⭐ START OF ONLY CHANGE FOR THIS REQUEST: Replace Text with Button and add conditional visibility
            // --- Toggle Button for Meals Logged ---
            Button(
                onClick = { showMealsList = !showMealsList }, // Toggle visibility
                modifier = Modifier
                    // REMOVE THIS LINE: .fillMaxWidth()
                    .padding(horizontal = 16.dp) // Keep this padding for horizontal alignment
            ) {
                Text(
                    text = if (showMealsList) "Hide Meals" else "Show Meals (${mealsForSelectedDate.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp)) // Keep this spacer after the button

            // --- Conditionally display Daily Meal List (Chronological) ---
            if (showMealsList) { // Only show if showMealsList is true
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
                            .weight(1f) // Allows the LazyColumn to take available space
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(mealsForSelectedDate) { meal ->
                            MealItem(meal = meal)
                        }
                    }
                }
            }
            // ⭐ END OF ONLY CHANGE FOR THIS REQUEST
        }
    }

    // Sign-out confirmation dialog (NO CHANGES HERE)
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
                        onSignOut() // This line remains unchanged as per your provided code
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
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MealItem(meal: Meal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Column {
                Text(
                    text = meal.foodName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = meal.timestamp.toDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${meal.calories} kcal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}