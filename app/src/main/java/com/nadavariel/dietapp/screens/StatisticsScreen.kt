package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.ui.stats.*
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import java.util.Locale

// Modern Color Palette
private val PrimaryGreen = Color(0xFF00C853)
private val AccentTeal = Color(0xFF00BFA5)
private val SoftBlue = Color(0xFF40C4FF)
private val WarmOrange = Color(0xFFFF6E40)
private val DeepPurple = Color(0xFF7C4DFF)
private val SunsetPink = Color(0xFFFF4081)
private val BackgroundGradient = listOf(Color(0xFFF8F9FA), Color(0xFFFFFFFF))
private val CardBackground = Color.White
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StatisticsScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    goalViewModel: GoalsViewModel = viewModel(),
    navController: NavController
) {
    val weeklyCalories by foodLogViewModel.weeklyCalories.collectAsState()
    val weeklyProtein by foodLogViewModel.weeklyProtein.collectAsState()
    val weeklyMacroPercentages by foodLogViewModel.weeklyMacroPercentages.collectAsState()
    val weeklyFiber by foodLogViewModel.weeklyFiber.collectAsState()
    val weeklySugar by foodLogViewModel.weeklySugar.collectAsState()
    val weeklySodium by foodLogViewModel.weeklySodium.collectAsState()
    val weeklyPotassium by foodLogViewModel.weeklyPotassium.collectAsState()
    val weeklyCalcium by foodLogViewModel.weeklyCalcium.collectAsState()
    val weeklyIron by foodLogViewModel.weeklyIron.collectAsState()
    val weeklyVitaminC by foodLogViewModel.weeklyVitaminC.collectAsState()

    val goals by goalViewModel.goals.collectAsState()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(BackgroundGradient))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Modern Header
            ModernHeader()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Stats
                item {
                    HeroStatsRow(
                        caloriesAvg = weeklyCalories.values.average().toInt(),
                        proteinAvg = weeklyProtein.values.average().toInt(),
                        macroBalance = weeklyMacroPercentages
                    )
                }

                // Category Cards
                item {
                    CategoryCard(
                        title = "Energy & Protein",
                        subtitle = "Daily calories and protein intake",
                        icon = Icons.Default.LocalFireDepartment,
                        color = WarmOrange,
                        onClick = { navController.navigate(NavRoutes.STATS_ENERGY) }
                    )
                }

                item {
                    CategoryCard(
                        title = "Macronutrients",
                        subtitle = "Protein, carbs, and fat balance",
                        icon = Icons.Default.PieChart,
                        color = PrimaryGreen,
                        onClick = { navController.navigate(NavRoutes.STATS_MACROS) }
                    )
                }

                item {
                    CategoryCard(
                        title = "Fiber & Sugar",
                        subtitle = "Carbohydrate quality metrics",
                        icon = Icons.Default.Spa,
                        color = AccentTeal,
                        onClick = { navController.navigate(NavRoutes.STATS_CARBS) }
                    )
                }

                item {
                    CategoryCard(
                        title = "Minerals",
                        subtitle = "Sodium, potassium, calcium, iron",
                        icon = Icons.Default.Science,
                        color = SoftBlue,
                        onClick = { navController.navigate(NavRoutes.STATS_MINERALS) }
                    )
                }

                item {
                    CategoryCard(
                        title = "Vitamins",
                        subtitle = "Essential vitamin intake",
                        icon = Icons.Default.Favorite,
                        color = SunsetPink,
                        onClick = { navController.navigate(NavRoutes.STATS_VITAMINS) }
                    )
                }

                // Bottom spacing
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

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
                color = TextPrimary
            )
            Text(
                text = "Your weekly nutrition summary",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun HeroStatsRow(
    caloriesAvg: Int,
    proteinAvg: Int,
    macroBalance: Map<String, Float>
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
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HeroStatItem(
                label = "Avg Calories",
                value = "$caloriesAvg",
                unit = "kcal",
                color = WarmOrange
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(Color(0xFFE0E0E0))
            )

            HeroStatItem(
                label = "Avg Protein",
                value = "$proteinAvg",
                unit = "g",
                color = PrimaryGreen
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(Color(0xFFE0E0E0))
            )

            HeroStatItem(
                label = "Balance",
                value = if (macroBalance.values.any { it > 0 }) "✓" else "-",
                unit = "",
                color = AccentTeal
            )
        }
    }
}

@Composable
private fun HeroStatItem(
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
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
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}