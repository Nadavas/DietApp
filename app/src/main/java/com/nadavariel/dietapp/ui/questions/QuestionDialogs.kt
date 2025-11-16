package com.nadavariel.dietapp.ui.questions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.nadavariel.dietapp.model.InputType
import com.nadavariel.dietapp.model.Question
import com.nadavariel.dietapp.ui.AppTheme
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
        containerColor = AppTheme.colors.ScreenBackground,
        title = {
            Text(
                question.text,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.DarkGreyText
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
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.VibrantGreen)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.DarkGreyText)
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
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.CardBackground)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = AppTheme.colors.VibrantGreen)
                        Spacer(Modifier.width(20.dp))
                        Text(
                            "Generating your plan...",
                            color = AppTheme.colors.DarkGreyText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        is DietPlanResult.Success -> {
            // --- THIS IS THE NEW LOGIC ---
            // No dialog. Just navigate and reset.
            // LaunchedEffect ensures this runs only once per success state.
            LaunchedEffect(result) {
                navController.navigate("goals")
                questionsViewModel.resetDietPlanResult()
            }
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

// --- DietPlanSuccessDialog and its helpers (SectionCard, MealPlanItem, FoodChip, MacroItem) have been DELETED ---

@Composable
internal fun DietPlanErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppTheme.colors.ScreenBackground,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Error", fontWeight = FontWeight.Bold, color = AppTheme.colors.DarkGreyText) },
        text = { Text(message, color = AppTheme.colors.DarkGreyText, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.VibrantGreen)
            ) {
                Text("Dismiss")
            }
        }
    )
}