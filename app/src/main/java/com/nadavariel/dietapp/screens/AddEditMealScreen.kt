// screens/AddEditMealScreen.kt
package com.nadavariel.dietapp.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMealScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    navController: NavController,
    mealToEdit: Meal? = null // Optional: Pass a meal here if editing
) {
    var foodName by remember { mutableStateOf(mealToEdit?.foodName ?: "") }
    var calories by remember { mutableStateOf(mealToEdit?.calories?.toString() ?: "") }
    // Calendar instance to hold selected date and time
    val selectedDateTime = remember { Calendar.getInstance().apply {
        mealToEdit?.timestamp?.toDate()?.let { time = it }
    }}

    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mealToEdit == null) "Add New Meal" else "Edit Meal") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = foodName,
                onValueChange = { onFoodNameChange -> foodName = onFoodNameChange },
                label = { Text("Food Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = calories,
                onValueChange = { newValue ->
                    // Allow only digits
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        calories = newValue
                    }
                },
                label = { Text("Calories (kcal)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Date Picker Button
            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                            selectedDateTime.set(Calendar.YEAR, year)
                            selectedDateTime.set(Calendar.MONTH, month) // Month is 0-indexed
                            selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        },
                        selectedDateTime.get(Calendar.YEAR),
                        selectedDateTime.get(Calendar.MONTH),
                        selectedDateTime.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Date: ${dateFormat.format(selectedDateTime.time)}")
            }

            // Time Picker Button
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _: TimePicker, hourOfDay: Int, minute: Int ->
                            selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            selectedDateTime.set(Calendar.MINUTE, minute)
                        },
                        selectedDateTime.get(Calendar.HOUR_OF_DAY),
                        selectedDateTime.get(Calendar.MINUTE),
                        false // Set to true for 24-hour format, false for AM/PM
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Time: ${timeFormat.format(selectedDateTime.time)}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val calValue = calories.toIntOrNull() ?: 0
                    if (foodName.isNotBlank() && calValue > 0) {
                        if (mealToEdit == null) {
                            // Log new meal
                            foodLogViewModel.logMeal(foodName, calValue, selectedDateTime.time)
                        } else {
                            // Update existing meal
                            foodLogViewModel.updateMeal(
                                mealToEdit.id,
                                foodName,
                                calValue,
                                Timestamp(selectedDateTime.time)
                            )
                        }
                        navController.popBackStack() // Go back after logging/editing
                    } else {
                        // TODO: Show a snackbar or toast for invalid input
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = foodName.isNotBlank() && (calories.toIntOrNull() ?: 0) > 0
            ) {
                Text(if (mealToEdit == null) "Add Meal" else "Save Changes", fontSize = 18.sp)
            }
        }
    }
}