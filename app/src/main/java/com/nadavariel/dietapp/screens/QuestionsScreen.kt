package com.nadavariel.dietapp.screens

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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.data.QuestionnaireConstants
import com.nadavariel.dietapp.model.InputType
import com.nadavariel.dietapp.model.Question
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.AppDatePickerDialog
import com.nadavariel.dietapp.ui.AppTopBar
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.QuestionsViewModel
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

private enum class ScreenState { LANDING, EDITING, QUIZ_MODE }

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    navController: NavController,
    questionsViewModel: QuestionsViewModel = viewModel(),
    authViewModel: AuthViewModel,
    foodLogViewModel: com.nadavariel.dietapp.viewmodel.FoodLogViewModel,
    startQuiz: Boolean,
    source: String
) {
    val questions = QuestionnaireConstants.questions
    val savedAnswers by questionsViewModel.userAnswers.collectAsState()
    var screenState by remember { mutableStateOf(ScreenState.LANDING) }
    var quizAnswers by remember { mutableStateOf<List<String?>>(emptyList()) }
    var quizCurrentIndex by remember { mutableIntStateOf(0) }
    var editAnswers by remember { mutableStateOf<List<String?>>(emptyList()) }
    var questionToEditIndex by remember { mutableStateOf<Int?>(null) }

    fun updateOptimisticWeight(answers: List<String?>) {
        val questions = QuestionnaireConstants.questions
        val weightQuestionIndex = questions.indexOfFirst { it.text == QuestionnaireConstants.TARGET_WEIGHT_QUESTION }

        if (weightQuestionIndex != -1) {
            val weightAnswer = answers.getOrNull(weightQuestionIndex)
            val weightVal = weightAnswer?.split(" ")?.firstOrNull()?.toFloatOrNull()

            if (weightVal != null && weightVal > 0f) {
                foodLogViewModel.setTargetWeightOptimistically(weightVal)
            }
        }
    }

    LaunchedEffect(savedAnswers, screenState) {
        if (screenState != ScreenState.QUIZ_MODE && savedAnswers.isNotEmpty()) {
            editAnswers = questions.map { q ->
                savedAnswers.find { it.question == q.text }?.answer
            }
        }
    }

    LaunchedEffect(Unit) {
        if (startQuiz) {
            quizAnswers = List(questions.size) { null }
            quizCurrentIndex = 0
            screenState = ScreenState.QUIZ_MODE
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.screenBackground,
        topBar = {
            val titleText = when(screenState) {
                ScreenState.LANDING -> "Questionnaire"
                ScreenState.EDITING -> "Edit Answers"
                ScreenState.QUIZ_MODE -> "Question ${quizCurrentIndex + 1} of ${questions.size}"
            }

            val hideBackButton = startQuiz
                    && screenState == ScreenState.QUIZ_MODE
                    && quizCurrentIndex == 0
                    && source == "onboarding"

            AppTopBar(
                title = titleText,
                showBack = !hideBackButton,
                onBack = {
                    when (screenState) {
                        ScreenState.LANDING -> navController.popBackStack()
                        ScreenState.QUIZ_MODE -> {
                            if (quizCurrentIndex > 0) {
                                quizCurrentIndex--
                            } else {
                                if (startQuiz) navController.popBackStack()
                                else screenState = ScreenState.LANDING
                            }
                        }
                        else -> screenState = ScreenState.LANDING
                    }
                }
            )
        }
    ) { paddingValues ->
        when (screenState) {
            ScreenState.LANDING -> {
                val allAnswered = editAnswers.isNotEmpty() && editAnswers.all { !it.isNullOrBlank() }
                LandingContent(
                    modifier = Modifier.padding(paddingValues),
                    allQuestionsAnswered = allAnswered,
                    onEditAnswers = {
                        editAnswers = questions.map { q ->
                            savedAnswers.find { it.question == q.text }?.answer
                        }
                        screenState = ScreenState.EDITING
                    },
                    onRetakeQuiz = {
                        quizAnswers = List(questions.size) { null }
                        quizCurrentIndex = 0
                        screenState = ScreenState.QUIZ_MODE
                    }
                )
            }
            ScreenState.EDITING -> {
                EditingContent(
                    modifier = Modifier.padding(paddingValues),
                    answers = editAnswers,
                    onEditClick = { index -> questionToEditIndex = index },
                    onSaveAndGenerate = {
                        questionsViewModel.saveAnswersAndRegeneratePlan(
                            authViewModel,
                            questions,
                            editAnswers
                        )
                        updateOptimisticWeight(editAnswers)

                        if (source == "account") {
                            navController.navigate(NavRoutes.ACCOUNT) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        } else {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(NavRoutes.LANDING) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
            ScreenState.QUIZ_MODE -> {
                QuizModeContent(
                    modifier = Modifier.padding(paddingValues),
                    currentIndex = quizCurrentIndex,
                    question = questions[quizCurrentIndex],
                    currentAnswer = quizAnswers.getOrNull(quizCurrentIndex),
                    onAnswerChanged = { answer ->
                        quizAnswers = quizAnswers.toMutableList().apply {
                            while (size <= quizCurrentIndex) add(null)
                            set(quizCurrentIndex, answer)
                        }
                    },
                    onNext = {
                        if (quizCurrentIndex < questions.lastIndex) {
                            quizCurrentIndex++
                        } else {
                            questionsViewModel.saveAnswersAndRegeneratePlan(
                                authViewModel,
                                questions,
                                quizAnswers
                            )
                            updateOptimisticWeight(quizAnswers)

                            if (source == "account") {
                                navController.navigate(NavRoutes.ACCOUNT) {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            } else {
                                navController.navigate(NavRoutes.HOME) {
                                    popUpTo(NavRoutes.LANDING) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    },
                    canProceed = !quizAnswers.getOrNull(quizCurrentIndex).isNullOrBlank()
                )
            }
        }

        if (questionToEditIndex != null) {
            val index = questionToEditIndex!!
            EditQuestionDialog(
                question = questions[index],
                currentAnswer = editAnswers.getOrNull(index),
                onDismiss = { questionToEditIndex = null },
                onSave = { newAnswer ->
                    editAnswers = editAnswers.toMutableList().also { it[index] = newAnswer }
                    questionToEditIndex = null
                }
            )
        }
    }
}

// -------------------------------
// --------- COMPOSABLES ---------
// -------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun QuestionInput(
    question: Question,
    currentAnswer: String?,
    onSave: (String) -> Unit
) {
    when {
        question.inputType == InputType.DOB -> DobInput(currentAnswer, onSave)
        question.inputType == InputType.HEIGHT -> AnimatedSliderInput(
            currentAnswer = currentAnswer,
            onSave = onSave,
            valueRange = 120f..220f,
            defaultVal = 170f,
            step = 1f,
            unit = "cm"
        )
        question.inputType == InputType.WEIGHT -> AnimatedSliderInput(
            currentAnswer = currentAnswer,
            onSave = onSave,
            valueRange = 40f..150f,
            defaultVal = 70f,
            step = 0.5f,
            unit = "kg"
        )
        question.inputType == InputType.TARGET_WEIGHT -> AnimatedSliderInput(
            currentAnswer = currentAnswer,
            onSave = onSave,
            valueRange = 40f..150f,
            defaultVal = 70f,
            step = 0.5f,
            unit = "kg"
        )
        question.options != null && question.inputType == InputType.EXERCISE_TYPE -> ExerciseTypeInput(
            options = question.options,
            currentAnswer = currentAnswer,
            onSave = onSave
        )
        question.options != null -> OptionsInput(question.options, currentAnswer, onSave)
        else -> TextInput(currentAnswer, onValueChange = onSave)
    }
}

@Composable
private fun TextInput(currentValue: String?, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = currentValue ?: "",
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Type your answer...") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppTheme.colors.primaryGreen,
            focusedLabelColor = AppTheme.colors.primaryGreen,
            cursorColor = AppTheme.colors.primaryGreen
        ),
        minLines = 3
    )
}

@Composable
private fun OptionsInput(options: List<String>, currentAnswer: String?, onSave: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.forEach { opt ->
            val selected = currentAnswer == opt
            OptionCardItem(
                text = opt,
                isSelected = selected,
                onClick = { onSave(opt) }
            )
        }
    }
}

@Composable
private fun OptionCardItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AppTheme.colors.primaryGreen else AppTheme.colors.lightGreyText.copy(alpha = 0.3f)
    val containerColor = AppTheme.colors.cardBackground

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) AppTheme.colors.primaryGreen else AppTheme.colors.darkGreyText,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = AppTheme.colors.primaryGreen
                )
            }
        }
    }
}

