package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.nadavariel.dietapp.ui.stats.*
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import java.util.Locale

// Reuse color palette
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

// ===== ENERGY & PROTEIN DETAIL SCREEN =====
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyDetailScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    goalViewModel: GoalsViewModel = viewModel(),
    navController: NavController
) {
    val weeklyCalories by foodLogViewModel.weeklyCalories.collectAsState()
    val weeklyProtein by foodLogViewModel.weeklyProtein.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    DetailScreenScaffold(
        title = "Energy & Protein",
        icon = Icons.Default.LocalFireDepartment,
        color = WarmOrange,
        navController = navController
    ) {
        item {
            DetailStatCard(
                title = "Daily Calories",
                weeklyData = weeklyCalories,
                target = goals.getOrNull(0)?.value?.toIntOrNull(),
                label = "kcal",
                color = WarmOrange
            )
        }

        item {
            DetailStatCard(
                title = "Daily Protein",
                weeklyData = weeklyProtein,
                target = goals.getOrNull(1)?.value?.toIntOrNull(),
                label = "g",
                color = PrimaryGreen
            )
        }

        item {
            InsightCard(
                insights = buildList {
                    val avgCalories = weeklyCalories.values.average().toInt()
                    val avgProtein = weeklyProtein.values.average().toInt()
                    val calorieTarget = goals.getOrNull(0)?.value?.toIntOrNull()
                    val proteinTarget = goals.getOrNull(1)?.value?.toIntOrNull()

                    if (calorieTarget != null) {
                        val diff = ((avgCalories.toFloat() / calorieTarget) * 100).toInt() - 100
                        add(
                            if (diff >= 0) "You're consuming ${diff}% above your calorie target"
                            else "You're consuming ${-diff}% below your calorie target"
                        )
                    }

                    if (proteinTarget != null && avgProtein >= proteinTarget) {
                        add("Great job hitting your protein goals! 💪")
                    } else if (proteinTarget != null) {
                        add("Try adding more protein-rich foods to meet your goal")
                    }
                }
            )
        }
    }
}

// ===== MACROS DETAIL SCREEN =====
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacrosDetailScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    navController: NavController
) {
    val weeklyMacroPercentages by foodLogViewModel.weeklyMacroPercentages.collectAsState()
    val weeklyProtein by foodLogViewModel.weeklyProtein.collectAsState()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    DetailScreenScaffold(
        title = "Macronutrients",
        icon = Icons.Default.PieChart,
        color = PrimaryGreen,
        navController = navController
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Weekly Balance",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val hasMacroData = weeklyMacroPercentages.values.any { it > 0f }
                    if (!hasMacroData) {
                        EmptyChartState(message = "Log meals to see your macro distribution")
                    } else {
                        BeautifulPieChart(data = weeklyMacroPercentages)
                    }
                }
            }
        }

        item {
            MacroBreakdownCard(macroPercentages = weeklyMacroPercentages)
        }

        item {
            InsightCard(
                insights = listOf(
                    "Balanced macros support overall health and energy",
                    "Aim for 10-35% protein, 45-65% carbs, 20-35% fat"
                )
            )
        }
    }
}

// ===== CARBS DETAIL SCREEN =====
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarbsDetailScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    goalViewModel: GoalsViewModel = viewModel(),
    navController: NavController
) {
    val weeklyFiber by foodLogViewModel.weeklyFiber.collectAsState()
    val weeklySugar by foodLogViewModel.weeklySugar.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    DetailScreenScaffold(
        title = "Fiber & Sugar",
        icon = Icons.Default.Spa,
        color = AccentTeal,
        navController = navController
    ) {
        item {
            DetailStatCard(
                title = "Daily Fiber",
                weeklyData = weeklyFiber,
                target = goals.getOrNull(2)?.value?.toIntOrNull(),
                label = "g",
                color = AccentTeal
            )
        }

        item {
            DetailStatCard(
                title = "Daily Sugar",
                weeklyData = weeklySugar,
                target = goals.getOrNull(3)?.value?.toIntOrNull(),
                label = "g",
                color = SunsetPink
            )
        }

        item {
            InsightCard(
                insights = listOf(
                    "Fiber aids digestion and helps maintain stable blood sugar",
                    "Limit added sugars to less than 10% of daily calories"
                )
            )
        }
    }
}

