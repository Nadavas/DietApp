package com.nadavariel.dietapp.ui.home

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.model.MealSection
import com.nadavariel.dietapp.ui.AppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalorieSummaryCard(
    totalCalories: Int,
    goalCalories: Int,
    currentWeekStartDate: LocalDate,
    selectedDate: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onGoToToday: () -> Unit
) {
    val remaining = max(0, goalCalories - totalCalories)
    val progress = if (goalCalories > 0) (totalCalories.toFloat() / goalCalories.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "progressAnimation",
        animationSpec = spring(stiffness = 50f)
    )

    val circleColor = AppTheme.colors.primaryGreen

    // State for the Calendar Dialog
    var showDatePicker by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Calorie Info Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Progress Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color(0xFFF0F0F0),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 24f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = circleColor,
                            startAngle = -90f,
                            sweepAngle = 360 * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = 24f, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$remaining",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary,
                            fontSize = 32.sp
                        )
                        Text(
                            text = "left",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Stats Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CalorieStatRow(
                        label = "Consumed",
                        value = totalCalories,
                        color = AppTheme.colors.primaryGreen
                    )
                    HorizontalDivider(color = AppTheme.colors.divider)
                    CalorieStatRow(
                        label = "Goal",
                        value = goalCalories,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }

            // --- Date Navigation Section ---

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(
                color = AppTheme.colors.divider.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))

            val weekDays = remember(currentWeekStartDate) {
                (0..6).map { currentWeekStartDate.plusDays(it.toLong()) }
            }
            val today = LocalDate.now()
            val isTodayVisible = weekDays.any { it.isEqual(today) }

            // Month/Year navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPreviousWeek,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        "Previous Week",
                        tint = AppTheme.colors.primaryGreen
                    )
                }

                // Clickable Month/Year Text
                val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDate.format(formatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                }

                IconButton(
                    onClick = onNextWeek,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        "Next Week",
                        tint = AppTheme.colors.primaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Week days Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { date ->
                    DayOfWeekItem(
                        date = date,
                        isSelected = date.isEqual(selectedDate),
                        isToday = date.isEqual(today),
                        onClick = { onDateSelected(date) }
                    )
                }
            }

            // "Go to Today" button
            if (!isTodayVisible || !selectedDate.isEqual(today)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = onGoToToday,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(
                            "Go to Today",
                            color = AppTheme.colors.primaryGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // --- Date Picker Dialog ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onDateSelected(newDate)
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.primaryGreen)
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.textSecondary)
                ) {
                    Text("Cancel")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White,
            )
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
}

