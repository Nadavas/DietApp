package com.nadavariel.dietapp.screens

import android.annotation.SuppressLint
import android.graphics.Paint
import android.os.Build
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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

    // --- LOGIC FIX START ---
    val currentWeight = weightHistory.lastOrNull()?.weight ?: userProfile.startingWeight
    val totalGoal = abs(targetWeight - userProfile.startingWeight)

    // Determine goal direction
    val isWeightLossGoal = targetWeight < userProfile.startingWeight

    // Calculate raw progress based on direction:
    // If Loss Goal: Start - Current (Positive = Progress)
    // If Gain Goal: Current - Start (Positive = Progress)
    val progressAmount = if (isWeightLossGoal) {
        userProfile.startingWeight - currentWeight
    } else {
        currentWeight - userProfile.startingWeight
    }

    // Only calculate progress percentage if we moved in the RIGHT direction
    // coerceIn(0f, 100f) ensures negative progress (moving wrong way) stays at 0%
    val progressPercentage = if (totalGoal > 0) {
        (progressAmount / totalGoal * 100f).coerceIn(0f, 100f)
    } else 0f

    // Amount of progress used for "Next Milestone" calculations (clamped to 0 if negative)
    val currentValidProgress = progressAmount.coerceAtLeast(0f)

    val isGainingGoal = targetWeight > userProfile.startingWeight
    // --- LOGIC FIX END ---

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Modern Header with progress
            PersonalizedHeader(
                navController = navController,
                userName = userProfile.name,
                progressPercentage = progressPercentage
            )

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
                    // Story Card - Your Journey
                    item {
                        JourneyStoryCard(
                            startingWeight = userProfile.startingWeight,
                            currentWeight = currentWeight,
                            targetWeight = targetWeight,
                            progressPercentage = progressPercentage,
                            isGainingGoal = isGainingGoal,
                            daysTracking = weightHistory.size
                        )
                    }

                    // Next Milestone Card
                    item {
                        NextMilestoneCard(
                            progressPercentage = progressPercentage,
                            currentProgress = currentValidProgress,
                            totalGoal = totalGoal
                        )
                    }

                    // Trophy Section with motivation
                    item {
                        MotivationalTrophySection(
                            progressPercentage = progressPercentage,
                            currentWeight = currentWeight,
                            targetWeight = targetWeight
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
                                    Column {
                                        Text(
                                            text = "Your Progress",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = AppTheme.colors.textPrimary
                                        )
                                        Text(
                                            text = "Every point is a victory",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = AppTheme.colors.textSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
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
                                text = "View History",
                                icon = Icons.Default.History,
                                color = AppTheme.colors.softBlue,
                                modifier = Modifier.weight(1f),
                                onClick = { showHistoryDialog = true }
                            )
                        }
                    }

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
private fun PersonalizedHeader(
    navController: NavController,
    userName: String,
    progressPercentage: Float
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Journey",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = when {
                            progressPercentage >= 75f -> "You're almost there! 🎉"
                            progressPercentage >= 50f -> "Halfway through! Keep going! 💪"
                            progressPercentage >= 25f -> "Great start! Stay consistent! ✨"
                            progressPercentage > 0f -> "Every journey begins with a single step! 🌟"
                            else -> "Your transformation starts today! 🚀"
                        },
                        fontSize = 13.sp,
                        color = AppTheme.colors.primaryGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun JourneyStoryCard(
    startingWeight: Float,
    currentWeight: Float,
    targetWeight: Float,
    progressPercentage: Float,
    isGainingGoal: Boolean,
    daysTracking: Int
) {
    // Calculate ACTUAL change regardless of goal
    val rawDiff = currentWeight - startingWeight
    val weightChangeAbs = abs(rawDiff)
    val weightRemaining = abs(targetWeight - currentWeight)

    // Did we actually gain or lose?
    val actuallyGained = rawDiff > 0
    val actionWord = if (actuallyGained) "gained" else "lost"

    // Check if moving in wrong direction (Setback)
    // If progress is 0 but we have tracked days and the weight changed significantly
    val isSetback = progressPercentage == 0f && daysTracking > 0 && weightChangeAbs > 0.1f

    val storyText = when {
        progressPercentage >= 100f -> "🎊 Incredible! You've reached your goal! You $actionWord ${String.format("%.1f", weightChangeAbs)} kg!"
        progressPercentage >= 75f -> "🔥 You're in the final stretch! Only ${String.format("%.1f", weightRemaining)} kg to go!"
        progressPercentage >= 50f -> "💪 Amazing progress! You've $actionWord ${String.format("%.1f", weightChangeAbs)} kg and you're halfway there!"
        progressPercentage >= 25f -> "✨ Great start! You've $actionWord ${String.format("%.1f", weightChangeAbs)} kg. Keep the momentum!"
        isSetback -> "⚠️ You've moved ${String.format("%.1f", weightChangeAbs)} kg away from your goal. Don't give up, get back on track!"
        progressPercentage > 0f -> "🌱 Your journey has begun! ${String.format("%.1f", weightChangeAbs)} kg $actionWord. Stay consistent!"
        else -> "🚀 Ready to start your transformation? Log your first weight to begin!"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AppTheme.colors.primaryGreen.copy(alpha = 0.15f),
                            AppTheme.colors.primaryGreen.copy(alpha = 0.05f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your Story",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = storyText,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(if (isSetback) AppTheme.colors.warmOrange else AppTheme.colors.primaryGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${progressPercentage.toInt()}%",
                            fontSize = 18.sp,
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
                    JourneyStatBubble(
                        value = String.format("%.1f", startingWeight),
                        label = "Start",
                        color = AppTheme.colors.textSecondary
                    )

                    JourneyStatBubble(
                        value = String.format("%.1f", currentWeight),
                        label = "Current",
                        color = if (isSetback) AppTheme.colors.warmOrange else AppTheme.colors.primaryGreen,
                        isHighlight = true
                    )

                    JourneyStatBubble(
                        value = String.format("%.1f", targetWeight),
                        label = "Goal",
                        color = AppTheme.colors.warmOrange
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Animated Progress Bar
                AnimatedJourneyProgressBar(progressPercentage = progressPercentage)

                if (daysTracking > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = AppTheme.colors.textSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$daysTracking ${if (daysTracking == 1) "entry" else "entries"} logged",
                            fontSize = 12.sp,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyStatBubble(
    value: String,
    label: String,
    color: Color,
    isHighlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (isHighlight) 70.dp else 60.dp)
                .clip(CircleShape)
                .background(
                    if (isHighlight) {
                        Brush.radialGradient(
                            listOf(
                                color.copy(alpha = 0.3f),
                                color.copy(alpha = 0.1f)
                            )
                        )
                    } else {
                        // FIX: Wrap the Color in SolidColor so both branches return a Brush
                        SolidColor(color.copy(alpha = 0.1f))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    fontSize = if (isHighlight) 20.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "kg",
                    fontSize = 10.sp,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlight) color else AppTheme.colors.textSecondary
        )
    }
}

@Composable
private fun AnimatedJourneyProgressBar(progressPercentage: Float) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercentage,
        animationSpec = tween(1200, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(Color.White.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress / 100f)
                .clip(RoundedCornerShape(7.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            AppTheme.colors.primaryGreen,
                            AppTheme.colors.primaryGreen.copy(alpha = 0.8f)
                        )
                    )
                )
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
private fun NextMilestoneCard(
    progressPercentage: Float,
    currentProgress: Float,
    totalGoal: Float
) {
    // If progress is 0 or negative (wrong direction), next milestone is 25%
    val effectiveProgressPercent = progressPercentage.coerceAtLeast(0f)

    val nextMilestone = when {
        effectiveProgressPercent < 25f -> 25f
        effectiveProgressPercent < 50f -> 50f
        effectiveProgressPercent < 75f -> 75f
        effectiveProgressPercent < 100f -> 100f
        else -> null
    }

    if (nextMilestone != null) {
        // Calculate raw amount needed to hit milestone from START
        val amountNeededForMilestone = (nextMilestone / 100f * totalGoal)
        // Subtract what we have already achieved (currentProgress is 0 if we went backward)
        val remainingToMilestone = amountNeededForMilestone - currentProgress

        val badge = when (nextMilestone) {
            25f -> "🥉"
            50f -> "🥈"
            75f -> "🥇"
            else -> "🏆"
        }
        val milestoneName = when (nextMilestone) {
            25f -> "First Steps"
            50f -> "Halfway Hero"
            75f -> "Almost There"
            else -> "Goal Complete"
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppTheme.colors.warmOrange.copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = badge,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Next: $milestoneName",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = "${String.format("%.1f", remainingToMilestone)} kg to unlock",
                            fontSize = 12.sp,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AppTheme.colors.warmOrange,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MotivationalTrophySection(
    progressPercentage: Float,
    currentWeight: Float,
    targetWeight: Float
) {
    val badges = listOf(
        BadgeData(25f, "First Steps", "🥉", AppTheme.colors.warmOrange, "You're on your way!"),
        BadgeData(50f, "Halfway Hero", "🥈", AppTheme.colors.softBlue, "Keep pushing forward!"),
        BadgeData(75f, "Almost There", "🥇", AppTheme.colors.primaryGreen, "The finish line awaits!")
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
                Column {
                    Text(
                        text = "Achievement Gallery",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = "Unlock badges as you progress",
                        fontSize = 12.sp,
                        color = AppTheme.colors.textSecondary
                    )
                }
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
                    MotivationalBadgeItem(
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
    val color: Color,
    val motivation: String
)

@Composable
private fun MotivationalBadgeItem(badge: BadgeData, isUnlocked: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f)
    )

    val rotation by rememberInfiniteTransition().animateFloat(
        initialValue = if (isUnlocked) -5f else 0f,
        targetValue = if (isUnlocked) 5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .scale(scale)
                .graphicsLayer { rotationZ = if (isUnlocked) rotation else 0f }
                .clip(CircleShape)
                .background(
                    if (isUnlocked)
                        Brush.radialGradient(
                            listOf(
                                badge.color.copy(alpha = 0.4f),
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = badge.emoji,
                    fontSize = 44.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (isUnlocked) 1f else 0.3f
                    }
                )
                if (isUnlocked) {
                    Text(
                        text = "✓",
                        fontSize = 16.sp,
                        color = badge.color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = "${badge.threshold.toInt()}%",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isUnlocked) badge.color else AppTheme.colors.textSecondary
        )

        Text(
            text = badge.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(90.dp)
        )

        if (isUnlocked) {
            Text(
                text = badge.motivation,
                fontSize = 10.sp,
                color = badge.color,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(90.dp)
            )
        } else {
            Text(
                text = "Locked",
                fontSize = 10.sp,
                color = AppTheme.colors.textSecondary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
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
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

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
                        .clickable(onClick = { showDatePicker = true })
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = Calendar.getInstance().apply { timeInMillis = millis }
                            // Prevent future dates
                            selectedDate = if (newDate.after(Calendar.getInstance())) {
                                Calendar.getInstance()
                            } else {
                                newDate
                            }
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
            colors = DatePickerDefaults.colors(containerColor = Color.White)
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