package com.nadavariel.dietapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.ui.QuestionColors.DarkGreyText
import com.nadavariel.dietapp.ui.QuestionColors.ScreenBackgroundColor
import com.nadavariel.dietapp.ui.questions.*
import com.nadavariel.dietapp.viewmodel.QuestionsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    navController: NavController,
    questionsViewModel: QuestionsViewModel = viewModel(),
    startQuiz: Boolean // <-- 1. ADD NEW PARAMETER
) {
    var screenState by remember { mutableStateOf(ScreenState.LANDING) }
    val savedAnswers by questionsViewModel.userAnswers.collectAsState()

    // Separate state for quiz mode - this gets cleared when retaking
    var quizAnswers by remember { mutableStateOf<List<String?>>(emptyList()) }
    var quizCurrentIndex by remember { mutableIntStateOf(0) }

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

    // --- 2. ADD THIS LAUNCHED EFFECT ---
    // This will run once when the screen is composed.
    // If we're navigated here with startQuiz=true,
    // it will set up the quiz and change the state.
    LaunchedEffect(Unit) {
        if (startQuiz) {
            // This is the same logic from the onRetakeQuiz lambda
            quizAnswers = List(questions.size) { null }
            quizCurrentIndex = 0
            screenState = ScreenState.QUIZ_MODE
        }
    }
    // --- END OF FIX ---

    // --- DIALOGS for API Results ---
    HandleDietPlanResultDialogs(navController, questionsViewModel)

    Scaffold(
        containerColor = ScreenBackgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when(screenState) {
                            ScreenState.LANDING -> "Questionnaire"
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
                            ScreenState.LANDING -> {
                                // This logic is from our previous fix
                                if (navController.previousBackStackEntry != null) {
                                    navController.popBackStack()
                                } else {
                                    navController.navigate(NavRoutes.HOME) {
                                        popUpTo(navController.graph.id) { inclusive = true }
                                    }
                                }
                            }
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