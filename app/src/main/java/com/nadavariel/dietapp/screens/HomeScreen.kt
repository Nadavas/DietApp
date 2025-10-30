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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add // Import Add icon
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete // Import Delete icon
import androidx.compose.material.icons.filled.Edit // Import Edit icon
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    foodLogViewModel: FoodLogViewModel,
    goalViewModel: GoalsViewModel = viewModel(),
    navController: NavController,
) {
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val selectedDate by foodLogViewModel.selectedDateState.collectAsState()
    val currentWeekStartDate by foodLogViewModel.currentWeekStartDateState.collectAsState()
    val mealsForSelectedDate by foodLogViewModel.mealsForSelectedDate.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    val weightHistory by foodLogViewModel.weightHistory.collectAsState()
    val targetWeight by foodLogViewModel.targetWeight.collectAsState()

    // State for all dialogs
    var showLogWeightDialog by remember { mutableStateOf(false) }
    var showManageWeightDialog by remember { mutableStateOf(false) }
    var weightEntryToEdit by remember { mutableStateOf<WeightEntry?>(null) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var mealToDelete by remember { mutableStateOf<Meal?>(null) }
    var mealWithActionsShownId by remember { mutableStateOf<String?>(null) }

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

    val groupedMeals = remember(mealsForSelectedDate) {
        mealsForSelectedDate
            .groupBy { meal -> MealSection.getMealSection(meal.timestamp.toDate()) }
            .toSortedMap(compareBy { it.ordinal })
    }

    val screenBackgroundColor = Color(0xFFF7F9FC)

    // --- DIALOGS ---

    if (showLogWeightDialog) {
        LogWeightDialog(
            entryToEdit = weightEntryToEdit, // Pass the entry to edit (if any)
            onDismiss = {
                showLogWeightDialog = false
                weightEntryToEdit = null // Clear edit state on dismiss
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
                weightEntryToEdit = entry // Set the entry to edit
                showManageWeightDialog = false // Close manage dialog
                showLogWeightDialog = true // Open log/edit dialog
            },
            onDelete = { entry ->
                foodLogViewModel.deleteWeightEntry(entry.id)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    HeaderSection(
                        userName = userProfile.name,
                        avatarId = userProfile.avatarId,
                        onAvatarClick = { navController.navigate(NavRoutes.MY_PROFILE) }
                    )
                },
                actions = {
                    // Removed weight icon from here
                    IconButton(
                        onClick = { navController.navigate(NavRoutes.NOTIFICATIONS) }
                    ) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBackgroundColor,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        containerColor = screenBackgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { mealWithActionsShownId = null },
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                WeightProgressGraph(
                    startingWeight = userProfile.startingWeight,
                    targetWeight = targetWeight,
                    history = weightHistory,
                    onAddClick = { // Pass lambda for + button
                        weightEntryToEdit = null // Ensure we are adding, not editing
                        showLogWeightDialog = true
                    },
                    onManageClick = { // Pass lambda for Manage button
                        showManageWeightDialog = true
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

            if (mealsForSelectedDate.isEmpty()) {
                item { EmptyState() }
            } else {
                groupedMeals.forEach { (section, mealsInSection) ->
                    stickyHeader {
                        MealSectionHeader(section, Modifier.background(screenBackgroundColor))
                    }
                    items(mealsInSection, key = { it.id }) { meal ->
                        MealItem(
                            meal = meal,
                            sectionColor = section.color,
                            showActions = mealWithActionsShownId == meal.id,
                            onToggleActions = { clickedMealId ->
                                mealWithActionsShownId = if (mealWithActionsShownId == clickedMealId) null else clickedMealId
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

    if (showDeleteConfirmationDialog && mealToDelete != null) {
        StyledAlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false; mealToDelete = null },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogWeightDialog(
    entryToEdit: WeightEntry?, // Can be null (for adding) or existing (for editing)
    onDismiss: () -> Unit,
    onSave: (weight: Float, date: Calendar) -> Unit, // For new entries
    onUpdate: (id: String, weight: Float, date: Calendar) -> Unit // For updating entries
) {
    val isEditMode = entryToEdit != null
    val title = if (isEditMode) "Edit Weight Entry" else "Log Your Weight"

    // Set initial state based on whether we are editing or adding
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
                // Keep original time for precision if needed, or reset
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
                    label = { Text("Weight (kg)") }, // Simplified label
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

                DateDisplayRow(
                    label = "Date",
                    date = selectedDate.time,
                    formatter = dateFormatter,
                    onClick = { datePickerDialog.show() }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val weight = weightInput.toFloatOrNull()
                    if (weight != null && weight > 0) {
                        if (isEditMode) {
                            onUpdate(entryToEdit.id, weight, selectedDate)
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
private fun DateDisplayRow(
    label: String,
    date: Date,
    formatter: SimpleDateFormat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = formatter.format(date),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun WeightProgressGraph(
    startingWeight: Float,
    targetWeight: Float,
    history: List<WeightEntry>,
    onAddClick: () -> Unit,
    onManageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row with Title and Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Weight Progress",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onAddClick) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Log New Weight",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val currentWeight = history.lastOrNull()?.weight ?: startingWeight

            if (history.isEmpty() && startingWeight <= 0) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Log your weight to see your progress graph!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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

                // "Last Updated" text
                if (history.isNotEmpty() && history.last().timestamp != null) {
                    val lastDate = history.last().timestamp!!.toDate()
                    val formattedDate = remember(lastDate) {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(lastDate)
                    }
                    Text(
                        text = "Last updated: $formattedDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // "Manage Entries" Button
                TextButton(
                    onClick = onManageClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Manage Entries")
                }
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
            color = if (isMain) MaterialTheme.colorScheme.primary else Color.Gray,
            fontWeight = if (isMain) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = weightText,
            style = if (isMain) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            color = if (isMain) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        if (isMain) {
            Text(
                text = "kg",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val targetColor = Color(0xFF4CAF50)
    val grayColor = Color.Gray

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

        val effectiveHistory = if (history.isEmpty()) {
            listOf(WeightEntry(id = "start", weight = startingWeight, timestamp = com.google.firebase.Timestamp.now()))
        } else {
            history
        }

        val minWeight = (allWeights.minOrNull() ?: startingWeight)
        val maxWeight = (allWeights.maxOrNull() ?: startingWeight)

        val verticalPadding = (maxWeight - minWeight) * 0.1f
        val yMin = (minWeight - verticalPadding).coerceAtLeast(0f)
        val yMax = (maxWeight + verticalPadding).coerceAtLeast(yMin + 1f)
        val weightRange = (yMax - yMin)

        val yMid = (yMin + yMax) / 2

        val totalPoints = effectiveHistory.size + 1 // +1 for startingWeight
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

        val points = mutableListOf<Offset>()
        points.add(Offset(getX(0), getY(startingWeight)))
        effectiveHistory.forEachIndexed { index, entry ->
            points.add(Offset(getX(index + 1), getY(entry.weight)))
        }

        drawContext.canvas.nativeCanvas.drawText(
            "Start",
            getX(0),
            size.height - with(density) { 4.dp.toPx() },
            textPaint
        )

        if (effectiveHistory.size > 1) {
            val midIndex = effectiveHistory.size / 2
            val lastIndex = effectiveHistory.size - 1

            effectiveHistory[midIndex].timestamp?.toDate()?.let { midDate ->
                drawContext.canvas.nativeCanvas.drawText(
                    dateFormat.format(midDate),
                    getX(midIndex + 1),
                    size.height - with(density) { 4.dp.toPx() },
                    textPaint
                )
            }

            if (midIndex != lastIndex) {
                effectiveHistory[lastIndex].timestamp?.toDate()?.let { lastDate ->
                    drawContext.canvas.nativeCanvas.drawText(
                        dateFormat.format(lastDate),
                        getX(lastIndex + 1),
                        size.height - with(density) { 4.dp.toPx() },
                        textPaint
                    )
                }
            }
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

/**
 * A dialog to manage (edit/delete) all weight history entries.
 */
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
                                    Icon(Icons.Default.Delete, "Delete Entry", tint = MaterialTheme.colorScheme.error)
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