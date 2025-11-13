package com.nadavariel.dietapp.ui.stats

import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.nadavariel.dietapp.util.RoundedBarChartRenderer
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor // <-- IMPORT ADDED
import kotlin.math.log10 // <-- IMPORT ADDED
import kotlin.math.pow  // <-- IMPORT ADDED

// Consistent Color Definitions
private val HealthyGreenCompose = androidx.compose.ui.graphics.Color(0xFF4CAF50)
private val AccentGreenCompose = androidx.compose.ui.graphics.Color(0xFF81C784)
private val GridLineColor = Color.parseColor("#EEEEEE")
private val AxisTextColor = Color.parseColor("#616161")
private val TargetLineColor = Color.parseColor("#FF6E40") // Warm orange for target
private val TargetBackgroundColor = Color.parseColor("#FFF3E0") // Light orange background

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BeautifulBarChart(
    weeklyData: Map<LocalDate, Int>,
    target: Int?,
    label: String
) {
    val primaryColor = HealthyGreenCompose.toArgb()
    val accentColor = AccentGreenCompose.toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()

    val sortedDates = weeklyData.keys.sorted()
    val dayLabels = sortedDates.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    val barEntries = sortedDates.mapIndexed { index, date ->
        BarEntry(index.toFloat(), weeklyData[date]?.toFloat() ?: 0f)
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        factory = { context ->
            BarChart(context).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                val renderer = RoundedBarChartRenderer(this, animator, viewPortHandler)
                renderer.setCornerRadius(15f)
                this.renderer = renderer
                setExtraOffsets(5f, 15f, 5f, 15f)
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
                    textColor = AxisTextColor
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index in dayLabels.indices) dayLabels[index] else ""
                        }
                    }
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    enableGridDashedLine(8f, 8f, 0f)
                    gridColor = GridLineColor
                    setLabelCount(5, false)
                    axisMinimum = 0f
                    setDrawAxisLine(false)
                    textColor = AxisTextColor
                    textSize = 11f

                    val maxBar = (barEntries.maxOfOrNull { it.y } ?: 0f)
                    val targetValue = target?.toFloat() ?: 0f

                    // --- THIS IS THE FIX ---
                    // Calculate a "clean" maximum value based on the data and label
                    axisMaximum = calculateRoundedAxisMax(maxBar, targetValue) // <-- Label no longer needed
                    // --- END OF FIX ---

                    target?.let {
                        // Create a beautiful target line with enhanced styling
                        val targetLine = LimitLine(it.toFloat(), "").apply {
                            lineWidth = 3f
                            lineColor = TargetLineColor
                            enableDashedLine(15f, 10f, 0f)

                            // Custom label styling
                            textColor = TargetLineColor
                            textSize = 11f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
                        }

                        removeAllLimitLines()
                        addLimitLine(targetLine)
                        setDrawLimitLinesBehindData(false)
                    } ?: run {
                        removeAllLimitLines()
                    }
                }
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            val todayIndex = sortedDates.indexOf(LocalDate.now())

            val dataSet = BarDataSet(barEntries, "Data").apply {
                colors = barEntries.indices.map { i ->
                    if (i == todayIndex) accentColor else primaryColor
                }
                setDrawValues(true)
                valueTextColor = onSurfaceColor
                valueTextSize = 10f
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return if (value > 0) value.toInt().toString() else ""
                    }
                }
                highLightColor = primaryColor
                highLightAlpha = 80
            }

            chart.data = BarData(dataSet).apply { barWidth = 0.5f }

            // Add custom target label if target exists
            target?.let {
                chart.axisLeft.limitLines.firstOrNull()?.label = "Goal: $it $label"
            }

            chart.invalidate()
            chart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseOutCubic)
        }
    )
}

