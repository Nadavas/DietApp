package com.nadavariel.dietapp.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.model.MealSection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// NOTE: The duplicate glassmorphism modifier function has been removed from this file.
// It will now use the one defined in HomeHeaderComposables.kt.

@Composable
fun CalorieSummaryCard(totalCalories: Int, goalCalories: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(24.dp)
            )
    ) {
        // 1. BACKGROUND LAYER: This is the blurred glass effect.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassmorphism(shape = RoundedCornerShape(24.dp))
        )

        // 2. CONTENT LAYER: This Column contains the sharp, readable text and progress bar.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Daily Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$totalCalories",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "/ $goalCalories kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            val progress = (totalCalories.toFloat() / goalCalories.toFloat()).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun MealSectionHeader(section: MealSection) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(section.color, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = section.sectionName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
            .bringIntoViewRequester(bringIntoViewRequester)
            .clickable {
                onToggleActions(meal.id)
                coroutineScope.launch {
                    delay(250)
                    bringIntoViewRequester.bringIntoView()
                }
            }
    ) {
        // 1. BACKGROUND LAYER: The blurred glass effect sits here.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassmorphism()
        )

        // 2. CONTENT LAYER: This Column holds all the sharp text and icons.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .animateContentSize(animationSpec = spring())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val servingInfo = if (!meal.servingAmount.isNullOrBlank() && !meal.servingUnit.isNullOrBlank()) {
                        "(${meal.servingAmount} ${meal.servingUnit})"
                    } else ""
                    Text(
                        text = meal.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = servingInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                AnimatedContent(
                    targetState = showActions,
                    transitionSpec = {
                        (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                    }, label = "actions_calories_switch"
                ) { targetState ->
                    if (targetState) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { onEdit(meal) }) {
                                Icon(Icons.Default.Edit, "Edit Meal", tint = Color.White)
                            }
                            IconButton(onClick = { onDelete(meal) }) {
                                Icon(Icons.Default.Delete, "Delete Meal", tint = MaterialTheme.colorScheme.errorContainer)
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${meal.calories} kcal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = sectionColor
                            )
                            val formattedTime = meal.timestamp.toDate().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalTime()
                                .format(DateTimeFormatter.ofPattern("HH:mm"))
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Column {
                    Divider(
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    )
                    NutritionDetailsTable(meal)
                }
            }
        }
    }
}

@Composable
fun NutritionDetailsTable(meal: Meal) {
    var microNutrientsVisible by remember { mutableStateOf(false) }
    val dividerColor = Color.White.copy(alpha = 0.2f)

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth()
        .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        Text(
            text = "Macronutrients (g)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NutritionDetailItem("Protein", meal.protein, "g")
            NutritionDetailItem("Carbs", meal.carbohydrates, "g")
            NutritionDetailItem("Fat", meal.fat, "g")
        }

        // Section separator with show/hide button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Divider
            Divider(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                color = dividerColor
            )

            // Show/Hide Button
            IconButton(
                onClick = {
                    microNutrientsVisible = !microNutrientsVisible

                    if (microNutrientsVisible) {
                        coroutineScope.launch {
                            delay(250)
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                },
                modifier = Modifier
                    .size(24.dp)
                    .background(dividerColor, CircleShape)
                    .clip(CircleShape)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (microNutrientsVisible) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (microNutrientsVisible) "Hide Micronutrients" else "Show Micronutrients",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Right Divider
            Divider(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                color = dividerColor
            )
        }

        // Micronutrients Section (Toggleable)
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
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    NutritionDetailItem("Fiber", meal.fiber, "g")
                    NutritionDetailItem("Sugar", meal.sugar, "g")
                    NutritionDetailItem("Sodium", meal.sodium, "mg")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    NutritionDetailItem("Potassium", meal.potassium, "mg")
                    NutritionDetailItem("Calcium", meal.calcium, "mg")
                    NutritionDetailItem("Iron", meal.iron, "mg")
                    NutritionDetailItem("Vit C", meal.vitaminC, "mg")
                }
            }
        }
    }
}

@Composable
fun RowScope.NutritionDetailItem(label: String, value: Double?, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = Color.White.copy(alpha = 0.8f)
        )
        Text(
            text = value?.let { "${String.format("%.1f", it)}$unit" } ?: "–",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "No meals logged for this day.\nTime to add one! 🥗",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}