package com.nadavariel.dietapp.ui.home

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.graphics.Paint
import android.os.Build
import android.widget.DatePicker
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
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
import kotlin.math.abs

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

    // Calculate progress metrics
    val totalGoal = abs(targetWeight - startingWeight)
    val currentProgress = abs(currentWeight - startingWeight)
    val progressPercentage = if (totalGoal > 0) (currentProgress / totalGoal * 100f).coerceIn(0f, 100f) else 0f
    val isGainingWeight = targetWeight > startingWeight
    val isOnTrack = if (isGainingWeight) currentWeight >= startingWeight else currentWeight <= startingWeight

    // Animated values
    val animatedProgress by animateFloatAsState(
        targetValue = if (expanded) progressPercentage else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = { expanded = !expanded }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Animated gradient background
            AnimatedGradientBackground(expanded = expanded, isOnTrack = isOnTrack)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = spring())
            ) {
                // Header Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Column {
                        // Title with animated icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedTrendIcon(
                                    isGainingWeight = isGainingWeight,
                                    isOnTrack = isOnTrack
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Weight Progress",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.colors.textPrimary
                                    )
                                    AnimatedProgressText(
                                        progressPercentage = progressPercentage,
                                        expanded = expanded
                                    )
                                }
                            }

                            val rotation by animateFloatAsState(
                                targetValue = if (expanded) 180f else 0f,
                                label = "rotation"
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = AppTheme.colors.primaryGreen,
                                modifier = Modifier
                                    .size(32.dp)
                                    .rotate(rotation)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Weight Stats Row with enhanced design
                        EnhancedWeightStatsRow(
                            startingWeight = startingWeight,
                            currentWeight = currentWeight,
                            targetWeight = targetWeight,
                            expanded = expanded
                        )
                    }
                }

                // Expanded Content
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                        // Progress Bar with percentage
                        AnimatedProgressBar(
                            progress = animatedProgress,
                            isOnTrack = isOnTrack
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Enhanced Chart
                        EnhancedWeightLineChart(
                            startingWeight = startingWeight,
                            targetWeight = targetWeight,
                            history = history,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )

                        if (history.isNotEmpty() && history.last().timestamp != null) {
                            val lastDate = history.last().timestamp!!.toDate()
                            val formattedDate = remember(lastDate) {
                                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(lastDate)
                            }
                            Text(
                                text = "Last updated: $formattedDate",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            EnhancedActionButton(
                                text = "Log Weight",
                                icon = Icons.Default.Add,
                                onClick = onAddClick,
                                modifier = Modifier.weight(1f),
                                isPrimary = true
                            )
                            EnhancedActionButton(
                                text = "Manage",
                                icon = Icons.Default.Edit,
                                onClick = onManageClick,
                                modifier = Modifier.weight(1f),
                                isPrimary = false
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedGradientBackground(expanded: Boolean, isOnTrack: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetX"
    )

    val alpha by animateFloatAsState(
        targetValue = if (expanded) 0.15f else 0.08f,
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (expanded) 600.dp else 150.dp)
            .background(
                Brush.linearGradient(
                    colors = if (isOnTrack) {
                        listOf(
                            AppTheme.colors.primaryGreen.copy(alpha = alpha),
                            Color(0xFF4CAF50).copy(alpha = alpha * 0.5f),
                            Color.Transparent
                        )
                    } else {
                        listOf(
                            Color(0xFFFF9800).copy(alpha = alpha),
                            Color(0xFFFFC107).copy(alpha = alpha * 0.5f),
                            Color.Transparent
                        )
                    },
                    start = Offset(offsetX, 0f),
                    end = Offset(offsetX + 500f, 500f)
                )
            )
    )
}

@Composable
fun AnimatedTrendIcon(isGainingWeight: Boolean, isOnTrack: Boolean) {
    val scale by rememberInfiniteTransition(label = "icon").animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .background(
                color = if (isOnTrack) AppTheme.colors.primaryGreen.copy(alpha = 0.2f)
                else Color(0xFFFF9800).copy(alpha = 0.2f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isGainingWeight) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
            contentDescription = null,
            tint = if (isOnTrack) AppTheme.colors.primaryGreen else Color(0xFFFF9800),
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun AnimatedProgressText(progressPercentage: Float, expanded: Boolean) {
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val animatedPercentage by animateFloatAsState(
            targetValue = progressPercentage,
            animationSpec = tween(durationMillis = 1000),
            label = "percentage"
        )

        Text(
            text = "${animatedPercentage.toInt()}% to goal",
            fontSize = 12.sp,
            color = AppTheme.colors.primaryGreen,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EnhancedWeightStatsRow(
    startingWeight: Float,
    currentWeight: Float,
    targetWeight: Float,
    expanded: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        EnhancedWeightStat(
            label = "Start",
            weight = startingWeight,
            isMain = false,
            expanded = expanded,
            delay = 0
        )
        EnhancedWeightStat(
            label = "Current",
            weight = currentWeight,
            isMain = true,
            expanded = expanded,
            delay = 100
        )
        EnhancedWeightStat(
            label = "Target",
            weight = targetWeight,
            isMain = false,
            expanded = expanded,
            delay = 200
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun EnhancedWeightStat(
    label: String,
    weight: Float,
    isMain: Boolean,
    expanded: Boolean,
    delay: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (isMain) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    var animatedWeight by remember { mutableStateOf(0f) }

    LaunchedEffect(weight, expanded) {
        if (expanded) {
            kotlinx.coroutines.delay(delay.toLong())
            animate(
                initialValue = animatedWeight,
                targetValue = weight,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            ) { value, _ ->
                animatedWeight = value
            }
        } else {
            animatedWeight = weight
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.scale(scale)
    ) {
        Text(
            text = label,
            style = if (isMain) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (isMain) AppTheme.colors.primaryGreen else AppTheme.colors.textSecondary,
            fontWeight = if (isMain) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = if (isMain) {
                Modifier
                    .size(80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            } else Modifier
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val weightText = if (weight > 0) String.format("%.1f", if (expanded) animatedWeight else weight) else "---"
                Text(
                    text = weightText,
                    style = if (isMain) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                    color = if (isMain) AppTheme.colors.primaryGreen else AppTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun AnimatedProgressBar(progress: Float, isOnTrack: Boolean) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.titleSmall,
                color = AppTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${progress.toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                color = if (isOnTrack) AppTheme.colors.primaryGreen else Color(0xFFFF9800),
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.Gray.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress / 100f)
                    .background(
                        Brush.horizontalGradient(
                            colors = if (isOnTrack) {
                                listOf(AppTheme.colors.primaryGreen, Color(0xFF4CAF50))
                            } else {
                                listOf(Color(0xFFFF9800), Color(0xFFFFC107))
                            }
                        ),
                        shape = RoundedCornerShape(6.dp)
                    )
            )
        }
    }
}

@Composable
fun EnhancedActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPrimary) AppTheme.colors.primaryGreen else Color.Transparent,
            contentColor = if (isPrimary) Color.White else AppTheme.colors.primaryGreen
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isPrimary) 4.dp else 0.dp
        ),
        border = if (!isPrimary) androidx.compose.foundation.BorderStroke(
            1.dp,
            AppTheme.colors.primaryGreen.copy(alpha = 0.5f)
        ) else null
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold
        )
    }
}

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

    // Animation for drawing the chart
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

        // Draw Y-axis labels
        drawContext.canvas.nativeCanvas.drawText(
            String.format("%.1f", yMax),
            yAxisPadding - with(density) { 6.dp.toPx() },
            getY(yMax) + with(density) { 4.dp.toPx() },
            yAxisTextPaint
        )
        val yMid = (yMin + yMax) / 2
        if (yMid > yMin && (yMax - yMid) > 1f) {
            drawContext.canvas.nativeCanvas.drawText(
                String.format("%.1f", yMid),
                yAxisPadding - with(density) { 6.dp.toPx() },
                getY(yMid) + with(density) { 4.dp.toPx() },
                yAxisTextPaint
            )
        }

        // Target weight dashed line
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

        // Calculate animated points
        val points = allPointsWithStart.mapIndexed { index, (weight, _) ->
            Offset(getX(index), getY(weight))
        }

        val animatedPointCount = (points.size * animationProgress).toInt().coerceAtLeast(1)
        val visiblePoints = points.take(animatedPointCount)

        // Draw gradient fill under the line
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

        // Draw the main line
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

        // Draw points with glow effect
        visiblePoints.forEach { point ->
            // Glow
            drawCircle(
                color = primaryColor.copy(alpha = 0.3f),
                radius = with(density) { 8.dp.toPx() },
                center = point
            )
            // Main circle
            drawCircle(
                color = primaryColor,
                radius = with(density) { 5.dp.toPx() },
                center = point
            )
            // Inner circle
            drawCircle(
                color = Color.White,
                radius = with(density) { 2.5.dp.toPx() },
                center = point
            )
        }

        // Draw X-axis labels
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

// Keep the existing dialog components unchanged
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
                onManageClick = onManageClick,
                showStats = true
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
internal fun WeightProgressDialogContent(
    startingWeight: Float,
    targetWeight: Float,
    history: List<WeightEntry>,
    onAddClick: () -> Unit,
    onManageClick: () -> Unit,
    showStats: Boolean = true
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
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

            Spacer(modifier = Modifier.height(16.dp))

            EnhancedWeightLineChart(
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

    Canvas(modifier = modifier.fillMaxSize()) {
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
                    items(history.reversed()) { entry ->
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