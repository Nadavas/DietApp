@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.screens

import android.R.style as AndroidRStyle
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome // FIX: Replaced Sparkle with a standard icon
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GeminiResult
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMealScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    navController: NavController,
    mealToEdit: Meal? = null,
) {
    // --- STATE AND LOGIC (UNCHANGED) ---
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val context = LocalContext.current
    val isEditMode = mealToEdit != null

    var foodName by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var servingAmountText by remember { mutableStateOf("") }
    // FIX: Corrected typo from mutableState of to mutableStateOf
    var servingUnitText by remember { mutableStateOf("") }
    var selectedDateTimeState by remember { mutableStateOf(Calendar.getInstance()) }

    val geminiResult by foodLogViewModel.geminiResult.collectAsState()

    LaunchedEffect(mealToEdit) {
        if (isEditMode) {
            mealToEdit?.let {
                foodName = it.foodName
                caloriesText = it.calories.toString()
                servingAmountText = it.servingAmount.orEmpty()
                servingUnitText = it.servingUnit.orEmpty()
                proteinText = it.protein?.toString() ?: ""
                carbsText = it.carbohydrates?.toString() ?: ""
                fatText = it.fat?.toString() ?: ""
                selectedDateTimeState = Calendar.getInstance().apply { time = it.timestamp.toDate() }
            }
        } else {
            // Reset fields for Add mode
            foodName = ""
            caloriesText = ""
            servingAmountText = ""
            servingUnitText = ""
            proteinText = ""
            carbsText = ""
            fatText = ""
            selectedDateTimeState = Calendar.getInstance()
        }
    }

    LaunchedEffect(geminiResult) {
        if (geminiResult is GeminiResult.Success) {
            val successResult = geminiResult as GeminiResult.Success
            val mealTimestamp = Timestamp(selectedDateTimeState.time)

            successResult.foodInfoList.forEach { foodInfo ->
                // FIX: Replaced the incorrect Triple destructuring with direct variable assignments.
                // A Triple can only hold 3 values, and this was attempting to use 7.
                val geminiFoodName = foodInfo.food_name
                val geminiCalories = foodInfo.calories?.toIntOrNull()
                val geminiServingAmount = foodInfo.serving_amount
                val geminiServingUnit = foodInfo.serving_unit
                val geminiProtein = foodInfo.protein?.toDoubleOrNull()
                val geminiCarbs = foodInfo.carbohydrates?.toDoubleOrNull()
                val geminiFat = foodInfo.fat?.toDoubleOrNull()

                if (geminiFoodName != null && geminiCalories != null) {
                    foodLogViewModel.logMeal(
                        geminiFoodName,
                        geminiCalories,
                        geminiServingAmount,
                        geminiServingUnit,
                        mealTimestamp,
                        geminiProtein,
                        geminiCarbs,
                        geminiFat
                    )
                }
            }
            navController.popBackStack()
            foodLogViewModel.resetGeminiResult()
        }
    }

    // --- UI ---
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEditMode) "Edit Meal" else "Add New Meal") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- MEAL DESCRIPTION ---
            item {
                SectionCard(title = if (isEditMode) "Meal Name" else "Describe Your Meal") {
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (!isEditMode) "e.g., 'A bowl of oatmeal with blueberries and a glass of orange juice'" else "Meal Name") },
                        leadingIcon = { Icon(if (isEditMode) Icons.Default.EditNote else Icons.Default.AutoAwesome, contentDescription = null) },
                        minLines = if (!isEditMode) 3 else 1,
                    )
                }
            }

            // --- MANUAL DETAILS (EDIT MODE ONLY) ---
            if (isEditMode) {
                item {
                    SectionCard(title = "Serving & Calories") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = servingAmountText,
                                onValueChange = { servingAmountText = it },
                                label = { Text("Amount") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = servingUnitText,
                                onValueChange = { servingUnitText = it },
                                label = { Text("Unit") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = caloriesText,
                            onValueChange = { if (it.all(Char::isDigit)) caloriesText = it },
                            label = { Text("Calories (kcal)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
                item {
                    SectionCard(title = "Nutritional Info") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = proteinText,
                                onValueChange = { proteinText = it },
                                label = { Text("Protein (g)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = carbsText,
                                onValueChange = { carbsText = it },
                                label = { Text("Carbs (g)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = fatText,
                                onValueChange = { fatText = it },
                                label = { Text("Fat (g)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }
                    }
                }
            }


            // --- DATE & TIME PICKER ---
            item {
                SectionCard(title = "Date & Time") {
                    DateTimePickerRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "Date",
                        value = dateFormat.format(selectedDateTimeState.time)
                    ) {
                        val datePickerDialog = DatePickerDialog(
                            context,
                            { _: DatePicker, year, month, day ->
                                selectedDateTimeState = (selectedDateTimeState.clone() as Calendar).apply {
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, day)
                                }
                            },
                            selectedDateTimeState.get(Calendar.YEAR),
                            selectedDateTimeState.get(Calendar.MONTH),
                            selectedDateTimeState.get(Calendar.DAY_OF_MONTH)
                        )
                        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
                        datePickerDialog.show()
                    }

                    Divider(Modifier.padding(vertical = 8.dp))

                    DateTimePickerRow(
                        icon = Icons.Default.Schedule,
                        label = "Time",
                        value = timeFormat.format(selectedDateTimeState.time)
                    ) {
                        TimePickerDialog(
                            context,
                            AndroidRStyle.Theme_Holo_Light_Dialog_NoActionBar,
                            { _: TimePicker, hour, minute ->
                                val newCalendar = (selectedDateTimeState.clone() as Calendar).apply {
                                    set(Calendar.HOUR_OF_DAY, hour)
                                    set(Calendar.MINUTE, minute)
                                }
                                // Ensure time is not in the future
                                if (newCalendar.after(Calendar.getInstance())) {
                                    selectedDateTimeState = Calendar.getInstance()
                                } else {
                                    selectedDateTimeState = newCalendar
                                }
                            },
                            selectedDateTimeState.get(Calendar.HOUR_OF_DAY),
                            selectedDateTimeState.get(Calendar.MINUTE),
                            true
                        ).show()
                    }
                }
            }

            // --- SUBMIT BUTTON ---
            item {
                val isButtonEnabled = if (isEditMode) {
                    foodName.isNotBlank() && (caloriesText.toIntOrNull() ?: 0) > 0
                } else {
                    foodName.isNotBlank() && geminiResult !is GeminiResult.Loading
                }

                Button(
                    onClick = {
                        if (isEditMode) {
                            val calValue = caloriesText.toIntOrNull() ?: 0
                            val mealTimestamp = Timestamp(selectedDateTimeState.time)
                            if (mealToEdit != null) {
                                foodLogViewModel.updateMeal(
                                    mealToEdit.id, foodName, calValue, servingAmountText, servingUnitText,
                                    mealTimestamp, proteinText.toDoubleOrNull(), carbsText.toDoubleOrNull(), fatText.toDoubleOrNull()
                                )
                            }
                            navController.popBackStack()
                        } else {
                            foodLogViewModel.analyzeImageWithGemini(foodName)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = isButtonEnabled,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    AnimatedContent(
                        targetState = geminiResult is GeminiResult.Loading,
                        label = "button_state_animation"
                    ) { isLoading ->
                        if (isLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = LocalContentColor.current,
                                    strokeWidth = 2.dp
                                )
                                Text("Analyzing...")
                            }
                        } else {
                            Text(if (isEditMode) "Save Changes" else "Add Meal with AI", fontSize = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------
// |                       NEW HELPER COMPOSABLES FOR CLEAN UI                    |
// --------------------------------------------------------------------------------

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun DateTimePickerRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End
        )
    }
}