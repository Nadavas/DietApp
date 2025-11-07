package com.nadavariel.dietapp.screens

import android.app.DatePickerDialog
import android.graphics.Paint
import android.os.Build
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
import com.nadavariel.dietapp.model.WeightEntry
import com.nadavariel.dietapp.ui.account.StyledAlertDialog
import com.nadavariel.dietapp.ui.home.*
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Design tokens
private val PrimaryGreen = Color(0xFF00C853)
private val CardBackground = Color.White
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)
private val BackgroundGradient = listOf(Color.White, Color(0xFFF7F9FC))
private val PageBackgroundColor = Color(0xFFF7F9FC) // Solid color for blending

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    foodLogViewModel: FoodLogViewModel,
    goalViewModel: GoalsViewModel = viewModel(),
    navController: NavController,
    openWeightLog: Boolean = false
) {
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val selectedDate by foodLogViewModel.selectedDateState.collectAsState()
    val currentWeekStartDate by foodLogViewModel.currentWeekStartDateState.collectAsState()
    val mealsForSelectedDate by foodLogViewModel.mealsForSelectedDate.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    val weightHistory by foodLogViewModel.weightHistory.collectAsState()
    val targetWeight by foodLogViewModel.targetWeight.collectAsState()

    var showLogWeightDialog by remember { mutableStateOf(false) }
    var showManageWeightDialog by remember { mutableStateOf(false) }
    var weightEntryToEdit by remember { mutableStateOf<WeightEntry?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<Meal?>(null) }
    var mealWithActionsShownId by remember { mutableStateOf<String?>(null) }

    var showWeightGraphDialog by remember { mutableStateOf(false) }

    val totalCaloriesForSelectedDate = remember(mealsForSelectedDate) {
        mealsForSelectedDate.sumOf { it.calories }
    }
    val goalCalories = remember(goals) {
        goals.firstOrNull()?.value?.toIntOrNull() ?: 2000
    }

    val missingGoals = remember(goals) {
        goals.filter { goal ->
            goal.value.isNullOrBlank() || goal.value == "0"
        }.map { goal ->
            when {
                goal.text.contains("calorie", ignoreCase = true) -> "Calorie"
                goal.text.contains("protein", ignoreCase = true) -> "Protein"
                else -> "Goal"
            }
        }.distinct()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            foodLogViewModel.resetDateToTodayIfNeeded()
        }
    }

    LaunchedEffect(openWeightLog) {
        if (openWeightLog) {
            showLogWeightDialog = true
        }
    }

    val groupedMeals = remember(mealsForSelectedDate) {
        mealsForSelectedDate
            .groupBy { meal -> MealSection.getMealSection(meal.timestamp.toDate()) }
            .toSortedMap(compareBy { it.ordinal })
    }

    if (showLogWeightDialog) {
        LogWeightDialog(
            entryToEdit = weightEntryToEdit,
            onDismiss = {
                showLogWeightDialog = false
                weightEntryToEdit = null
            },
            onSave = { weight, date ->
                foodLogViewModel.addWeightEntry(weight, date)
                showLogWeightDialog = false
                weightEntryToEdit = null
            },
            onUpdate = { id, weight, date ->
                foodLogViewModel.updateWeightEntry(id, weight, date)
                showLogWeightDialog = false
                weightEntryToEdit = null
            }
        )
    }

    if (showManageWeightDialog) {
        ManageWeightHistoryDialog(
            history = weightHistory,
            onDismiss = { showManageWeightDialog = false },
            onEdit = { entry ->
                weightEntryToEdit = entry
                showManageWeightDialog = false
                showLogWeightDialog = true
            },
            onDelete = { entry ->
                foodLogViewModel.deleteWeightEntry(entry.id)
            }
        )
    }

    if (showWeightGraphDialog) {
        WeightGraphDialog(
            startingWeight = userProfile.startingWeight,
            targetWeight = targetWeight,
            history = weightHistory,
            onDismiss = { showWeightGraphDialog = false },
            onManageClick = {
                showWeightGraphDialog = false
                showManageWeightDialog = true
            },
            onAddClick = {
                showWeightGraphDialog = false
                weightEntryToEdit = null
                showLogWeightDialog = true
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(BackgroundGradient))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ModernHomeHeader(
                userName = userProfile.name,
                avatarId = userProfile.avatarId,
                onAvatarClick = { navController.navigate(NavRoutes.MY_PROFILE) },
                onNotificationsClick = { navController.navigate(NavRoutes.NOTIFICATIONS) }
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
                if (missingGoals.isNotEmpty()) {
                    item {
                        MissingGoalsWarning(
                            missingGoals = missingGoals,
                            onSetGoalsClick = { navController.navigate(NavRoutes.GOALS) }
                        )
                    }
                }

                item {
                    CompactWeightDateRow(
                        startingWeight = userProfile.startingWeight,
                        currentWeight = weightHistory.lastOrNull()?.weight
                            ?: userProfile.startingWeight,
                        targetWeight = targetWeight,
                        history = weightHistory,
                        onWeightClick = {
                            weightEntryToEdit = null
                            showLogWeightDialog = true
                        },
                        onGraphClick = {
                            showWeightGraphDialog = true
                        }
                    )
                }

                item {
                    DatePickerSection(
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

                item {
                    CalorieSummaryCard(
                        totalCalories = totalCaloriesForSelectedDate,
                        goalCalories = goalCalories
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Meals",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (mealsForSelectedDate.isNotEmpty()) {
                            Text(
                                text = "${mealsForSelectedDate.size} meals",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                if (mealsForSelectedDate.isEmpty()) {
                    item { EmptyState() }
                } else {
                    groupedMeals.forEach { (section, mealsInSection) ->
                        stickyHeader {
                            MealSectionHeader(
                                section,
                                // FIX: Apply a solid background color that matches the page's
                                // gradient, instead of applying a *new* gradient which causes the boxy look.
                                Modifier.background(
                                    PageBackgroundColor
                                )
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
}

@Composable
private fun ModernHomeHeader(
    userName: String,
    avatarId: String?,
    onAvatarClick: () -> Unit,
    onNotificationsClick: () -> Unit
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
            HeaderSection(
                userName = userName,
                avatarId = avatarId,
                onAvatarClick = onAvatarClick
            )
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = TextPrimary
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CompactWeightDateRow(
    startingWeight: Float,
    currentWeight: Float,
    targetWeight: Float,
    history: List<WeightEntry>,
    onWeightClick: () -> Unit,
    onGraphClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Weight Section (Left side)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onWeightClick),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Weight",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (currentWeight > 0) String.format(
                                    "%.1f",
                                    currentWeight
                                ) else "---",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                            Text(
                                text = "kg",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }

                if (targetWeight > 0 && currentWeight > 0) {
                    val diff = currentWeight - targetWeight
                    val diffText = if (diff > 0) "+%.1f" else "%.1f"
                    Text(
                        text = "${String.format(diffText, diff)} kg to goal",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 48.dp, top = 4.dp)
                    )
                } else if (startingWeight > 0) {
                    Text(
                        text = "Start: ${String.format("%.1f", startingWeight)} kg",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 48.dp, top = 4.dp)
                    )
                }
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFE0E0E0))
            )

            // *** MODIFIED: Right side now has header + graph ***
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(onClick = onGraphClick),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Added Header
                Text(
                    text = "Click graph to view progress",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // 2. Graph (with placeholder)
                if (history.isEmpty() && startingWeight <= 0) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .height(60.dp) // Match graph height
                    ) {
                        Text(
                            "Log weight",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            "to see graph",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                } else {
                    // 3. Pass targetWeight to mini graph
                    MiniWeightLineChart(
                        startingWeight = startingWeight,
                        targetWeight = targetWeight, // Pass target
                        history = history,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp) // Fixed height
                            .padding(end = 8.dp)
                    )
                }
            }
        }
    }
}

// *** MODIFIED: Mini graph now has axes and target line ***
@Composable
fun MiniWeightLineChart(
    startingWeight: Float,
    targetWeight: Float,
    history: List<WeightEntry>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val primaryColor = PrimaryGreen
    val grayColor = TextSecondary
    val targetColor = Color(0xFF4CAF50)
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

    // Setup paint for axis labels
    val textPaint = remember(density) {
        Paint().apply {
            color = grayColor.toArgb()
            textAlign = Paint.Align.LEFT
            textSize = with(density) { 10.sp.toPx() } // Small text
        }
    }
    val xTextPaint = remember(density) {
        Paint().apply {
            color = grayColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 10.sp.toPx() } // Small text
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val allWeights = (history.map { it.weight } + startingWeight + targetWeight).filter { it > 0 }
        if (allWeights.isEmpty() && startingWeight <= 0) return@Canvas

        val historyWeights = history.map { it.weight }
        val allPoints = (listOf(startingWeight) + historyWeights).filter { it > 0 }

        if (allPoints.isEmpty()) return@Canvas

        val minWeight = allWeights.minOrNull() ?: 0f
        val maxWeight = allWeights.maxOrNull() ?: 0f

        val verticalPadding = (maxWeight - minWeight) * 0.15f // A bit more padding
        val yMin = (minWeight - verticalPadding).coerceAtLeast(0f)
        val yMax = (maxWeight + verticalPadding).coerceAtLeast(yMin + 1f)
        val weightRange = (yMax - yMin)

        // Define padding for axes
        val yAxisPadding = with(density) { 20.dp.toPx() } // Space for Y labels
        val xAxisPadding = with(density) { 12.dp.toPx() } // Space for X labels
        val graphWidth = size.width - yAxisPadding
        val graphHeight = size.height - xAxisPadding

        val totalPoints = allPoints.size
        val xSpacing = graphWidth / (totalPoints - 1).coerceAtLeast(1)

        fun getY(weight: Float): Float {
            return graphHeight - ((weight - yMin) / weightRange) * graphHeight
        }

        fun getX(index: Int): Float {
            return yAxisPadding + (index * xSpacing)
        }

        // --- Draw Axes and Labels ---
        val yMinLabel = String.format("%.0f", yMin)
        val yMaxLabel = String.format("%.0f", yMax)

        // Y-Axis Labels
        drawContext.canvas.nativeCanvas.drawText(
            yMaxLabel,
            yAxisPadding - with(density) { 4.dp.toPx() }, // Position left of padding
            getY(yMax) + with(density) { 3.dp.toPx() }, // Align with line
            textPaint
        )
        drawContext.canvas.nativeCanvas.drawText(
            yMinLabel,
            yAxisPadding - with(density) { 4.dp.toPx() },
            getY(yMin) - with(density) { 3.dp.toPx() },
            textPaint
        )

        // X-Axis Labels
        drawContext.canvas.nativeCanvas.drawText(
            "Start",
            getX(0),
            size.height - with(density) { 2.dp.toPx() }, // Position below graph
            xTextPaint
        )
        if (allPoints.size > 1) {
            drawContext.canvas.nativeCanvas.drawText(
                "Now",
                getX(allPoints.size - 1),
                size.height - with(density) { 2.dp.toPx() },
                xTextPaint
            )
        }

        // Draw Target Line
        if (targetWeight > 0) {
            val targetY = getY(targetWeight)
            drawLine(
                color = targetColor,
                start = Offset(yAxisPadding, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = with(density) { 1.dp.toPx() },
                pathEffect = pathEffect
            )
        }

        // --- Draw Weight Line Path ---
        val points = allPoints.mapIndexed { index, weight ->
            Offset(getX(index), getY(weight))
        }

        if (points.size == 1) {
            drawCircle(
                color = primaryColor,
                radius = with(density) { 2.dp.toPx() },
                center = points.first()
            )
        } else {
            val path = Path()
            path.moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { path.lineTo(it.x, it.y) }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(
                    width = with(density) { 2.5.dp.toPx() },
                    cap = StrokeCap.Round
                )
            )
        }
    }
}


@Composable
fun WeightGraphDialog(
    startingWeight: Float,
    targetWeight: Float,
    history: List<WeightEntry>,
    onDismiss: () -> Unit,
    onManageClick: () -> Unit,
    onAddClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            WeightProgressDialogContent(
                startingWeight = startingWeight,
                targetWeight = targetWeight,
                history = history,
                onAddClick = onAddClick,
                onManageClick = onManageClick
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun WeightProgressDialogContent(
    startingWeight: Float,
    targetWeight: Float,
    history: List<WeightEntry>,
    onAddClick: () -> Unit,
    onManageClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Weight Progress",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            IconButton(onClick = onAddClick) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Log New Weight",
                    tint = PrimaryGreen
                )
            }
        }

        val currentWeight = history.lastOrNull()?.weight ?: startingWeight

        if (history.isEmpty() && startingWeight <= 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Log your weight to see your progress graph!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                WeightStat(label = "Start", weight = startingWeight)
                WeightStat(label = "Current", weight = currentWeight, isMain = true)
                WeightStat(label = "Target", weight = targetWeight)
            }

            Spacer(modifier = Modifier.height(16.dp))

            WeightLineChart(
                startingWeight = startingWeight,
                targetWeight = targetWeight,
                history = history,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
            )

            if (history.isNotEmpty() && history.last().timestamp != null) {
                val lastDate = history.last().timestamp!!.toDate()
                val formattedDate = remember(lastDate) {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(lastDate)
                }
                Text(
                    text = "Last updated: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            TextButton(
                onClick = onManageClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Manage Entries", color = PrimaryGreen)
            }
        }
    }
}


