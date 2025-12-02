package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.nadavariel.dietapp.ui.AppTheme
import kotlinx.coroutines.delay
import java.time.LocalDate

// -----------------------------------------------------------------------------
// DATA MODELS & LOGIC
// -----------------------------------------------------------------------------

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val color: Color,
    // Condition uses pre-calculated primitive stats, so it doesn't care about the Map types
    val condition: (daysLogged: Int, avgCals: Int, avgProtein: Int, macroMap: Map<String, Float>) -> Boolean
)

object AchievementRepository {

    // Helper to safely get macro percentage as 0.0 - 1.0 scale
    // This handles 0-100 inputs AND 0-1 inputs automatically.
    private fun getMacro(map: Map<String, Float>, keys: List<String>): Float {
        // 1. Find the value using multiple possible keys
        val value = keys.firstNotNullOfOrNull { map[it] } ?: return 0f

        // 2. Normalize: If value is > 1.0 (e.g., 45.5), treat it as percentage (0.455)
        // If it's already small (e.g. 0.45), leave it alone.
        return if (value > 1.0f) value / 100f else value
    }

    val allAchievements = listOf(
        // --- Consistency Badges (Logic was fine) ---
        Achievement("c1", "The Starter", "Logged food at least 1 day this week.", "🌱", Color(0xFF8BC34A)) { d, _, _, _ -> d >= 1 },
        Achievement("c2", "Momentum", "Logged 3 days this week. Building a habit!", "🚀", Color(0xFF03A9F4)) { d, _, _, _ -> d >= 3 },
        Achievement("c3", "Week Warrior", "Perfect week! You logged all 7 days.", "🔥", Color(0xFFFF5722)) { d, _, _, _ -> d >= 7 },
        Achievement("c4", "Consistent", "Logged at least 5 days this week.", "🗓️", Color(0xFF9C27B0)) { d, _, _, _ -> d >= 5 },

        // --- Calorie Badges (Logic was fine) ---
        Achievement("k1", "On Target", "Avg calories 1800-2500.", "🎯", Color(0xFFFFC107)) { _, c, _, _ -> c in 1800..2500 },
        Achievement("k2", "Light & Lean", "Avg calories under 1800.", "🪶", Color(0xFF00BCD4)) { _, c, _, _ -> c in 1000..1799 },
        Achievement("k3", "The Builder", "Avg calories over 2500.", "🏗️", Color(0xFF795548)) { _, c, _, _ -> c > 2500 },

        // --- Protein Badges (Logic was fine) ---
        Achievement("p1", "Protein Starter", "Avg > 60g protein/day.", "🥚", Color(0xFFCDDC39)) { _, _, p, _ -> p > 60 },
        Achievement("p2", "Muscle Maker", "Avg > 100g protein/day.", "🥩", Color(0xFFF44336)) { _, _, p, _ -> p > 100 },
        Achievement("p3", "Arnold Mode", "Avg > 150g protein/day.", "💪", Color(0xFF673AB7)) { _, _, p, _ -> p > 150 },

        // --- Macro Balance Badges (FIXED) ---
        Achievement("m1", "Balanced Plate", "Protein > 20%, Carbs > 30%, Fat < 35%.", "⚖️", Color(0xFF4CAF50)) { _, _, _, m ->
            val p = getMacro(m, listOf("Protein", "Pro"))
            val c = getMacro(m, listOf("Carbohydrates", "Carbs", "Carb"))
            val f = getMacro(m, listOf("Fat", "Fats"))

            p > 0.2f && c > 0.3f && f < 0.35f && f > 0.0f // Ensure fat isn't 0 (missing data)
        },
        Achievement("m2", "Low Carb", "Carbs kept under 25% of total energy.", "🥑", Color(0xFF009688)) { _, _, _, m ->
            val c = getMacro(m, listOf("Carbohydrates", "Carbs"))
            // Only award if we actually have data (c > 0)
            c in 0.01f..0.25f
        },
        Achievement("m3", "Carb Loader", "Carbs over 50%.", "🍝", Color(0xFFFF9800)) { _, _, _, m ->
            val c = getMacro(m, listOf("Carbohydrates", "Carbs"))
            c > 0.5f
        },
        Achievement("m4", "Keto Zone", "High Fat (>60%) and very low carbs (<10%).", "🥓", Color(0xFFD84315)) { _, _, _, m ->
            val f = getMacro(m, listOf("Fat", "Fats"))
            // Pass default 1.0f (100%) for Carbs if missing, so we don't accidentally award low carb for missing data
            val cRaw = m["Carbohydrates"] ?: m["Carbs"]
            val c = if (cRaw == null) 1.0f else if (cRaw > 1f) cRaw / 100f else cRaw

            f > 0.6f && c < 0.1f
        },

        // --- Lifestyle & Fun (FIXED) ---
        Achievement("v1", "Iron Will", "Logged 6+ days with high protein.", "⚔️", Color(0xFF607D8B)) { d, _, p, _ -> d >= 6 && p > 100 },
        Achievement("v2", "Hydration Hero", "Consistent logging (4+ days).", "💧", Color(0xFF2196F3)) { d, _, _, _ -> d >= 4 },
        Achievement("v3", "Sweet Tooth", "Carbs are high (>55%).", "🍭", Color(0xFFE91E63)) { _, _, _, m ->
            val c = getMacro(m, listOf("Carbohydrates", "Carbs"))
            c > 0.55f
        },
        Achievement("v4", "Clean Sheet", "You logged at least 1500 kcal for 5+ days.", "📝", Color(0xFF3F51B5)) { d, c, _, _ -> d >= 5 && c >= 1500 },

        // --- Special ---
        Achievement("s1", "Minimalist", "Logged strictly (1-2 days) but with high protein.", "🎯", Color(0xFFFFD54F)) { d, _, p, _ -> d in 1..2 && p > 100 },
        Achievement("s2", "Feast Mode", "Average calories very high (>3000).", "🍗", Color(0xFF8D6E63)) { _, c, _, _ -> c > 3000 }
    )
}

