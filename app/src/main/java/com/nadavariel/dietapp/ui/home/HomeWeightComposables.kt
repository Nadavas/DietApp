package com.nadavariel.dietapp.ui.home

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Paint
import android.os.Build
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import com.nadavariel.dietapp.model.WeightEntry
import com.nadavariel.dietapp.ui.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableWeightCard(
    startingWeight: Float,
    currentWeight: Float,
    targetWeight: Float,
    history: List<WeightEntry>,
    onAddClick: () -> Unit,
    onManageClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { expanded = !expanded } // Click the whole card to toggle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring()) // Correctly animates the Column
        ) {
            // This Box provides the correct padding for the collapsed state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 24.dp, bottom = 24.dp)
            ) {
                // --- Collapsed Content ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Stats row
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WeightStat(label = "Start", weight = startingWeight)
                        WeightStat(label = "Current", weight = currentWeight, isMain = true)
                        WeightStat(label = "Target", weight = targetWeight)
                    }

                    // Animated expansion icon
                    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .rotate(rotation)
                    )
                }
            }

            // --- Expanded Content ---
            if (expanded) {
                Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                        color = AppTheme.colors.divider.copy(alpha = 0.5f)
                    )

                    // --- START OF FIX ---
                    // Re-using the composable from the dialog, but telling it NOT to show stats
                    WeightProgressDialogContent(
                        startingWeight = startingWeight,
                        targetWeight = targetWeight,
                        history = history,
                        onAddClick = onAddClick,
                        onManageClick = onManageClick,
                        showStats = false // <-- This prevents the duplication
                    )
                    // --- END OF FIX ---
                }
            }
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
            // --- START OF FIX ---
            // The dialog version should continue to show the stats
            WeightProgressDialogContent(
                startingWeight = startingWeight,
                targetWeight = targetWeight,
                history = history,
                onAddClick = onAddClick,
                onManageClick = onManageClick,
                showStats = true
            )
            // --- END OF FIX ---
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
internal fun WeightProgressDialogContent(
    startingWeight: Float,
    targetWeight: Float,
    history: List<WeightEntry>,
    onAddClick: () -> Unit,
    onManageClick: () -> Unit,
    showStats: Boolean = true // <-- FIX: Added new parameter with a default
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
                color = AppTheme.colors.textPrimary
            )
            IconButton(onClick = onAddClick) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Log New Weight",
                    tint = AppTheme.colors.primaryGreen
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
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {

            // --- START OF FIX ---
            // This Row will only be composed if showStats is true
            if (showStats) {
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
            }
            // --- END OF FIX ---

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
                // --- FIX 1: Corrected 'yyyY' to 'yyyy' ---
                val formattedDate = remember(lastDate) {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(lastDate)
                }
                Text(
                    text = "Last updated: $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
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
                Text("Manage Entries", color = AppTheme.colors.primaryGreen)
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun WeightStat(label: String, weight: Float, isMain: Boolean = false) {
    val weightText = if (weight > 0) String.format("%.1f", weight) else "---"
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = if (isMain) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (isMain) AppTheme.colors.primaryGreen else AppTheme.colors.textSecondary,
            fontWeight = if (isMain) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = weightText,
            style = if (isMain) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            color = if (isMain) AppTheme.colors.primaryGreen else AppTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold
        )
        if (isMain) {
            Text(
                text = "kg",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary
            )
        }
    }
}

// ... WeightLineChart, LogWeightDialog, and ManageWeightHistoryDialog are unchanged ...

@SuppressLint("DefaultLocale")
@Composable
fun WeightLineChart(
    startingWeight: Float,
    targetWeight: Float,
    history: List<WeightEntry>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    val primaryColor = AppTheme.colors.primaryGreen
    val targetColor = Color(0xFF4CAF50)
    val grayColor = AppTheme.colors.textSecondary

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