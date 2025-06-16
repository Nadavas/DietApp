package com.nadavariel.dietapp.screens

// Essential AndroidX Compose imports
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ExitToApp // Ensure this is imported for the sign-out icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Corrected imports for your project structure
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.AuthViewModel
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.model.Meal

// Standard Java/Kotlin Date/Time imports
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    foodLogViewModel: FoodLogViewModel,
    navController: NavController,
    onSignOut: () -> Unit
) {
    // Directly access the mutableStateOf properties from ViewModels
    val currentUser = authViewModel.currentUser
    val userProfile = authViewModel.userProfile
    val userName = userProfile.name

    val selectedDate = foodLogViewModel.selectedDate
    val mealsForSelectedDate = foodLogViewModel.mealsForSelectedDate

    // Derived state: Total calories for the selected day
    val totalCaloriesForSelectedDate = remember(mealsForSelectedDate) {
        mealsForSelectedDate.sumOf { it.calories }
    }

    Scaffold(
        // We are removing the topBar here to gain full control of its content
        // topBar = { ... }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply Scaffold's padding
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // --- Custom Top Bar Content (Mimics TopAppBar, but with more control) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp), // Adjust padding as needed
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End // Pushes the icon to the end
            ) {
                // Spacer to push the icon to the right
                Spacer(Modifier.weight(1f))

                // Sign Out Icon (kept in top-right)
                IconButton(onClick = {
                    authViewModel.signOut()
                    onSignOut()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }


            // ⭐ NEW: Welcome text (now inside the main Column, allowing vertical spacing)
            Text(
                text = "Welcome, ${userName.ifBlank { "Guest" }}!",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center, // Center the text horizontally
                style = MaterialTheme.typography.headlineMedium, // Make text larger
                fontWeight = FontWeight.Bold // Make it bold for emphasis
            )

            // ⭐ OLD CHANGE: Spacer that was used to push content below the original TopAppBar.
            // This can now be combined or adjusted with the new Spacer above the Welcome text.
            // Keeping it for now but you might want to remove or adjust it.
            Spacer(modifier = Modifier.height(16.dp))


            // --- Date Navigation and Display ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = selectedDate.format(DateTimeFormatter.ofPattern("MMM")),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { foodLogViewModel.selectDate(selectedDate.minusDays(1)) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous Day", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("EEE")), // Day of week (Mon, Tue)
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("d")), // Day of month (1, 10)
                                style = MaterialTheme.typography.displayMedium, // Larger font for the day number
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        IconButton(onClick = { foodLogViewModel.selectDate(selectedDate.plusDays(1)) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next Day", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total Calories: ${totalCaloriesForSelectedDate} kcal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- Daily Meal List (Chronological) ---
            Text(
                text = "Meals Logged",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (mealsForSelectedDate.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // Provide some height for the message
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
                        .weight(1f) // Makes the LazyColumn fill remaining height
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
    }
}

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
                    // Format timestamp to display only time, e.g., "14:30"
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