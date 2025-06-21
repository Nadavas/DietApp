package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StatisticsScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Statistics Page",
                style = MaterialTheme.typography.headlineSmall
            )
            // Add the Bar Chart
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp), // Increased height for better visibility
                factory = { context ->
                    BarChart(context).apply {
                        // Configure the chart
                        description.isEnabled = false
                        setTouchEnabled(true)
                        isDragEnabled = true
                        setScaleEnabled(true)
                        setPinchZoom(true)

                        // Set a subtle background
                        setBackgroundColor(android.graphics.Color.argb(20, 240, 248, 255)) // Light cyan background

                        // X-axis configuration
                        xAxis.apply {
                            setDrawGridLines(false)
                            labelCount = 7
                            valueFormatter = object : ValueFormatter() {
                                override fun getFormattedValue(value: Float): String {
                                    return value.toInt().toString()
                                }
                            }
                            setCenterAxisLabels(false)
                            granularity = 1f
                            textColor = android.graphics.Color.DKGRAY // Darker text for contrast
                            textSize = 12f
                        }

                        // Y-axis configuration
                        axisLeft.apply {
                            setDrawGridLines(true)
                            axisMinimum = 0f
                            axisMaximum = 7f
                            textColor = android.graphics.Color.DKGRAY // Darker text for contrast
                            textSize = 12f
                            setDrawAxisLine(true) // Add axis line
                        }
                        axisRight.isEnabled = false

                        // Data for bar chart [0, 1, 2, 3, 4, 5, 6]
                        val entries = listOf(
                            BarEntry(0f, 0f),
                            BarEntry(1f, 1f),
                            BarEntry(2f, 2f),
                            BarEntry(3f, 3f),
                            BarEntry(4f, 4f),
                            BarEntry(5f, 5f),
                            BarEntry(6f, 6f)
                        )
                        val dataSet = BarDataSet(entries, "Values").apply {
                            // Gradient fill for bars
                            val gradientColors = intArrayOf(
                                android.graphics.Color.rgb(70, 130, 180), // SteelBlue
                                android.graphics.Color.rgb(135, 206, 235)  // SkyBlue
                            )
                            gradientColors.forEach { color ->
                                setColor(color)
                            }
                            valueTextSize = 14f
                            setDrawValues(true)
                            valueTextColor = android.graphics.Color.WHITE // White text for contrast
                            barShadowColor = android.graphics.Color.argb(50, 0, 0, 0) // Subtle shadow
                        }
                        data = BarData(dataSet).apply {
                            barWidth = 0.6f // Slightly wider bars
                        }

                        // Animate the chart
                        animateY(1000) // Smooth animation over 1 second
                        invalidate()
                    }
                }
            )
        }
    }
}