@Composable
fun WeightStat(label: String, weight: Float, isMain: Boolean = false) {
    val weightText = if (weight > 0) String.format("%.1f", weight) else "---"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = if (isMain) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (isMain) PrimaryGreen else TextSecondary,
            fontWeight = if (isMain) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = weightText,
            style = if (isMain) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            color = if (isMain) PrimaryGreen else TextPrimary,
            fontWeight = FontWeight.Bold
        )
        if (isMain) {
            Text(
                text = "kg",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun WeightLineChart(
    startingWeight: Float,
    targetWeight: Float,
    history: List<WeightEntry>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    val primaryColor = PrimaryGreen
    val targetColor = Color(0xFF4CAF50)
    val grayColor = TextSecondary

    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }

    val textPaint = remember(density) {
        Paint().apply {
            color = grayColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 12.sp.toPx() }
        }
    }
    val yAxisTextPaint = remember(density) {
        Paint().apply {
            color = grayColor.toArgb()
            textAlign = Paint.Align.RIGHT
            textSize = with(density) { 12.sp.toPx() }
        }
    }

    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        val yAxisPadding = with(density) { 30.dp.toPx() }
        val xAxisPadding = with(density) { 20.dp.toPx() }
        val graphWidth = size.width - yAxisPadding
        val graphHeight = size.height - xAxisPadding

        val allWeights = (history.map { it.weight } + startingWeight + targetWeight).filter { it > 0 }
        if (allWeights.isEmpty() && startingWeight <= 0) return@Canvas

        val historyPoints = history.map { it.weight to (it.timestamp?.toDate() ?: Date()) }
        val startDate = historyPoints.firstOrNull()?.second ?: Date()
        val allPointsWithStart = (listOf(startingWeight to startDate) + historyPoints).filter { it.first > 0 }

        if (allPointsWithStart.isEmpty()) return@Canvas

        val minWeight = allWeights.minOrNull() ?: 0f
        val maxWeight = allWeights.maxOrNull() ?: 0f

        val verticalPadding = (maxWeight - minWeight) * 0.1f
        val yMin = (minWeight - verticalPadding).coerceAtLeast(0f)
        val yMax = (maxWeight + verticalPadding).coerceAtLeast(yMin + 1f)
        val weightRange = (yMax - yMin)

        val yMid = (yMin + yMax) / 2

        val totalPoints = allPointsWithStart.size
        val xSpacing = graphWidth / (totalPoints - 1).coerceAtLeast(1)

        fun getY(weight: Float): Float {
            return graphHeight - ((weight - yMin) / weightRange) * graphHeight
        }

        fun getX(index: Int): Float {
            return yAxisPadding + (index * xSpacing)
        }

        drawContext.canvas.nativeCanvas.drawText(
            String.format("%.1f", yMax),
            yAxisPadding - with(density) { 4.dp.toPx() },
            getY(yMax) + with(density) { 4.dp.toPx() },
            yAxisTextPaint
        )
        if (yMid > yMin && (yMax - yMid) > 1f) {
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.1f", yMid),
                yAxisPadding - with(density) { 4.dp.toPx() },
                getY(yMid) + with(density) { 4.dp.toPx() },
                yAxisTextPaint
            )
        }
        drawContext.canvas.nativeCanvas.drawText(
            String.format("%.1f", yMin),
            yAxisPadding - with(density) { 4.dp.toPx() },
            getY(yMin) - with(density) { 4.dp.toPx() },
            yAxisTextPaint
        )

        val points = allPointsWithStart.mapIndexed { index, (weight, _) ->
            Offset(getX(index), getY(weight))
        }

        drawContext.canvas.nativeCanvas.drawText(
            "Start",
            getX(0),
            size.height - with(density) { 4.dp.toPx() },
            textPaint
        )

        if (allPointsWithStart.size > 2) {
            val midIndex = allPointsWithStart.size / 2
            val lastIndex = allPointsWithStart.size - 1

            val midDate = allPointsWithStart[midIndex].second
            drawContext.canvas.nativeCanvas.drawText(
                dateFormat.format(midDate),
                getX(midIndex),
                size.height - with(density) { 4.dp.toPx() },
                textPaint
            )

            if (midIndex != lastIndex) {
                val lastDate = allPointsWithStart[lastIndex].second
                drawContext.canvas.nativeCanvas.drawText(
                    dateFormat.format(lastDate),
                    getX(lastIndex),
                    size.height - with(density) { 4.dp.toPx() },
                    textPaint
                )
            }
        } else if (allPointsWithStart.size == 2) {
            val lastDate = allPointsWithStart[1].second
            drawContext.canvas.nativeCanvas.drawText(
                dateFormat.format(lastDate),
                getX(1),
                size.height - with(density) { 4.dp.toPx() },
                textPaint
            )
        }

        if (targetWeight > 0) {
            val targetY = getY(targetWeight)
            drawLine(
                color = targetColor,
                start = Offset(yAxisPadding, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = with(density) { 1.dp.toPx() },
                pathEffect = pathEffect
            )
        }

        if (points.size == 1) {
            drawCircle(
                color = primaryColor,
                radius = with(density) { 4.dp.toPx() },
                center = points.first()
            )
            drawCircle(
                color = Color.White,
                radius = with(density) { 2.dp.toPx() },
                center = points.first()
            )
        } else {
            val path = Path()
            path.moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { path.lineTo(it.x, it.y) }

            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(
                    width = with(density) { 3.dp.toPx() },
                    cap = StrokeCap.Round
                )
            )

            points.forEach { point ->
                drawCircle(
                    color = primaryColor,
                    radius = with(density) { 4.dp.toPx() },
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = with(density) { 2.dp.toPx() },
                    center = point
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWeightDialog(
    entryToEdit: WeightEntry?,
    onDismiss: () -> Unit,
    onSave: (weight: Float, date: Calendar) -> Unit,
    onUpdate: (id: String, weight: Float, date: Calendar) -> Unit
) {
    val isEditMode = entryToEdit != null
    val title = if (isEditMode) "Edit Weight Entry" else "Log Your Weight"

    var weightInput by remember {
        mutableStateOf(entryToEdit?.weight?.toString() ?: "")
    }
    var selectedDate by remember {
        mutableStateOf(
            entryToEdit?.timestamp?.toDate()?.let {
                Calendar.getInstance().apply { time = it }
            } ?: Calendar.getInstance()
        )
    }
    var isError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            selectedDate = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
        },
        selectedDate.get(Calendar.YEAR),
        selectedDate.get(Calendar.MONTH),
        selectedDate.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.maxDate = System.currentTimeMillis()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Enter your weight and the date you weighed in.")
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = {
                        isError = false
                        weightInput = it
                    },
                    label = { Text("Weight (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text("Please enter a valid weight.")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { datePickerDialog.show() })
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = dateFormatter.format(selectedDate.time),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightInput.toFloatOrNull()
                    if (weight != null && weight > 0) {
                        if (isEditMode) {
                            if (entryToEdit != null) {
                                onUpdate(entryToEdit.id, weight, selectedDate)
                            }
                        } else {
                            onSave(weight, selectedDate)
                        }
                    } else {
                        isError = true
                    }
                }
            ) {
                Text(if (isEditMode) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ManageWeightHistoryDialog(
    history: List<WeightEntry>,
    onDismiss: () -> Unit,
    onEdit: (WeightEntry) -> Unit,
    onDelete: (WeightEntry) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Weight Entries") },
        text = {
            if (history.isEmpty()) {
                Text("No weight history to manage.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(history.reversed()) { entry -> // Show most recent first
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${entry.weight} kg",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = entry.timestamp?.toDate()?.let {
                                        dateFormatter.format(it)
                                    } ?: "No date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Row {
                                IconButton(onClick = { onEdit(entry) }) {
                                    Icon(Icons.Default.Edit, "Edit Entry")
                                }
                                IconButton(onClick = { onDelete(entry) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        "Delete Entry",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}