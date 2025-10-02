package com.nadavariel.dietapp.screens

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.nadavariel.dietapp.util.RoundedBarChartRenderer
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
// 🌟 Import the new data class
import com.nadavariel.dietapp.viewmodel.GraphPreference

import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    goalViewModel: GoalsViewModel = viewModel(),
    @Suppress("UNUSED_PARAMETER") navController: NavController
) {
    // --- STATE AND DATA (FoodLogViewModel) ---
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

    // 🌟 GRAPH PREFERENCE STATE NOW FROM FoodLogViewModel
    val graphPreferences by foodLogViewModel.graphPreferences.collectAsState()

    // --- GOALS (GoalsViewModel) ---
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
        // Preference fetch is included in refreshStatistics or init of FoodLogViewModel
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
                    onEditClicked = { showBottomSheet = true } // Action to show sheet
                )
            }

            // 🌟 RENDER CHARTS BASED ON SAVED PREFERENCES
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

    // 🌟 MODAL BOTTOM SHEET
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            GraphSortBottomSheet(
                preferences = graphPreferences,
                onPreferencesUpdated = { newPreferences ->
                    // 🌟 ACTION NOW CALLS FoodLogViewModel
                    foodLogViewModel.saveGraphPreferences(newPreferences)
                },
                onDismiss = { showBottomSheet = false }
            )
        }
    }
}

// --------------------------------------------------------------------------------
// |                       HELPER COMPOSABLES (Unmodified from previous step)     |
// --------------------------------------------------------------------------------

@Composable
fun StatisticsHeader(onEditClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Your Progress",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "A visual summary of your weekly diet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onEditClicked) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Edit Graphs Order and Visibility",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ChartItem(
    title: String,
    icon: ImageVector,
    weeklyData: Map<LocalDate, Int>,
    target: Int?,
    label: String
) {
    StatisticCard(title = title, icon = icon) {
        if (weeklyData.isEmpty() || weeklyData.values.all { it == 0 }) {
            EmptyChartState(message = "Log your meals to see your progress here!")
        } else {
            BeautifulBarChart(
                weeklyData = weeklyData,
                primaryColor = MaterialTheme.colorScheme.primary.toArgb(),
                target = target,
                label = label
            )
        }
    }
}

@Composable
fun PieChartItem(
    title: String,
    data: Map<String, Float>,
    primaryColor: Int,
    onSurfaceColor: Int
) {
    StatisticCard(title = title, icon = Icons.Default.PieChart) {
        val hasMacroData = data.values.any { it > 0f }
        if (!hasMacroData) {
            EmptyChartState(message = "Log meals with nutritional data from yesterday to see your macro distribution!")
        } else {
            BeautifulPieChart(
                data = data,
                primaryColor = primaryColor,
                onSurfaceColor = onSurfaceColor
            )
        }
    }
}

@Composable
fun StatisticCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                content()
            }
        }
    }
}

@Composable
fun EmptyChartState(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp)
    )
}

// --------------------------------------------------------------------------------
// |                SORTING BOTTOM SHEET IMPLEMENTATION (Unmodified)              |
// --------------------------------------------------------------------------------

