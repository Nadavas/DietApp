package com.nadavariel.dietapp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.model.Goal
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import kotlin.math.cos
import kotlin.math.sin

// --- DESIGN TOKENS (matching HomeScreen) ---
private val VibrantGreen = Color(0xFF4CAF50)
private val DarkGreyText = Color(0xFF333333)
private val LightGreyText = Color(0xFF757575)
private val ScreenBackgroundColor = Color(0xFFF7F9FC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    navController: NavController,
    goalsViewModel: GoalsViewModel = viewModel()
) {
    val goals by goalsViewModel.goals.collectAsStateWithLifecycle()
    val userWeight by goalsViewModel.userWeight.collectAsState()
    val hasAiGeneratedGoals by goalsViewModel.hasAiGeneratedGoals.collectAsState()
    val dietPlan by goalsViewModel.currentDietPlan.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Your Nutrition Plan",
                        fontWeight = FontWeight.Bold,
                        color = DarkGreyText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        },
        containerColor = ScreenBackgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI Generated Badge - Enhanced
            AnimatedVisibility(
                visible = hasAiGeneratedGoals && dietPlan != null,
                enter = fadeIn() + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + scaleOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box {
                        // Decorative gradient background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            VibrantGreen.copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Animated sparkle icon container
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(VibrantGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = VibrantGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "AI-Personalized Plan",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkGreyText
                                )
                                Text(
                                    "Tailored to your profile and goals",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LightGreyText
                                )
                            }
                        }
                    }
                }
            }

            // Hero Calorie Card - Stunning redesign
            dietPlan?.let { plan ->
                AnimatedCalorieHeroCard(
                    dailyCalories = plan.dailyCalories
                )

                // Macronutrient Breakdown Section
                Text(
                    "Daily Macros",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreyText,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EnhancedMacroCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FitnessCenter,
                        label = "Protein",
                        value = plan.proteinGrams,
                        unit = "g",
                        color = Color(0xFF2196F3), // Blue
                        progress = 0.75f
                    )
                    EnhancedMacroCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Grain,
                        label = "Carbs",
                        value = plan.carbsGrams,
                        unit = "g",
                        color = Color(0xFFFF9800), // Orange
                        progress = 0.85f
                    )
                    EnhancedMacroCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WaterDrop,
                        label = "Fat",
                        value = plan.fatGrams,
                        unit = "g",
                        color = Color(0xFF9C27B0), // Purple
                        progress = 0.65f
                    )
                }

                // Protein Recommendations Card
                if (userWeight.toInt() > 0) {
                    ProteinRecommendationCard(userWeight.toInt())
                }

                // AI Recommendations - Enhanced design
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF3E0)), // Light orange
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFFF9800),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                "Personalized Tips",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DarkGreyText
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            plan.recommendations,
                            style = MaterialTheme.typography.bodyLarge,
                            color = DarkGreyText.copy(alpha = 0.85f),
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight.times(1.5f)
                        )
                    }
                }

                // Disclaimer - Subtle and informative
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF8E1) // Very light yellow
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFFFA726),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            plan.disclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkGreyText.copy(alpha = 0.7f),
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight.times(1.4f)
                        )
                    }
                }
            }

            // Editable Goals Section
            if (goals.isNotEmpty()) {
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = LightGreyText.copy(alpha = 0.2f)
                )

                Text(
                    "Custom Adjustments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreyText
                )

                Text(
                    "Fine-tune your targets manually",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LightGreyText,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                goals.forEach { goal ->
                    EditableGoalCard(
                        goal = goal,
                        onValueChange = { newValue ->
                            goalsViewModel.updateAnswer(goal.id, newValue)
                        }
                    )
                }

                // Save Button - Enhanced
                Button(
                    onClick = {
                        goalsViewModel.saveUserAnswers()
                        navController.popBackStack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VibrantGreen
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Save Custom Goals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Empty state - Enhanced
            if (dietPlan == null) {
                EmptyDietPlanState(
                    onNavigateToQuestions = { navController.navigate("questions") }
                )
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AnimatedCalorieHeroCard(dailyCalories: Int) {
    val animatedCalories by animateFloatAsState(
        targetValue = dailyCalories.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "calorieAnimation"
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            // Decorative background pattern
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.1f)
            ) {
                val spacing = 60f
                for (i in 0..10) {
                    drawCircle(
                        color = VibrantGreen,
                        radius = 40f,
                        center = Offset(
                            x = size.width * 0.2f + i * spacing,
                            y = size.height * 0.5f + sin(i * 0.5f) * 30f
                        ),
                        alpha = 0.3f
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Flame icon with circular background
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    VibrantGreen.copy(alpha = 0.3f),
                                    VibrantGreen.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = VibrantGreen
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Daily Calorie Target",
                    style = MaterialTheme.typography.titleMedium,
                    color = LightGreyText,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = animatedCalories.toInt().toString(),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = VibrantGreen
                    )
                    Text(
                        text = " kcal",
                        style = MaterialTheme.typography.headlineSmall,
                        color = DarkGreyText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedMacroCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: Int,
    unit: String,
    color: Color,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "progressAnimation"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circular progress indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(60.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background circle
                    drawCircle(
                        color = color.copy(alpha = 0.15f),
                        radius = size.minDimension / 2
                    )
                    // Progress arc
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = LightGreyText
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "$value",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreyText
                )
                Text(
                    unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightGreyText,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun ProteinRecommendationCard(weightKg: Int) {
    val nonActiveMin = (weightKg * 0.8).toInt()
    val nonActiveMax = (weightKg * 1).toInt()
    val activeMin = (weightKg * 1.2).toInt()
    val activeMax = (weightKg * 2).toInt()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE3F2FD)), // Light blue
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MonitorWeight,
                        contentDescription = null,
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        "Protein Guide",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreyText
                    )
                    Text(
                        "Based on $weightKg kg body weight",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LightGreyText
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Recommendation rows
            RecommendationRow(
                label = "Light Activity",
                range = "$nonActiveMin–$nonActiveMax g/day",
                color = Color(0xFF66BB6A)
            )
            Spacer(Modifier.height(8.dp))
            RecommendationRow(
                label = "Active Lifestyle",
                range = "$activeMin–$activeMax g/day",
                color = Color(0xFF42A5F5)
            )
        }
    }
}

@Composable
fun RecommendationRow(label: String, range: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = DarkGreyText
        )
        Text(
            range,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun EditableGoalCard(
    goal: Goal,
    onValueChange: (String) -> Unit
) {
    var textValue by remember(goal.value) { mutableStateOf(goal.value ?: "") }

    val isCalorieGoal = goal.text.contains("calorie", ignoreCase = true)
    val isProteinGoal = goal.text.contains("protein", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = goal.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = DarkGreyText
            )

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
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibrantGreen,
                    focusedLabelColor = VibrantGreen
                ),
                trailingIcon = {
                    if (textValue.isNotBlank()) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = VibrantGreen
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun EmptyDietPlanState(onNavigateToQuestions: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Decorative illustration placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(VibrantGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = VibrantGreen.copy(alpha = 0.6f)
                )
            }

            Text(
                "No Nutrition Plan Yet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = DarkGreyText
            )

            Text(
                "Complete our quick questionnaire to receive your personalized AI-generated diet plan",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = LightGreyText,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight.times(1.5f)
            )

            Button(
                onClick = onNavigateToQuestions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibrantGreen
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    "Start Questionnaire",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}