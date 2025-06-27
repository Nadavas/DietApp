package com.nadavariel.dietapp.screens

import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StatisticsScreen(
    navController: NavController,
    foodLogViewModel: FoodLogViewModel = viewModel()
) {
    val weeklyCalories by foodLogViewModel.weeklyCalories.collectAsState()
    val caloriesByTimeOfDay by foodLogViewModel.caloriesByTimeOfDay.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Statistics") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
                    .height(300.dp),
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
                    .height(300.dp),
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
                    BeautifulPieChart(caloriesByTimeOfDay)
                }
            }
        }
    }
}

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

@Composable
fun BeautifulPieChart(data: Map<String, Float>) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PieChart(context).apply {
                setUsePercentValues(true)
                setDrawEntryLabels(true)
                description.isEnabled = false
                isRotationEnabled = true
                setDrawCenterText(true)
                centerText = "Meals by Time"
                setEntryLabelColor(Color.DKGRAY)
                setEntryLabelTextSize(12f)
                setHoleColor(Color.TRANSPARENT)
                legend.isEnabled = true
            }
        },
        update = { chart ->
            val entries = data.entries
                .filter { it.value > 0 }
                .map { PieEntry(it.value, it.key) }

            val colors = listOf(
                "#FFA726".toColorInt(), // Morning
                "#66BB6A".toColorInt(), // Afternoon
                "#42A5F5".toColorInt(), // Evening
                "#AB47BC".toColorInt()  // Night (if you ever add it)
            )

            val dataSet = PieDataSet(entries, "Time of Day").apply {
                this.colors = colors
                valueTextSize = 14f
                valueTextColor = Color.DKGRAY
            }

            val pieData = PieData(dataSet).apply {
                setValueFormatter(object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return "${value.toInt()}%"
                    }
                })
            }

            chart.data = pieData
            chart.invalidate()
            chart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseOutBack)
        }
    )
}