@Composable
fun GraphSortBottomSheet(
    preferences: List<GraphPreference>,
    onPreferencesUpdated: (List<GraphPreference>) -> Unit,
    onDismiss: () -> Unit
) {
    var mutablePreferences by remember { mutableStateOf(preferences.sortedBy { it.order }.toMutableList()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Edit Graph Order & Visibility",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            items(mutablePreferences, key = { it.id }) { pref ->
                val currentIndex = mutablePreferences.indexOf(pref)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Switch(
                            checked = pref.isVisible,
                            onCheckedChange = { isChecked ->
                                mutablePreferences = mutablePreferences.map {
                                    if (it.id == pref.id) it.copy(isVisible = isChecked) else it
                                }.toMutableList()
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = pref.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (pref.isMacro) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }

                    // Order Control Arrows
                    Row {
                        IconButton(
                            onClick = {
                                if (currentIndex > 0) {
                                    val newIndex = currentIndex - 1
                                    mutablePreferences.add(newIndex, mutablePreferences.removeAt(currentIndex))
                                }
                            },
                            enabled = currentIndex > 0
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                        }

                        IconButton(
                            onClick = {
                                if (currentIndex < mutablePreferences.size - 1) {
                                    val newIndex = currentIndex + 1
                                    mutablePreferences.add(newIndex + 1, mutablePreferences.removeAt(currentIndex))
                                }
                            },
                            enabled = currentIndex < mutablePreferences.size - 1
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = {
                val finalPreferences = mutablePreferences.mapIndexed { index, pref ->
                    pref.copy(order = index)
                }

                onPreferencesUpdated(finalPreferences)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save Preferences")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}


// --------------------------------------------------------------------------------
// |                CHART COMPOSABLES (Unmodified from previous step)             |
// --------------------------------------------------------------------------------

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BeautifulBarChart(
    weeklyData: Map<LocalDate, Int>,
    primaryColor: Int,
    target: Int?,
    label: String
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val limitLineColor = MaterialTheme.colorScheme.error.toArgb()
    val accentColor = MaterialTheme.colorScheme.tertiary.toArgb()

    val sortedDates = weeklyData.keys.sorted()
    val dayLabels = sortedDates.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    val barEntries = sortedDates.mapIndexed { index, date ->
        BarEntry(index.toFloat(), weeklyData[date]?.toFloat() ?: 0f)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            BarChart(context).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                val renderer = RoundedBarChartRenderer(this, animator, viewPortHandler)
                renderer.setCornerRadius(25f)
                this.renderer = renderer
                setExtraOffsets(10f, 10f, 10f, 20f)

                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)
                setDrawBorders(false)
                setBackgroundColor(Color.TRANSPARENT)

                setTouchEnabled(true)
                setPinchZoom(false)
                isDoubleTapToZoomEnabled = false
                isHighlightPerTapEnabled = true

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    setDrawAxisLine(false)
                    granularity = 1f
                    textColor = onSurfaceVariantColor
                    textSize = 12f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index in dayLabels.indices) dayLabels[index] else ""
                        }
                    }
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    enableGridDashedLine(10f, 10f, 0f)
                    gridColor = onSurfaceVariantColor
                    setLabelCount(5, true)
                    axisMinimum = 0f
                    setDrawAxisLine(false)
                    textColor = onSurfaceVariantColor
                    textSize = 12f

                    val maxBar = (barEntries.maxOfOrNull { it.y } ?: 0f)
                    val targetValue = target?.toFloat() ?: 0f

                    axisMaximum = maxOf(maxBar, targetValue) * 1.15f

                    target?.let {
                        val targetLine = LimitLine(it.toFloat(), "Target: $it $label").apply {
                            lineWidth = 2f
                            lineColor = limitLineColor
                            textColor = limitLineColor
                            textSize = 12f
                            enableDashedLine(10f, 10f, 0f)
                            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                        }
                        removeAllLimitLines()
                        addLimitLine(targetLine)
                    } ?: run {
                        removeAllLimitLines()
                    }
                }

                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val todayIndex = sortedDates.indexOf(LocalDate.now())

            val dataSet = BarDataSet(barEntries, "Weekly Data").apply {
                colors = barEntries.indices.map { i ->
                    if (i == todayIndex) accentColor else primaryColor
                }

                setDrawValues(true)
                valueTextColor = onSurfaceColor
                valueTextSize = 11f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value > 0) value.toInt().toString() else ""
                    }
                }

                highLightColor = primaryColor
                highLightAlpha = 100
            }

            chart.data = BarData(dataSet).apply { barWidth = 0.6f }
            chart.invalidate()
            chart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseOutCubic)
        }
    )
}

@Composable
fun BeautifulPieChart(
    data: Map<String, Float>,
    primaryColor: Int,
    onSurfaceColor: Int
) {
    val macroColors = remember {
        listOf(
            androidx.compose.ui.graphics.Color(0xFF4CAF50).toArgb(), // Protein (Green)
            androidx.compose.ui.graphics.Color(0xFF2196F3).toArgb(), // Carbs (Blue)
            androidx.compose.ui.graphics.Color(0xFFFF9800).toArgb(), // Fat (Orange)
        )
    }

    val entries = data.entries
        .filter { it.value > 0 }
        .map { PieEntry(it.value, it.key) }

    val entryColors = entries.map { entry ->
        when (entry.label) {
            "Protein" -> macroColors[0]
            "Carbs" -> macroColors[1]
            "Fat" -> macroColors[2]
            else -> primaryColor // Fallback
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PieChart(context).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                description.isEnabled = false
                setBackgroundColor(Color.TRANSPARENT)

                setExtraOffsets(0f, 0f, 35f, 0f)

                setUsePercentValues(true)

                legend.isEnabled = true
                legend.apply {
                    verticalAlignment = Legend.LegendVerticalAlignment.TOP
                    horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                    orientation = Legend.LegendOrientation.VERTICAL
                    setDrawInside(false)
                    yEntrySpace = 5f
                    xOffset = 10f
                    textColor = onSurfaceColor
                    textSize = 14f
                }

                isDrawHoleEnabled = true
                setHoleColor(Color.TRANSPARENT)
                holeRadius = 60f
                transparentCircleRadius = 65f
                setDrawCenterText(true)
                centerText = "Macros"
                setCenterTextSize(14f)
                setCenterTextColor(primaryColor)

                isRotationEnabled = false
                isHighlightPerTapEnabled = false
                setDrawEntryLabels(false)
            }
        },
        update = { chart ->
            val dataSet = PieDataSet(entries, "Macronutrients").apply {
                this.colors = entryColors
                sliceSpace = 3f
                valueTextColor = onSurfaceColor
                valueTextSize = 12f
                valueTypeface = Typeface.DEFAULT_BOLD

                yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
                xValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
                valueLinePart1Length = 0f
                valueLinePart2Length = 0f
            }

            val pieData = PieData(dataSet).apply {
                setValueFormatter(object : ValueFormatter() {
                    private val format = DecimalFormat("###,##0.0")

                    override fun getFormattedValue(value: Float): String {
                        return "${format.format(value)}%"
                    }

                    override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                        return getFormattedValue(value)
                    }
                })
            }

            chart.legend.setCustom(entries.mapIndexed { index, entry ->
                LegendEntry(
                    entry.label,
                    com.github.mikephil.charting.components.Legend.LegendForm.SQUARE,
                    10f,
                    2f,
                    null,
                    entryColors[index]
                )
            })

            chart.data = pieData
            chart.invalidate()
            chart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseOutCubic)
        }
    )
}