@Composable
private fun DobInput(currentAnswer: String?, onSave: (String) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }

    // Parse existing answer: YYYY-MM-DD -> Calendar
    val cal = remember(currentAnswer) {
        Calendar.getInstance().apply {
            if (!currentAnswer.isNullOrBlank()) {
                try {
                    val parts = currentAnswer.split("-").map { it.toInt() }
                    // Calendar months are 0-indexed
                    set(parts[0], parts[1] - 1, parts[2])
                } catch (_: Exception) { /* Ignore */ }
            }
        }
    }

    val hasAnswer = !currentAnswer.isNullOrBlank()

    Card(
        onClick = { showDatePicker = true },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        border = BorderStroke(1.dp, AppTheme.colors.lightGreyText.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = "Pick Date",
                tint = AppTheme.colors.primaryGreen,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = if (hasAnswer) currentAnswer else "Select Date of Birth",
                color = if (hasAnswer) AppTheme.colors.primaryGreen else AppTheme.colors.darkGreyText,
                fontWeight = if (hasAnswer) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = cal,
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedCal ->
                // Format back to YYYY-MM-DD string for storage
                val isoDate = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    selectedCal.get(Calendar.YEAR),
                    selectedCal.get(Calendar.MONTH) + 1,
                    selectedCal.get(Calendar.DAY_OF_MONTH)
                )
                onSave(isoDate)
                showDatePicker = false
            }
        )
    }
}

