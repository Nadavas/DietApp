package com.nadavariel.dietapp.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.model.MealSection
import com.nadavariel.dietapp.model.NutrientDef
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.ui.components.StyledAlertDialog
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.components.UserAvatar
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@SuppressLint("DefaultLocale")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    foodLogViewModel: FoodLogViewModel,
    goalViewModel: GoalsViewModel = viewModel(),
    navController: NavController,
    isGeneratingPlan: Boolean = false
) {
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val isLoadingProfile by authViewModel.isLoadingProfile.collectAsStateWithLifecycle()
    val hasDismissedPlanTip by authViewModel.hasDismissedPlanTip.collectAsStateWithLifecycle()
    val isPreferencesLoaded by authViewModel.isPreferencesLoaded.collectAsStateWithLifecycle()
    val selectedDate by foodLogViewModel.selectedDateState.collectAsState()
    val currentWeekStartDate by foodLogViewModel.currentWeekStartDateState.collectAsState()
    val mealsForSelectedDate by foodLogViewModel.mealsForSelectedDate.collectAsState()
    val weightHistory by foodLogViewModel.weightHistory.collectAsState()
    val isLoadingLogs by foodLogViewModel.isLoadingLogs.collectAsStateWithLifecycle()
    val targetWeight by foodLogViewModel.targetWeight.collectAsState()
    val isTargetWeightLoaded by foodLogViewModel.isTargetWeightLoaded.collectAsState()
    val goals by goalViewModel.goals.collectAsState()
    val dietPlan by goalViewModel.currentDietPlan.collectAsStateWithLifecycle()
    val isLoadingPlan by goalViewModel.isLoadingPlan.collectAsStateWithLifecycle()

    val isScreenLoading = isLoadingProfile || isLoadingLogs || isLoadingPlan || !isPreferencesLoaded || !isTargetWeightLoaded // FIX: Simply wait until the VM says target weight is checked.

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<Meal?>(null) }
    var mealWithActionsShownId by remember { mutableStateOf<String?>(null) }
    var showDailyTotalsDialog by remember { mutableStateOf(false) }

    val totalCaloriesForSelectedDate = remember(mealsForSelectedDate) {
        mealsForSelectedDate.sumOf { it.calories }
    }
    val goalCalories = remember(goals) {
        goals.firstOrNull()?.value?.toIntOrNull() ?: 0
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            foodLogViewModel.resetDateToTodayIfNeeded()
        }
    }

    val groupedMeals = remember(mealsForSelectedDate) {
        mealsForSelectedDate
            .groupBy { meal -> MealSection.getMealSection(meal.timestamp.toDate()) }
            .toSortedMap(compareBy { it.ordinal })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(AppTheme.colors.homeGradient))
    ) {
        if (isScreenLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppTheme.colors.primaryGreen)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                ModernHomeHeader(
                    userName = userProfile.name,
                    avatarId = userProfile.avatarId,
                    onAvatarClick = { navController.navigate(NavRoutes.MY_PROFILE) }
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { mealWithActionsShownId = null },
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // --- DIET PLAN NAVIGATION ALERT SECTION ---
                    if (dietPlan != null && !hasDismissedPlanTip) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.1f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_diet_plan),
                                            contentDescription = null,
                                            tint = AppTheme.colors.primaryGreen,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Your Plan is Ready!",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AppTheme.colors.darkGreyText
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "You can access your personalized diet plan anytime in the Account tab.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppTheme.colors.textSecondary
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { authViewModel.dismissPlanTip() }) {
                                            Text("Got it", color = AppTheme.colors.textSecondary)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                authViewModel.dismissPlanTip()
                                                navController.navigate(NavRoutes.DIET_PLAN)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AppTheme.colors.primaryGreen
                                            )
                                        ) {
                                            Text("View Plan")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- UNANSWERED QUIZ ALERT SECTION ---
                    if (dietPlan == null && !isGeneratingPlan) {
                        item {
                            Card(
                                onClick = {
                                    navController.navigate("${NavRoutes.QUESTIONS}?startQuiz=true&source=home")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = AppTheme.colors.warmOrange.copy(alpha = 0.15f)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(AppTheme.colors.warmOrange.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = AppTheme.colors.warmOrange,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Action Required",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AppTheme.colors.textPrimary
                                        )
                                        Text(
                                            text = "Complete your questionnaire to get your personalized plan.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AppTheme.colors.textSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.ChevronRight,
                                        contentDescription = "Go",
                                        tint = AppTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    // --- SECTION 1: WEIGHT ---
                    item {
                        val currentWeight = weightHistory.lastOrNull()?.weight ?: userProfile.startingWeight
                        val startWeight = userProfile.startingWeight


                        WeightStatusCard(
                            currentWeight = currentWeight,
                            startWeight = startWeight,
                            targetWeight = targetWeight,
                            onClick = { navController.navigate(NavRoutes.WEIGHT_TRACKER) }
                        )
                    }

                    // --- SECTION 2: CALORIES ---
                    item {
                        CardSectionHeader("Calorie Tracker")
                    }

                    item {
                        CalorieSummaryCard(
                            totalCalories = totalCaloriesForSelectedDate,
                            goalCalories = goalCalories,
                            currentWeekStartDate = currentWeekStartDate,
                            selectedDate = selectedDate,
                            onPreviousWeek = { foodLogViewModel.previousWeek() },
                            onNextWeek = { foodLogViewModel.nextWeek() },
                            onDateSelected = { date ->
                                foodLogViewModel.selectDate(date)
                                mealWithActionsShownId = null
                            },
                            onGoToToday = { foodLogViewModel.goToToday() }
                        )
                    }

                    // --- SECTION 3: MEALS ---
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left side: Title + Summary Button
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Today's Meals",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTheme.colors.textPrimary
                                )

                                // Only show button if there are meals
                                if (mealsForSelectedDate.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { showDailyTotalsDialog = true },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Assessment,
                                            contentDescription = "Calculate Daily Totals",
                                            tint = AppTheme.colors.primaryGreen
                                        )
                                    }
                                }
                            }

                            // Right side: Meal count
                            if (mealsForSelectedDate.isNotEmpty()) {
                                Text(
                                    text = "${mealsForSelectedDate.size} meals",
                                    fontSize = 14.sp,
                                    color = AppTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    if (mealsForSelectedDate.isEmpty()) {
                        item { HomeEmptyState() }
                    } else {
                        groupedMeals.forEach { (section, mealsInSection) ->
                            stickyHeader {
                                MealSectionHeader(
                                    section,
                                    Modifier.background(AppTheme.colors.screenBackground)
                                )
                            }
                            items(mealsInSection, key = { it.id }) { meal ->
                                MealItem(
                                    meal = meal,
                                    sectionColor = section.color,
                                    showActions = mealWithActionsShownId == meal.id,
                                    onToggleActions = { clickedMealId ->
                                        mealWithActionsShownId =
                                            if (mealWithActionsShownId == clickedMealId) null else clickedMealId
                                    },
                                    onDelete = {
                                        mealToDelete = it
                                        showDeleteConfirmationDialog = true
                                        mealWithActionsShownId = null
                                    },
                                    onEdit = {
                                        navController.navigate("${NavRoutes.ADD_EDIT_MEAL}/${it.id}")
                                        mealWithActionsShownId = null
                                    }
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showDeleteConfirmationDialog && mealToDelete != null) {
        StyledAlertDialog(
            onDismissRequest = {
                showDeleteConfirmationDialog = false; mealToDelete = null
            },
            title = "Confirm Deletion",
            text = "Are you sure you want to delete this meal: ${mealToDelete?.foodName}?",
            confirmButtonText = "Delete",
            dismissButtonText = "Cancel",
            onConfirm = {
                mealToDelete?.let { foodLogViewModel.deleteMeal(it.id) }
                showDeleteConfirmationDialog = false
                mealToDelete = null
            }
        )
    }

    // Daily Nutrition Summary Dialog
    if (showDailyTotalsDialog) {

        // Macros (Specific Colors)
        val macroList = listOf(
            NutrientDef("Protein", "g", AppTheme.colors.primaryGreen) { it.protein },
            NutrientDef("Carbohydrates", "g", AppTheme.colors.activeLifestyle) { it.carbohydrates },
            NutrientDef("Fat", "g", AppTheme.colors.disclaimerIcon) { it.fat }
        )

        // Micros (Generic Color)
        val microList = listOf(
            NutrientDef("Fiber", "g", AppTheme.colors.textPrimary) { it.fiber },
            NutrientDef("Sugar", "g", AppTheme.colors.textPrimary) { it.sugar },
            NutrientDef("Sodium", "mg", AppTheme.colors.textPrimary) { it.sodium },
            NutrientDef("Potassium", "mg", AppTheme.colors.textPrimary) { it.potassium },
            NutrientDef("Calcium", "mg", AppTheme.colors.textPrimary) { it.calcium },
            NutrientDef("Iron", "mg", AppTheme.colors.textPrimary) { it.iron },
            NutrientDef("Vitamin C", "mg", AppTheme.colors.textPrimary) { it.vitaminC },
            NutrientDef("Vitamin A", "mcg", AppTheme.colors.textPrimary) { it.vitaminA },
            NutrientDef("Vitamin B12", "mcg", AppTheme.colors.textPrimary) { it.vitaminB12 }
        )

        AlertDialog(
            onDismissRequest = { showDailyTotalsDialog = false },
            title = {
                Text("Daily Nutrition Summary", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Loop for Macros
                    macroList.forEach { nutrient ->
                        val total = mealsForSelectedDate.sumOf { nutrient.selector(it) ?: 0.0 }
                        NutritionRow(nutrient.label, total, nutrient.unit, nutrient.color)
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = AppTheme.colors.textSecondary.copy(alpha = 0.2f)
                    )

                    // Loop for Micros
                    microList.forEach { nutrient ->
                        val total = mealsForSelectedDate.sumOf { nutrient.selector(it) ?: 0.0 }
                        NutritionRow(nutrient.label, total, nutrient.unit, nutrient.color)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDailyTotalsDialog = false }) {
                    Text("Close", color = AppTheme.colors.primaryGreen)
                }
            },
            containerColor = Color.White
        )
    }
}

// -------------------------------
// --------- COMPOSABLES ---------
// -------------------------------

@SuppressLint("DefaultLocale")
@Composable
private fun NutritionRow(label: String, value: Double, unit: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label:", color = AppTheme.colors.textPrimary)
        Text(
            text = String.format("%.1f %s", value, unit),
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun CardSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.textPrimary,
        modifier = modifier.padding(start = 4.dp)
    )
}

@Composable
private fun WeightStatusCard(
    currentWeight: Float,
    startWeight: Float,
    targetWeight: Float,
    onClick: () -> Unit
) {

    val isDataReady = targetWeight > 0f && startWeight > 0f
    val totalGoalDiff = abs(targetWeight - startWeight)
    val isWeightLossGoal = if (isDataReady) targetWeight < startWeight else true

    val progressAmount = if (isWeightLossGoal) {
        startWeight - currentWeight
    } else {
        currentWeight - startWeight
    }

    // Clamp percentage between 0-100
    val progressPercentage = if (totalGoalDiff > 0) {
        (progressAmount / totalGoalDiff * 100f).coerceIn(0f, 100f)
    } else 0f

    val rawDiff = currentWeight - startWeight

    val progressText = if (isDataReady) {
        when {
            rawDiff > 0 -> "gained"
            rawDiff < 0 -> "lost"
            else -> if (isWeightLossGoal) "lost" else "gained"
        }
    } else {
        ""
    }

    val formattedProgress = "%.1f".format(abs(rawDiff))

    val isSetback = if (isDataReady) {
        if (isWeightLossGoal) rawDiff > 0 else rawDiff < 0
    } else false

    val badge = when {
        progressPercentage >= 100f -> "🏆"
        progressPercentage >= 75f -> "🥇"
        progressPercentage >= 50f -> "🥈"
        progressPercentage >= 25f -> "🥉"
        progressPercentage >= 10f -> "⚡" // Added "The Spark"
        else -> null
    }

    val badgeTitle = when {
        progressPercentage >= 100f -> "Champion"
        progressPercentage >= 75f -> "Almost There"
        progressPercentage >= 50f -> "Halfway"
        progressPercentage >= 25f -> "First Steps"
        progressPercentage >= 10f -> "The Spark"
        else -> null
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "WEIGHT PROGRESS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${progressPercentage.toInt()}%",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSetback) AppTheme.colors.warmOrange else AppTheme.colors.primaryGreen
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "complete",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isDataReady) {
                        if (isSetback) "⚠️ $formattedProgress kg $progressText" else "$formattedProgress kg $progressText"
                    } else {
                        if (targetWeight <= 0f) "No goal set" else "Loading..."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSetback) AppTheme.colors.warmOrange else AppTheme.colors.textSecondary,
                    fontWeight = if (isSetback) FontWeight.Medium else FontWeight.Normal
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (badge != null && badgeTitle != null) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    listOf(
                                        AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                                        AppTheme.colors.primaryGreen.copy(alpha = 0.05f)
                                    )
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badge,
                            fontSize = 28.sp
                        )
                    }
                    Text(
                        text = badgeTitle,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.primaryGreen,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "View details",
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernHomeHeader(
    userName: String,
    avatarId: String?,
    onAvatarClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HomeHeaderSection(
                userName = userName,
                avatarId = avatarId,
                onAvatarClick = onAvatarClick
            )
        }
    }
}

@Composable
private fun HomeHeaderSection(userName: String, avatarId: String?, onAvatarClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
                fontSize = 14.sp
            )
            Text(
                text = userName.ifBlank { "User" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                fontSize = 24.sp
            )
        }

        Spacer(Modifier.width(16.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp)
                .clickable(onClick = onAvatarClick)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f), CircleShape)
            )
            UserAvatar(
                avatarId = avatarId,
                size = 60.dp,
                modifier = Modifier.clickable(onClick = onAvatarClick)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalorieSummaryCard(
    totalCalories: Int,
    goalCalories: Int,
    currentWeekStartDate: LocalDate,
    selectedDate: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onGoToToday: () -> Unit
) {
    val circleColor = AppTheme.colors.primaryGreen
    val remaining = max(0, goalCalories - totalCalories)
    val progress = if (goalCalories > 0) (totalCalories.toFloat() / goalCalories.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "progressAnimation",
        animationSpec = spring(stiffness = 50f)
    )

    var showDatePicker by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(110.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Color(0xFFF0F0F0),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 24f, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = circleColor,
                            startAngle = -90f,
                            sweepAngle = 360 * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = 24f, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$remaining",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = AppTheme.colors.textPrimary,
                            fontSize = 32.sp
                        )
                        Text(
                            text = "left",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CalorieStatRow(
                        label = "Consumed",
                        value = totalCalories,
                        color = AppTheme.colors.primaryGreen
                    )
                    HorizontalDivider(color = AppTheme.colors.divider)
                    CalorieStatRow(
                        label = "Goal",
                        value = goalCalories,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                color = AppTheme.colors.divider.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val weekDays = remember(currentWeekStartDate) {
                (0..6).map { currentWeekStartDate.plusDays(it.toLong()) }
            }
            val today = LocalDate.now()
            val isTodayVisible = weekDays.any { it.isEqual(today) }

            // Month/Year navigation Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPreviousWeek,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        "Previous Week",
                        tint = AppTheme.colors.primaryGreen
                    )
                }

                // Clickable Month/Year Text
                val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedDate.format(formatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                }

                IconButton(
                    onClick = onNextWeek,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        "Next Week",
                        tint = AppTheme.colors.primaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Week days Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { date ->
                    DayOfWeekItem(
                        date = date,
                        isSelected = date.isEqual(selectedDate),
                        isToday = date.isEqual(today),
                        onClick = { onDateSelected(date) }
                    )
                }
            }

            // "Go to Today" button
            if (!isTodayVisible || !selectedDate.isEqual(today)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = onGoToToday,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(
                            "Go to Today",
                            color = AppTheme.colors.primaryGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // --- Date Picker Dialog ---
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onDateSelected(newDate)
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.primaryGreen)
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.textSecondary)
                ) {
                    Text("Cancel")
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = Color.White,
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = AppTheme.colors.primaryGreen,
                    selectedDayContentColor = Color.White,
                    todayDateBorderColor = AppTheme.colors.primaryGreen,
                    todayContentColor = AppTheme.colors.primaryGreen
                )
            )
        }
    }
}

