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
    mealToEdit: Meal? = null // Null if adding a new meal, otherwise contains the meal to edit
) {
    // Formats for displaying date and time
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val context = LocalContext.current

    // State variables
    var foodName by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var selectedDateTimeState by remember { mutableStateOf(Calendar.getInstance()) }


    // Initializes the screen state (based on whether a meal is being edited)
    LaunchedEffect(mealToEdit) {
        if (mealToEdit != null) {
            foodName = mealToEdit.foodName
            caloriesText = mealToEdit.calories.toString()
            val newCalendar = Calendar.getInstance().apply { time = mealToEdit.timestamp.toDate() }
            selectedDateTimeState = newCalendar
        } else {
            foodName = ""
            caloriesText = ""
            val now = Calendar.getInstance()
            selectedDateTimeState = now
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mealToEdit == null) "Add New Meal" else "Edit Meal") },
                navigationIcon = {
                    // Back button only for edit
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
            // Food name field
            OutlinedTextField(
                value = foodName,
                onValueChange = { onFoodNameChange -> foodName = onFoodNameChange },
                label = { Text("Food Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Calories field
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

                            // Clamp time to now if the date is today
                            val now = Calendar.getInstance()
                            if (newCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                                newCalendar.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                                newCalendar.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)
                            ) {
                                if (newCalendar.after(now)) {
                                    newCalendar.time = now.time
                                }
                            }
                            selectedDateTimeState = newCalendar
                        },
                        selectedDateTimeState.get(Calendar.YEAR),
                        selectedDateTimeState.get(Calendar.MONTH),
                        selectedDateTimeState.get(Calendar.DAY_OF_MONTH)
                    )
                    // Prevents selecting a future date
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

                    // If the selected date is today, initial time is now
                    val initialHour = if (isSelectedDateToday) now.get(Calendar.HOUR_OF_DAY) else selectedDateTimeState.get(Calendar.HOUR_OF_DAY)
                    val initialMinute = if (isSelectedDateToday) now.get(Calendar.MINUTE) else selectedDateTimeState.get(Calendar.MINUTE)

                    TimePickerDialog(
                        context,
                        AndroidRStyle.Theme_Holo_Light_Dialog_NoActionBar,
                        { _: TimePicker, hourOfDay: Int, minute: Int ->
                            val newCalendar = selectedDateTimeState.clone() as Calendar
                            newCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            newCalendar.set(Calendar.MINUTE, minute)
                            newCalendar.set(Calendar.SECOND, 0)
                            newCalendar.set(Calendar.MILLISECOND, 0)

                            // Clamps the time to the current time if the date is today and the selected time is in the future
                            if (isSelectedDateToday) {
                                val currentSystemTimeCalendar = Calendar.getInstance().apply {
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                if (newCalendar.after(currentSystemTimeCalendar)) {
                                    newCalendar.set(Calendar.HOUR_OF_DAY, currentSystemTimeCalendar.get(Calendar.HOUR_OF_DAY))
                                    newCalendar.set(Calendar.MINUTE, currentSystemTimeCalendar.get(Calendar.MINUTE))
                                }
                            }
                            selectedDateTimeState = newCalendar
                        },
                        initialHour,
                        initialMinute,
                        true
                    ).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Time: ${timeFormat.format(selectedDateTimeState.time)}")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add/Edit meal button
            Button(
                onClick = {
                    val calValue = caloriesText.toIntOrNull() ?: 0
                    if (foodName.isNotBlank() && calValue > 0) {
                        val mealTimestamp = selectedDateTimeState.time

                        // Final check to prevent future timestamps
                        val now = Date()
                        if (mealTimestamp.after(now)) {
                            return@Button
                        }

                        if (mealToEdit == null) {
                            // Add a new meal
                            foodLogViewModel.logMeal(foodName, calValue, mealTimestamp)
                        } else {
                            // Updates an existing meal
                            foodLogViewModel.updateMeal(
                                mealToEdit.id,
                                foodName,
                                calValue,
                                Timestamp(mealTimestamp)
                            )
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),

                // Button is only enabled if fields are valid
                enabled = foodName.isNotBlank() && (caloriesText.toIntOrNull() ?: 0) > 0
            ) {
                Text(if (mealToEdit == null) "Add Meal" else "Save Changes", fontSize = 18.sp)
            }

        }
    }
}