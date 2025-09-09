package com.nadavariel.dietapp.screens

import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.nadavariel.dietapp.util.RoundedBarChartRenderer
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StatisticsScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    @Suppress("UNUSED_PARAMETER")
    navController: NavController
) {
    // Get state from viewmodel
    val weeklyCalories by foodLogViewModel.weeklyCalories.collectAsState()
    val caloriesByTimeOfDay by foodLogViewModel.caloriesByTimeOfDay.collectAsState()
    val foodLogViewModel: FoodLogViewModel = viewModel()

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Statistics") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Bar chart of weekly calories
            Text(
                text = "Your weekly calorie intake",
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (weeklyCalories.isEmpty() || weeklyCalories.values.all { it == 0 }) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Log your meals to see your weekly progress here!",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = onSurfaceColor
                        )
                    }
                } else {
                    BeautifulBarChart(
                        weeklyCalories = weeklyCalories,
                        primaryColor = primaryColor.toArgb()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Pie chart of calories by time of day
            Text(
                text = "Calorie distribution by time of day",
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (caloriesByTimeOfDay.isEmpty() || caloriesByTimeOfDay.values.all { it == 0f }) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Log meals throughout the day to see your habits here!",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = onSurfaceColor
                        )
                    }
                } else {
                    BeautifulPieChart(caloriesByTimeOfDay, primaryColor.toArgb())
                }
            }
        }
    }
}

// Composable for the bar chart
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BeautifulBarChart(
    weeklyCalories: Map<LocalDate, Int>,
    primaryColor: Int
) {
    val sortedDates = weeklyCalories.keys.sorted()
    val dayLabels = sortedDates.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    val barEntries = sortedDates.mapIndexed { index, date ->
        BarEntry(index.toFloat(), weeklyCalories[date]?.toFloat() ?: 0f)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            BarChart(context).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                val renderer = RoundedBarChartRenderer(this, animator, viewPortHandler)
                renderer.setCornerRadius(25f)
                this.renderer = renderer

                setExtraOffsets(0f, 0f, 0f, 20f)
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
                    textColor = Color.GRAY
                    textSize = 11f
                    yOffset = 12f
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
                    gridColor = Color.LTGRAY
                    setLabelCount(6, false)
                    axisMinimum = 0f
                    setDrawAxisLine(false)
                    textColor = Color.GRAY
                    textSize = 11f
                }

                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val todayIndex = sortedDates.indexOf(LocalDate.now())

            val dataSet = BarDataSet(barEntries, "Weekly Calories").apply {
                val alpha = (0.6f * 255).toInt()
                val startColor = Color.argb(
                    alpha,
                    Color.red(primaryColor),
                    Color.green(primaryColor),
                    Color.blue(primaryColor)
                )

                colors = List(barEntries.size) { i ->
                    if (i == todayIndex) Color.argb(255, 255, 127, 80)
                    else primaryColor
                }

                setGradientColor(startColor, primaryColor)
                setDrawValues(true)
                valueTextColor = Color.DKGRAY
                valueTextSize = 11f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value > 0) value.toInt().toString() else ""
                    }
                }

                highLightColor = primaryColor
                highLightAlpha = 100
            }

            chart.data = BarData(dataSet).apply {
                barWidth = 0.6f
            }

            chart.invalidate()
            chart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseOutBack)
        }
    )
}

// Composable for the pie chart
@Composable
fun BeautifulPieChart(
    data: Map<String, Float>,
    primaryColor: Int
) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(270.dp), // Restrict height to avoid overflow
        factory = { context ->
            PieChart(context).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                description.isEnabled = false
                setBackgroundColor(Color.TRANSPARENT)

                setExtraOffsets(10f, 10f, 10f, 10f)
                setUsePercentValues(true)

                legend.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(Color.TRANSPARENT)
                holeRadius = 50f
                transparentCircleRadius = 60f
                setDrawCenterText(true)
                centerText = "Meals by\nTime"
                setCenterTextSize(12f)
                setCenterTextColor(Color.DKGRAY)

                isRotationEnabled = false
                isHighlightPerTapEnabled = false
                setDrawEntryLabels(false)
            }
        },
        update = { chart ->
            val entries = data.entries
                .filter { it.value > 0 }
                .map { PieEntry(it.value, it.key) }

            val alpha = (0.9f * 255).toInt()
            val baseColor = Color.argb(
                alpha,
                Color.red(primaryColor),
                Color.green(primaryColor),
                Color.blue(primaryColor)
            )
            val colorsList = listOf(
                ColorUtils.blendARGB(baseColor, Color.BLACK, 0.01f), // Slightly darker
                ColorUtils.blendARGB(baseColor, Color.BLACK, 0.3f), // Medium darker
                ColorUtils.blendARGB(baseColor, Color.BLACK, 0.6f)  // Darkest
            )

            val dataSet = PieDataSet(entries, "Time of Day").apply {
                this.colors = colorsList
                sliceSpace = 3f
                valueTextColor = Color.DKGRAY
                valueTextSize = 12f
                yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                valueLinePart1Length = 0.5f
                valueLinePart2Length = 0.5f
                valueLineColor = Color.LTGRAY
            }

            val pieData = PieData(dataSet).apply {
                setValueFormatter(object : ValueFormatter() {
                    private val format = DecimalFormat("###,###,##0")
                    override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                        val label = pieEntry?.label ?: ""
                        return "$label ${format.format(value)}%"
                    }

                    override fun getFormattedValue(value: Float): String {
                        return "${format.format(value)}%"
                    }
                })
            }

            chart.data = pieData
            chart.invalidate()
            chart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseOutBack)
        }
    )
}

