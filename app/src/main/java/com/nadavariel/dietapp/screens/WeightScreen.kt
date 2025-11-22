package com.nadavariel.dietapp.screens

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Paint
import android.os.Build
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.model.WeightEntry
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.account.StyledAlertDialog
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(
    navController: NavController,
    foodLogViewModel: FoodLogViewModel,
    authViewModel: AuthViewModel,
    openWeightLog: Boolean = false
) {
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val isLoadingProfile by authViewModel.isLoadingProfile.collectAsStateWithLifecycle()
    val isLoadingLogs by foodLogViewModel.isLoadingLogs.collectAsStateWithLifecycle()
    val weightHistory by foodLogViewModel.weightHistory.collectAsStateWithLifecycle()
    val targetWeight by foodLogViewModel.targetWeight.collectAsStateWithLifecycle()

    val isScreenLoading = isLoadingProfile || isLoadingLogs

    var showLogDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<WeightEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<WeightEntry?>(null) }

    LaunchedEffect(openWeightLog, isScreenLoading) {
        if (openWeightLog && !isScreenLoading) {
            showLogDialog = true
            navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>("openWeightLog")
        }
    }

    val currentWeight = weightHistory.lastOrNull()?.weight ?: userProfile.startingWeight
    val totalGoal = abs(targetWeight - userProfile.startingWeight)
    val currentProgress = abs(currentWeight - userProfile.startingWeight)
    val progressPercentage = if (totalGoal > 0) (currentProgress / totalGoal * 100f).coerceIn(0f, 100f) else 0f

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Modern Header
            ModernWeightHeader(navController)

            if (isScreenLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppTheme.colors.primaryGreen)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Achievement Summary Card
                    item {
                        AchievementSummaryCard(
                            startingWeight = userProfile.startingWeight,
                            currentWeight = currentWeight,
                            targetWeight = targetWeight,
                            progressPercentage = progressPercentage
                        )
                    }

                    // Progress Chart Card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Progress Chart",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.colors.textPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = AppTheme.colors.primaryGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                EnhancedWeightLineChart(
                                    startingWeight = userProfile.startingWeight,
                                    targetWeight = targetWeight,
                                    history = weightHistory,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(250.dp)
                                )
                            }
                        }
                    }

                    // Trophy Section
                    item {
                        TrophySection(progressPercentage = progressPercentage)
                    }

                    // Action Buttons
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ActionButton(
                                text = "Log Weight",
                                icon = Icons.Default.Add,
                                color = AppTheme.colors.primaryGreen,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    entryToEdit = null
                                    showLogDialog = true
                                }
                            )
                            ActionButton(
                                text = "Manage History",
                                icon = Icons.Default.History,
                                color = AppTheme.colors.softBlue,
                                modifier = Modifier.weight(1f),
                                onClick = { showHistoryDialog = true }
                            )
                        }
                    }

                    // Bottom spacing
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Dialogs
    if (showLogDialog) {
        LogWeightDialog(
            entryToEdit = entryToEdit,
            onDismiss = {
                showLogDialog = false
                entryToEdit = null
            },
            onSave = { weight, date ->
                foodLogViewModel.addWeightEntry(weight, date)
                showLogDialog = false
            },
            onUpdate = { id, weight, date ->
                foodLogViewModel.updateWeightEntry(id, weight, date)
                showLogDialog = false
            }
        )
    }

    if (showHistoryDialog) {
        ManageHistoryDialog(
            weightHistory = weightHistory,
            onDismiss = { showHistoryDialog = false },
            onEdit = {
                entryToEdit = it
                showHistoryDialog = false
                showLogDialog = true
            },
            onDelete = { entryToDelete = it }
        )
    }

    if (entryToDelete != null) {
        StyledAlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = "Delete Entry",
            text = "Are you sure you want to delete this weight entry?",
            confirmButtonText = "Delete",
            dismissButtonText = "Cancel",
            onConfirm = {
                entryToDelete?.let { foodLogViewModel.deleteWeightEntry(it.id) }
                entryToDelete = null
            }
        )
    }
}

// -----------------------------------------------------------------------------
// UI COMPONENTS
// -----------------------------------------------------------------------------

