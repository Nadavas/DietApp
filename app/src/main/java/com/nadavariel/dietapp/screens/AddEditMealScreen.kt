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
import java.util.Date
import android.R.style as AndroidRStyle

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMealScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    navController: NavController,
    mealToEdit: Meal? = null // Optional: Pass a meal here if editing
) {
    val context = LocalContext.current

    var foodName by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var selectedDateTimeState by remember { mutableStateOf(Calendar.getInstance()) }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    LaunchedEffect(mealToEdit) {
        if (mealToEdit != null) {
            foodName = mealToEdit.foodName
            caloriesText = mealToEdit.calories.toString()
            val newCalendar = Calendar.getInstance().apply { time = mealToEdit.timestamp.toDate() }
            selectedDateTimeState = newCalendar
        } else {
            foodName = ""
            caloriesText = ""
            // Ensure new meals default to current date/time, and not in the future.
            val now = Calendar.getInstance()
            selectedDateTimeState = now
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mealToEdit == null) "Add New Meal" else "Edit Meal") },
                navigationIcon = {
                    // ⭐ MODIFIED: Only show IconButton if mealToEdit is not null
                    if (mealToEdit != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
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
                    val datePickerDialog = DatePickerDialog(
                        context,
                        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                            val newCalendar = selectedDateTimeState.clone() as Calendar
                            newCalendar.set(Calendar.YEAR, year)
                            newCalendar.set(Calendar.MONTH, month)
                            newCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                            // ⭐ NEW LOGIC FOR DATE: If selected date is today, clamp time to now
                            val now = Calendar.getInstance()
                            if (newCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                newCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                                newCalendar.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
                            ) {
                                // If the newly set date is today, ensure the time is not in the future
                                if (newCalendar.after(now)) {
                                    newCalendar.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                                    newCalendar.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                                    newCalendar.set(Calendar.SECOND, now.get(Calendar.SECOND))
                                    newCalendar.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND))
                                }
                            }
                            selectedDateTimeState = newCalendar
                        },
                        selectedDateTimeState.get(Calendar.YEAR),
                        selectedDateTimeState.get(Calendar.MONTH),
                        selectedDateTimeState.get(Calendar.DAY_OF_MONTH)
                    )
                    // ⭐ NEW: Set maximum date to today
                    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
                    datePickerDialog.show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Date: ${dateFormat.format(selectedDateTimeState.time)}")
            }

            // Time Picker Button
            OutlinedButton(
                onClick = {
                    val now = Calendar.getInstance()
                    val isSelectedDateToday = selectedDateTimeState.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            selectedDateTimeState.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                            selectedDateTimeState.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)

                    // ⭐ NEW LOGIC FOR TIME: If the selected date is today, initial time is now
                    val initialHour = if (isSelectedDateToday) now.get(Calendar.HOUR_OF_DAY) else selectedDateTimeState.get(Calendar.HOUR_OF_DAY)
                    val initialMinute = if (isSelectedDateToday) now.get(Calendar.MINUTE) else selectedDateTimeState.get(Calendar.MINUTE)

                    TimePickerDialog(
                        context,
                        AndroidRStyle.Theme_Holo_Light_Dialog_NoActionBar,
                        { _: TimePicker, hourOfDay: Int, minute: Int ->
                            val newCalendar = selectedDateTimeState.clone() as Calendar
                            newCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            newCalendar.set(Calendar.MINUTE, minute)
                            newCalendar.set(Calendar.SECOND, 0) // Clear seconds/milliseconds for clean comparison
                            newCalendar.set(Calendar.MILLISECOND, 0)

                            // ⭐ NEW LOGIC FOR TIME: If selected date is today and chosen time is in future, clamp
                            if (isSelectedDateToday) {
                                val currentSystemTimeCalendar = Calendar.getInstance().apply {
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                if (newCalendar.after(currentSystemTimeCalendar)) {
                                    // If the chosen time is in the future for today's date, clamp to current time
                                    newCalendar.set(Calendar.HOUR_OF_DAY, currentSystemTimeCalendar.get(Calendar.HOUR_OF_DAY))
                                    newCalendar.set(Calendar.MINUTE, currentSystemTimeCalendar.get(Calendar.MINUTE))
                                }
                            }
                            selectedDateTimeState = newCalendar
                        },
                        initialHour,
                        initialMinute,
                        true // Set to true for 24-hour format, false for AM/PM
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Time: ${timeFormat.format(selectedDateTimeState.time)}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val calValue = caloriesText.toIntOrNull() ?: 0
                    if (foodName.isNotBlank() && calValue > 0) {
                        // Pass the Date object from selectedDateTimeState
                        val mealTimestamp = selectedDateTimeState.time

                        // ⭐ NEW VALIDATION: Perform a final check before saving
                        val now = Date()
                        if (mealTimestamp.after(now)) {
                            // This case should ideally not be reached if UI pickers work correctly,
                            // but it's a good final defense.
                            // You might want to show a Toast/Snackbar here for the user.
                            // Toast.makeText(context, "Meal time cannot be in the future.", Toast.LENGTH_SHORT).show()
                            return@Button // Prevent saving if time is in the future
                        }

                        if (mealToEdit == null) {
                            foodLogViewModel.logMeal(foodName, calValue, mealTimestamp)
                        } else {
                            foodLogViewModel.updateMeal(
                                mealToEdit.id,
                                foodName,
                                calValue,
                                Timestamp(mealTimestamp)
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