@Composable
fun BeautifulPieChart(
    data: Map<String, Float>
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val centerTextColor = HealthyGreenCompose.toArgb()

    val macroColors = remember {
        listOf(
            HealthyGreenCompose.toArgb(),
            Color.parseColor("#42A5F5"),
            Color.parseColor("#FFA726")
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
            else -> macroColors.random()
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        factory = { context ->
            PieChart(context).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                description.isEnabled = false
                setBackgroundColor(Color.TRANSPARENT)
                setExtraOffsets(5f, 5f, 35f, 5f)
                setUsePercentValues(true)

                legend.apply {
                    verticalAlignment = Legend.LegendVerticalAlignment.CENTER
                    horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                    orientation = Legend.LegendOrientation.VERTICAL
                    setDrawInside(false)
                    yEntrySpace = 6f
                    xOffset = 5f
                    textColor = AxisTextColor
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    isWordWrapEnabled = true
                }

                isDrawHoleEnabled = true
                setHoleColor(Color.TRANSPARENT)
                holeRadius = 55f
                transparentCircleRadius = 60f
                setDrawCenterText(true)
                centerText = "Macros"
                setCenterTextSize(16f)
                setCenterTextTypeface(Typeface.DEFAULT_BOLD)
                setCenterTextColor(centerTextColor)

                isRotationEnabled = false
                isHighlightPerTapEnabled = true
                setDrawEntryLabels(false)
            }
        },
        update = { chart ->
            val dataSet = PieDataSet(entries, "").apply {
                this.colors = entryColors
                sliceSpace = 2f
                valueTextColor = Color.WHITE
                valueTextSize = 11f
                valueTypeface = Typeface.DEFAULT_BOLD
                yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE
                xValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE

                valueLinePart1Length = 0f
                valueLinePart2Length = 0f
                valueLineColor = Color.TRANSPARENT
            }

            val pieData = PieData(dataSet).apply {
                setValueFormatter(object : ValueFormatter() {
                    private val format = DecimalFormat("##0")
                    override fun getFormattedValue(value: Float): String = "${format.format(value)}%"
                    override fun getPieLabel(value: Float, pieEntry: PieEntry?): String = ""
                })
                setDrawValues(true)
            }

            chart.legend.setCustom(entries.mapIndexed { index, entry ->
                LegendEntry(
                    "${entry.label} ${DecimalFormat("##0").format(entry.value)}%",
                    Legend.LegendForm.CIRCLE,
                    10f,
                    Float.NaN,
                    null,
                    entryColors[index]
                )
            })

            chart.data = pieData
            chart.invalidate()
            chart.animateY(900, com.github.mikephil.charting.animation.Easing.EaseInOutQuad)
        }
    )
}

// --- NEW HELPER FUNCTION ---
/**
 * Calculates a "clean" maximum value for the Y-axis.
 * This rounds the highest visible value (data or target) up to the
 * nearest "nice" number (e.g., 42 -> 50, 1800 -> 2000, 3100 -> 4000).
 */
private fun calculateRoundedAxisMax(maxValue: Float, targetValue: Float): Float {
    // 1. Find the highest value we need to display (including padding)
    val highestValue = maxOf(maxValue * 1.1f, targetValue * 1.2f, 10f) // Use 10f as a minimum

    // 2. Find the "magnitude" (the nearest power of 10 below the number)
    // e.g., 3600 -> 1000
    // e.g., 42 -> 10
    // e.g., 18 -> 10
    val magnitude = 10.0.pow(floor(log10(highestValue.toDouble()))).toFloat()

    // 3. Round the highest value UP to the nearest magnitude
    // e.g., (3600 / 1000) -> 3.6 -> ceil -> 4.0 -> 4.0 * 1000 = 4000
    // e.g., (42 / 10) -> 4.2 -> ceil -> 5.0 -> 5.0 * 10 = 50
    // e.g., (18 / 10) -> 1.8 -> ceil -> 2.0 -> 2.0 * 10 = 20
    val newMax = ceil(highestValue / magnitude) * magnitude

    return newMax
}