// ===== MINERALS DETAIL SCREEN =====
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineralsDetailScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    goalViewModel: GoalsViewModel = viewModel(),
    navController: NavController
) {
    val weeklySodium by foodLogViewModel.weeklySodium.collectAsState()
    val weeklyPotassium by foodLogViewModel.weeklyPotassium.collectAsState()
    val weeklyCalcium by foodLogViewModel.weeklyCalcium.collectAsState()
    val weeklyIron by foodLogViewModel.weeklyIron.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    DetailScreenScaffold(
        title = "Minerals",
        icon = Icons.Default.Science,
        color = SoftBlue,
        navController = navController
    ) {
        item {
            DetailStatCard(
                title = "Sodium",
                weeklyData = weeklySodium,
                target = goals.getOrNull(4)?.value?.toIntOrNull(),
                label = "mg",
                color = SoftBlue
            )
        }

        item {
            DetailStatCard(
                title = "Potassium",
                weeklyData = weeklyPotassium,
                target = goals.getOrNull(5)?.value?.toIntOrNull(),
                label = "mg",
                color = DeepPurple
            )
        }

        item {
            DetailStatCard(
                title = "Calcium",
                weeklyData = weeklyCalcium,
                target = goals.getOrNull(6)?.value?.toIntOrNull(),
                label = "mg",
                color = AccentTeal
            )
        }

        item {
            DetailStatCard(
                title = "Iron",
                weeklyData = weeklyIron,
                target = goals.getOrNull(7)?.value?.toIntOrNull(),
                label = "mg",
                color = WarmOrange
            )
        }

        item {
            InsightCard(
                insights = listOf(
                    "Minerals support bone health, muscle function, and metabolism",
                    "Balance sodium and potassium for healthy blood pressure"
                )
            )
        }
    }
}

// ===== VITAMINS DETAIL SCREEN =====
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitaminsDetailScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    goalViewModel: GoalsViewModel = viewModel(),
    navController: NavController
) {
    val weeklyVitaminC by foodLogViewModel.weeklyVitaminC.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    DetailScreenScaffold(
        title = "Vitamins",
        icon = Icons.Default.Favorite,
        color = SunsetPink,
        navController = navController
    ) {
        item {
            DetailStatCard(
                title = "Vitamin C",
                weeklyData = weeklyVitaminC,
                target = goals.getOrNull(8)?.value?.toIntOrNull(),
                label = "mg",
                color = SunsetPink
            )
        }

        item {
            InsightCard(
                insights = listOf(
                    "Vitamin C supports immune function and collagen production",
                    "Citrus fruits, berries, and peppers are excellent sources"
                )
            )
        }
    }
}

// ===== SHARED COMPONENTS FOR DETAIL SCREENS =====

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreenScaffold(
    title: String,
    icon: ImageVector,
    color: Color,
    navController: NavController,
    content: LazyListScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
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
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(BackgroundGradient))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DetailStatCard(
    title: String,
    weeklyData: Map<java.time.LocalDate, Int>,
    target: Int?,
    label: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // **FIX: 'average' is defined HERE, in the parent scope**
            val average = if (weeklyData.isEmpty()) 0 else weeklyData.values.average().toInt()

            // Title and Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    // **FIX: 'average' was removed from here**

                    Text(
                        text = "$average $label", // This now reads from the parent scope
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "daily average",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                target?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Goal",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "$it $label",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        // **FIX: This now works because 'average' is visible from the parent scope**
                        val percentage = ((average.toFloat() / it) * 100).toInt()

                        Text(
                            text = "$percentage%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (percentage >= 80) PrimaryGreen else WarmOrange,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Chart
            if (weeklyData.isEmpty() || weeklyData.values.all { it == 0 }) {
                EmptyChartState(message = "Log meals to see your progress")
            } else {
                BeautifulBarChart(
                    weeklyData = weeklyData,
                    target = target,
                    label = label
                )
            }
        }
    }
}

@Composable
private fun MacroBreakdownCard(macroPercentages: Map<String, Float>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Breakdown",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            MacroRow("Protein", macroPercentages["Protein"] ?: 0f, PrimaryGreen)
            HorizontalDivider(color = Color(0xFFEEEEEE))
            MacroRow("Carbs", macroPercentages["Carbs"] ?: 0f, SoftBlue)
            HorizontalDivider(color = Color(0xFFEEEEEE))
            MacroRow("Fat", macroPercentages["Fat"] ?: 0f, WarmOrange)
        }
    }
}

@Composable
private fun MacroRow(name: String, percentage: Float, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
        }
        Text(
            text = "${percentage.toInt()}%",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun InsightCard(insights: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Insights",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            insights.forEach { insight ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "•",
                        fontSize = 14.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = insight,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}