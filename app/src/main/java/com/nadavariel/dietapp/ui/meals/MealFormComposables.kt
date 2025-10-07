package com.nadavariel.dietapp.ui.meals

import android.R.style as AndroidRStyle
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nadavariel.dietapp.ui.home.glassmorphism
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// --- Base Component ---

@Composable
fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
    ) {
        // Background layer with glassmorphism
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassmorphism(shape = RoundedCornerShape(16.dp))
        )

        // Content layer
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

// --- Date/Time Pickers ---

@Composable
fun DateTimePickerSection(
    selectedDateTimeState: Calendar,
    onDateTimeUpdate: (Calendar) -> Unit
) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    SectionCard(title = "Date & Time") {
        DateTimePickerRow(
            icon = Icons.Default.CalendarMonth,
            label = "Date",
            value = dateFormat.format(selectedDateTimeState.time)
        ) {
            val datePickerDialog = DatePickerDialog(
                context,
                { _: DatePicker, year, month, day ->
                    val newCalendar = (selectedDateTimeState.clone() as Calendar).apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                    }
                    onDateTimeUpdate(newCalendar)
                },
                selectedDateTimeState.get(Calendar.YEAR),
                selectedDateTimeState.get(Calendar.MONTH),
                selectedDateTimeState.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color.White.copy(alpha = 0.2f)
        )

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
                    if (newCalendar.after(Calendar.getInstance())) {
                        onDateTimeUpdate(Calendar.getInstance())
                    } else {
                        onDateTimeUpdate(newCalendar)
                    }
                },
                selectedDateTimeState.get(Calendar.HOUR_OF_DAY),
                selectedDateTimeState.get(Calendar.MINUTE),
                true
            ).show()
        }
    }
}

@Composable
private fun DateTimePickerRow(
    icon: ImageVector,
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
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.End
        )
    }
}

// --- Nutrient Input Sections ---

@Composable
fun ServingAndCaloriesSection(
    servingAmountText: String, onServingAmountChange: (String) -> Unit,
    servingUnitText: String, onServingUnitChange: (String) -> Unit,
    caloriesText: String, onCaloriesChange: (String) -> Unit
) {
    SectionCard(title = "Serving & Calories") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = servingAmountText,
                onValueChange = onServingAmountChange,
                label = { Text("Amount", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color.White
                )
            )
            OutlinedTextField(
                value = servingUnitText,
                onValueChange = onServingUnitChange,
                label = { Text("Unit", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color.White
                )
            )
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = caloriesText,
            onValueChange = { if (it.all(Char::isDigit)) onCaloriesChange(it) },
            label = { Text("Calories (kcal)", color = Color.White.copy(alpha = 0.7f)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.6f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = Color.White
            )
        )
    }
}

@Composable
fun MacronutrientsSection(
    proteinText: String, onProteinChange: (String) -> Unit,
    carbsText: String, onCarbsChange: (String) -> Unit,
    fatText: String, onFatChange: (String) -> Unit
) {
    SectionCard(title = "Macronutrients (g)") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = proteinText,
                onValueChange = onProteinChange,
                label = { Text("Protein", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color.White
                )
            )
            OutlinedTextField(
                value = carbsText,
                onValueChange = onCarbsChange,
                label = { Text("Carbs", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color.White
                )
            )
            OutlinedTextField(
                value = fatText,
                onValueChange = onFatChange,
                label = { Text("Fat", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color.White
                )
            )
        }
    }
}

@Composable
fun MicronutrientsSection(
    fiberText: String, onFiberChange: (String) -> Unit,
    sugarText: String, onSugarChange: (String) -> Unit,
    sodiumText: String, onSodiumChange: (String) -> Unit,
    potassiumText: String, onPotassiumChange: (String) -> Unit,
    calciumText: String, onCalciumChange: (String) -> Unit,
    ironText: String, onIronChange: (String) -> Unit,
    vitaminCText: String, onVitaminCChange: (String) -> Unit,
) {
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color.White.copy(alpha = 0.6f),
        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
        cursorColor = Color.White
    )

    SectionCard(title = "Micronutrients & Fiber") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = fiberText,
                onValueChange = onFiberChange,
                label = { Text("Fiber (g)", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = textFieldColors
            )
            OutlinedTextField(
                value = sugarText,
                onValueChange = onSugarChange,
                label = { Text("Sugar (g)", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = textFieldColors
            )
            OutlinedTextField(
                value = sodiumText,
                onValueChange = onSodiumChange,
                label = { Text("Sodium (mg)", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = textFieldColors
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = potassiumText,
                onValueChange = onPotassiumChange,
                label = { Text("Potassium (mg)", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = textFieldColors
            )
            OutlinedTextField(
                value = calciumText,
                onValueChange = onCalciumChange,
                label = { Text("Calcium (mg)", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ironText,
                onValueChange = onIronChange,
                label = { Text("Iron (mg)", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = textFieldColors
            )
            OutlinedTextField(
                value = vitaminCText,
                onValueChange = onVitaminCChange,
                label = { Text("Vit C (mg)", color = Color.White.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = textFieldColors
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}