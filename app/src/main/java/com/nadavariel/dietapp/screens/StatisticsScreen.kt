package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import com.nadavariel.dietapp.ui.stats.StatisticsHeader
import com.nadavariel.dietapp.ui.stats.ChartItem
import com.nadavariel.dietapp.ui.stats.PieChartItem
import com.nadavariel.dietapp.ui.stats.GraphSortBottomSheet

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    goalViewModel: GoalsViewModel = viewModel(),
    @Suppress("UNUSED_PARAMETER") navController: NavController
) {
    // --- STATE AND DATA ---
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

    // --- GOALS ---
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

    // --- UI STATE ---
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    // --- UI ---
    val screenBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            MaterialTheme.colorScheme.background
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBrush)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                StatisticsHeader(
                    onEditClicked = { showBottomSheet = true }
                )
            }

            // RENDER CHARTS BASED ON SAVED PREFERENCES
            items(graphPreferences.filter { it.isVisible }.sortedBy { it.order }, key = { it.id }) { pref ->
                when (pref.id) {
                    "calories" -> ChartItem(
                        title = pref.title,
                        icon = Icons.Default.LocalFireDepartment,
                        weeklyData = weeklyCalories,
                        target = calorieTarget,
                        label = "kcal"
                    )
                    "protein" -> ChartItem(
                        title = pref.title,
                        icon = Icons.Default.FitnessCenter,
                        weeklyData = weeklyProtein,
                        target = proteinTarget,
                        label = "g"
                    )
                    "macros_pie" -> PieChartItem(
                        title = pref.title,
                        data = yesterdayMacroPercentages,
                        primaryColor = primaryColor.toArgb(),
                        onSurfaceColor = onSurfaceColor.toArgb()
                    )
                    "fiber" -> ChartItem(
                        title = pref.title,
                        icon = Icons.Default.Favorite,
                        weeklyData = weeklyFiber,
                        target = fiberTarget,
                        label = "g"
                    )
                    "sugar" -> ChartItem(
                        title = pref.title,
                        icon = Icons.Default.WaterDrop,
                        weeklyData = weeklySugar,
                        target = sugarTarget,
                        label = "g"
                    )
                    "sodium" -> ChartItem(
                        title = pref.title,
                        icon = Icons.Default.WaterDrop,
                        weeklyData = weeklySodium,
                        target = sodiumTarget,
                        label = "mg"
                    )
                    "potassium" -> ChartItem(
                        title = pref.title,
                        icon = Icons.Default.Favorite,
                        weeklyData = weeklyPotassium,
                        target = potassiumTarget,
                        label = "mg"
                    )
                    "calcium" -> ChartItem(
                        title = pref.title,
                        icon = Icons.Default.AcUnit,
                        weeklyData = weeklyCalcium,
                        target = calciumTarget,
                        label = "mg"
                    )
                    "iron" -> ChartItem(
                        title = pref.title,
                        icon = Icons.Default.AcUnit,
                        weeklyData = weeklyIron,
                        target = ironTarget,
                        label = "mg"
                    )
                    "vitamin_c" -> ChartItem(
                        title = pref.title,
                        icon = Icons.Default.LocalHospital,
                        weeklyData = weeklyVitaminC,
                        target = vitaminCTarget,
                        label = "mg"
                    )
                }
            }
        }
    }

    // MODAL BOTTOM SHEET
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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