@Composable
private fun ModernWeightHeader(navController: NavController) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = AppTheme.colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Weight Journey",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
                Text(
                    text = "Track your progress towards your goal",
                    fontSize = 13.sp,
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun AchievementSummaryCard(
    startingWeight: Float,
    currentWeight: Float,
    targetWeight: Float,
    progressPercentage: Float
) {
    val weightLost = abs(currentWeight - startingWeight)
    val weightRemaining = abs(targetWeight - currentWeight)
    val isGaining = targetWeight > startingWeight

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Program Summary",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.colors.primaryGreen)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${progressPercentage.toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AchievementStatItem(
                    label = "Starting",
                    value = String.format("%.1f", startingWeight),
                    unit = "kg",
                    icon = Icons.Default.FlagCircle,
                    color = AppTheme.colors.textSecondary
                )

                AchievementStatItem(
                    label = if (isGaining) "Gained" else "Lost",
                    value = String.format("%.1f", weightLost),
                    unit = "kg",
                    icon = if (isGaining) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    color = AppTheme.colors.primaryGreen
                )

                AchievementStatItem(
                    label = "Remaining",
                    value = String.format("%.1f", weightRemaining),
                    unit = "kg",
                    icon = Icons.Default.Flag,
                    color = AppTheme.colors.warmOrange
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressPercentage / 100f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    AppTheme.colors.primaryGreen,
                                    AppTheme.colors.primaryGreen.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun AchievementStatItem(
    label: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppTheme.colors.textSecondary,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = unit,
                fontSize = 11.sp,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun TrophySection(progressPercentage: Float) {
    val badges = listOf(
        BadgeData(25f, "First Steps", "🥉", AppTheme.colors.warmOrange),
        BadgeData(50f, "Halfway Hero", "🥈", AppTheme.colors.softBlue),
        BadgeData(75f, "Almost There", "🥇", AppTheme.colors.primaryGreen)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Achievements",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = AppTheme.colors.warmOrange,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                badges.forEach { badge ->
                    BadgeItem(
                        badge = badge,
                        isUnlocked = progressPercentage >= badge.threshold
                    )
                }
            }
        }
    }
}

private data class BadgeData(
    val threshold: Float,
    val title: String,
    val emoji: String,
    val color: Color
)

@Composable
private fun BadgeItem(badge: BadgeData, isUnlocked: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (isUnlocked)
                        Brush.radialGradient(
                            listOf(
                                badge.color.copy(alpha = 0.3f),
                                badge.color.copy(alpha = 0.1f)
                            )
                        )
                    else
                        Brush.radialGradient(
                            listOf(
                                Color.Gray.copy(alpha = 0.2f),
                                Color.Gray.copy(alpha = 0.05f)
                            )
                        )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge.emoji,
                fontSize = 40.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = if (isUnlocked) 1f else 0.3f
                }
            )
        }

        Text(
            text = "${badge.threshold.toInt()}%",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isUnlocked) badge.color else AppTheme.colors.textSecondary
        )

        Text(
            text = badge.title,
            fontSize = 11.sp,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(90.dp)
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
fun ManageHistoryDialog(
    weightHistory: List<WeightEntry>,
    onDismiss: () -> Unit,
    onEdit: (WeightEntry) -> Unit,
    onDelete: (WeightEntry) -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Weight History",
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
        },
        containerColor = Color.White,
        text = {
            if (weightHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No weight entries yet",
                        color = AppTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(weightHistory.reversed(), key = { it.id }) { entry ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = AppTheme.colors.cardBackground
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "${entry.weight} kg",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = entry.timestamp?.toDate()
                                            ?.let { dateFormatter.format(it) } ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AppTheme.colors.textSecondary
                                    )
                                }
                                Row {
                                    IconButton(onClick = { onEdit(entry) }) {
                                        Icon(
                                            Icons.Rounded.Edit,
                                            contentDescription = "Edit",
                                            tint = AppTheme.colors.softBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(onClick = { onDelete(entry) }) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppTheme.colors.primaryGreen
                )
            ) {
                Text("Close")
            }
        }
    )
}

