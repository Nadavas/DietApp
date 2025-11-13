package com.nadavariel.dietapp.ui.questions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.nadavariel.dietapp.model.DietPlan
import com.nadavariel.dietapp.model.ExampleMeal
import com.nadavariel.dietapp.model.InputType
import com.nadavariel.dietapp.model.Question
import com.nadavariel.dietapp.ui.QuestionColors.CardBackgroundColor
import com.nadavariel.dietapp.ui.QuestionColors.DarkGreyText
import com.nadavariel.dietapp.ui.QuestionColors.LightGreyText
import com.nadavariel.dietapp.ui.QuestionColors.ScreenBackgroundColor
import com.nadavariel.dietapp.ui.QuestionColors.VibrantGreen
import com.nadavariel.dietapp.viewmodel.DietPlanResult
import com.nadavariel.dietapp.viewmodel.QuestionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditQuestionDialog(
    question: Question,
    currentAnswer: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var tempAnswer by remember(currentAnswer) { mutableStateOf(currentAnswer ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ScreenBackgroundColor,
        title = {
            Text(
                question.text,
                fontWeight = FontWeight.Bold,
                color = DarkGreyText
            )
        },
        text = {
            QuestionInput(
                question = question,
                currentAnswer = tempAnswer,
                onSave = { newAnswer ->
                    when (question.inputType) {
                        InputType.DOB -> onSave(newAnswer) // Save immediately
                        InputType.TEXT -> tempAnswer = newAnswer // Wait for confirm
                        // Sliders update tempAnswer
                        InputType.HEIGHT, InputType.WEIGHT, InputType.TARGET_WEIGHT -> tempAnswer = newAnswer
                        // Options save immediately
                        null -> onSave(newAnswer)
                        // Exercise types save immediately
                        InputType.EXERCISE_TYPE -> onSave(newAnswer)
                    }
                }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    // For text, height, or weight inputs, apply when pressing Save
                    if (question.inputType == InputType.TEXT ||
                        question.inputType == InputType.HEIGHT ||
                        question.inputType == InputType.WEIGHT ||
                        question.inputType == InputType.TARGET_WEIGHT) {
                        onSave(tempAnswer)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = DarkGreyText)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun HandleDietPlanResultDialogs(
    navController: NavController,
    questionsViewModel: QuestionsViewModel
) {
    val dietPlanResult by questionsViewModel.dietPlanResult.collectAsState()

    when (val result = dietPlanResult) {
        is DietPlanResult.Loading -> {
            Dialog(onDismissRequest = {}) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = VibrantGreen)
                        Spacer(Modifier.width(20.dp))
                        Text(
                            "Generating your plan...",
                            color = DarkGreyText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        is DietPlanResult.Success -> {
            DietPlanSuccessDialog(
                plan = result.plan,
                onDismiss = { questionsViewModel.resetDietPlanResult() },
                onApplyToGoals = {
                    questionsViewModel.applyDietPlanToGoals(result.plan)
                    questionsViewModel.resetDietPlanResult()
                    navController.navigate("goals")
                }
            )
        }
        is DietPlanResult.Error -> {
            DietPlanErrorDialog(
                message = result.message,
                onDismiss = { questionsViewModel.resetDietPlanResult() }
            )
        }
        is DietPlanResult.Idle -> {}
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DietPlanSuccessDialog(plan: DietPlan, onDismiss: () -> Unit, onApplyToGoals: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ScreenBackgroundColor,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                "Your Personalized Plan",
                fontWeight = FontWeight.Bold,
                color = DarkGreyText
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Overview
                SectionCard(
                    title = "Your Health Overview",
                    icon = Icons.Default.PersonSearch
                ) {
                    Text(
                        text = plan.healthOverview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkGreyText,
                        lineHeight = 22.sp
                    )
                }

                // 2. Strategy
                SectionCard(
                    title = "Your Goal Strategy",
                    icon = Icons.Default.Flag
                ) {
                    Text(
                        text = plan.goalStrategy,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkGreyText,
                        lineHeight = 22.sp
                    )
                }

                // 3. Concrete Plan (Targets, Guidelines, Training)
                SectionCard(
                    title = "Your Concrete Plan",
                    icon = Icons.Default.Checklist
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Targets
                        Column {
                            Text(
                                "Daily Targets",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkGreyText
                            )
                            Spacer(Modifier.height(8.dp))
                            // Calorie Card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = VibrantGreen.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Daily Calories",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = VibrantGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        // THIS IS THE FIX: Accessing the nested value
                                        "${plan.concretePlan.targets.dailyCalories} kcal",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = VibrantGreen,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            // Macro Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                // THIS IS THE FIX: Accessing the nested values
                                MacroItem("Protein", plan.concretePlan.targets.proteinGrams, "g")
                                MacroItem("Carbs", plan.concretePlan.targets.carbsGrams, "g")
                                MacroItem("Fat", plan.concretePlan.targets.fatGrams, "g")
                            }
                        }

                        HorizontalDivider(color = LightGreyText.copy(alpha = 0.2f))

                        // Meal Guidelines
                        Column {
                            Text(
                                "Meal Guidelines",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkGreyText
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                plan.concretePlan.mealGuidelines.mealFrequency,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkGreyText,
                                fontStyle = FontStyle.Italic
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Foods to Emphasize:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = VibrantGreen)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                plan.concretePlan.mealGuidelines.foodsToEmphasize.forEach { FoodChip(it, true) }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("Foods to Limit:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Color(0xFFD32F2F))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                plan.concretePlan.mealGuidelines.foodsToLimit.forEach { FoodChip(it, false) }
                            }
                        }

                        HorizontalDivider(color = LightGreyText.copy(alpha = 0.2f))

                        // Training Advice
                        Column {
                            Text(
                                "Training Advice",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DarkGreyText
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                plan.concretePlan.trainingAdvice,
                                style = MaterialTheme.typography.bodyMedium,
                                color = DarkGreyText,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                // 4. Example Meal Plan
                SectionCard(
                    title = "Example Meal Plan",
                    icon = Icons.Default.Restaurant
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MealPlanItem("Breakfast", plan.exampleMealPlan.breakfast)
                        MealPlanItem("Lunch", plan.exampleMealPlan.lunch)
                        MealPlanItem("Dinner", plan.exampleMealPlan.dinner)
                        MealPlanItem("Snacks", plan.exampleMealPlan.snacks)
                    }
                }

                // 5. Disclaimer
                Text(
                    plan.disclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = LightGreyText,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onApplyToGoals,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
            ) {
                Text("Apply to Goals")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = DarkGreyText)
            ) {
                Text("Close")
            }
        }
    )
}

// Helper composable for the new dialog
@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LightGreyText.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = icon, contentDescription = null, tint = VibrantGreen)
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreyText
                )
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// Helper for Meal Plan (USES ExampleMeal)
@Composable
private fun MealPlanItem(mealType: String, meal: ExampleMeal) { // <-- USES ExampleMeal
    Column {
        Text(
            mealType,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = VibrantGreen
        )
        Text(
            meal.description,
            style = MaterialTheme.typography.bodyMedium,
            color = DarkGreyText
        )
        Text(
            "~${meal.estimatedCalories} kcal",
            style = MaterialTheme.typography.bodySmall,
            color = LightGreyText
        )
    }
}

// Helper for Food Chips
@Composable
private fun FoodChip(text: String, isGood: Boolean) {
    val chipColor = if (isGood) VibrantGreen else Color(0xFFD32F2F)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chipColor.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = chipColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}

// This is the MacroItem, replacing your old one
@Composable
private fun MacroItem(label: String, value: Int, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = LightGreyText
        )
        Text(
            "$value$unit",
            style = MaterialTheme.typography.titleLarge,
            color = DarkGreyText,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun DietPlanErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ScreenBackgroundColor,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Error", fontWeight = FontWeight.Bold, color = DarkGreyText) },
        text = { Text(message, color = DarkGreyText, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
            ) {
                Text("Dismiss")
            }
        }
    )
}