package com.nadavariel.dietapp.util

import android.graphics.*
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.buffer.BarBuffer
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler

class RoundedBarChartRenderer(
    chart: BarDataProvider,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : BarChartRenderer(chart, animator, viewPortHandler) {

    private var cornerRadius = 0f

    fun setCornerRadius(radius: Float) {
        this.cornerRadius = radius
    }

    override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
        val trans = mChart.getTransformer(dataSet.axisDependency)

        mBarBorderPaint.color = dataSet.barBorderColor
        mBarBorderPaint.strokeWidth = dataSet.barBorderWidth

        val drawBorder = dataSet.barBorderWidth > 0f
        val phaseX = mAnimator.phaseX
        val phaseY = mAnimator.phaseY

        if (mChart.isDrawBarShadowEnabled) {
            mShadowPaint.color = dataSet.barShadowColor
            val barData = mChart.barData
            val barWidth = barData.barWidth
            val barWidthHalf = barWidth / 2.0f
            var i = 0
            val count = Math.min(
                Math.ceil((dataSet.entryCount.toFloat() * phaseX).toDouble()).toInt(),
                dataSet.entryCount
            )
            while (i < count) {
                val e = dataSet.getEntryForIndex(i)
                val x = e.x
                mBarRect.left = x - barWidthHalf
                mBarRect.right = x + barWidthHalf
                trans.rectValueToPixel(mBarRect)
                if (!mViewPortHandler.isInBoundsLeft(mBarRect.right)) {
                    i++
                    continue
                }
                if (!mViewPortHandler.isInBoundsRight(mBarRect.left)) break

                mBarRect.top = mViewPortHandler.contentTop()
                mBarRect.bottom = mViewPortHandler.contentBottom()
                c.drawRoundRect(mBarRect, cornerRadius, cornerRadius, mShadowPaint)
                i++
            }
        }

        val buffer: BarBuffer = mBarBuffers[index]
        buffer.setPhases(phaseX, phaseY)
        buffer.setDataSet(index)
        buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
        buffer.setBarWidth(mChart.barData.barWidth)
        buffer.feed(dataSet)
        trans.pointValuesToPixel(buffer.buffer)

        val isSingleColor = dataSet.colors.size == 1
        if (isSingleColor) mRenderPaint.color = dataSet.color

        mRenderPaint.setShadowLayer(8f, 0f, 4f, Color.argb(40, 0, 0, 0))

        var j = 0
        while (j < buffer.size()) {
            if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) {
                j += 4
                continue
            }
            if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j])) break

            if (!isSingleColor) {
                mRenderPaint.color = dataSet.getColor(j / 4)
            }

            if (dataSet.gradientColor != null) {
                val gradientColor = dataSet.gradientColor
                mRenderPaint.shader = LinearGradient(
                    buffer.buffer[j],
                    buffer.buffer[j + 3],
                    buffer.buffer[j],
                    buffer.buffer[j + 1],
                    gradientColor.startColor,
                    gradientColor.endColor,
                    Shader.TileMode.MIRROR
                )
            }

            val rect = RectF(
                buffer.buffer[j],
                buffer.buffer[j + 1],
                buffer.buffer[j + 2],
                buffer.buffer[j + 3]
            )
            val path = Path().apply {
                val radii = floatArrayOf(
                    cornerRadius, cornerRadius, cornerRadius, cornerRadius,
                    0f, 0f, 0f, 0f
                )
                addRoundRect(rect, radii, Path.Direction.CW)
            }

            c.drawPath(path, mRenderPaint)

            if (drawBorder) {
                val borderRect = RectF(
                    buffer.buffer[j],
                    buffer.buffer[j + 1],
                    buffer.buffer[j + 2],
                    buffer.buffer[j + 3]
                )
                c.drawRoundRect(borderRect, cornerRadius, cornerRadius, mBarBorderPaint)
            }

            j += 4
        }

        mRenderPaint.clearShadowLayer()
    }
}
