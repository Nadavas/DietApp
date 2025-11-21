package com.nadavariel.dietapp.ui.questions

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import com.nadavariel.dietapp.model.InputType
import com.nadavariel.dietapp.model.Question
import com.nadavariel.dietapp.ui.AppTheme

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
        containerColor = AppTheme.colors.screenBackground,
        title = {
            Text(
                question.text,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.darkGreyText
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
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primaryGreen)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.darkGreyText)
            ) {
                Text("Cancel")
            }
        }
    )
}
