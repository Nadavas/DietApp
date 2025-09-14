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
import com.nadavariel.dietapp.viewmodel.GeminiResult
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
    mealToEdit: Meal? = null
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val context = LocalContext.current

    var foodName by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var selectedDateTimeState by remember { mutableStateOf(Calendar.getInstance()) }

    val geminiResult by foodLogViewModel.geminiResult.collectAsState()

    LaunchedEffect(mealToEdit) {
        if (mealToEdit != null) {
            foodName = mealToEdit.foodName
            caloriesText = mealToEdit.calories.toString()
            val newCalendar = Calendar.getInstance().apply { time = mealToEdit.timestamp.toDate() }
            selectedDateTimeState = newCalendar
        } else {
            foodName = ""
            caloriesText = ""
        }
    }

    LaunchedEffect(geminiResult) {
        if (geminiResult is GeminiResult.Success) {
            val successResult = geminiResult as GeminiResult.Success
            val geminiFoodName = successResult.foodInfo.food_name
            val geminiCalories = successResult.foodInfo.calories?.toIntOrNull()

            if (geminiFoodName != null && geminiCalories != null) {
                val mealTimestamp = Timestamp(selectedDateTimeState.time)

                foodLogViewModel.logMeal(geminiFoodName, geminiCalories, mealTimestamp)

                navController.popBackStack()
            }

            foodLogViewModel.resetGeminiResult()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mealToEdit == null) "Add New Meal" else "Edit Meal") },
                navigationIcon = {
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
                label = { Text("What did you eat?") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3, // Set the minimum number of lines
            )

            if (mealToEdit != null) {
                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            caloriesText = newValue
                        }
                    },
                    label = { Text("Calories") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            OutlinedButton(
                onClick = {
                    val datePickerDialog = DatePickerDialog(
                        context,
                        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                            val newCalendar = selectedDateTimeState.clone() as Calendar
                            newCalendar.set(Calendar.YEAR, year)
                            newCalendar.set(Calendar.MONTH, month)
                            newCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

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
                    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
                    datePickerDialog.show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Date: ${dateFormat.format(selectedDateTimeState.time)}")
            }

            OutlinedButton(
                onClick = {
                    val now = Calendar.getInstance()
                    val isSelectedDateToday = selectedDateTimeState.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                            selectedDateTimeState.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                            selectedDateTimeState.get(Calendar.DAY_OF_MONTH) == now.get(Calendar.DAY_OF_MONTH)

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

            Button(
                onClick = {
                    if (mealToEdit == null) {
                        foodLogViewModel.analyzeImageWithGemini(foodName)
                    } else {
                        val calValue = caloriesText.toIntOrNull() ?: 0
                        if (foodName.isNotBlank() && calValue > 0) {
                            val mealTimestamp = selectedDateTimeState.time
                            val now = Date()
                            if (mealTimestamp.after(now)) {
                                return@Button
                            }
                            foodLogViewModel.updateMeal(
                                mealToEdit.id,
                                foodName,
                                calValue,
                                Timestamp(mealTimestamp)
                            )
                            navController.popBackStack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = if (mealToEdit == null) {
                    foodName.isNotBlank()
                } else {
                    foodName.isNotBlank() && (caloriesText.toIntOrNull() ?: 0) > 0
                }
            ) {
                val buttonText = if (mealToEdit == null) {
                    when (geminiResult) {
                        is GeminiResult.Loading -> "Analyzing..."
                        else -> "Analyze and Add Meal"
                    }
                } else {
                    "Save Changes"
                }
                Text(buttonText, fontSize = 18.sp)
            }
        }
    }
}