/**
 * Replaces HeightInput, WeightInput, and TargetWeightInput
 */
@Composable
private fun AnimatedSliderInput(
    currentAnswer: String?,
    onSave: (String) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    defaultVal: Float,
    step: Float,
    unit: String
) {
    val (initialValue, _) = remember(currentAnswer) {
        if (currentAnswer.isNullOrBlank()) {
            defaultVal to unit
        } else {
            val parsed = currentAnswer.replace(Regex("[^0-9.]"), "").toFloatOrNull()
            (parsed ?: defaultVal) to unit
        }
    }

    var sliderValue by remember { mutableFloatStateOf(initialValue) }

    val saveValue = {
        val roundedValue = (sliderValue / step).roundToInt() * step
        sliderValue = roundedValue
        onSave(String.format(Locale.US, "%.1f %s", roundedValue, unit))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        border = BorderStroke(1.dp, AppTheme.colors.lightGreyText.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format(Locale.US, "%.1f %s", sliderValue, unit),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.primaryGreen
            )
            Spacer(Modifier.height(16.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = valueRange,
                onValueChangeFinished = { saveValue() },
                colors = SliderDefaults.colors(
                    thumbColor = AppTheme.colors.primaryGreen,
                    activeTrackColor = AppTheme.colors.primaryGreen,
                    inactiveTrackColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    sliderValue = (sliderValue - step).coerceIn(valueRange)
                    saveValue()
                }) {
                    Icon(Icons.Default.Remove, "Decrease", tint = AppTheme.colors.darkGreyText)
                }
                IconButton(onClick = {
                    sliderValue = (sliderValue + step).coerceIn(valueRange)
                    saveValue()
                }) {
                    Icon(Icons.Default.Add, "Increase", tint = AppTheme.colors.darkGreyText)
                }
            }
        }
    }
}

/**
 * Replaces the TextInput for exercise types
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseTypeInput(
    options: List<String>,
    currentAnswer: String?,
    onSave: (String) -> Unit
) {
    val selectedOptions by remember(currentAnswer) {
        mutableStateOf(currentAnswer?.split(", ")?.toSet() ?: emptySet())
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = selectedOptions.contains(option)
            ExerciseChip(
                text = option,
                isSelected = isSelected,
                onClick = {
                    val newSet = selectedOptions.toMutableSet()
                    if (isSelected) {
                        newSet.remove(option)
                    } else {
                        newSet.add(option)
                    }
                    onSave(newSet.sorted().joinToString(", "))
                }
            )
        }
    }
}

/**
 * A selectable chip for the ExerciseTypeInput
 */
@Composable
private fun ExerciseChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) AppTheme.colors.primaryGreen else AppTheme.colors.lightGreyText.copy(alpha = 0.3f)
    val containerColor = AppTheme.colors.cardBackground
    val contentColor = if (isSelected) AppTheme.colors.primaryGreen else AppTheme.colors.darkGreyText

    val icon = remember(text) {
        when {
            text.contains("Cardio", ignoreCase = true) -> Icons.AutoMirrored.Filled.DirectionsRun
            text.contains("Strength", ignoreCase = true) -> Icons.Default.FitnessCenter
            text.contains("Yoga", ignoreCase = true) -> Icons.Default.SelfImprovement
            text.contains("Sports", ignoreCase = true) -> Icons.Default.SportsBasketball
            text.contains("Swimming", ignoreCase = true) -> Icons.Default.Pool
            text.contains("HIIT", ignoreCase = true) -> Icons.Default.LocalFireDepartment
            else -> Icons.AutoMirrored.Filled.HelpOutline
        }
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun LandingContent(
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
private fun EditingContent(
    modifier: Modifier = Modifier,
    answers: List<String?>,
    onEditClick: (Int) -> Unit,
    onSaveAndGenerate: () -> Unit
) {
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
private fun QuizModeContent(
    modifier: Modifier = Modifier,
    currentIndex: Int,
    question: Question,
    currentAnswer: String?,
    onAnswerChanged: (String) -> Unit,
    onNext: () -> Unit,
    canProceed: Boolean
) {
    val questions = QuestionnaireConstants.questions

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
private fun EditQuestionDialog(
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