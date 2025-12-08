package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
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
import com.nadavariel.dietapp.data.QuestionnaireConstants // <--- 1. Import Constants
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.questions.*
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.QuestionsViewModel

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    navController: NavController,
    questionsViewModel: QuestionsViewModel = viewModel(),
    authViewModel: AuthViewModel,
    // --- 1. ADD THIS PARAMETER ---
    foodLogViewModel: com.nadavariel.dietapp.viewmodel.FoodLogViewModel,
    startQuiz: Boolean,
    source: String
) {
    var screenState by remember { mutableStateOf(ScreenState.LANDING) }
    val savedAnswers by questionsViewModel.userAnswers.collectAsState()

    val questions = QuestionnaireConstants.questions

    var quizAnswers by remember { mutableStateOf<List<String?>>(emptyList()) }
    var quizCurrentIndex by remember { mutableIntStateOf(0) }

    var editAnswers by remember { mutableStateOf<List<String?>>(emptyList()) }

    var questionToEditIndex by remember { mutableStateOf<Int?>(null) }

    // Helper function to extract and update weight
    fun updateOptimisticWeight(answers: List<String?>) {
        val questions = QuestionnaireConstants.questions
        val weightQuestionIndex = questions.indexOfFirst { it.text == QuestionnaireConstants.TARGET_WEIGHT_QUESTION }

        if (weightQuestionIndex != -1) {
            val weightAnswer = answers.getOrNull(weightQuestionIndex)
            // Extract number from string like "75 kg"
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
            TopAppBar(
                title = {
                    Text(
                        text = when(screenState) {
                            ScreenState.LANDING -> "Questionnaire"
                            ScreenState.EDITING -> "Edit Answers"
                            ScreenState.QUIZ_MODE -> "Question ${quizCurrentIndex + 1} of ${questions.size}"
                        },
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.darkGreyText
                    )
                },
                navigationIcon = {
                    val hideBackButton = startQuiz
                            && screenState == ScreenState.QUIZ_MODE
                            && quizCurrentIndex == 0
                            && source == "onboarding"

                    if (!hideBackButton) {
                        IconButton(onClick = {
                            when (screenState) {
                                ScreenState.LANDING -> {
                                    navController.popBackStack()
                                }
                                ScreenState.QUIZ_MODE -> {
                                    if (quizCurrentIndex > 0) {
                                        quizCurrentIndex--
                                    } else {
                                        if (startQuiz) {
                                            navController.popBackStack()
                                        } else {
                                            screenState = ScreenState.LANDING
                                        }
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
                                tint = AppTheme.colors.darkGreyText
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.screenBackground
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
                        // FIX: Update weight immediately so Home screen doesn't flicker
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
                            // FIX: Update weight immediately so Home screen doesn't flicker
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

private enum class ScreenState { LANDING, EDITING, QUIZ_MODE }