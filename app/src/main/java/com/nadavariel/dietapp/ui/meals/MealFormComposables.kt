package com.nadavariel.dietapp.ui.meals

import android.R.style as AndroidRStyle
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// --- DESIGN TOKENS TO MATCH YOUR HOME SCREEN DESIGN ---
private val HealthyGreen = Color(0xFF4CAF50)
private val LightGreyText = Color(0xFF757575)

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
                .background(HealthyGreen, CircleShape) // Using the correct green
        )
        Text(
            text = title.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = LightGreyText,
            letterSpacing = 1.2.sp
        )
    }
}

// --- NEW SUB-SECTION HEADER ---
@Composable
fun SubSectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        // Centered horizontally, with 4.dp top/bottom padding
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center // <-- This centers the content
    ) {
        Text(
            text = title.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = LightGreyText,
            letterSpacing = 1.2.sp
        )
    }
}
// --- END OF NEW COMPOSABLE ---

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
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = LightGreyText) } },
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HealthyGreen,
            focusedLabelColor = HealthyGreen,
            cursorColor = HealthyGreen,
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
                ThemedOutlinedTextField(value = servingAmountText, onValueChange = onServingAmountChange, label = "Amount", modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                ThemedOutlinedTextField(value = servingUnitText, onValueChange = onServingUnitChange, label = "Unit (e.g., g)", modifier = Modifier.weight(1.5f), singleLine = true)
            }
            Spacer(Modifier.height(8.dp))
            ThemedOutlinedTextField(value = caloriesText, onValueChange = { if (it.all(Char::isDigit)) onCaloriesChange(it) }, label = "Total Calories (kcal)*", leadingIcon = Icons.Outlined.LocalFireDepartment, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
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
                ThemedOutlinedTextField(value = proteinText, onValueChange = onProteinChange, label = "Protein (g)", leadingIcon = Icons.Outlined.FitnessCenter, modifier = commonModifier, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                ThemedOutlinedTextField(value = carbsText, onValueChange = onCarbsChange, label = "Carbs (g)", leadingIcon = Icons.Outlined.Bolt, modifier = commonModifier, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                ThemedOutlinedTextField(value = fatText, onValueChange = onFatChange, label = "Fat (g)", leadingIcon = Icons.Outlined.WaterDrop, modifier = commonModifier, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MicronutrientsSection(
    fiberText: String, onFiberChange: (String) -> Unit, sugarText: String, onSugarChange: (String) -> Unit,
    sodiumText: String, onSodiumChange: (String) -> Unit, potassiumText: String, onPotassiumChange: (String) -> Unit,
    calciumText: String, onCalciumChange: (String) -> Unit, ironText: String, onIronChange: (String) -> Unit,
    vitaminCText: String, onVitaminCChange: (String) -> Unit,
    // START: Added for Vitamin A and B12
    vitaminAText: String, onVitaminAChange: (String) -> Unit,
    vitaminB12Text: String, onVitaminB12Change: (String) -> Unit
    // END: Added for Vitamin A and B12
) {
    Column {
        SectionHeader(title = "Micronutrients & Fiber")
        FormCard {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NutrientTextField(label = "Fiber (g)", value = fiberText, onValueChange = onFiberChange)
                NutrientTextField(label = "Sugar (g)", value = sugarText, onValueChange = onSugarChange)
                NutrientTextField(label = "Sodium (mg)", value = sodiumText, onValueChange = onSodiumChange)
                NutrientTextField(label = "Potassium (mg)", value = potassiumText, onValueChange = onPotassiumChange)
                NutrientTextField(label = "Calcium (mg)", value = calciumText, onValueChange = onCalciumChange)
                NutrientTextField(label = "Iron (mg)", value = ironText, onValueChange = onIronChange)
                NutrientTextField(label = "Vit C (mg)", value = vitaminCText, onValueChange = onVitaminCChange)
                // START: Added for Vitamin A and B12
                NutrientTextField(label = "Vit A (μg)", value = vitaminAText, onValueChange = onVitaminAChange)
                NutrientTextField(label = "Vit B12 (μg)", value = vitaminB12Text, onValueChange = onVitaminB12Change)
                // END: Added for Vitamin A and B12
            }
        }
    }
}

@Composable
private fun NutrientTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    ThemedOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.widthIn(min = 120.dp)
    )
}

@Composable
fun DateTimePickerSection(
    selectedDateTimeState: Calendar,
    onDateTimeUpdate: (Calendar) -> Unit
) {
    val context = LocalContext.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Column {
        SectionHeader(title = "Date & Time")
        FormCard {
            DateTimePickerRow(
                icon = Icons.Default.CalendarMonth,
                label = "Date",
                value = dateFormat.format(selectedDateTimeState.time)
            ) {
                DatePickerDialog(context, { _: DatePicker, y, m, d ->
                    val newCal = (selectedDateTimeState.clone() as Calendar).apply { set(y, m, d) }
                    onDateTimeUpdate(newCal)
                }, selectedDateTimeState.get(Calendar.YEAR), selectedDateTimeState.get(Calendar.MONTH), selectedDateTimeState.get(Calendar.DAY_OF_MONTH)
                ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DateTimePickerRow(
                icon = Icons.Default.Schedule,
                label = "Time",
                value = timeFormat.format(selectedDateTimeState.time)
            ) {
                TimePickerDialog(context, AndroidRStyle.Theme_Holo_Light_Dialog_NoActionBar, { _, h, m ->
                    val newCal = (selectedDateTimeState.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
                    onDateTimeUpdate(if (newCal.after(Calendar.getInstance())) Calendar.getInstance() else newCal)
                }, selectedDateTimeState.get(Calendar.HOUR_OF_DAY), selectedDateTimeState.get(Calendar.MINUTE), true).show()
            }
        }
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
            color = HealthyGreen,
            textAlign = TextAlign.End
        )
    }
}