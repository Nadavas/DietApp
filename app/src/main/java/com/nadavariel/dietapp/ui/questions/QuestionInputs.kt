package com.nadavariel.dietapp.ui.questions

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nadavariel.dietapp.model.InputType
import com.nadavariel.dietapp.model.Question
import com.nadavariel.dietapp.ui.AppTheme
import java.util.*
import kotlin.math.roundToInt

@Composable
internal fun QuestionInput(
    question: Question,
    currentAnswer: String?,
    onSave: (String) -> Unit
) {
    // UI REFRESH: Routing to new/styled inputs
    when {
        question.inputType == InputType.DOB -> DobInput(currentAnswer, onSave)

        // NEW: Using AnimatedSliderInput for all three
        question.inputType == InputType.HEIGHT -> AnimatedSliderInput(
            currentAnswer = currentAnswer,
            onSave = onSave,
            valueRange = 120f..220f,
            defaultVal = 170f,
            step = 1f,
            unit = "cm"
        )
        question.inputType == InputType.WEIGHT -> AnimatedSliderInput(
            currentAnswer = currentAnswer,
            onSave = onSave,
            valueRange = 40f..150f,
            defaultVal = 70f,
            step = 0.5f,
            unit = "kg"
        )
        question.inputType == InputType.TARGET_WEIGHT -> AnimatedSliderInput(
            currentAnswer = currentAnswer,
            onSave = onSave,
            valueRange = 40f..150f,
            defaultVal = 70f,
            step = 0.5f,
            unit = "kg"
        )

        question.options != null && question.inputType == InputType.EXERCISE_TYPE -> ExerciseTypeInput(
            options = question.options,
            currentAnswer = currentAnswer,
            onSave = onSave
        )
        question.options != null -> OptionsInput(question.options, currentAnswer, onSave)
        else -> TextInput(currentAnswer, onValueChange = onSave)
    }
}

@Composable
private fun TextInput(currentValue: String?, onValueChange: (String) -> Unit) {
    // UI REFRESH: Styled text field
    OutlinedTextField(
        value = currentValue ?: "",
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Type your answer...") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppTheme.colors.VibrantGreen,
            focusedLabelColor = AppTheme.colors.VibrantGreen,
            cursorColor = AppTheme.colors.VibrantGreen
        ),
        minLines = 3 // Makes more sense for "types of exercise"
    )
}

@Composable
private fun OptionsInput(options: List<String>, currentAnswer: String?, onSave: (String) -> Unit) {
    // UI REFRESH: Using new OptionCardItem
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { opt ->
            val selected = currentAnswer == opt
            OptionCardItem(
                text = opt,
                isSelected = selected,
                onClick = { onSave(opt) }
            )
        }
    }
}

// UI REFRESH: New Composable for OptionsInput
@Composable
private fun OptionCardItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AppTheme.colors.VibrantGreen else AppTheme.colors.LightGreyText.copy(alpha = 0.3f)
    val containerColor = if (isSelected) AppTheme.colors.VibrantGreen.copy(alpha = 0.05f) else AppTheme.colors.CardBackground

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) AppTheme.colors.VibrantGreen else AppTheme.colors.DarkGreyText,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = AppTheme.colors.VibrantGreen
                )
            }
        }
    }
}

