package com.nadavariel.dietapp.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.viewmodel.QuestionsViewModel

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    navController: NavController,
    questionsViewModel: QuestionsViewModel = viewModel()
) {
    val questions = listOf(
        Question(
            text = "How often do you exercise?",
            options = listOf("Every day", "A few times a week", "Rarely", "Never")
        ),
        Question(
            text = "Do you prefer eating at home or dining out?",
            options = listOf("At home", "Dining out", "Depends on the day")
        )
    )

    // Get state from the viewmodel
    val savedAnswers by questionsViewModel.userAnswers.collectAsState()

    // State variables
    var currentIndex by remember { mutableIntStateOf(0) }
    var answers by remember { mutableStateOf(mutableListOf<String?>().apply { repeat(questions.size) { add(null) } }) }

    val currentQuestion = questions[currentIndex]

    // Load saved answers from the viewmodel
    LaunchedEffect(savedAnswers) {
        if (savedAnswers.isNotEmpty()) {
            val newAnswers = questions.map { question ->
                savedAnswers.find { it.question == question.text }?.answer
            }.toMutableList()
            answers = newAnswers
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Questions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Question and options
            Text(text = currentQuestion.text, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

            currentQuestion.options.forEach { option ->
                val selected = answers.getOrNull(currentIndex) == option
                OutlinedButton(
                    onClick = {
                        answers = answers.toMutableList().also { it[currentIndex] = option }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(option)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Next/submit button
            Button(
                onClick = {
                    if (currentIndex < questions.lastIndex) {
                        currentIndex++
                    } else {
                        // All questions answered – handle submission here
                        questionsViewModel.saveUserAnswers(questions, answers)
                        navController.popBackStack()
                    }
                },
                enabled = answers.getOrNull(currentIndex) != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentIndex < questions.lastIndex) "Next" else "Submit")
            }
        }
    }
}

data class Question(
    val text: String,
    val options: List<String>
)