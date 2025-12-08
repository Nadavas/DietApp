package com.nadavariel.dietapp.ui.questions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nadavariel.dietapp.data.QuestionnaireConstants // <--- 1. Import Constants
import com.nadavariel.dietapp.model.Question
import com.nadavariel.dietapp.ui.AppTheme

@Composable
internal fun LandingContent(
    modifier: Modifier = Modifier,
    allQuestionsAnswered: Boolean,
    onEditAnswers: () -> Unit,
    onRetakeQuiz: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // UI REFRESH: Styled icon presentation
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    if (allQuestionsAnswered) AppTheme.colors.primaryGreen.copy(alpha = 0.1f)
                    else AppTheme.colors.lightGreyText.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (allQuestionsAnswered) Icons.Default.Check else Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (allQuestionsAnswered) AppTheme.colors.primaryGreen else AppTheme.colors.lightGreyText
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (allQuestionsAnswered) "All Set!" else "Questionnaire Incomplete",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.darkGreyText,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (allQuestionsAnswered) "You can edit your answers or retake the quiz to get an updated diet plan." else "Please complete it to generate a personalized diet plan.",
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.colors.lightGreyText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (allQuestionsAnswered) {
            Button(
                onClick = onEditAnswers,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primaryGreen)
            ) {
                Text("Edit Answers")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRetakeQuiz,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppTheme.colors.darkGreyText),
                border = BorderStroke(1.dp, AppTheme.colors.lightGreyText.copy(alpha = 0.3f))
            ) {
                Text("Retake Whole Quiz")
            }
        } else {
            Button(
                onClick = onRetakeQuiz,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primaryGreen)
            ) {
                Text("Complete Quiz")
            }
        }
    }
}

@Composable
internal fun EditingContent(
    modifier: Modifier = Modifier,
    answers: List<String?>,
    onEditClick: (Int) -> Unit,
    onSaveAndGenerate: () -> Unit
) {
    // 2. Access the question list from constants
    val questions = QuestionnaireConstants.questions

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Tap on a question to provide or update your answer.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.colors.lightGreyText,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp, end = 4.dp)
                )
            }
            itemsIndexed(questions) { index, question ->
                QuestionItemCard(
                    question = question.text,
                    answer = answers.getOrNull(index),
                    onClick = { onEditClick(index) }
                )
            }
        }

        Button(
            onClick = onSaveAndGenerate,
            enabled = answers.size == questions.size && answers.all { !it.isNullOrBlank() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primaryGreen)
        ) {
            Text("Save Changes & Get Plan")
        }
    }
}

// UI REFRESH: New Card-based composable for EditingContent
@Composable
private fun QuestionItemCard(question: String, answer: String?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    question,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.darkGreyText
                )
                Text(
                    text = if (answer.isNullOrBlank()) "Tap to answer" else answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (answer.isNullOrBlank()) AppTheme.colors.lightGreyText else AppTheme.colors.primaryGreen,
                    fontWeight = if (answer.isNullOrBlank()) FontWeight.Normal else FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AppTheme.colors.primaryGreen)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
internal fun QuizModeContent(
    modifier: Modifier = Modifier,
    currentIndex: Int,
    question: Question,
    currentAnswer: String?,
    onAnswerChanged: (String) -> Unit,
    onNext: () -> Unit,
    canProceed: Boolean
) {
    // 3. Access the question list from constants
    val questions = QuestionnaireConstants.questions

    // UI REFRESH: Animated progress
    val progress by animateFloatAsState(
        targetValue = (currentIndex + 1).toFloat() / questions.size,
        animationSpec = tween(600),
        label = "progressAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // UI REFRESH: Styled progress indicator
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(8.dp)
                .clip(CircleShape),
            color = AppTheme.colors.primaryGreen,
            trackColor = AppTheme.colors.primaryGreen.copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round
        )

        Text(
            question.text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.darkGreyText,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // Use key to force recomposition when question changes
            key(currentIndex) {
                QuestionInput(
                    question = question,
                    currentAnswer = currentAnswer,
                    onSave = onAnswerChanged
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onNext,
            enabled = canProceed,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primaryGreen)
        ) {
            Text(if (currentIndex < questions.lastIndex) "Next" else "Submit & Get Plan")
        }
    }
}

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
