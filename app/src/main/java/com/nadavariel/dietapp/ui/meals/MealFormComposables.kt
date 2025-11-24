@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.ui.meals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadavariel.dietapp.ui.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// --- THIS IS NOW THE ONLY SectionHeader ---
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(AppTheme.colors.primaryGreen, CircleShape) // Using the correct green
        )
        Text(
            text = title.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.lightGreyText,
            letterSpacing = 1.2.sp
        )
    }
}

// --- An elegant white card container, matching the home screen cards ---
@Composable
fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

// --- The reusable, consistently styled text field ---
@Composable
fun ThemedOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = AppTheme.colors.lightGreyText) } },
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppTheme.colors.primaryGreen,
            focusedLabelColor = AppTheme.colors.primaryGreen,
            cursorColor = AppTheme.colors.primaryGreen,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

@Composable
fun ServingAndCaloriesSection(
    servingAmountText: String, onServingAmountChange: (String) -> Unit,
    servingUnitText: String, onServingUnitChange: (String) -> Unit,
    caloriesText: String, onCaloriesChange: (String) -> Unit
) {
    Column {
        SectionHeader(title = "Serving & Calories")
        FormCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemedOutlinedTextField(
                    value = servingAmountText,
                    onValueChange = onServingAmountChange,
                    label = "Amount",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                ThemedOutlinedTextField(
                    value = servingUnitText,
                    onValueChange = onServingUnitChange,
                    label = "Unit (e.g. g, ml)",
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(8.dp))
            ThemedOutlinedTextField(
                value = caloriesText,
                onValueChange = { if (it.all(Char::isDigit)) onCaloriesChange(it) },
                label = "Total Calories (kcal)*",
                leadingIcon = Icons.Outlined.LocalFireDepartment,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
fun MacronutrientsSection(
    proteinText: String, onProteinChange: (String) -> Unit,
    carbsText: String, onCarbsChange: (String) -> Unit,
    fatText: String, onFatChange: (String) -> Unit
) {
    Column {
        SectionHeader(title = "Macronutrients")
        FormCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val commonModifier = Modifier.weight(1f)
                // Icons removed to provide space for text labels
                ThemedOutlinedTextField(
                    value = proteinText,
                    onValueChange = onProteinChange,
                    label = "Protein (g)",
                    leadingIcon = null,
                    modifier = commonModifier,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                ThemedOutlinedTextField(
                    value = carbsText,
                    onValueChange = onCarbsChange,
                    label = "Carbs (g)",
                    leadingIcon = null,
                    modifier = commonModifier,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                ThemedOutlinedTextField(
                    value = fatText,
                    onValueChange = onFatChange,
                    label = "Fat (g)",
                    leadingIcon = null,
                    modifier = commonModifier,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun MicronutrientsSection(
    fiberText: String, onFiberChange: (String) -> Unit, sugarText: String, onSugarChange: (String) -> Unit,
    sodiumText: String, onSodiumChange: (String) -> Unit, potassiumText: String, onPotassiumChange: (String) -> Unit,
    calciumText: String, onCalciumChange: (String) -> Unit, ironText: String, onIronChange: (String) -> Unit,
    vitaminCText: String, onVitaminCChange: (String) -> Unit,
    vitaminAText: String, onVitaminAChange: (String) -> Unit,
    vitaminB12Text: String, onVitaminB12Change: (String) -> Unit
) {
    // Helper data class to manage the list simply
    data class NutrientData(val label: String, val value: String, val onChange: (String) -> Unit)

    val micros = listOf(
        NutrientData("Fiber (g)", fiberText, onFiberChange),
        NutrientData("Sugar (g)", sugarText, onSugarChange),
        NutrientData("Sodium (mg)", sodiumText, onSodiumChange),
        NutrientData("Potassium (mg)", potassiumText, onPotassiumChange),
        NutrientData("Calcium (mg)", calciumText, onCalciumChange),
        NutrientData("Iron (mg)", ironText, onIronChange),
        NutrientData("Vitamin C (mg)", vitaminCText, onVitaminCChange),
        NutrientData("Vitamin A (mcg)", vitaminAText, onVitaminAChange),
        NutrientData("Vitamin B12 (mcg)", vitaminB12Text, onVitaminB12Change)
    )

    Column {
        SectionHeader(title = "Micronutrients & Fiber")
        FormCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                micros.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { item ->
                            NutrientTextField(
                                label = item.label,
                                value = item.value,
                                onValueChange = item.onChange,
                                modifier = Modifier.weight(1f) // Equal width for all
                            )
                        }
                        // If the last row has only 1 item, add a spacer to keep the grid structure
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutrientTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ThemedOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier // Modifier passed here controls width (weight)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerSection(
    selectedDateTimeState: Calendar,
    onDateTimeUpdate: (Calendar) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column {
        SectionHeader(title = "Date & Time")
        FormCard {
            DateTimePickerRow(
                icon = Icons.Default.CalendarMonth,
                label = "Date",
                value = dateFormat.format(selectedDateTimeState.time)
            ) {
                showDatePicker = true
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DateTimePickerRow(
                icon = Icons.Default.Schedule,
                label = "Time",
                value = timeFormat.format(selectedDateTimeState.time)
            ) {
                showTimePicker = true
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateTimeState.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Calendar.getInstance().apply { timeInMillis = millis }
                        // Preserve original time
                        val updatedCal = (selectedDateTimeState.clone() as Calendar).apply {
                            set(Calendar.YEAR, newDate.get(Calendar.YEAR))
                            set(Calendar.MONTH, newDate.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, newDate.get(Calendar.DAY_OF_MONTH))
                        }

                        // Prevent future dates
                        if (updatedCal.after(Calendar.getInstance())) {
                            // Keep it to now if future
                            onDateTimeUpdate(Calendar.getInstance())
                        } else {
                            onDateTimeUpdate(updatedCal)
                        }
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = AppTheme.colors.primaryGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = AppTheme.colors.textSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color.White)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = AppTheme.colors.primaryGreen,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = AppTheme.colors.primaryGreen,
                    todayContentColor = AppTheme.colors.primaryGreen
                )
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedDateTimeState.get(Calendar.HOUR_OF_DAY),
            initialMinute = selectedDateTimeState.get(Calendar.MINUTE),
            is24Hour = true
        )

        var showTimeInput by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val updatedCal = (selectedDateTimeState.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }

                    // Prevent future time
                    if (updatedCal.after(Calendar.getInstance())) {
                        // Keep it to now if future
                        onDateTimeUpdate(Calendar.getInstance())
                    } else {
                        onDateTimeUpdate(updatedCal)
                    }
                    showTimePicker = false
                }) {
                    Text("OK", color = AppTheme.colors.primaryGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = AppTheme.colors.textSecondary)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    if (showTimeInput) {
                        TimeInput(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                timeSelectorSelectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                                timeSelectorSelectedContentColor = AppTheme.colors.primaryGreen,
                                timeSelectorUnselectedContainerColor = AppTheme.colors.textSecondary.copy(alpha = 0.1f),
                                timeSelectorUnselectedContentColor = AppTheme.colors.textPrimary
                            )
                        )
                    } else {
                        TimePicker(
                            state = timePickerState,
                            colors = TimePickerDefaults.colors(
                                clockDialSelectedContentColor = Color.White,
                                clockDialUnselectedContentColor = AppTheme.colors.textPrimary,
                                selectorColor = AppTheme.colors.primaryGreen,
                                containerColor = Color.White,
                                periodSelectorBorderColor = AppTheme.colors.primaryGreen,
                                periodSelectorSelectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                                periodSelectorSelectedContentColor = AppTheme.colors.primaryGreen,
                                periodSelectorUnselectedContainerColor = Color.Transparent,
                                periodSelectorUnselectedContentColor = AppTheme.colors.textSecondary,
                                timeSelectorSelectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                                timeSelectorSelectedContentColor = AppTheme.colors.primaryGreen,
                                timeSelectorUnselectedContainerColor = AppTheme.colors.textSecondary.copy(alpha = 0.1f),
                                timeSelectorUnselectedContentColor = AppTheme.colors.textPrimary
                            )
                        )
                    }

                    IconButton(onClick = { showTimeInput = !showTimeInput }) {
                        Icon(
                            imageVector = if (showTimeInput) Icons.Rounded.AccessTime else Icons.Filled.Keyboard,
                            contentDescription = "Toggle Input Mode",
                            tint = AppTheme.colors.primaryGreen
                        )
                    }
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun DateTimePickerRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value, style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.primaryGreen,
            textAlign = TextAlign.End
        )
    }
}