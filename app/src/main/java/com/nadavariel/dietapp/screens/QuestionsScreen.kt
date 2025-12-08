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
    startQuiz: Boolean,
    source: String // 1. Receive source parameter
) {
    // 2. Use the local ScreenState enum defined below
    var screenState by remember { mutableStateOf(ScreenState.LANDING) }
    val savedAnswers by questionsViewModel.userAnswers.collectAsState()

    // 3. Use QuestionnaireConstants.questions throughout
    val questions = QuestionnaireConstants.questions

    var quizAnswers by remember { mutableStateOf<List<String?>>(emptyList()) }
    var quizCurrentIndex by remember { mutableIntStateOf(0) }

    var editAnswers by remember { mutableStateOf<List<String?>>(emptyList()) }

    var questionToEditIndex by remember { mutableStateOf<Int?>(null) }

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
                                        // FIX 2: If we forced 'startQuiz', going back from Q1 should exit the screen,
                                        // instead of showing the Landing page they skipped.
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
                        // 2. Conditional Navigation for Edit Mode
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
                            // 3. Conditional Navigation for Quiz Mode
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