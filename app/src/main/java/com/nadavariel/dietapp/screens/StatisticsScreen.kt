package com.nadavariel.dietapp.screens

import android.graphics.Color
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
    // --- STATE AND DATA (LOGIC UNCHANGED) ---
    val weeklyCalories by foodLogViewModel.weeklyCalories.collectAsState()
    val caloriesByTimeOfDay by foodLogViewModel.caloriesByTimeOfDay.collectAsState()
    val weeklyProtein by foodLogViewModel.weeklyProtein.collectAsState()
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

            // --- PIE CHART CARD ---
            item {
                StatisticCard(
                    title = "Calorie Distribution",
                    icon = Icons.Default.PieChart
                ) {
                    if (caloriesByTimeOfDay.isEmpty() || caloriesByTimeOfDay.values.all { it == 0f }) {
                        EmptyChartState(message = "Log meals throughout the day to see your habits!")
                    } else {
                        BeautifulPieChart(
                            data = caloriesByTimeOfDay,
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
// |                       NEW HELPER COMPOSABLES FOR CLEAN UI                    |
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
// |                CHART COMPOSABLES (FIXED COLOR ACCESS)                        |
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
    val accentColor = MaterialTheme.colorScheme.tertiary.toArgb() // ✅ moved here at top-level

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
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            PieChart(context).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                description.isEnabled = false
                setBackgroundColor(Color.TRANSPARENT)

                setExtraOffsets(5f, 5f, 5f, 5f)
                setUsePercentValues(true)

                legend.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(Color.TRANSPARENT)
                holeRadius = 60f
                transparentCircleRadius = 65f
                setDrawCenterText(true)
                centerText = "Meal Habits"
                setCenterTextSize(14f)
                setCenterTextColor(primaryColor)

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
                ColorUtils.blendARGB(baseColor, Color.BLACK, 0.01f),
                ColorUtils.blendARGB(baseColor, Color.BLACK, 0.3f),
                ColorUtils.blendARGB(baseColor, Color.BLACK, 0.6f)
            )

            val dataSet = PieDataSet(entries, "Time of Day").apply {
                this.colors = colorsList
                sliceSpace = 3f
                valueTextColor = onSurfaceColor
                valueTextSize = 12f
                yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                valueLinePart1Length = 0.5f
                valueLinePart2Length = 0.5f
                valueLineColor = onSurfaceColor
            }

            val pieData = PieData(dataSet).apply {
                setValueFormatter(object : ValueFormatter() {
                    private val format = DecimalFormat("###,##0.0")
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
            chart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseOutCubic)
        }
    )
}