// Keep existing chart and dialog components
@SuppressLint("DefaultLocale")
@Composable
fun EnhancedWeightLineChart(
    startingWeight: Float,
    targetWeight: Float,
    history: List<WeightEntry>,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val primaryColor = AppTheme.colors.primaryGreen
    val targetColor = Color(0xFF4CAF50)
    val grayColor = AppTheme.colors.textSecondary
    val gradientColors = listOf(
        primaryColor.copy(alpha = 0.3f),
        primaryColor.copy(alpha = 0.1f),
        Color.Transparent
    )

    val dateFormat = remember { SimpleDateFormat("d MMM", Locale.getDefault()) }
    val textPaint = remember(density) {
        Paint().apply {
            color = grayColor.toArgb()
            textAlign = Paint.Align.CENTER
            textSize = with(density) { 11.sp.toPx() }
        }
    }
    val yAxisTextPaint = remember(density) {
        Paint().apply {
            color = grayColor.toArgb()
            textAlign = Paint.Align.RIGHT
            textSize = with(density) { 11.sp.toPx() }
        }
    }

    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "chartAnimation"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val yAxisPadding = with(density) { 35.dp.toPx() }
        val xAxisPadding = with(density) { 25.dp.toPx() }
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

        val verticalPadding = (maxWeight - minWeight) * 0.15f
        val yMin = (minWeight - verticalPadding).coerceAtLeast(0f)
        val yMax = (maxWeight + verticalPadding).coerceAtLeast(yMin + 1f)
        val weightRange = (yMax - yMin)

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
            yAxisPadding - with(density) { 6.dp.toPx() },
            getY(yMax) + with(density) { 4.dp.toPx() },
            yAxisTextPaint
        )

        if (targetWeight > 0) {
            val targetY = getY(targetWeight)
            drawLine(
                color = targetColor,
                start = Offset(yAxisPadding, targetY),
                end = Offset(size.width, targetY),
                strokeWidth = with(density) { 2.dp.toPx() },
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
        }

        val points = allPointsWithStart.mapIndexed { index, (weight, _) ->
            Offset(getX(index), getY(weight))
        }

        val animatedPointCount = (points.size * animationProgress).toInt().coerceAtLeast(1)
        val visiblePoints = points.take(animatedPointCount)

        if (visiblePoints.size > 1) {
            val gradientPath = Path().apply {
                moveTo(visiblePoints.first().x, graphHeight)
                lineTo(visiblePoints.first().x, visiblePoints.first().y)
                visiblePoints.drop(1).forEach { lineTo(it.x, it.y) }
                lineTo(visiblePoints.last().x, graphHeight)
                close()
            }

            drawPath(
                path = gradientPath,
                brush = Brush.verticalGradient(
                    colors = gradientColors,
                    startY = 0f,
                    endY = graphHeight
                )
            )
        }

        if (visiblePoints.size > 1) {
            val linePath = Path()
            linePath.moveTo(visiblePoints.first().x, visiblePoints.first().y)
            visiblePoints.drop(1).forEach { linePath.lineTo(it.x, it.y) }

            drawPath(
                path = linePath,
                color = primaryColor,
                style = Stroke(
                    width = with(density) { 3.dp.toPx() },
                    cap = StrokeCap.Round
                )
            )
        }

        visiblePoints.forEach { point ->
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = with(density) { 8.dp.toPx() },
                center = point
            )
            drawCircle(
                color = primaryColor,
                radius = with(density) { 5.dp.toPx() },
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = with(density) { 2.5.dp.toPx() },
                center = point
            )
        }

        if (animationProgress > 0.5f) {
            drawContext.canvas.nativeCanvas.drawText(
                "Start",
                getX(0),
                size.height - with(density) { 6.dp.toPx() },
                textPaint
            )

            if (allPointsWithStart.size > 2) {
                val midIndex = allPointsWithStart.size / 2
                val lastIndex = allPointsWithStart.size - 1

                val midDate = allPointsWithStart[midIndex].second
                drawContext.canvas.nativeCanvas.drawText(
                    dateFormat.format(midDate),
                    getX(midIndex),
                    size.height - with(density) { 6.dp.toPx() },
                    textPaint
                )

                if (midIndex != lastIndex) {
                    val lastDate = allPointsWithStart[lastIndex].second
                    drawContext.canvas.nativeCanvas.drawText(
                        dateFormat.format(lastDate),
                        getX(lastIndex),
                        size.height - with(density) { 6.dp.toPx() },
                        textPaint
                    )
                }
            } else if (allPointsWithStart.size == 2) {
                val lastDate = allPointsWithStart[1].second
                drawContext.canvas.nativeCanvas.drawText(
                    dateFormat.format(lastDate),
                    getX(1),
                    size.height - with(density) { 6.dp.toPx() },
                    textPaint
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
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
        },
        containerColor = Color.White,
        text = {
            Column {
                Text(
                    text = "Enter your weight and the date you weighed in.",
                    color = AppTheme.colors.textSecondary
                )
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
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primaryGreen,
                        focusedLabelColor = AppTheme.colors.primaryGreen,
                        cursorColor = AppTheme.colors.primaryGreen
                    )
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
                        tint = AppTheme.colors.primaryGreen
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = dateFormatter.format(selectedDate.time),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.primaryGreen
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
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.primaryGreen,
                    contentColor = Color.White
                )
            ) {
                Text(if (isEditMode) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppTheme.colors.textSecondary
                )
            ) {
                Text("Cancel")
            }
        }
    )
}