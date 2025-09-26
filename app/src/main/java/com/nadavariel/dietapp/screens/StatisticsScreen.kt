package com.nadavariel.dietapp.screens

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.Legend // 🌟 NEW IMPORT
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.nadavariel.dietapp.util.RoundedBarChartRenderer
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
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
    val goals by goalViewModel.goals.collectAsState()

    val calorieTarget = goals.firstOrNull()?.value?.toIntOrNull()
    val proteinTarget = goals.getOrNull(1)?.value?.toIntOrNull()

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

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
                StatisticsHeader()
            }

            // --- CALORIES CARD ---
            item {
                StatisticCard(
                    title = "Weekly Calorie Intake",
                    icon = Icons.Default.LocalFireDepartment
                ) {
                    if (weeklyCalories.isEmpty() || weeklyCalories.values.all { it == 0 }) {
                        EmptyChartState(message = "Log your meals to see your weekly progress here!")
                    } else {
                        BeautifulBarChart(
                            weeklyData = weeklyCalories,
                            primaryColor = primaryColor.toArgb(),
                            target = calorieTarget,
                            label = "kcal"
                        )
                    }
                }
            }

            // --- PROTEIN CARD ---
            item {
                StatisticCard(
                    title = "Weekly Protein Intake",
                    icon = Icons.Default.FitnessCenter
                ) {
                    if (weeklyProtein.isEmpty() || weeklyProtein.values.all { it == 0 }) {
                        EmptyChartState(message = "Log your meals to see your protein progress here!")
                    } else {
                        BeautifulBarChart(
                            weeklyData = weeklyProtein,
                            primaryColor = primaryColor.toArgb(),
                            target = proteinTarget,
                            label = "g"
                        )
                    }
                }
            }

            // --- PIE CHART CARD (Updated for Macronutrients) ---
            item {
                StatisticCard(
                    title = "Yesterday's Macronutrient Distribution", // Changed title
                    icon = Icons.Default.PieChart
                ) {
                    val hasMacroData = yesterdayMacroPercentages.values.any { it > 0f }
                    if (!hasMacroData) {
                        EmptyChartState(message = "Log meals with nutritional data from yesterday to see your macro distribution!") // Changed message
                    } else {
                        BeautifulPieChart(
                            data = yesterdayMacroPercentages, // Use new macro data
                            primaryColor = primaryColor.toArgb(),
                            onSurfaceColor = onSurfaceColor.toArgb()
                        )
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------------------------------
// |                       HELPER COMPOSABLES                                     |
// --------------------------------------------------------------------------------

@Composable
fun StatisticsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
// |                CHART COMPOSABLES                                             |
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
    // Macro colors definition
    val macroColors = remember {
        listOf(
            androidx.compose.ui.graphics.Color(0xFF4CAF50).toArgb(), // Protein (Green)
            androidx.compose.ui.graphics.Color(0xFF2196F3).toArgb(), // Carbs (Blue)
            androidx.compose.ui.graphics.Color(0xFFFF9800).toArgb(), // Fat (Orange)
        )
    }

    // Map entries for easy use in chart and legend
    val entries = data.entries
        .filter { it.value > 0 }
        .map { PieEntry(it.value, it.key) }

    // Map entry labels to the defined macro color for the chart and legend
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

                // Re-introducing a larger right offset to make space for the vertical legend.
                // Left offset remains small, as labels are now gone from the left.
                setExtraOffsets(0f, 0f, 35f, 0f)

                setUsePercentValues(true)

                // 🌟 FIX: Enable Legend
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
                // Disable drawing labels and lines outside the pie
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

                // 🌟 FIX: Do not draw external value lines or labels
                yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
                xValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
                valueLinePart1Length = 0f
                valueLinePart2Length = 0f
            }

            val pieData = PieData(dataSet).apply {
                // 🌟 FIX: New ValueFormatter to show ONLY the percentage inside the slice.
                // The Legend will display the Macro Name.
                setValueFormatter(object : ValueFormatter() {
                    private val format = DecimalFormat("###,##0.0")

                    // We only want the percentage value, without the label, inside the slice
                    override fun getFormattedValue(value: Float): String {
                        return "${format.format(value)}%"
                    }

                    // The macro name is handled by the Legend, so this is now irrelevant
                    override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                        return getFormattedValue(value)
                    }
                })
            }

            // 🌟 FIX: Update legend entries to map correctly to data and colors
            chart.legend.setCustom(entries.mapIndexed { index, entry ->
                LegendEntry(
                    entry.label,
                    // CORRECTED ENUM NAME: Use Legend.LegendForm.SQUARE
                    com.github.mikephil.charting.components.Legend.LegendForm.SQUARE,
                    10f, // size
                    2f, // line length (irrelevant for square)
                    null, // text label form
                    entryColors[index]
                )
            })

            chart.data = pieData
            chart.invalidate()
            chart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseOutCubic)
        }
    )
}