package com.nadavariel.dietapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.viewmodel.GoalsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    navController: NavController,
    goalsViewModel: GoalsViewModel = viewModel()
) {
    val goals by goalsViewModel.goals.collectAsStateWithLifecycle()
    val userWeight by goalsViewModel.userWeight.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Set Your Goals") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.weight(1f, fill = true)) {
                val weightKg = userWeight.toInt()

                // If the user's weight is not set, display a prompt
                if (weightKg == 0) {
                    Text(
                        text = "To get a personalized protein recommendation, please enter your weight in the profile screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                goals.forEach { goal ->
                    Text(
                        text = goal.text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    if (goal.text.contains("protein", ignoreCase = true)) {
                        val nonActiveMin = (weightKg * 0.8).toInt()
                        val nonActiveMax = (weightKg * 1).toInt()
                        val activeMin = (weightKg * 1.2).toInt()
                        val activeMax = (weightKg * 2).toInt()

                        Text(
                            text = "Based on your weight, your daily protein target should be approximately:\n" +
                                   "$nonActiveMin–$nonActiveMax g if you're a non-active person.\n" +
                                   "$activeMin–$activeMax g if you're an active person.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    var textValue by remember { mutableStateOf(goal.value ?: "") }
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { newValue ->
                            textValue = newValue
                            goalsViewModel.updateAnswer(goal.id, newValue)
                        },
                        label = { Text("Enter your goal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Button(
                onClick = {
                    goalsViewModel.saveUserAnswers()
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = true
            ) {
                Text("Set Goals")
            }
        }
    }
}