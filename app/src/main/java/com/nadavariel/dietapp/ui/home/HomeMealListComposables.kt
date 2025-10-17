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
import androidx.compose.material.icons.outlined.RestaurantMenu // Added for EmptyState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.max

// --- DESIGN TOKENS ---
private val VibrantGreen = Color(0xFF4CAF50)
private val LightGrey = Color(0xFFF0F0F0)
private val DarkGreyText = Color(0xFF333333)
private val LightGreyText = Color(0xFF757575)

// --- REIMAGINED: CalorieSummaryCard ---
@Composable
fun CalorieSummaryCard(totalCalories: Int, goalCalories: Int) {
    val remaining = max(0, goalCalories - totalCalories)
    val progress = if (goalCalories > 0) (totalCalories.toFloat() / goalCalories.toFloat()) else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progressAnimation", animationSpec = spring(stiffness = 50f))

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = LightGrey,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 30f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = VibrantGreen,
                        startAngle = -90f,
                        sweepAngle = 360 * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 30f, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$remaining",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = DarkGreyText
                    )
                    Text(text = "KCAL LEFT", style = MaterialTheme.typography.bodySmall, color = LightGreyText)
                }
            }
            Spacer(modifier = Modifier.width(24.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryStat(label = "Consumed", value = totalCalories)
                Divider()
                SummaryStat(label = "Goal", value = goalCalories)
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: Int) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = LightGreyText)
        Text(
            text = "$value kcal",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = DarkGreyText
        )
    }
}


// --- REIMAGINED: MealSectionHeader ---
@Composable
fun MealSectionHeader(section: MealSection, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(section.color, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = section.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = DarkGreyText
        )
    }
}


// --- REIMAGINED: MealItem ---
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .clickable {
                onToggleActions(meal.id)
                coroutineScope.launch {
                    delay(250) // Wait for animation to start
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
                Column(modifier = Modifier.weight(1f)) {
                    val servingInfo = if (!meal.servingAmount.isNullOrBlank() && !meal.servingUnit.isNullOrBlank()) {
                        "(${meal.servingAmount} ${meal.servingUnit})"
                    } else ""
                    Text(
                        text = meal.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreyText
                    )
                    if (servingInfo.isNotBlank()) {
                        Text(
                            text = servingInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = LightGreyText
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                // AnimatedSwitcher for Calories/Actions
                AnimatedContent(
                    targetState = showActions,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.9f)).togetherWith(fadeOut() + scaleOut(targetScale = 0.9f))
                    },
                    label = "actions_calories_switch"
                ) { targetState ->
                    if (targetState) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                        ) {
                            IconButton(onClick = { onEdit(meal) }) {
                                Icon(Icons.Default.Edit, "Edit Meal", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { onDelete(meal) }) {
                                Icon(Icons.Default.Delete, "Delete Meal", tint = MaterialTheme.colorScheme.error)
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
                                color = LightGreyText
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
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    NutritionDetailsTable(meal)
                }
            }
        }
    }
}


// --- REIMAGINED: NutritionDetailsTable (Colors updated) ---
@OptIn(ExperimentalLayoutApi::class) // Added for FlowRow
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
            text = "Macronutrients (g)",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = LightGreyText,
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

        // Section separator
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
                    indication = null, // No ripple
                    // FIX: Correct way to create an interaction source
                    interactionSource = remember { MutableInteractionSource() }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Divider(modifier = Modifier.weight(1f))
            Icon(
                imageVector = if (microNutrientsVisible) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (microNutrientsVisible) "Hide" else "Show More",
                tint = LightGreyText,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Divider(modifier = Modifier.weight(1f))
        }

        // Micronutrients Section
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
                    color = LightGreyText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // FIX: Use the modern, built-in FlowRow
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
fun NutritionDetailItem(label: String, value: Double?, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = LightGreyText
        )
        Text(
            text = value?.let { "${String.format("%.1f", it)}$unit" } ?: "–",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = DarkGreyText
        )
    }
}


// --- REIMAGINED: EmptyState ---
@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // FIX: Use a valid ImageVector and content description
        Icon(
            imageVector = Icons.Outlined.RestaurantMenu,
            contentDescription = "No meals",
            modifier = Modifier.size(64.dp),
            tint = LightGreyText.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No meals logged yet",
            style = MaterialTheme.typography.headlineSmall,
            color = DarkGreyText
        )
        Text(
            "Tap the '+' button to add your first meal.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            lineHeight = 24.sp,
            color = LightGreyText
        )
    }
}
