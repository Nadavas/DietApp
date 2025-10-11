package com.nadavariel.dietapp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.data.Goal
import com.nadavariel.dietapp.viewmodel.GoalsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    navController: NavController,
    goalsViewModel: GoalsViewModel = viewModel()
) {
    val goals by goalsViewModel.goals.collectAsStateWithLifecycle()
    val userWeight by goalsViewModel.userWeight.collectAsState()
    val hasAiGeneratedGoals by goalsViewModel.hasAiGeneratedGoals.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Goals") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Generated Badge
            AnimatedVisibility(
                visible = hasAiGeneratedGoals,
                enter = fadeIn() + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = fadeOut() + scaleOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Column {
                            Text(
                                "AI-Generated Goals",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                "These targets were personalized based on your profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Weight Warning
            if (userWeight.toInt() == 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "To get personalized recommendations, please enter your weight in the profile screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Goals Cards
            goals.forEach { goal ->
                GoalCard(
                    goal = goal,
                    userWeight = userWeight,
                    onValueChange = { newValue ->
                        goalsViewModel.updateAnswer(goal.id, newValue)
                    }
                )
            }

            // Save Button
            Button(
                onClick = {
                    goalsViewModel.saveUserAnswers()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = true
            ) {
                Text("Save Goals")
            }
        }
    }
}

@Composable
fun GoalCard(
    goal: Goal,
    userWeight: Float,
    onValueChange: (String) -> Unit
) {
    var textValue by remember(goal.value) { mutableStateOf(goal.value ?: "") }

    val isCalorieGoal = goal.text.contains("calorie", ignoreCase = true)
    val isProteinGoal = goal.text.contains("protein", ignoreCase = true)

    val icon: ImageVector = when {
        isCalorieGoal -> Icons.Default.LocalFireDepartment
        isProteinGoal -> Icons.Default.FitnessCenter
        else -> Icons.Default.Check
    }

    val gradientColors = when {
        isCalorieGoal -> listOf(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        )
        isProteinGoal -> listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        )
        else -> listOf(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(gradientColors)
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = when {
                        isCalorieGoal -> "Daily Calorie Target"
                        isProteinGoal -> "Daily Protein Target"
                        else -> goal.text
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Recommendations for protein
            if (isProteinGoal && userWeight.toInt() > 0) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val weightKg = userWeight.toInt()
                        val nonActiveMin = (weightKg * 0.8).toInt()
                        val nonActiveMax = (weightKg * 1).toInt()
                        val activeMin = (weightKg * 1.2).toInt()
                        val activeMax = (weightKg * 2).toInt()

                        Text(
                            "Recommended ranges based on your weight:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "• Non-active: $nonActiveMin–$nonActiveMax g/day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            "• Active: $activeMin–$activeMax g/day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Input Field
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    onValueChange(newValue)
                },
                label = {
                    Text(when {
                        isCalorieGoal -> "Target (kcal)"
                        isProteinGoal -> "Target (g)"
                        else -> "Enter your goal"
                    })
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    if (textValue.isNotBlank()) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            // Current Value Display
            if (textValue.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Your Target:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "$textValue ${when {
                                isCalorieGoal -> "kcal/day"
                                isProteinGoal -> "g/day"
                                else -> ""
                            }}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}