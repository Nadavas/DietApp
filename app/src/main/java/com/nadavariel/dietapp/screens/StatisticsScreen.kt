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
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel

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
private fun HeroStatsRow(
    caloriesAvg: Int,
    proteinAvg: Int,
    macroBalance: Map<String, Float>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.cardBackground),
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
                color = AppTheme.colors.warmOrange
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(AppTheme.colors.statsBackground)
            )

            HeroStatItem(
                label = "Avg Protein",
                value = "$proteinAvg",
                unit = "g",
                color = AppTheme.colors.statsGreen
            )

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp)
                    .background(AppTheme.colors.statsBackground)
            )

            HeroStatItem(
                label = "Balance",
                value = if (macroBalance.values.any { it > 0 }) "✓" else "-",
                unit = "",
                color = AppTheme.colors.accentTeal
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
            color = AppTheme.colors.textSecondary,
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
                    color = AppTheme.colors.textSecondary,
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