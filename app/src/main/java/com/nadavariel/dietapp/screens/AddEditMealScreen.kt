// screens/AddEditMealScreen.kt
@file:Suppress("DEPRECATION")

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
import androidx.compose.runtime.* // Import all needed runtime components
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
import java.util.Date // Ensure java.util.Date is imported
import android.R.style as AndroidRStyle // Alias to avoid conflict with your own R

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMealScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    navController: NavController,
    mealToEdit: Meal? = null // Optional: Pass a meal here if editing
) {
    val context = LocalContext.current

    // ⭐ FIX: Wrap Calendar in mutableStateOf to make it observable
    var foodName by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var selectedDateTimeState by remember { mutableStateOf(Calendar.getInstance()) } // ⭐ Use this for observation

    // Use remember for SimpleDateFormat instances to prevent re-creation
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) } // Corrected pattern to ensure year is shown

    // ⭐ LaunchedEffect to update fields when mealToEdit changes
    LaunchedEffect(mealToEdit) {
        if (mealToEdit != null) {
            // Set values from the meal to edit
            foodName = mealToEdit.foodName
            caloriesText = mealToEdit.calories.toString()
            // Create a new Calendar instance and set its time
            val newCalendar = Calendar.getInstance().apply { time = mealToEdit.timestamp.toDate() }
            selectedDateTimeState = newCalendar // ⭐ Update the state variable
        } else {
            // Reset fields for adding a new meal
            foodName = ""
            caloriesText = ""
            // Create a new Calendar instance and set it to current time
            val newCalendar = Calendar.getInstance().apply { time = Date() }
            selectedDateTimeState = newCalendar // ⭐ Update the state variable
        }
    }

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
                value = caloriesText,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        caloriesText = newValue
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
                            // ⭐ FIX: Create a copy and update the state variable
                            val newCalendar = selectedDateTimeState.clone() as Calendar
                            newCalendar.set(Calendar.YEAR, year)
                            newCalendar.set(Calendar.MONTH, month)
                            newCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                            selectedDateTimeState = newCalendar // ⭐ Update the state
                        },
                        selectedDateTimeState.get(Calendar.YEAR),
                        selectedDateTimeState.get(Calendar.MONTH),
                        selectedDateTimeState.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                // ⭐ FIX: Use selectedDateTimeState.time
                Text("Date: ${dateFormat.format(selectedDateTimeState.time)}")
            }

            // Time Picker Button
            OutlinedButton(
                onClick = {
                    val currentHour = selectedDateTimeState.get(Calendar.HOUR_OF_DAY)
                    val currentMinute = selectedDateTimeState.get(Calendar.MINUTE)

                    TimePickerDialog(
                        context,
                        AndroidRStyle.Theme_Holo_Light_Dialog_NoActionBar,
                        { _: TimePicker, hourOfDay: Int, minute: Int ->
                            // ⭐ FIX: Create a copy and update the state variable
                            val newCalendar = selectedDateTimeState.clone() as Calendar
                            newCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            newCalendar.set(Calendar.MINUTE, minute)
                            selectedDateTimeState = newCalendar // ⭐ Update the state
                        },
                        currentHour,
                        currentMinute,
                        true // Set to true for 24-hour format, false for AM/PM
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                // ⭐ FIX: Use selectedDateTimeState.time
                Text("Time: ${timeFormat.format(selectedDateTimeState.time)}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val calValue = caloriesText.toIntOrNull() ?: 0
                    if (foodName.isNotBlank() && calValue > 0) {
                        if (mealToEdit == null) {
                            // ⭐ FIX: Use selectedDateTimeState.time when logging
                            foodLogViewModel.logMeal(foodName, calValue, selectedDateTimeState.time)
                        } else {
                            // ⭐ FIX: Use selectedDateTimeState.time when updating
                            foodLogViewModel.updateMeal(
                                mealToEdit.id,
                                foodName,
                                calValue,
                                Timestamp(selectedDateTimeState.time)
                            )
                        }
                        navController.popBackStack()
                    } else {
                        // TODO: Show a snackbar or toast for invalid input
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = foodName.isNotBlank() && (caloriesText.toIntOrNull() ?: 0) > 0
            ) {
                Text(if (mealToEdit == null) "Add Meal" else "Save Changes", fontSize = 18.sp)
            }
        }
    }
}