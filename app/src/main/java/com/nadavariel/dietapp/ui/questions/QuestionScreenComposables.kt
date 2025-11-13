package com.nadavariel.dietapp.ui.questions

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nadavariel.dietapp.model.Question
import com.nadavariel.dietapp.ui.QuestionColors.CardBackgroundColor
import com.nadavariel.dietapp.ui.QuestionColors.DarkGreyText
import com.nadavariel.dietapp.ui.QuestionColors.LightGreyText
import com.nadavariel.dietapp.ui.QuestionColors.VibrantGreen

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
                    if (allQuestionsAnswered) VibrantGreen.copy(alpha = 0.1f)
                    else LightGreyText.copy(alpha = 0.1f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (allQuestionsAnswered) Icons.Default.Check else Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (allQuestionsAnswered) VibrantGreen else LightGreyText
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (allQuestionsAnswered) "All Set!" else "Questionnaire Incomplete",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DarkGreyText,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (allQuestionsAnswered) "You can edit your answers or retake the quiz to get an updated diet plan." else "Please complete it to generate a personalized diet plan.",
            style = MaterialTheme.typography.bodyLarge,
            color = LightGreyText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (allQuestionsAnswered) {
            Button(
                onClick = onEditAnswers,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
            ) {
                Text("Edit Answers")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRetakeQuiz,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreyText),
                border = BorderStroke(1.dp, LightGreyText.copy(alpha = 0.3f))
            ) {
                Text("Retake Whole Quiz")
            }
        } else {
            Button(
                onClick = onRetakeQuiz,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
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
                    color = LightGreyText,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp, end = 4.dp)
                )
            }
            itemsIndexed(questions) { index, question ->
                // UI REFRESH: Replaced with new QuestionItemCard
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
            colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
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
        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
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
                    color = DarkGreyText
                )
                Text(
                    text = if (answer.isNullOrBlank()) "Tap to answer" else answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (answer.isNullOrBlank()) LightGreyText else VibrantGreen,
                    fontWeight = if (answer.isNullOrBlank()) FontWeight.Normal else FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = VibrantGreen)
        }
    }
}

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
            color = VibrantGreen,
            trackColor = VibrantGreen.copy(alpha = 0.1f),
            strokeCap = StrokeCap.Round
        )

        Text(
            question.text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DarkGreyText,
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
            colors = ButtonDefaults.buttonColors(containerColor = VibrantGreen)
        ) {
            Text(if (currentIndex < questions.lastIndex) "Next" else "Submit & Get Plan")
        }
    }
}