package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.model.Achievement
import com.nadavariel.dietapp.model.AchievementRepository
import com.nadavariel.dietapp.model.CalculatedStats
import com.nadavariel.dietapp.ui.AppMainHeader
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
    val weeklyMicros = emptyMap<String, Float>()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppMainHeader(
                title = "Nutrition Insights",
                subtitle = "Your weekly nutrition summary"
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    WeeklyAchievementsCarousel(
                        weeklyCalories = weeklyCalories,
                        weeklyProtein = weeklyProtein.mapValues { it.value.toFloat() },
                        weeklyMacroPercentages = weeklyMacroPercentages,
                        weeklyAverageMicros = weeklyMicros,
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
                        text = "Nutritional Breakdown",
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
                        color = AppTheme.colors.vividGreen,
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

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// -------------------------------
// --------- COMPOSABLES ---------
// -------------------------------

private fun calculateStats(
    weeklyCalories: Map<LocalDate, Int>,
    weeklyProtein: Map<LocalDate, Float>
): CalculatedStats {
    val activeDaysCals = weeklyCalories.values.filter { it > 0 }
    val activeDaysProtein = weeklyProtein.values.filter { it > 0f }

    val daysLogged = activeDaysCals.size
    val avgCals = if (activeDaysCals.isNotEmpty()) activeDaysCals.average().toInt() else 0
    val avgProtein = if (activeDaysProtein.isNotEmpty()) activeDaysProtein.average().toInt() else 0

    return CalculatedStats(daysLogged, avgCals, avgProtein)
}

@Composable
private fun WeeklyAchievementsCarousel(
    weeklyCalories: Map<LocalDate, Int>,
    weeklyProtein: Map<LocalDate, Float>,
    weeklyMacroPercentages: Map<String, Float>,
    weeklyAverageMicros: Map<String, Float>,
    onSeeAllClick: () -> Unit
) {
    val stats = remember(weeklyCalories, weeklyProtein) {
        calculateStats(weeklyCalories, weeklyProtein)
    }

    val unlockedAchievements = remember(stats, weeklyMacroPercentages, weeklyAverageMicros) {
        AchievementRepository.allAchievements.filter {
            it.condition(stats.daysLogged, stats.avgCals, stats.avgProtein, weeklyMacroPercentages, weeklyAverageMicros)
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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

@Composable
fun AllAchievementsScreen(
    navController: NavController,
    weeklyCalories: Map<LocalDate, Int>,
    weeklyProtein: Map<LocalDate, Float>,
    weeklyMacroPercentages: Map<String, Float>
) {
    val stats = remember(weeklyCalories, weeklyProtein) {
        calculateStats(weeklyCalories, weeklyProtein)
    }

    val weeklyMicros = emptyMap<String, Float>()
    val allBadges = AchievementRepository.allAchievements

    var selectedBadge by remember { mutableStateOf<Achievement?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                    val isUnlocked = badge.condition(stats.daysLogged, stats.avgCals, stats.avgProtein, weeklyMacroPercentages, weeklyMicros)
                    AchievementGridItem(
                        badge = badge,
                        isUnlocked = isUnlocked,
                        onClick = { selectedBadge = badge }
                    )
                }
            }
        }

        if (selectedBadge != null) {
            val isUnlocked = selectedBadge!!.condition(stats.daysLogged, stats.avgCals, stats.avgProtein, weeklyMacroPercentages, weeklyMicros)
            AchievementDetailDialog(
                badge = selectedBadge!!,
                isUnlocked = isUnlocked,
                onDismiss = { selectedBadge = null }
            )
        }
    }
}

@Composable
private fun AchievementGridItem(
    badge: Achievement,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(
                if (isUnlocked)
                    Brush.radialGradient(listOf(badge.color.copy(alpha = 0.4f), badge.color.copy(alpha = 0.1f)))
                else
                    SolidColor(Color.Gray.copy(alpha = 0.1f))
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge.emoji,
                fontSize = 32.sp,
                modifier = Modifier.graphicsLayer { alpha = if (isUnlocked) 1f else 0.3f }
            )
            Box(
                modifier = Modifier.fillMaxSize().padding(6.dp),
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
private fun AchievementDetailDialog(
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
                modifier = Modifier.fillMaxWidth().padding(24.dp),
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
                    modifier = Modifier.size(120.dp).clip(CircleShape).background(
                        if (isUnlocked)
                            Brush.radialGradient(listOf(badge.color.copy(alpha = 0.4f), badge.color.copy(alpha = 0.1f)))
                        else
                            SolidColor(Color.Gray.copy(alpha = 0.1f))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge.emoji,
                        fontSize = 60.sp,
                        modifier = Modifier.graphicsLayer { alpha = if (isUnlocked) 1f else 0.3f }
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

@Composable
private fun CategoryCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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