@Composable
internal fun DobInput(currentAnswer: String?, onSave: (String) -> Unit) {
    val context = LocalContext.current
    val cal = Calendar.getInstance()

    // Parse existing answer if available
    if (!currentAnswer.isNullOrBlank()) {
        try {
            val parts = currentAnswer.split("-").map { it.toInt() }
            cal.set(parts[0], parts[1] - 1, parts[2])
        } catch (_: Exception) { /* Ignore */ }
    }

    // UI REFRESH: Styled as a Card
    val hasAnswer = !currentAnswer.isNullOrBlank()
    Card(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val isoDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                    onSave(isoDate)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.CardBackground),
        border = BorderStroke(1.dp, AppTheme.colors.LightGreyText.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = "Pick Date",
                tint = AppTheme.colors.VibrantGreen,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                if (hasAnswer) currentAnswer else "Select Date of Birth",
                color = if (hasAnswer) AppTheme.colors.VibrantGreen else AppTheme.colors.DarkGreyText,
                fontWeight = if (hasAnswer) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Replaces HeightInput, WeightInput, and TargetWeightInput
 */
@Composable
private fun AnimatedSliderInput(
    currentAnswer: String?,
    onSave: (String) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultVal: Float,
    step: Float,
    unit: String
) {
    // Parse the initial value from the answer string, or use default
    val (initialValue, _) = remember(currentAnswer) {
        if (currentAnswer.isNullOrBlank()) {
            defaultVal to unit
        } else {
            val parsed = currentAnswer.replace(Regex("[^0-9.]"), "").toFloatOrNull()
            (parsed ?: defaultVal) to unit
        }
    }

    var sliderValue by remember { mutableFloatStateOf(initialValue) }

    // This function will be called when the slider stops moving
    val saveValue = {
        val roundedValue = (sliderValue / step).roundToInt() * step
        sliderValue = roundedValue
        onSave(String.format(Locale.US, "%.1f %s", roundedValue, unit))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.CardBackground),
        border = BorderStroke(1.dp, AppTheme.colors.LightGreyText.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large text display
            Text(
                text = String.format(Locale.US, "%.1f %s", sliderValue, unit),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.VibrantGreen
            )
            Spacer(Modifier.height(16.dp))

            // Slider
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = valueRange,
                onValueChangeFinished = { saveValue() },
                colors = SliderDefaults.colors(
                    thumbColor = AppTheme.colors.VibrantGreen,
                    activeTrackColor = AppTheme.colors.VibrantGreen,
                    inactiveTrackColor = AppTheme.colors.VibrantGreen.copy(alpha = 0.2f)
                )
            )

            // Fine-tune buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    sliderValue = (sliderValue - step).coerceIn(valueRange)
                    saveValue()
                }) {
                    Icon(Icons.Default.Remove, "Decrease", tint = AppTheme.colors.DarkGreyText)
                }
                IconButton(onClick = {
                    sliderValue = (sliderValue + step).coerceIn(valueRange)
                    saveValue()
                }) {
                    Icon(Icons.Default.Add, "Increase", tint = AppTheme.colors.DarkGreyText)
                }
            }
        }
    }
}

/**
 * Replaces the TextInput for exercise types
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseTypeInput(
    options: List<String>,
    currentAnswer: String?,
    onSave: (String) -> Unit
) {
    // Convert comma-separated string to a Set for state
    val selectedOptions by remember(currentAnswer) {
        mutableStateOf(currentAnswer?.split(", ")?.toSet() ?: emptySet())
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = selectedOptions.contains(option)
            ExerciseChip(
                text = option,
                isSelected = isSelected,
                onClick = {
                    // Create new set based on selection
                    val newSet = selectedOptions.toMutableSet()
                    if (isSelected) {
                        newSet.remove(option)
                    } else {
                        newSet.add(option)
                    }
                    // Save as a sorted, comma-separated string
                    onSave(newSet.sorted().joinToString(", "))
                }
            )
        }
    }
}

/**
 * A selectable chip for the ExerciseTypeInput
 */
@Composable
private fun ExerciseChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AppTheme.colors.VibrantGreen else AppTheme.colors.LightGreyText.copy(alpha = 0.3f)
    val containerColor = if (isSelected) AppTheme.colors.VibrantGreen.copy(alpha = 0.05f) else AppTheme.colors.CardBackground
    val contentColor = if (isSelected) AppTheme.colors.VibrantGreen else AppTheme.colors.DarkGreyText

    // Simple keyword to icon mapping
    val icon = remember(text) {
        when {
            text.contains("Cardio", ignoreCase = true) -> Icons.AutoMirrored.Filled.DirectionsRun
            text.contains("Strength", ignoreCase = true) -> Icons.Default.FitnessCenter
            text.contains("Yoga", ignoreCase = true) -> Icons.Default.SelfImprovement
            text.contains("Sports", ignoreCase = true) -> Icons.Default.SportsBasketball
            text.contains("Swimming", ignoreCase = true) -> Icons.Default.Pool
            text.contains("HIIT", ignoreCase = true) -> Icons.Default.LocalFireDepartment
            else -> Icons.AutoMirrored.Filled.HelpOutline
        }
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
            )
        }
    }
}