@Composable
private fun CalorieStatRow(label: String, value: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
            Text(
                text = "kcal",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DayOfWeekItem(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)

    val backgroundColor = when {
        isSelected -> AppTheme.colors.primaryGreen
        else -> Color.Transparent
    }
    val contentColor = when {
        isSelected -> Color.White
        else -> AppTheme.colors.textPrimary
    }

    val borderModifier = if (isToday && !isSelected) {
        Modifier.border(2.dp, AppTheme.colors.primaryGreen.copy(alpha = 0.5f), shape)
    } else Modifier

    Column(
        modifier = Modifier
            .width(44.dp) // Narrow enough to fit 7 days
            .height(60.dp) // Tall enough to fit text
            .clip(shape)
            .background(backgroundColor)
            .then(borderModifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) contentColor else AppTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}

@Composable
private fun MealSectionHeader(section: MealSection, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(section.color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = section.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary,
            fontSize = 18.sp
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MealItem(
    meal: Meal,
    sectionColor: Color,
    showActions: Boolean,
    onToggleActions: (String) -> Unit,
    onDelete: (Meal) -> Unit,
    onEdit: (Meal) -> Unit
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
            .clickable {
                onToggleActions(meal.id)
                coroutineScope.launch {
                    delay(250)
                    bringIntoViewRequester.bringIntoView()
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Meal info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = meal.foodName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        fontSize = 16.sp
                    )

                    val servingInfo = if (!meal.servingAmount.isNullOrBlank() && !meal.servingUnit.isNullOrBlank()) {
                        "${meal.servingAmount} ${meal.servingUnit}"
                    } else ""

                    if (servingInfo.isNotBlank()) {
                        Text(
                            text = servingInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Calories or Actions
                AnimatedContent(
                    targetState = showActions,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.8f)).togetherWith(
                            fadeOut() + scaleOut(targetScale = 0.8f)
                        )
                    },
                    label = "actions_calories_switch"
                ) { targetState ->
                    if (targetState) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFF3F4F6))
                        ) {
                            IconButton(onClick = { onEdit(meal) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    "Edit Meal",
                                    tint = AppTheme.colors.primaryGreen
                                )
                            }
                            IconButton(onClick = { onDelete(meal) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    "Delete Meal",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${meal.calories}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = sectionColor,
                                fontSize = 20.sp
                            )
                            Text(
                                text = "kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Expanded nutrition details
            AnimatedVisibility(
                visible = showActions,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = AppTheme.colors.divider
                    )
                    NutritionDetailsTable(meal)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NutritionDetailsTable(meal: Meal) {
    var microNutrientsVisible by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester)
    ) {
        Text(
            text = "Macronutrients",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.padding(bottom = 12.dp),
            fontSize = 13.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NutritionDetailItem(
                label = "Protein",
                value = meal.protein,
                unit = "g",
                color = AppTheme.colors.primaryGreen,
                modifier = Modifier.weight(1f)
            )
            NutritionDetailItem(
                label = "Carbs",
                value = meal.carbohydrates,
                unit = "g",
                color = Color(0xFF00BFA5),
                modifier = Modifier.weight(1f)
            )
            NutritionDetailItem(
                label = "Fat",
                value = meal.fat,
                unit = "g",
                color = Color(0xFFFF6E40),
                modifier = Modifier.weight(1f)
            )
        }

        // Expandable micronutrients
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .clickable(
                    onClick = {
                        microNutrientsVisible = !microNutrientsVisible
                        if (microNutrientsVisible) {
                            coroutineScope.launch {
                                delay(250)
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AppTheme.colors.divider
            )
            Icon(
                imageVector = if (microNutrientsVisible) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (microNutrientsVisible) "Hide" else "Show More",
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(horizontal = 8.dp).size(20.dp)
            )
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = AppTheme.colors.divider
            )
        }

        AnimatedVisibility(
            visible = microNutrientsVisible,
            enter = expandVertically(expandFrom = Alignment.Top),
            exit = shrinkVertically(shrinkTowards = Alignment.Top)
        ) {
            Column {
                Text(
                    text = "Micronutrients & Fiber",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = 12.dp),
                    fontSize = 13.sp
                )

                // 3 Rows of 3 items logic
                val microNutrients = listOf(
                    Triple("Fiber", meal.fiber, "g"),
                    Triple("Sugar", meal.sugar, "g"),
                    Triple("Sodium", meal.sodium, "mg"),
                    Triple("Potassium", meal.potassium, "mg"),
                    Triple("Calcium", meal.calcium, "mg"),
                    Triple("Iron", meal.iron, "mg"),
                    Triple("Vitamin C", meal.vitaminC, "mg"),
                    Triple("Vitamin A", meal.vitaminA, "mcg"),
                    Triple("Vitamin B12", meal.vitaminB12, "mcg")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    microNutrients.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp), // Reduced vertical padding
                            // spacedBy(0.dp) ensures we use the full width via weights, effectively minimizing padding between columns
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            rowItems.forEach { (label, value, unit) ->
                                NutritionDetailItem(
                                    label = label,
                                    value = value,
                                    unit = unit,
                                    // weight(1f) ensures every column is exactly 1/3 of the width, creating perfect alignment
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun NutritionDetailItem(
    label: String,
    value: Double?,
    unit: String,
    color: Color = AppTheme.colors.textPrimary,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = AppTheme.colors.textSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Visible
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value?.let { "${String.format("%.1f", it)} $unit" } ?: "–",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Visible
        )
    }
}

@Composable
private fun HomeEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Restaurant,
            contentDescription = "No meals",
            modifier = Modifier.size(80.dp),
            tint = AppTheme.colors.textSecondary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "No meals logged yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary,
            fontSize = 20.sp
        )
    }
}