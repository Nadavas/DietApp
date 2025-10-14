package com.nadavariel.dietapp.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.data.DietPlan
import com.nadavariel.dietapp.viewmodel.DietPlanResult
import com.nadavariel.dietapp.viewmodel.QuestionsViewModel
import java.util.*

// --- Data Models ---
enum class InputType { DOB, HEIGHT, WEIGHT, TEXT }
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
    Question("What is your current weight?", inputType = InputType.WEIGHT),
    Question("What is your primary fitness goal?", options = listOf("Lose weight", "Gain muscle", "Maintain current weight", "Improve overall health")),
    Question("Do you have a target weight or body composition goal in mind?", inputType = InputType.TEXT),
    Question("How aggressive do you want to be with your fitness goal timeline?", options = listOf("Very aggressive (1–2 months)", "Moderate (3–6 months)", "Gradual (6+ months or no rush)")),
    Question("How would you describe your daily activity level outside of exercise?", options = listOf("Sedentary", "Lightly active", "Moderately active", "Very active")),
    Question("How many days per week do you engage in structured exercise?", options = listOf("0-1", "2-3", "4-5", "6-7")),
    Question("What types of exercise do you typically perform?", inputType = InputType.TEXT)
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
        topBar = {
            TopAppBar(
                title = {
                    Text(when(screenState) {
                        ScreenState.LANDING -> "Your Profile"
                        ScreenState.EDITING -> "Edit Answers"
                        ScreenState.QUIZ_MODE -> "Question ${quizCurrentIndex + 1} of ${questions.size}"
                    })
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    onCompleteQuiz = {
                        editAnswers = questions.map { q ->
                            savedAnswers.find { it.question == q.text }?.answer
                        }
                        screenState = ScreenState.EDITING
                    },
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
    onCompleteQuiz: () -> Unit,
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
        Icon(
            imageVector = if (allQuestionsAnswered) Icons.Default.Check else Icons.Default.Edit,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = if (allQuestionsAnswered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (allQuestionsAnswered) "Your profile is complete!" else "Your profile is incomplete",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (allQuestionsAnswered) "You can edit your answers or retake the quiz to get an updated diet plan." else "Please complete the quiz to generate a personalized diet plan.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (allQuestionsAnswered) {
            Button(onClick = onEditAnswers, modifier = Modifier.fillMaxWidth()) {
                Text("Edit Answers")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onRetakeQuiz, modifier = Modifier.fillMaxWidth()) {
                Text("Retake Whole Quiz")
            }
        } else {
            Button(onClick = onRetakeQuiz, modifier = Modifier.fillMaxWidth()) {
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
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            item {
                Text(
                    "Tap on a question to provide or update your answer.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            itemsIndexed(questions) { index, question ->
                QuestionItem(
                    question = question.text,
                    answer = answers.getOrNull(index),
                    onClick = { onEditClick(index) }
                )
                if (index < questions.lastIndex) {
                    HorizontalDivider()
                }
            }
        }

        Button(
            onClick = onSaveAndGenerate,
            enabled = answers.size == questions.size && answers.all { !it.isNullOrBlank() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Save Changes & Get Plan")
        }
    }
}

@Composable
private fun QuestionItem(question: String, answer: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(question, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (answer.isNullOrBlank()) "Tap to answer" else answer,
                style = MaterialTheme.typography.bodyMedium,
                color = if (answer.isNullOrBlank()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                fontWeight = if (answer.isNullOrBlank()) FontWeight.Normal else FontWeight.Bold
            )
        }
        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / questions.size },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Text(
            question.text,
            style = MaterialTheme.typography.headlineSmall,
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

        Button(
            onClick = onNext,
            enabled = canProceed,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (currentIndex < questions.lastIndex) "Next" else "Submit & Get Plan")
        }
    }
}

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
        title = { Text(question.text) },
        text = {
            QuestionInput(
                question = question,
                currentAnswer = tempAnswer,
                onSave = { newAnswer ->
                    when (question.inputType) {
                        InputType.DOB -> onSave(newAnswer) // Save immediately for DOB
                        InputType.TEXT -> tempAnswer = newAnswer // Wait for confirm
                        InputType.HEIGHT, InputType.WEIGHT -> tempAnswer = newAnswer // Don't close dialog
                        null -> onSave(newAnswer) // For options list, can save immediately
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
                        question.inputType == InputType.WEIGHT) {
                        onSave(tempAnswer)
                    }
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun QuestionInput(
    question: Question,
    currentAnswer: String?,
    onSave: (String) -> Unit
) {
    when {
        question.inputType == InputType.DOB -> DobInput(currentAnswer, onSave)
        question.inputType == InputType.HEIGHT -> HeightInput(currentAnswer, onSave)
        question.inputType == InputType.WEIGHT -> WeightInput(currentAnswer, onSave)
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
        placeholder = { Text("Type your answer...") }
    )
}

@Composable
private fun OptionsInput(options: List<String>, currentAnswer: String?, onSave: (String) -> Unit) {
    Column {
        options.forEach { opt ->
            val selected = currentAnswer == opt
            OutlinedButton(
                onClick = { onSave(opt) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Text(opt)
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

    OutlinedButton(
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", modifier = Modifier.padding(end = 8.dp))
        Text(if (currentAnswer.isNullOrBlank()) "Select Date of Birth" else currentAnswer)
    }
}

@Composable
private fun HeightInput(currentAnswer: String?, onSave: (String) -> Unit) {
    val (initialValue, initialUnit) = remember(currentAnswer) {
        parseUnitValue(currentAnswer, "cm")
    }

    var value by remember(currentAnswer) { mutableStateOf(initialValue) }
    var unit by remember(currentAnswer) { mutableStateOf(initialUnit) }

    // Save whenever value or unit changes (but only if value is not blank)
    LaunchedEffect(value, unit) {
        if (value.isNotBlank()) {
            onSave("$value $unit")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                value = newValue.filter { it.isDigit() }
            },
            placeholder = { Text("Enter height") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("cm", "ft").forEach { u ->
                FilterChip(
                    selected = unit == u,
                    onClick = { unit = u },
                    label = { Text(u) }
                )
            }
        }
    }
}

@Composable
private fun WeightInput(currentAnswer: String?, onSave: (String) -> Unit) {
    val (initialValue, initialUnit) = remember(currentAnswer) {
        parseUnitValue(currentAnswer, "kg", "[^0-9.]")
    }

    var value by remember(currentAnswer) { mutableStateOf(initialValue) }
    var unit by remember(currentAnswer) { mutableStateOf(initialUnit) }

    // Save whenever value or unit changes (but only if value is not blank)
    LaunchedEffect(value, unit) {
        if (value.isNotBlank()) {
            onSave("$value $unit")
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                value = newValue.filter { it.isDigit() || it == '.' }
            },
            placeholder = { Text("Enter weight") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("kg", "lbs").forEach { u ->
                FilterChip(
                    selected = unit == u,
                    onClick = { unit = u },
                    label = { Text(u) }
                )
            }
        }
    }
}

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
                Card(modifier = Modifier.padding(32.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(16.dp))
                        Text("Generating your plan...")
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

@Composable
fun DietPlanSuccessDialog(plan: DietPlan, onDismiss: () -> Unit, onApplyToGoals: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your Personalized Diet Plan") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Daily Calories Target", style = MaterialTheme.typography.labelLarge)
                        Text("${plan.dailyCalories} kcal", style = MaterialTheme.typography.headlineMedium)
                    }
                }
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Macronutrient Breakdown", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroItem("Protein", plan.proteinGrams, "g")
                            MacroItem("Carbs", plan.carbsGrams, "g")
                            MacroItem("Fat", plan.fatGrams, "g")
                        }
                    }
                }
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Recommendations", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(plan.recommendations, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(plan.disclaimer, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = onApplyToGoals) {
                Text("Apply to Goals")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun MacroItem(label: String, value: Int, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text("$value$unit", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun DietPlanErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}