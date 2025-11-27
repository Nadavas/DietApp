package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StatisticsScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    navController: NavController
) {
    val weeklyCalories by foodLogViewModel.weeklyCalories.collectAsState()
    val weeklyProtein by foodLogViewModel.weeklyProtein.collectAsState()
    val weeklyMacroPercentages by foodLogViewModel.weeklyMacroPercentages.collectAsState()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(AppTheme.colors.statsGradient))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Modern Header
            ModernHeader()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ADD THE CAROUSEL HERE
                item {
                    WeeklyAchievementsCarousel(
                        weeklyCalories = weeklyCalories,
                        weeklyProtein = weeklyProtein.mapValues { it.value.toFloat() },
                        weeklyMacroPercentages = weeklyMacroPercentages,
                        onSeeAllClick = {
                            navController.navigate(NavRoutes.ALL_ACHIEVEMENTS)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Text(
                        text = "Your Nutritional Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                }

                item {
                    CategoryCard(
                        title = "Energy & Protein",
                        subtitle = "Daily calories and protein intake",
                        icon = Icons.Default.LocalFireDepartment,
                        color = AppTheme.colors.warmOrange,
                        onClick = { navController.navigate(NavRoutes.STATS_ENERGY) }
                    )
                }

                item {
                    CategoryCard(
                        title = "Macronutrients",
                        subtitle = "Protein, carbs, and fat balance",
                        icon = Icons.Default.PieChart,
                        color = AppTheme.colors.statsGreen,
                        onClick = { navController.navigate(NavRoutes.STATS_MACROS) }
                    )
                }

                item {
                    CategoryCard(
                        title = "Fiber & Sugar",
                        subtitle = "Carbohydrate quality metrics",
                        icon = Icons.Default.Spa,
                        color = AppTheme.colors.accentTeal,
                        onClick = { navController.navigate(NavRoutes.STATS_CARBS) }
                    )
                }

                item {
                    CategoryCard(
                        title = "Minerals",
                        subtitle = "Sodium, potassium, calcium, iron",
                        icon = Icons.Default.Science,
                        color = AppTheme.colors.softBlue,
                        onClick = { navController.navigate(NavRoutes.STATS_MINERALS) }
                    )
                }

                item {
                    CategoryCard(
                        title = "Vitamins",
                        subtitle = "Essential vitamin intake",
                        icon = Icons.Default.Favorite,
                        color = AppTheme.colors.sunsetPink,
                        onClick = { navController.navigate(NavRoutes.STATS_VITAMINS) }
                    )
                }

                // Bottom spacing
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// NEW ACHIEVEMENT COMPONENTS
// -----------------------------------------------------------------------------

private data class NutritionBadgeData(
    val id: Int,
    val title: String,
    val emoji: String,
    val color: Color,
    val isUnlocked: Boolean
)

@Composable
private fun NutritionAchievementsSection(
    weeklyCalories: Map<LocalDate, Int>,
    weeklyProtein: Map<LocalDate, Int>,
    weeklyMacroPercentages: Map<String, Float>
) {
    // 1. Calculate Statistics for Unlocks
    val daysLogged = weeklyCalories.size
    val avgProtein = if (weeklyProtein.isNotEmpty()) weeklyProtein.values.average() else 0.0
    val fatPercentage = weeklyMacroPercentages["Fat"] ?: 0f
    // Assuming balanced if Protein > 20%, Carbs > 30%, Fat < 40% (Example logic)
    val isBalanced = (weeklyMacroPercentages["Protein"] ?: 0f) > 0.2f &&
            (weeklyMacroPercentages["Carbohydrates"] ?: 0f) > 0.3f

    // 2. Define Badges and Check Conditions
    val badges = listOf(
        NutritionBadgeData(
            1, "Starter", "🔥", AppTheme.colors.warmOrange,
            isUnlocked = daysLogged >= 1
        ),
        NutritionBadgeData(
            2, "Consistent", "📅", AppTheme.colors.softBlue,
            isUnlocked = daysLogged >= 3
        ),
        NutritionBadgeData(
            3, "Week Warrior", "⚔️", AppTheme.colors.primaryGreen,
            isUnlocked = daysLogged >= 7
        ),
        NutritionBadgeData(
            4, "Iron Lifter", "🥩", AppTheme.colors.sunsetPink,
            isUnlocked = avgProtein > 80 // Example: Average > 80g protein
        ),
        NutritionBadgeData(
            5, "Lean Machine", "🥑", AppTheme.colors.accentTeal,
            isUnlocked = fatPercentage in 0.15f..0.35f // Healthy Fat Range
        ),
        NutritionBadgeData(
            6, "Vita-Boost", "🍋", AppTheme.colors.warmOrange,
            isUnlocked = isBalanced // Awarded for balanced macros
        )
    )

    // 3. Cyclic Scrolling Logic
    val startIndex = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2 % badges.size)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    LaunchedEffect(Unit) {
        while (true) {
            listState.scrollBy(1.5f)
            delay(20)
        }
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
                .padding(vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Highlights",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = "$daysLogged days tracked this week",
                        fontSize = 11.sp,
                        color = AppTheme.colors.textSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AppTheme.colors.warmOrange,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                userScrollEnabled = true
            ) {
                items(Int.MAX_VALUE) { index ->
                    val badge = badges[index % badges.size]
                    NutritionBadgeItemCompact(badge = badge)
                }
            }
        }
    }
}

@Composable
private fun NutritionBadgeItemCompact(badge: NutritionBadgeData) {
    val scale by animateFloatAsState(
        targetValue = if (badge.isUnlocked) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f), label = ""
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(70.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    if (badge.isUnlocked)
                        Brush.radialGradient(
                            listOf(badge.color.copy(alpha = 0.4f), badge.color.copy(alpha = 0.1f))
                        )
                    else
                        Brush.radialGradient(
                            listOf(Color.Gray.copy(alpha = 0.2f), Color.Gray.copy(alpha = 0.05f))
                        )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge.emoji,
                fontSize = 28.sp,
                modifier = Modifier.graphicsLayer { alpha = if (badge.isUnlocked) 1f else 0.3f }
            )
            if (badge.isUnlocked) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = "✓",
                        fontSize = 12.sp,
                        color = badge.color,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Text(
            text = badge.title,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            lineHeight = 12.sp
        )
    }
}

// -----------------------------------------------------------------------------
// EXISTING COMPONENTS
// -----------------------------------------------------------------------------

@Composable
private fun ModernHeader() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Nutrition Insights",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
            Text(
                text = "Your weekly nutrition summary",
                fontSize = 14.sp,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = AppTheme.colors.textSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}