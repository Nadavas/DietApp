package com.nadavariel.dietapp.ui.stats

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.nadavariel.dietapp.util.RoundedBarChartRenderer
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

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