package com.nadavariel.dietapp.screens

import android.app.DatePickerDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle // <-- NEW IMPORT
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.model.DietPlan
import com.nadavariel.dietapp.model.ExampleMeal // <-- NEW IMPORT
import com.nadavariel.dietapp.viewmodel.DietPlanResult
import com.nadavariel.dietapp.viewmodel.QuestionsViewModel
import java.util.*
import kotlin.math.roundToInt

// --- DESIGN TOKENS (from ThreadsScreen) ---
private val VibrantGreen = Color(0xFF4CAF50)
private val DarkGreyText = Color(0xFF333333)
private val LightGreyText = Color(0xFF757575)
private val ScreenBackgroundColor = Color(0xFFF7F9FC)
private val CardBackgroundColor = Color.White

// --- Data Models ---
// MODIFICATION: Added EXERCISE_TYPE
enum class InputType { DOB, HEIGHT, WEIGHT, TEXT, TARGET_WEIGHT, EXERCISE_TYPE }
data class Question(
    val text: String,
    val options: List<String>? = null,
    val inputType: InputType? = null
)

// Define questions list outside the composable for reuse
private val questions = listOf(
    Question("What is your date of birth?", inputType = InputType.DOB),
    Question("What is your gender?", options = listOf("Male", "Female", "Other / Prefer not to say")),
    Question("What is your height?", inputType = InputType.HEIGHT),
    Question("What is your weight?", inputType = InputType.WEIGHT),
    Question("What is your primary fitness goal?", options = listOf("Lose weight", "Gain muscle", "Maintain current weight", "Improve overall health")),
    Question("Do you have a target weight or body composition goal in mind?", inputType = InputType.TARGET_WEIGHT),
    Question("How aggressive do you want to be with your fitness goal timeline?", options = listOf("Very aggressive (1–2 months)", "Moderate (3–6 months)", "Gradual (6+ months or no rush)")),
    Question("How would you describe your daily activity level outside of exercise?", options = listOf("Sedentary", "Lightly active", "Moderately active", "Very active")),
    Question("How many days per week do you engage in structured exercise?", options = listOf("0-1", "2-3", "4-5", "6-7")),
    // MODIFICATION: Changed to EXERCISE_TYPE and added options
    Question(
        "What types of exercise do you typically perform?",
        inputType = InputType.EXERCISE_TYPE,
        options = listOf("Cardio", "Strength Training", "Yoga / Pilates", "Team Sports", "Swimming", "HIIT", "Other")
    )
)