@Composable
private fun CalorieStatRow(label: String, value: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = "kcal",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DayOfWeekItem(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        isSelected -> AppTheme.colors.primaryGreen
        else -> Color.Transparent
    }
    val contentColor = when {
        isSelected -> Color.White
        else -> AppTheme.colors.textPrimary
    }
    // Change shape to RoundedCorner
    val shape = RoundedCornerShape(16.dp)

    val borderModifier = if (isToday && !isSelected) {
        Modifier.border(2.dp, AppTheme.colors.primaryGreen.copy(alpha = 0.5f), shape)
    } else Modifier

    Column(
        modifier = Modifier
            .width(44.dp) // Narrow enough to fit 7 days
            .height(60.dp) // Tall enough to fit text
            .clip(shape)
            .background(backgroundColor)
            .then(borderModifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) contentColor else AppTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}


@Composable
fun MealSectionHeader(section: MealSection, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(section.color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = section.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary,
            fontSize = 18.sp
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MealItem(
    meal: Meal,
    sectionColor: Color,
    showActions: Boolean,
    onToggleActions: (String) -> Unit,
    onDelete: (Meal) -> Unit,
    onEdit: (Meal) -> Unit
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .clickable {
                onToggleActions(meal.id)
                coroutineScope.launch {
                    delay(250)
                    bringIntoViewRequester.bringIntoView()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Meal info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meal.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 16.sp
                    )

                    val servingInfo = if (!meal.servingAmount.isNullOrBlank() && !meal.servingUnit.isNullOrBlank()) {
                        "${meal.servingAmount} ${meal.servingUnit}"
                    } else ""

                    if (servingInfo.isNotBlank()) {
                        Text(
                            text = servingInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Calories or Actions
                AnimatedContent(
                    targetState = showActions,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.8f)).togetherWith(
                            fadeOut() + scaleOut(targetScale = 0.8f)
                        )
                    },
                    label = "actions_calories_switch"
                ) { targetState ->
                    if (targetState) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFF3F4F6))
                        ) {
                            IconButton(onClick = { onEdit(meal) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    "Edit Meal",
                                    tint = AppTheme.colors.primaryGreen
                                )
                            }
                            IconButton(onClick = { onDelete(meal) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    "Delete Meal",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${meal.calories}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = sectionColor,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Expanded nutrition details
            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = AppTheme.colors.divider
                    )
                    NutritionDetailsTable(meal)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NutritionDetailsTable(meal: Meal) {
    var microNutrientsVisible by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        Text(
            text = "Macronutrients",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 12.dp),
            fontSize = 13.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NutritionDetailItem(
                label = "Protein",
                value = meal.protein,
                unit = "g",
                color = AppTheme.colors.primaryGreen,
                modifier = Modifier.weight(1f)
            )
            NutritionDetailItem(
                label = "Carbs",
                value = meal.carbohydrates,
                unit = "g",
                color = Color(0xFF00BFA5),
                modifier = Modifier.weight(1f)
            )
            NutritionDetailItem(
                label = "Fat",
                value = meal.fat,
                unit = "g",
                color = Color(0xFFFF6E40),
                modifier = Modifier.weight(1f)
            )
        }

        // Expandable micronutrients
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable(
                    onClick = {
                        microNutrientsVisible = !microNutrientsVisible
                        if (microNutrientsVisible) {
                            coroutineScope.launch {
                                delay(250)
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AppTheme.colors.divider
            )
            Icon(
                imageVector = if (microNutrientsVisible) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (microNutrientsVisible) "Hide" else "Show More",
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AppTheme.colors.divider
            )
        }

        AnimatedVisibility(
            visible = microNutrientsVisible,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column {
                Text(
                    text = "Micronutrients & Fiber",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 12.dp),
                    fontSize = 13.sp
                )

                // 3 Rows of 3 items logic
                val microNutrients = listOf(
                    Triple("Fiber", meal.fiber, "g"),
                    Triple("Sugar", meal.sugar, "g"),
                    Triple("Sodium", meal.sodium, "mg"),
                    Triple("Potassium", meal.potassium, "mg"),
                    Triple("Calcium", meal.calcium, "mg"),
                    Triple("Iron", meal.iron, "mg"),
                    Triple("Vitamin C", meal.vitaminC, "mg"),
                    Triple("Vitamin A", meal.vitaminA, "mcg"),
                    Triple("Vitamin B12", meal.vitaminB12, "mcg")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    microNutrients.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp), // Reduced vertical padding
                            // spacedBy(0.dp) ensures we use the full width via weights, effectively minimizing padding between columns
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            rowItems.forEach { (label, value, unit) ->
                                NutritionDetailItem(
                                    label = label,
                                    value = value,
                                    unit = unit,
                                    // weight(1f) ensures every column is exactly 1/3 of the width, creating perfect alignment
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun NutritionDetailItem(
    label: String,
    value: Double?,
    unit: String,
    color: Color = AppTheme.colors.textPrimary,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier // Modifier passed here handles the width/weight
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = AppTheme.colors.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Visible
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value?.let { "${String.format("%.1f", it)} $unit" } ?: "–",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Visible
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Restaurant,
            contentDescription = "No meals",
            modifier = Modifier.size(80.dp),
            tint = AppTheme.colors.textSecondary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "No meals logged yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary,
            fontSize = 20.sp
        )
    }
}