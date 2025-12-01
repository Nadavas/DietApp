package com.nadavariel.dietapp.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import androidx.core.graphics.withSave

class ColorPieChartRenderer(
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

        val data = mChart.data
        val dataSets = data.dataSets
        val yValueSum = data.yValueSum
        val drawEntryLabels = mChart.isDrawEntryLabelsEnabled

        var angle: Float
        var xIndex = 0

        c.withSave {

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

                    angle = if (xIndex == 0) 0f
                    else absoluteAngles[xIndex - 1] * phaseX

                    val sliceAngle = drawAngles[xIndex]
                    val sliceSpaceMiddleAngle = sliceSpace / (Utils.FDEG2RAD * radius)
                    val angleOffset = (sliceAngle - sliceSpaceMiddleAngle / 2f) / 2f
                    if (sliceAngle > sliceSpaceMiddleAngle) {
                        angle += angleOffset
                    }

                    val transformedAngle = rotationAngle + angle * phaseY
                    val value =
                        if (mChart.isUsePercentValuesEnabled) entry.y / yValueSum * 100f else entry.y
                    val formattedValue = formatter.getFormattedValue(value)
                    val entryLabel = entry.label

                    val sliceXBase =
                        kotlin.math.cos(transformedAngle * Utils.FDEG2RAD.toDouble()).toFloat()
                    val sliceYBase =
                        kotlin.math.sin(transformedAngle * Utils.FDEG2RAD.toDouble()).toFloat()

                    val drawXOutside =
                        drawEntryLabels && xValuePosition == PieDataSet.ValuePosition.OUTSIDE_SLICE
                    val drawYOutside =
                        drawValues && yValuePosition == PieDataSet.ValuePosition.OUTSIDE_SLICE
                    val drawXInside =
                        drawEntryLabels && xValuePosition == PieDataSet.ValuePosition.INSIDE_SLICE
                    val drawYInside =
                        drawValues && yValuePosition == PieDataSet.ValuePosition.INSIDE_SLICE

                    val specificColor = dataSet.getColor(j)
                    mRenderPaint.color = specificColor
                    mValuePaint.color = specificColor
                    customLabelPaint.color = specificColor

                    // ... inside the loop in drawValues ...

                    if (drawXOutside || drawYOutside) {
                        val valueLinePart1OffsetPercentage =
                            dataSet.valueLinePart1OffsetPercentage / 100f
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
                        val isHorizontalSlice =
                            (normAngle in 315.0..360.0) || (normAngle in 0.0..45.0) || (normAngle in 135.0..225.0)

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
                        } else {
                            // CASE B: Slice is Vertical (Top/Bottom) -> Make Line HORIZONTAL

                            // 1. Calculate the line end (pt2x)
                            pt2x = if (pt1x < center.x) {
                                pt1x - (radius * valueLineLength2) // Left
                            } else {
                                pt1x + (radius * valueLineLength2) // Right
                            }

                            // 2. FORCE CENTER ALIGNMENT (Keeps Fat and % aligned nicely)
                            mValuePaint.textAlign = Paint.Align.CENTER
                            customLabelPaint.textAlign = Paint.Align.CENTER

                            // 3. PUSH TEXT OUTWARDS (To fix overlap and stay at edge)
                            // We add a 'cushion' (30f) so the text starts *after* the line ends
                            labelX = if (pt1x < center.x) {
                                // Left Side: Move text slightly more to the left of the tip
                                pt2x - 40f
                            } else {
                                // Right Side: Move text slightly more to the right of the tip
                                pt2x + 40f
                            }

                            // 4. Reset Y to default (Vertical centering on the line)
                            labelY = pt2y
                        }

                        // 5. Draw the lines
                        if (dataSet.valueLineColor != Color.TRANSPARENT) {
                            drawLine(pt0x, pt0y, pt1x, pt1y, mRenderPaint) // Radial Part
                            drawLine(pt1x, pt1y, pt2x, pt2y, mRenderPaint) // The 90-degree Turn
                        }

                        // 6. Draw Text (Smart Positioning)
                        if (drawValues && drawYOutside) {
                            // Draw Value
                            drawValue(this, formattedValue, labelX, labelY, specificColor)
                        }

                        if (drawEntryLabels && drawXOutside) {
                            // If Vertical Line (Stacked text), offset the label
                            if (isHorizontalSlice) {
                                val offset = if (pt1y < center.y) -lineHeight else lineHeight
                                drawText(entryLabel, labelX, labelY + offset, customLabelPaint)
                            }
                            // If Horizontal Line (Side-by-side text), put label on top of value or beside
                            else {
                                // For horizontal lines, we simply stack label above value for cleanness
                                drawText(entryLabel, labelX, labelY - lineHeight, customLabelPaint)
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

                        if (drawValues && drawYInside) drawValue(
                            this,
                            formattedValue,
                            x,
                            y,
                            Color.WHITE
                        )

                        if (drawEntryLabels && drawXInside) {
                            drawText(entryLabel, x, y + lineHeight, customLabelPaint)
                        }
                    }

                    xIndex++
                }
                MPPointF.recycleInstance(iconsOffset)
            }
            MPPointF.recycleInstance(center)
        }
    }
}