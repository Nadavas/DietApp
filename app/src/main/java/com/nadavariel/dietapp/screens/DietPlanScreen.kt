package com.nadavariel.dietapp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Spa
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.model.ExampleMeal
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.AppTopBar
import com.nadavariel.dietapp.viewmodel.DietPlanViewModel
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DietPlanScreen(
    navController: NavController,
    dietPlanViewModel: DietPlanViewModel = viewModel()
) {
    val hasAiGeneratedGoals by dietPlanViewModel.hasAiGeneratedGoals.collectAsState()
    val dietPlan by dietPlanViewModel.currentDietPlan.collectAsState()
    val userWeight by dietPlanViewModel.userWeight.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Nutrition Plan",
                onBack = { navController.popBackStack() }
            )
        },
        containerColor = AppTheme.colors.screenBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            AppTheme.colors.primaryGreen.copy(alpha = 0.15f),
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
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.primaryGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = AppTheme.colors.primaryGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "AI-Personalized Plan",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.darkGreyText
                                )
                                Text(
                                    "Tailored to your profile and goals",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppTheme.colors.lightGreyText
                                )
                            }
                        }
                    }
                }
            }

            // --- Hero Calorie Card ---
            dietPlan?.let { plan ->

                // --- Health Overview ---
                CollapsibleCard(
                    icon = Icons.Default.PersonSearch,
                    iconBackgroundColor = AppTheme.colors.skyBlue.copy(alpha = 0.1f),
                    iconTint = AppTheme.colors.skyBlue,
                    title = "Your Health Overview"
                ) {
                    Spacer(Modifier.height(8.dp))
                    plan.healthOverview.forEach { point ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.skyBlue,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.darkGreyText.copy(alpha = 0.85f),
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.4f)
                            )
                        }
                    }
                }

                // --- Goal Strategy ---
                CollapsibleCard(
                    icon = Icons.Default.Flag,
                    iconBackgroundColor = AppTheme.colors.darkPurple.copy(alpha = 0.1f),
                    iconTint = AppTheme.colors.darkPurple,
                    title = "Your Goal Strategy"
                ) {
                    Spacer(Modifier.height(8.dp))
                    plan.goalStrategy.forEach { point ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.darkPurple,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.darkGreyText.copy(alpha = 0.85f),
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.4f)
                            )
                        }
                    }
                }

                // --- Daily Calorie Target ---
                AnimatedCalorieHeroCard(
                    dailyCalories = plan.concretePlan.targets.dailyCalories
                )

                // --- Macronutrient Breakdown Section ---
                Text(
                    "Daily Macros",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.darkGreyText,
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
                        value = plan.concretePlan.targets.proteinGrams,
                        unit = "g",
                        color = AppTheme.colors.primaryGreen,
                        progress = 0.75f
                    )
                    EnhancedMacroCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Grain,
                        label = "Carbs",
                        value = plan.concretePlan.targets.carbsGrams,
                        unit = "g",
                        color = AppTheme.colors.skyBlue,
                        progress = 0.85f
                    )
                    EnhancedMacroCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WaterDrop,
                        label = "Fat",
                        value = plan.concretePlan.targets.fatGrams,
                        unit = "g",
                        color = AppTheme.colors.tangerine,
                        progress = 0.65f
                    )
                }

                // --- Protein Guide ---
                if (userWeight.toInt() > 0) {
                    ProteinRecommendationCard(userWeight.toInt())
                }

                // --- Meal Guidelines ---
                CollapsibleCard(
                    icon = Icons.AutoMirrored.Filled.Rule,
                    iconBackgroundColor = AppTheme.colors.purple.copy(alpha = 0.1f),
                    iconTint = AppTheme.colors.purple,
                    title = "Meal Guidelines"
                ) {
                    Spacer(Modifier.height(16.dp))

                    // Meal Frequency
                    Text(
                        plan.concretePlan.mealGuidelines.mealFrequency,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.darkGreyText,
                        fontStyle = FontStyle.Italic,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.4f)
                    )
                    Spacer(Modifier.height(16.dp))

                    // Foods to Emphasize
                    Text("Foods to Emphasize:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.primaryGreen)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        plan.concretePlan.mealGuidelines.foodsToEmphasize.forEach { FoodChip(it, true) }
                    }
                    Spacer(Modifier.height(16.dp))

                    // Foods to Limit
                    Text("Foods to Limit:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = AppTheme.colors.deepRed)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        plan.concretePlan.mealGuidelines.foodsToLimit.forEach { FoodChip(it, false) }
                    }
                }

                // --- Example Meal Plan ---
                CollapsibleCard(
                    icon = Icons.Default.Restaurant,
                    iconBackgroundColor = AppTheme.colors.accentTeal.copy(alpha = 0.1f),
                    iconTint = AppTheme.colors.accentTeal,
                    title = "Example Meal Plan"
                ) {
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MealPlanItem("Breakfast", plan.exampleMealPlan.breakfast)
                        HorizontalDivider(color = AppTheme.colors.lightGreyText.copy(alpha = 0.2f))
                        MealPlanItem("Lunch", plan.exampleMealPlan.lunch)
                        HorizontalDivider(color = AppTheme.colors.lightGreyText.copy(alpha = 0.2f))
                        MealPlanItem("Dinner", plan.exampleMealPlan.dinner)
                        HorizontalDivider(color = AppTheme.colors.lightGreyText.copy(alpha = 0.2f))
                        MealPlanItem("Snacks", plan.exampleMealPlan.snacks)
                    }
                }

                // --- Training Advice ---
                CollapsibleCard(
                    icon = Icons.Default.Lightbulb,
                    iconBackgroundColor = AppTheme.colors.orange.copy(alpha = 0.1f),
                    iconTint = AppTheme.colors.orange,
                    title = "Personalized Training Advice"
                ) {
                    Spacer(Modifier.height(8.dp))
                    plan.concretePlan.trainingAdvice.forEach { point ->
                        Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                            Text(
                                "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.orange,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = point,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.darkGreyText.copy(alpha = 0.85f),
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight.times(1.4f)
                            )
                        }
                    }
                }

                // --- Disclaimer ---
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AppTheme.colors.deepRed.copy(alpha = 0.1f)
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
                            tint = AppTheme.colors.softRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            plan.disclaimer,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.darkGreyText.copy(alpha = 0.7f),
                            lineHeight = MaterialTheme.typography.bodySmall.lineHeight.times(1.4f)
                        )
                    }
                }
            }

            // Empty state
            if (dietPlan == null) {
                EmptyDietPlanState(
                    onNavigateToQuestions = {
                        navController.navigate("questions?startQuiz=true&source=account")
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -------------------------------
// --------- COMPOSABLES ---------
// -------------------------------

@Composable
private fun CollapsibleCard(
    icon: ImageVector,
    iconBackgroundColor: Color,
    iconTint: Color,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "arrowRotation"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconBackgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.darkGreyText,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = AppTheme.colors.darkGreyText,
                    modifier = Modifier.rotate(rotationAngle)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    content()
                }
            }
        }
    }
}


