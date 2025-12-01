package com.nadavariel.dietapp.ui.stats

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.util.RoundedBarChartRenderer
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

import android.graphics.Canvas
import android.graphics.Paint
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BeautifulBarChart(
    weeklyData: Map<LocalDate, Int>,
    target: Int?,
    label: String,
    barColor: androidx.compose.ui.graphics.Color = AppTheme.colors.primaryGreen,
    goalColor: androidx.compose.ui.graphics.Color = AppTheme.colors.warmOrange
) {
    // 2. CHANGED: Use the passed barColor
    val primaryColor = barColor.toArgb()
    // 3. CHANGED: Derive accent color (for "Today") from barColor
    val accentColor = barColor.copy(alpha = 0.7f).toArgb()
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val gridLineColor = AppTheme.colors.lightGreyText.copy(alpha = 0.3f).toArgb()
    val targetLineColor = goalColor.toArgb()
    val axisTextColor = AppTheme.colors.axisText.toArgb()

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
                    textColor = axisTextColor
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
                    //enableGridDashedLine(8f, 8f, 0f)
                    gridLineWidth = 1.5f
                    gridColor = gridLineColor
                    setLabelCount(5, false)
                    axisMinimum = 0f
                    setDrawAxisLine(false)
                    textColor = axisTextColor
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
                            lineColor = targetLineColor
                            enableDashedLine(15f, 10f, 0f)

                            // Custom label styling
                            textColor = targetLineColor
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
    val proteinColor = AppTheme.colors.primaryGreen.toArgb()
    val carbsColor = AppTheme.colors.activeLifestyle.toArgb()
    val fatColor = AppTheme.colors.disclaimerIcon.toArgb()
    val centerTextColor = proteinColor

    val entries = data.entries
        .filter { it.value > 0 }
        .map { PieEntry(it.value, it.key) }

    val entryColors = entries.map { entry ->
        when (entry.label) {
            "Protein" -> proteinColor
            "Carbs" -> carbsColor
            "Fat" -> fatColor
            else -> proteinColor
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp),
        factory = { context ->
            PieChart(context).apply {
                // 1. Initialize Custom Renderer
                val myRenderer = SpecificColorPieRenderer(this, animator, viewPortHandler)

                // 2. Configure the Custom Paint (Text Size & Font) here
                myRenderer.customLabelPaint.textSize = Utils.convertDpToPixel(12f)
                myRenderer.customLabelPaint.typeface = Typeface.DEFAULT_BOLD

                // 3. Attach Renderer
                renderer = myRenderer

                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                description.isEnabled = false
                setBackgroundColor(Color.TRANSPARENT)
                setExtraOffsets(20f, 20f, 20f, 10f)
                setUsePercentValues(true)
                legend.isEnabled = false

                isDrawHoleEnabled = true
                setHoleColor(Color.TRANSPARENT)
                holeRadius = 55f
                transparentCircleRadius = 60f

                setDrawCenterText(true)
                centerText = "Macros"
                setCenterTextSize(18f)
                setCenterTextTypeface(Typeface.DEFAULT_BOLD)
                setCenterTextColor(centerTextColor)

                isRotationEnabled = false
                isHighlightPerTapEnabled = false

                // We still enable this flag so the loop runs, but the renderer handles the actual drawing
                setDrawEntryLabels(true)
            }
        },
        update = { chart ->
            val dataSet = PieDataSet(entries, "").apply {
                this.colors = entryColors
                sliceSpace = 2f

                valueLinePart1OffsetPercentage = 80f
                valueLinePart1Length = 0.2f
                valueLinePart2Length = 0.2f
                valueLineWidth = 1.5f
                valueLineColor = Color.BLACK // Ignored by custom renderer

                yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
                xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

                valueTextSize = 14f
                valueTypeface = Typeface.DEFAULT_BOLD
            }

            val pieData = PieData(dataSet).apply {
                setValueFormatter(object : ValueFormatter() {
                    private val format = DecimalFormat("##0")
                    override fun getFormattedValue(value: Float): String = "${format.format(value)}%"
                })
                setDrawValues(true)
            }

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



class SpecificColorPieRenderer(
    chart: PieChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : PieChartRenderer(chart, animator, viewPortHandler) {

    val customLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER // KEY FIX: Center text to save horizontal space
        textSize = Utils.convertDpToPixel(13f)
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun drawValues(c: Canvas) {
        val center = mChart.centerCircleBox
        val radius = mChart.radius
        var rotationAngle = mChart.rotationAngle
        val drawAngles = mChart.drawAngles
        val absoluteAngles = mChart.absoluteAngles
        val phaseX = mAnimator.phaseX
        val phaseY = mAnimator.phaseY

        // We don't really use hole radius for the line calculation in this specific logic,
        // but we keep the variable for safety.
        val holeRadiusPercent = mChart.holeRadius / 100f

        val data = mChart.data
        val dataSets = data.dataSets
        val yValueSum = data.yValueSum
        val drawEntryLabels = mChart.isDrawEntryLabelsEnabled

        var angle: Float
        var xIndex = 0

        c.save()

        for (i in dataSets.indices) {
            val dataSet = dataSets[i]
            val drawValues = dataSet.isDrawValuesEnabled

            if (!drawValues && !drawEntryLabels) continue

            val xValuePosition = dataSet.xValuePosition
            val yValuePosition = dataSet.yValuePosition

            applyValueTextStyle(dataSet)

            val lineHeight = Utils.calcTextHeight(mValuePaint, "Q") + Utils.convertDpToPixel(4f)
            val formatter = dataSet.valueFormatter
            val entryCount = dataSet.entryCount

            mValuePaint.color = Color.WHITE
            mRenderPaint.strokeWidth = Utils.convertDpToPixel(dataSet.valueLineWidth)

            val sliceSpace = getSliceSpace(dataSet)
            val iconsOffset = MPPointF.getInstance(dataSet.iconsOffset)
            iconsOffset.x = Utils.convertDpToPixel(iconsOffset.x)
            iconsOffset.y = Utils.convertDpToPixel(iconsOffset.y)

            for (j in 0 until entryCount) {
                val entry = dataSet.getEntryForIndex(j)

                if (xIndex == 0) angle = 0f
                else angle = absoluteAngles[xIndex - 1] * phaseX

                val sliceAngle = drawAngles[xIndex]
                val sliceSpaceMiddleAngle = sliceSpace / (Utils.FDEG2RAD * radius)
                val angleOffset = (sliceAngle - sliceSpaceMiddleAngle / 2f) / 2f
                if (sliceAngle > sliceSpaceMiddleAngle) {
                    angle += angleOffset
                }

                val transformedAngle = rotationAngle + angle * phaseY
                val value = if (mChart.isUsePercentValuesEnabled) entry.y / yValueSum * 100f else entry.y
                val formattedValue = formatter.getFormattedValue(value)
                val entryLabel = entry.label

                val sliceXBase = kotlin.math.cos(transformedAngle * Utils.FDEG2RAD.toDouble()).toFloat()
                val sliceYBase = kotlin.math.sin(transformedAngle * Utils.FDEG2RAD.toDouble()).toFloat()

                val drawXOutside = drawEntryLabels && xValuePosition == PieDataSet.ValuePosition.OUTSIDE_SLICE
                val drawYOutside = drawValues && yValuePosition == PieDataSet.ValuePosition.OUTSIDE_SLICE
                val drawXInside = drawEntryLabels && xValuePosition == PieDataSet.ValuePosition.INSIDE_SLICE
                val drawYInside = drawValues && yValuePosition == PieDataSet.ValuePosition.INSIDE_SLICE

                val specificColor = dataSet.getColor(j)
                mRenderPaint.color = specificColor
                mValuePaint.color = specificColor
                customLabelPaint.color = specificColor

                // ... inside the loop in drawValues ...

                if (drawXOutside || drawYOutside) {
                    val valueLinePart1OffsetPercentage = dataSet.valueLinePart1OffsetPercentage / 100f
                    val valueLineLength2 = dataSet.valueLinePart2Length

                    // 1. Calculate Radial Start (pt0) and Elbow (pt1)
                    val polyline2Radius = dataSet.valueLinePart1Length * radius + radius
                    val pt1x = polyline2Radius * sliceXBase + center.x
                    val pt1y = polyline2Radius * sliceYBase + center.y

                    val line1Radius = radius * valueLinePart1OffsetPercentage
                    val pt0x = line1Radius * sliceXBase + center.x
                    val pt0y = line1Radius * sliceYBase + center.y

                    // 2. Initialize End Point (pt2)
                    var pt2x = pt1x
                    var pt2y = pt1y

                    var labelX = pt2x
                    var labelY = pt2y

                    // 3. Normalize angle to 0-360 to detect quadrant
                    var normAngle = transformedAngle % 360.0
                    if (normAngle < 0) normAngle += 360.0

                    // 4. Check if the slice is "Horizontal" (Left/Right) or "Vertical" (Top/Bottom)
                    // Horizontal Slices: 315-45 (Right) and 135-225 (Left)
                    val isHorizontalSlice = (normAngle in 315.0..360.0) || (normAngle in 0.0..45.0) || (normAngle in 135.0..225.0)

                    if (isHorizontalSlice) {
                        // CASE A: Slice is Horizontal (Left/Right) -> Make Line VERTICAL (90 deg turn)

                        // Decide to go UP or DOWN based on position relative to center Y
                        if (pt1y < center.y) {
                            pt2y = pt1y - (radius * valueLineLength2) // Go UP
                            labelY = pt2y - 10f // Padding Above
                        } else {
                            pt2y = pt1y + (radius * valueLineLength2) // Go DOWN
                            labelY = pt2y + lineHeight + 5f // Padding Below
                        }

                        // Text Logic: Center Aligned (Stacked)
                        mValuePaint.textAlign = Paint.Align.CENTER
                        customLabelPaint.textAlign = Paint.Align.CENTER

                        labelX = pt2x // Center on the vertical line
                    }
                    else {
                        // CASE B: Slice is Vertical (Top/Bottom) -> Make Line HORIZONTAL

                        // 1. Calculate the line end (pt2x)
                        if (pt1x < center.x) {
                            pt2x = pt1x - (radius * valueLineLength2) // Left
                        } else {
                            pt2x = pt1x + (radius * valueLineLength2) // Right
                        }

                        // 2. FORCE CENTER ALIGNMENT (Keeps Fat and % aligned nicely)
                        mValuePaint.textAlign = Paint.Align.CENTER
                        customLabelPaint.textAlign = Paint.Align.CENTER

                        // 3. PUSH TEXT OUTWARDS (To fix overlap and stay at edge)
                        // We add a 'cushion' (30f) so the text starts *after* the line ends
                        if (pt1x < center.x) {
                            // Left Side: Move text slightly more to the left of the tip
                            labelX = pt2x - 40f
                        } else {
                            // Right Side: Move text slightly more to the right of the tip
                            labelX = pt2x + 40f
                        }

                        // 4. Reset Y to default (Vertical centering on the line)
                        labelY = pt2y
                    }

                    // 5. Draw the lines
                    if (dataSet.valueLineColor != Color.TRANSPARENT) {
                        c.drawLine(pt0x, pt0y, pt1x, pt1y, mRenderPaint) // Radial Part
                        c.drawLine(pt1x, pt1y, pt2x, pt2y, mRenderPaint) // The 90-degree Turn
                    }

                    // 6. Draw Text (Smart Positioning)
                    if (drawValues && drawYOutside) {
                        // Draw Value
                        drawValue(c, formattedValue, labelX, labelY, specificColor)
                    }

                    if (drawEntryLabels && drawXOutside) {
                        // If Vertical Line (Stacked text), offset the label
                        if (isHorizontalSlice) {
                            val offset = if (pt1y < center.y) -lineHeight else lineHeight
                            c.drawText(entryLabel, labelX, labelY + offset, customLabelPaint)
                        }
                        // If Horizontal Line (Side-by-side text), put label on top of value or beside
                        else {
                            // For horizontal lines, we simply stack label above value for cleanness
                            c.drawText(entryLabel, labelX, labelY - lineHeight, customLabelPaint)
                        }
                    }
                }

                // Handle Inside Cases (Standard)
                if (drawXInside || drawYInside) {
                    val x = (radius * 0.5f * sliceXBase) + center.x
                    val y = (radius * 0.5f * sliceYBase) + center.y

                    mValuePaint.color = Color.WHITE
                    customLabelPaint.color = Color.WHITE
                    customLabelPaint.textAlign = Paint.Align.CENTER

                    if (drawValues && drawYInside) drawValue(c, formattedValue, x, y, Color.WHITE)

                    if (drawEntryLabels && drawXInside) {
                        c.drawText(entryLabel, x, y + lineHeight, customLabelPaint)
                    }
                }

                xIndex++
            }
            MPPointF.recycleInstance(iconsOffset)
        }
        MPPointF.recycleInstance(center)
        c.restore()
    }
}