// -----------------------------------------------------------------------------
// HELPER: CALCULATE STATS CORRECTLY
// -----------------------------------------------------------------------------

data class CalculatedStats(
    val daysLogged: Int,
    val avgCals: Int,
    val avgProtein: Int
)

fun calculateStats(
    weeklyCalories: Map<LocalDate, Int>, // FIX: Changed String to LocalDate
    weeklyProtein: Map<LocalDate, Float> // FIX: Changed String to LocalDate
): CalculatedStats {
    val activeDaysCals = weeklyCalories.values.filter { it > 0 }
    val activeDaysProtein = weeklyProtein.values.filter { it > 0f }

    val daysLogged = activeDaysCals.size
    val avgCals = if (activeDaysCals.isNotEmpty()) activeDaysCals.average().toInt() else 0
    val avgProtein = if (activeDaysProtein.isNotEmpty()) activeDaysProtein.average().toInt() else 0

    return CalculatedStats(daysLogged, avgCals, avgProtein)
}

// -----------------------------------------------------------------------------
// COMPONENT: CAROUSEL (For Statistics Screen)
// -----------------------------------------------------------------------------

@Composable
fun WeeklyAchievementsCarousel(
    weeklyCalories: Map<LocalDate, Int>, // FIX: Changed String to LocalDate
    weeklyProtein: Map<LocalDate, Float>, // FIX: Changed String to LocalDate
    weeklyMacroPercentages: Map<String, Float>,
    onSeeAllClick: () -> Unit
) {
    val stats = remember(weeklyCalories, weeklyProtein) {
        calculateStats(weeklyCalories, weeklyProtein)
    }

    val unlockedAchievements = remember(stats, weeklyMacroPercentages) {
        AchievementRepository.allAchievements.filter {
            it.condition(stats.daysLogged, stats.avgCals, stats.avgProtein, weeklyMacroPercentages)
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(unlockedAchievements) {
        if (unlockedAchievements.isNotEmpty()) {
            while (true) {
                delay(3000)
                currentIndex = (currentIndex + 1) % unlockedAchievements.size
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Weekly Achievements",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (unlockedAchievements.isEmpty()) {
                    Text(text = "🌱", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Start Logging!",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Log your meals to unlock achievements.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                } else {
                    AnimatedContent(
                        targetState = unlockedAchievements[currentIndex],
                        transitionSpec = {
                            fadeIn() + slideInHorizontally { width -> width } togetherWith
                                    fadeOut() + slideOutHorizontally { width -> -width }
                        },
                        label = "AchievementCarousel"
                    ) { badge ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                badge.color.copy(alpha = 0.3f),
                                                badge.color.copy(alpha = 0.05f)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = badge.emoji, fontSize = 40.sp)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = badge.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = badge.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AppTheme.colors.textSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                modifier = Modifier.height(40.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSeeAllClick)
                    .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "View All Possible Achievements",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.primaryGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = AppTheme.colors.primaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// COMPONENT: ALL ACHIEVEMENTS SCREEN
// -----------------------------------------------------------------------------

@Composable
fun AllAchievementsScreen(
    navController: NavController,
    weeklyCalories: Map<LocalDate, Int>, // FIX: Changed String to LocalDate
    weeklyProtein: Map<LocalDate, Float>, // FIX: Changed String to LocalDate
    weeklyMacroPercentages: Map<String, Float>
) {
    val stats = remember(weeklyCalories, weeklyProtein) {
        calculateStats(weeklyCalories, weeklyProtein)
    }

    val allBadges = AchievementRepository.allAchievements
    var selectedBadge by remember { mutableStateOf<Achievement?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "All Achievements",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(allBadges) { badge ->
                    val isUnlocked = badge.condition(stats.daysLogged, stats.avgCals, stats.avgProtein, weeklyMacroPercentages)
                    AchievementGridItem(
                        badge = badge,
                        isUnlocked = isUnlocked,
                        onClick = { selectedBadge = badge }
                    )
                }
            }
        }

        if (selectedBadge != null) {
            val isUnlocked = selectedBadge!!.condition(stats.daysLogged, stats.avgCals, stats.avgProtein, weeklyMacroPercentages)
            AchievementDetailDialog(
                badge = selectedBadge!!,
                isUnlocked = isUnlocked,
                onDismiss = { selectedBadge = null }
            )
        }
    }
}

@Composable
fun AchievementGridItem(
    badge: Achievement,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    if (isUnlocked)
                        Brush.radialGradient(
                            listOf(badge.color.copy(alpha = 0.4f), badge.color.copy(alpha = 0.1f))
                        )
                    else
                        SolidColor(Color.Gray.copy(alpha = 0.1f))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge.emoji,
                fontSize = 32.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = if (isUnlocked) 1f else 0.3f
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (isUnlocked) {
                    Text("✓", color = badge.color, fontWeight = FontWeight.Bold)
                } else {
                    Text("🔒", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = badge.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isUnlocked) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary
        )
    }
}

@Composable
fun AchievementDetailDialog(
    badge: Achievement,
    isUnlocked: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        modifier = Modifier.clickable(onClick = onDismiss)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked)
                                Brush.radialGradient(
                                    listOf(badge.color.copy(alpha = 0.4f), badge.color.copy(alpha = 0.1f))
                                )
                            else
                                SolidColor(Color.Gray.copy(alpha = 0.1f))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge.emoji,
                        fontSize = 60.sp,
                        modifier = Modifier.graphicsLayer {
                            alpha = if (isUnlocked) 1f else 0.3f
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) AppTheme.colors.textPrimary else Color.Gray
                )

                if (!isUnlocked) {
                    Text(
                        text = "(Locked)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Red.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = AppTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}