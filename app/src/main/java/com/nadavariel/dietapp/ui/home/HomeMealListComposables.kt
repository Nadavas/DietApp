package com.nadavariel.dietapp.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.model.MealSection
import com.nadavariel.dietapp.ui.HomeColors.CardBackground
import com.nadavariel.dietapp.ui.HomeColors.DividerColor
import com.nadavariel.dietapp.ui.HomeColors.PrimaryGreen
import com.nadavariel.dietapp.ui.HomeColors.TextPrimary
import com.nadavariel.dietapp.ui.HomeColors.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

@Composable
fun CalorieSummaryCard(totalCalories: Int, goalCalories: Int) {
    val remaining = max(0, goalCalories - totalCalories)
    val progress = if (goalCalories > 0) (totalCalories.toFloat() / goalCalories.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "progressAnimation",
        animationSpec = spring(stiffness = 50f)
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Progress Circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background circle
                    drawArc(
                        color = Color(0xFFF0F0F0),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 24f, cap = StrokeCap.Round)
                    )
                    // Progress circle
                    drawArc(
                        color = PrimaryGreen,
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
                        color = TextPrimary,
                        fontSize = 32.sp
                    )
                    Text(
                        text = "left",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
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
                    color = PrimaryGreen
                )
                Divider(color = DividerColor)
                CalorieStatRow(
                    label = "Goal",
                    value = goalCalories,
                    color = TextSecondary
                )
            }
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
            color = TextSecondary,
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
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
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
            color = TextPrimary,
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
        colors = CardDefaults.cardColors(containerColor = CardBackground),
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
                        color = TextPrimary,
                        fontSize = 16.sp
                    )

                    val servingInfo = if (!meal.servingAmount.isNullOrBlank() && !meal.servingUnit.isNullOrBlank()) {
                        "${meal.servingAmount} ${meal.servingUnit}"
                    } else ""

                    if (servingInfo.isNotBlank()) {
                        Text(
                            text = servingInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
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
                                    tint = PrimaryGreen
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
                                color = TextSecondary,
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
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = DividerColor
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
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp),
            fontSize = 13.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NutritionDetailItem("Protein", meal.protein, "g", PrimaryGreen)
            NutritionDetailItem("Carbs", meal.carbohydrates, "g", Color(0xFF00BFA5))
            NutritionDetailItem("Fat", meal.fat, "g", Color(0xFFFF6E40))
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
            Divider(modifier = Modifier.weight(1f), color = DividerColor)
            Icon(
                imageVector = if (microNutrientsVisible) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (microNutrientsVisible) "Hide" else "Show More",
                tint = TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
            )
            Divider(modifier = Modifier.weight(1f), color = DividerColor)
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
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp),
                    fontSize = 13.sp
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    NutritionDetailItem("Fiber", meal.fiber, "g")
                    NutritionDetailItem("Sugar", meal.sugar, "g")
                    NutritionDetailItem("Sodium", meal.sodium, "mg")
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
fun NutritionDetailItem(
    label: String,
    value: Double?,
    unit: String,
    color: Color = TextPrimary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = TextSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value?.let { "${String.format("%.1f", it)}$unit" } ?: "–",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 15.sp
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
            tint = TextSecondary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "No meals logged yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 20.sp
        )
    }
}