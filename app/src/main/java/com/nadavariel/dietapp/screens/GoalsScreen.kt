// screens/GoalsScreen.kt
package com.nadavariel.dietapp.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.data.Goal
import com.nadavariel.dietapp.viewmodel.GoalsViewModel

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    navController: NavController,
    goalsViewModel: GoalsViewModel = viewModel()
) {
    // Define your list of goals
    val goals = listOf(
        Goal(
            text = "What's your primary weight goal?",
            options = listOf("Lose weight", "Maintain weight", "Gain weight")
        ),
        Goal(
            text = "How active do you want to be each week?",
            options = listOf(
                "Lightly active (1-2 workouts)",
                "Moderately active (3-4 workouts)",
                "Very active (5+ workouts)"
            )
        ),
        Goal(
            text = "Any specific dietary focus?",
            options = listOf("High protein", "Low carb", "Balanced", "Vegetarian")
        )
        // Add more goals as needed
    )

    var currentIndex by remember { mutableStateOf(0) }
    var selections by remember { mutableStateOf(List<String?>(goals.size) { null }) }

    // Edge case: No goals available
    if (goals.isEmpty()) {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No goals available at the moment.")
            }
        }
        return
    }

    val currentGoal = goals[currentIndex]
    val isLastGoal = currentIndex == goals.lastIndex

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Content
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Goal ${currentIndex + 1} of ${goals.size}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = currentGoal.text,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Options
                for (option in currentGoal.options) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (selections[currentIndex] == option),
                                onClick = {
                                    val newSelections = selections.toMutableList()
                                    newSelections[currentIndex] = option
                                    selections = newSelections
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selections[currentIndex] == option),
                            onClick = null // handled by parent selectable
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = option, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Next / Finish button
            Button(
                onClick = {
                    if (selections[currentIndex] == null) {
                        return@Button
                    }
                    if (!isLastGoal) {
                        currentIndex++
                    } else {
                        goalsViewModel.saveUserGoals(goals, selections)
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                enabled = selections[currentIndex] != null
            ) {
                Text(text = if (isLastGoal) "Finish" else "Next Goal")
            }
        }
    }
}