@Composable
private fun AnimatedCalorieHeroCard(dailyCalories: Int) {
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
            val circleColor = AppTheme.colors.primaryGreen
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.1f)
            ) {
                val spacing = 60f
                for (i in 0..10) {
                    drawCircle(
                        color = circleColor,
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
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    AppTheme.colors.warmOrange.copy(alpha = 0.3f),
                                    AppTheme.colors.warmOrange.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = AppTheme.colors.tangerine
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Daily Calorie Target",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTheme.colors.lightGreyText,
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
                        color = AppTheme.colors.tangerine
                    )
                    Text(
                        text = " kcal",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppTheme.colors.darkGreyText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EnhancedMacroCard(
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
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(60.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = color.copy(alpha = 0.15f),
                        radius = size.minDimension / 2
                    )
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
                color = AppTheme.colors.lightGreyText
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    "$value",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.darkGreyText
                )
                Text(
                    unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.lightGreyText,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun ProteinRecommendationCard(weightKg: Int) {
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
                        .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Spa,
                        contentDescription = null,
                        tint = AppTheme.colors.primaryGreen,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column {
                    Text(
                        "Protein Guide",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.darkGreyText
                    )
                    Text(
                        "Based on $weightKg kg body weight",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.lightGreyText
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            RecommendationRow(
                label = "Light Activity",
                range = "$nonActiveMin–$nonActiveMax g/day",
                color = AppTheme.colors.goldenYellow
            )
            Spacer(Modifier.height(8.dp))
            RecommendationRow(
                label = "Active Lifestyle",
                range = "$activeMin–$activeMax g/day",
                color = AppTheme.colors.skyBlue
            )
        }
    }
}

@Composable
private fun RecommendationRow(label: String, range: String, color: Color) {
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
            color = AppTheme.colors.darkGreyText
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
private fun EmptyDietPlanState(onNavigateToQuestions: () -> Unit) {
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
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = AppTheme.colors.primaryGreen.copy(alpha = 0.6f)
                )
            }

            Text(
                "No Nutrition Plan Yet",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = AppTheme.colors.darkGreyText
            )

            Text(
                "Complete our quick questionnaire to receive your personalized AI-generated diet plan",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = AppTheme.colors.lightGreyText,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight.times(1.5f)
            )

            Button(
                onClick = onNavigateToQuestions,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.primaryGreen
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

@Composable
private fun MealPlanItem(mealType: String, meal: ExampleMeal) {
    Column {
        Text(
            mealType,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.accentTeal
        )
        Text(
            meal.description,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.darkGreyText
        )
        Text(
            "~${meal.estimatedCalories} kcal",
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.lightGreyText
        )
    }
}

@Composable
private fun FoodChip(text: String, isGood: Boolean) {
    val chipColor = if (isGood) AppTheme.colors.primaryGreen else AppTheme.colors.deepRed
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chipColor.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, color = chipColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
    }
}