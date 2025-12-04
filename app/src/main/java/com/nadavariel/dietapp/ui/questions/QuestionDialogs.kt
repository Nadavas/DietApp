package com.nadavariel.dietapp.ui.questions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import com.nadavariel.dietapp.model.Question
import com.nadavariel.dietapp.ui.AppTheme

@RequiresApi(Build.VERSION_CODES.O)
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
                onSave = { newAnswer -> tempAnswer = newAnswer }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(tempAnswer)
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
