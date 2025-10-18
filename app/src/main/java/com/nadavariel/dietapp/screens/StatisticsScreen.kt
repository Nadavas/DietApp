package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    goalViewModel: GoalsViewModel = viewModel(),
    @Suppress("UNUSED_PARAMETER") navController: NavController
) {
    val weeklyCalories by foodLogViewModel.weeklyCalories.collectAsState()
    val weeklyProtein by foodLogViewModel.weeklyProtein.collectAsState()
    val yesterdayMacroPercentages by foodLogViewModel.yesterdayMacroPercentages.collectAsState()
    val weeklyFiber by foodLogViewModel.weeklyFiber.collectAsState()
    val weeklySugar by foodLogViewModel.weeklySugar.collectAsState()
    val weeklySodium by foodLogViewModel.weeklySodium.collectAsState()
    val weeklyPotassium by foodLogViewModel.weeklyPotassium.collectAsState()
    val weeklyCalcium by foodLogViewModel.weeklyCalcium.collectAsState()
    val weeklyIron by foodLogViewModel.weeklyIron.collectAsState()
    val weeklyVitaminC by foodLogViewModel.weeklyVitaminC.collectAsState()
    val graphPreferences by foodLogViewModel.graphPreferences.collectAsState()

    val goals by goalViewModel.goals.collectAsState()
    val calorieTarget = goals.getOrNull(0)?.value?.toIntOrNull()
    val proteinTarget = goals.getOrNull(1)?.value?.toIntOrNull()
    val fiberTarget = goals.getOrNull(2)?.value?.toIntOrNull()
    val sugarTarget = goals.getOrNull(3)?.value?.toIntOrNull()
    val sodiumTarget = goals.getOrNull(4)?.value?.toIntOrNull()
    val potassiumTarget = goals.getOrNull(5)?.value?.toIntOrNull()
    val calciumTarget = goals.getOrNull(6)?.value?.toIntOrNull()
    val ironTarget = goals.getOrNull(7)?.value?.toIntOrNull()
    val vitaminCTarget = goals.getOrNull(8)?.value?.toIntOrNull()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

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
            ModernHeader(onSettingsClick = { showBottomSheet = true })

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Hero Card - Macros Overview
                item {
                    MacroHeroCard(
                        macroPercentages = yesterdayMacroPercentages
                    )
                }

                // Energy Metrics Section
                item {
                    SectionDivider(title = "Energy & Protein", icon = Icons.Default.LocalFireDepartment)
                }

                item {
                    CompactStatCard(
                        title = "Calories",
                        weeklyData = weeklyCalories,
                        target = calorieTarget,
                        label = "kcal",
                        color = WarmOrange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    CompactStatCard(
                        title = "Protein",
                        weeklyData = weeklyProtein,
                        target = proteinTarget,
                        label = "g",
                        color = PrimaryGreen,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Fiber & Sugar Section
                item {
                    SectionDivider(title = "Carbohydrates", icon = Icons.Default.Spa)
                }

                item {
                    CompactStatCard(
                        title = "Fiber",
                        weeklyData = weeklyFiber,
                        target = fiberTarget,
                        label = "g",
                        color = AccentTeal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    CompactStatCard(
                        title = "Sugar",
                        weeklyData = weeklySugar,
                        target = sugarTarget,
                        label = "g",
                        color = SunsetPink,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Minerals Section
                item {
                    SectionDivider(title = "Minerals", icon = Icons.Default.Science)
                }

                item {
                    MineralGridCard(
                        sodium = Triple(weeklySodium, sodiumTarget, "mg"),
                        potassium = Triple(weeklyPotassium, potassiumTarget, "mg"),
                        calcium = Triple(weeklyCalcium, calciumTarget, "mg"),
                        iron = Triple(weeklyIron, ironTarget, "mg")
                    )
                }

                // Vitamins Section
                item {
                    SectionDivider(title = "Vitamins", icon = Icons.Default.Favorite)
                }

                item {
                    VitaminCard(
                        vitaminC = Triple(weeklyVitaminC, vitaminCTarget, "mg")
                    )
                }

                // Bottom spacing
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            GraphSortBottomSheet(
                preferences = graphPreferences,
                onPreferencesUpdated = { newPreferences ->
                    foodLogViewModel.saveGraphPreferences(newPreferences)
                },
                onDismiss = { showBottomSheet = false }
            )
        }
    }
}

@Composable
private fun ModernHeader(onSettingsClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Your Progress",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Weekly nutrition insights",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF5F5F5))
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun MacroHeroCard(macroPercentages: Map<String, Float>) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Macro Balance",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Icon(
                    Icons.Default.PieChart,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "Yesterday's distribution",
                fontSize = 13.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            val hasMacroData = macroPercentages.values.any { it > 0f }
            if (!hasMacroData) {
                EmptyChartState(message = "Log meals to see your macro distribution")
            } else {
                BeautifulPieChart(data = macroPercentages)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CompactStatCard(
    title: String,
    weeklyData: Map<java.time.LocalDate, Int>,
    target: Int?,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val average = if (weeklyData.isEmpty()) 0 else weeklyData.values.average().toInt()
            Text(
                text = "$average $label",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )

            Text(
                text = "avg/day",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (weeklyData.isEmpty() || weeklyData.values.all { it == 0 }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MineralGridCard(
    sodium: Triple<Map<java.time.LocalDate, Int>, Int?, String>,
    potassium: Triple<Map<java.time.LocalDate, Int>, Int?, String>,
    calcium: Triple<Map<java.time.LocalDate, Int>, Int?, String>,
    iron: Triple<Map<java.time.LocalDate, Int>, Int?, String>
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MineralRow("Sodium", sodium.first, sodium.second, sodium.third, SoftBlue)
            HorizontalDivider(color = Color(0xFFEEEEEE))
            MineralRow("Potassium", potassium.first, potassium.second, potassium.third, DeepPurple)
            HorizontalDivider(color = Color(0xFFEEEEEE))
            MineralRow("Calcium", calcium.first, calcium.second, calcium.third, AccentTeal)
            HorizontalDivider(color = Color(0xFFEEEEEE))
            MineralRow("Iron", iron.first, iron.second, iron.third, WarmOrange)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun MineralRow(
    name: String,
    weeklyData: Map<java.time.LocalDate, Int>,
    target: Int?,
    label: String,
    color: Color
) {
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            Column {
                Text(
                    text = name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                val average = if (weeklyData.isEmpty()) 0 else weeklyData.values.average().toInt()
                Text(
                    text = "$average $label avg",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        target?.let {
            val average = if (weeklyData.isEmpty()) 0 else weeklyData.values.average().toInt()
            val percentage = ((average.toFloat() / it) * 100).toInt().coerceIn(0, 200)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$percentage%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (percentage >= 80) PrimaryGreen else TextSecondary
                )
                Icon(
                    if (percentage >= 100) Icons.Default.CheckCircle else Icons.Default.Circle,
                    contentDescription = null,
                    tint = if (percentage >= 100) PrimaryGreen else Color(0xFFE0E0E0),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun VitaminCard(
    vitaminC: Triple<Map<java.time.LocalDate, Int>, Int?, String>
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vitamin C",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(WarmOrange)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (vitaminC.first.isEmpty() || vitaminC.first.values.all { it == 0 }) {
                EmptyChartState(message = "Log meals to track vitamin C intake")
            } else {
                BeautifulBarChart(
                    weeklyData = vitaminC.first,
                    target = vitaminC.second,
                    label = vitaminC.third
                )
            }
        }
    }
}

@Composable
private fun SectionDivider(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = PrimaryGreen,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title.uppercase(Locale.ROOT),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Color(0xFFE0E0E0))
        )
    }
}