// --- Screen State ---
private enum class ScreenState { LANDING, EDITING, QUIZ_MODE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    navController: NavController,
    questionsViewModel: QuestionsViewModel = viewModel()
) {
    var screenState by remember { mutableStateOf(ScreenState.LANDING) }
    val savedAnswers by questionsViewModel.userAnswers.collectAsState()

    // Separate state for quiz mode - this gets cleared when retaking
    var quizAnswers by remember { mutableStateOf<List<String?>>(emptyList()) }
    var quizCurrentIndex by remember { mutableStateOf(0) }

    // Local answers for editing mode
    var editAnswers by remember { mutableStateOf<List<String?>>(emptyList()) }

    // State for managing which question is being edited in a dialog
    var questionToEditIndex by remember { mutableStateOf<Int?>(null) }

    // Sync edit answers with saved answers from ViewModel when not in quiz mode
    LaunchedEffect(savedAnswers, screenState) {
        if (screenState != ScreenState.QUIZ_MODE && savedAnswers.isNotEmpty()) {
            editAnswers = questions.map { q ->
                savedAnswers.find { it.question == q.text }?.answer
            }
        }
    }

    // --- DIALOGS for API Results ---
    HandleDietPlanResultDialogs(navController, questionsViewModel)

    Scaffold(
        containerColor = ScreenBackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when(screenState) {
                            ScreenState.LANDING -> "Your Profile"
                            ScreenState.EDITING -> "Edit Answers"
                            ScreenState.QUIZ_MODE -> "Question ${quizCurrentIndex + 1} of ${questions.size}"
                        },
                        fontWeight = FontWeight.Bold,
                        color = DarkGreyText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (screenState) {
                            ScreenState.LANDING -> navController.popBackStack()
                            ScreenState.QUIZ_MODE -> {
                                // Go back to previous question or landing
                                if (quizCurrentIndex > 0) {
                                    quizCurrentIndex--
                                } else {
                                    quizAnswers = emptyList()
                                    quizCurrentIndex = 0
                                    screenState = ScreenState.LANDING
                                }
                            }
                            else -> {
                                screenState = ScreenState.LANDING
                            }
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkGreyText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScreenBackgroundColor
                )
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
                        // Initialize quiz with empty answers
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
                        questionsViewModel.saveAnswersAndRegeneratePlan(questions, editAnswers)
                        screenState = ScreenState.LANDING
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
                        // Update the answer for current question
                        quizAnswers = quizAnswers.toMutableList().apply {
                            // Ensure list is big enough
                            while (size <= quizCurrentIndex) add(null)
                            set(quizCurrentIndex, answer)
                        }
                    },
                    onNext = {
                        if (quizCurrentIndex < questions.lastIndex) {
                            quizCurrentIndex++
                        } else {
                            // Submit the quiz
                            questionsViewModel.saveAnswersAndRegeneratePlan(questions, quizAnswers)
                            quizAnswers = emptyList()
                            quizCurrentIndex = 0
                            screenState = ScreenState.LANDING
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
            text = if (allQuestionsAnswered) "Your profile is complete!" else "Your profile is incomplete",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = DarkGreyText,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (allQuestionsAnswered) "You can edit your answers or retake the quiz to get an updated diet plan." else "Please complete the quiz to generate a personalized diet plan.",
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
private fun EditingContent(
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
private fun QuizModeContent(
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
private fun QuestionInput(
    question: Question,
    currentAnswer: String?,
    onSave: (String) -> Unit
) {
    // UI REFRESH: Routing to new/styled inputs
    when {
        question.inputType == InputType.DOB -> DobInput(currentAnswer, onSave)

        // NEW: Using AnimatedSliderInput for all three
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
    // UI REFRESH: Styled text field
    OutlinedTextField(
        value = currentValue ?: "",
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Type your answer...") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = VibrantGreen,
            focusedLabelColor = VibrantGreen,
            cursorColor = VibrantGreen
        ),
        minLines = 3 // Makes more sense for "types of exercise"
    )
}

@Composable
private fun OptionsInput(options: List<String>, currentAnswer: String?, onSave: (String) -> Unit) {
    // UI REFRESH: Using new OptionCardItem
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

// UI REFRESH: New Composable for OptionsInput
@Composable
private fun OptionCardItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) VibrantGreen else LightGreyText.copy(alpha = 0.3f)
    val containerColor = if (isSelected) VibrantGreen.copy(alpha = 0.05f) else CardBackgroundColor

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
                color = if (isSelected) VibrantGreen else DarkGreyText,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = VibrantGreen
                )
            }
        }
    }
}

@Composable
fun DobInput(currentAnswer: String?, onSave: (String) -> Unit) {
    val context = LocalContext.current
    val cal = Calendar.getInstance()

    // Parse existing answer if available
    if (!currentAnswer.isNullOrBlank()) {
        try {
            val parts = currentAnswer.split("-").map { it.toInt() }
            cal.set(parts[0], parts[1] - 1, parts[2])
        } catch (e: Exception) { /* Ignore */ }
    }

    // UI REFRESH: Styled as a Card
    val hasAnswer = !currentAnswer.isNullOrBlank()
    Card(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day ->
                    val isoDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                    onSave(isoDate)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
        border = BorderStroke(1.dp, LightGreyText.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CalendarToday,
                contentDescription = "Pick Date",
                tint = VibrantGreen,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                if (hasAnswer) currentAnswer!! else "Select Date of Birth",
                color = if (hasAnswer) VibrantGreen else DarkGreyText,
                fontWeight = if (hasAnswer) FontWeight.Bold else FontWeight.Normal,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

// ---
// --- NEW COMPOSABLES START HERE ---
// ---

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
    // Parse the initial value from the answer string, or use default
    val (initialValue, _) = remember(currentAnswer) {
        if (currentAnswer.isNullOrBlank()) {
            defaultVal to unit
        } else {
            val parsed = currentAnswer.replace(Regex("[^0-9.]"), "").toFloatOrNull()
            (parsed ?: defaultVal) to unit
        }
    }

    var sliderValue by remember { mutableStateOf(initialValue) }

    // This function will be called when the slider stops moving
    val saveValue = {
        val roundedValue = (sliderValue / step).roundToInt() * step
        sliderValue = roundedValue
        onSave(String.format(Locale.US, "%.1f %s", roundedValue, unit))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
        border = BorderStroke(1.dp, LightGreyText.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large text display
            Text(
                text = String.format(Locale.US, "%.1f %s", sliderValue, unit),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = VibrantGreen
            )
            Spacer(Modifier.height(16.dp))

            // Slider
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = valueRange,
                onValueChangeFinished = { saveValue() },
                colors = SliderDefaults.colors(
                    thumbColor = VibrantGreen,
                    activeTrackColor = VibrantGreen,
                    inactiveTrackColor = VibrantGreen.copy(alpha = 0.2f)
                )
            )

            // Fine-tune buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    sliderValue = (sliderValue - step).coerceIn(valueRange)
                    saveValue()
                }) {
                    Icon(Icons.Default.Remove, "Decrease", tint = DarkGreyText)
                }
                IconButton(onClick = {
                    sliderValue = (sliderValue + step).coerceIn(valueRange)
                    saveValue()
                }) {
                    Icon(Icons.Default.Add, "Increase", tint = DarkGreyText)
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
    // Convert comma-separated string to a Set for state
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
                    // Create new set based on selection
                    val newSet = selectedOptions.toMutableSet()
                    if (isSelected) {
                        newSet.remove(option)
                    } else {
                        newSet.add(option)
                    }
                    // Save as a sorted, comma-separated string
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
    val borderColor = if (isSelected) VibrantGreen else LightGreyText.copy(alpha = 0.3f)
    val containerColor = if (isSelected) VibrantGreen.copy(alpha = 0.05f) else CardBackgroundColor
    val contentColor = if (isSelected) VibrantGreen else DarkGreyText

    // Simple keyword to icon mapping
    val icon = remember(text) {
        when {
            text.contains("Cardio", ignoreCase = true) -> Icons.Default.DirectionsRun
            text.contains("Strength", ignoreCase = true) -> Icons.Default.FitnessCenter
            text.contains("Yoga", ignoreCase = true) -> Icons.Default.SelfImprovement
            text.contains("Sports", ignoreCase = true) -> Icons.Default.SportsBasketball
            text.contains("Swimming", ignoreCase = true) -> Icons.Default.Pool
            text.contains("HIIT", ignoreCase = true) -> Icons.Default.LocalFireDepartment
            else -> Icons.Default.HelpOutline
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


// ---
// --- END OF NEW COMPOSABLES ---
// ---

private fun parseUnitValue(
    answer: String?,
    defaultUnit: String,
    stripRegex: String = "[^0-9]"
): Pair<String, String> {
    if (answer.isNullOrBlank()) return "" to defaultUnit
    val value = answer.replace(Regex(stripRegex), "")
    val unit = Regex("[a-zA-Z]+").find(answer)?.value ?: defaultUnit
    return value to unit
}

@Composable
private fun HandleDietPlanResultDialogs(
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

// ---
// --- !!! THIS IS THE FULLY REPLACED/UPDATED DIALOG !!! ---
// ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DietPlanSuccessDialog(plan: DietPlan, onDismiss: () -> Unit, onApplyToGoals: () -> Unit) {
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
fun DietPlanErrorDialog(message: String, onDismiss: () -> Unit) {
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