package com.nadavariel.dietapp.screens

import android.graphics.Color
import android.os.Build
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.nadavariel.dietapp.util.RoundedBarChartRenderer
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StatisticsScreen(
    navController: NavController,
    foodLogViewModel: FoodLogViewModel = viewModel()
) {
    val weeklyCalories by foodLogViewModel.weeklyCalories.collectAsState()

    // Define modern colors from the Material theme
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weekly Calorie Summary") },
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
            // Place the chart inside a Card for a modern, elevated look
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp), // A bit more height for aesthetics
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (weeklyCalories.isEmpty() || weeklyCalories.values.all { it == 0 }) {
                    // Improved empty state message
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
                    // Display the chart with the new, beautiful styling
                    BeautifulBarChart(
                        weeklyCalories = weeklyCalories,
                        primaryColor = primaryColor.toArgb(),
                        surfaceColor = surfaceColor.toArgb()
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BeautifulBarChart(
    weeklyCalories: Map<LocalDate, Int>,
    primaryColor: Int,
    surfaceColor: Int
) {
    // Prepare data for the chart
    val sortedDates = weeklyCalories.keys.sorted()
    val dayLabels = sortedDates.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    val barEntries = sortedDates.mapIndexed { index, date ->
        BarEntry(index.toFloat(), weeklyCalories[date]?.toFloat() ?: 0f)
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            BarChart(context).apply {
                // Set the custom renderer for rounded bars
                val roundedRenderer = RoundedBarChartRenderer(this, animator, viewPortHandler)
                roundedRenderer.setCornerRadius(25f) // Set the corner radius in dp
                renderer = roundedRenderer

                // General Styling
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)
                setDrawBorders(false)
                setBackgroundColor(Color.TRANSPARENT)

                // Interaction
                setTouchEnabled(true)
                setPinchZoom(false)
                isDoubleTapToZoomEnabled = false
                isHighlightPerTapEnabled = true

                // X-Axis
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    setDrawAxisLine(false) // Hide the axis line for a cleaner look
                    granularity = 1f
                    textColor = Color.GRAY
                    textSize = 12f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            val index = value.toInt()
                            return if (index >= 0 && index < dayLabels.size) dayLabels[index] else ""
                        }
                    }
                }

                // Y-Axis (Left)
                axisLeft.apply {
                    setDrawGridLines(true)
                    gridColor = surfaceColor // Use a subtle color from the theme
                    setLabelCount(6, false)
                    axisMinimum = 0f
                    setDrawAxisLine(false)
                    textColor = Color.GRAY
                    textSize = 12f
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            // Display "1k" instead of "1000" for large numbers
                            return if (value >= 1000) "${(value / 1000).toInt()}k" else value.toInt().toString()
                        }
                    }
                }

                // Y-Axis (Right)
                axisRight.isEnabled = false
            }
        },
        update = { chart ->
            // ... (other update code) ...

            val dataSet = BarDataSet(barEntries, "Weekly Calories").apply {

                // --- FIX: Apply a beautiful gradient correctly ---
                val alpha = (0.6f * 255).toInt() // Target alpha for startColor
                val red = Color.red(primaryColor)       // Extract red from original primaryColor INT
                val green = Color.green(primaryColor)   // Extract green
                val blue = Color.blue(primaryColor)    // Extract blue

                val startColor = Color.argb(
                    alpha, // The new alpha
                    red,   // Original red (as int 0-255)
                    green, // Original green (as int 0-255)
                    blue   // Original blue (as int 0-255)
                )
                val endColor = primaryColor // This is already the correct Int ARGB format for the end of the gradient
                setGradientColor(startColor, endColor)

                // Value text on top of bars
                setDrawValues(true)
                valueTextColor = Color.DKGRAY
                valueTextSize = 11f
                valueFormatter = object : ValueFormatter() {
                    // Show value only if it's greater than 0
                    override fun getFormattedValue(value: Float): String {
                        return if (value > 0) value.toInt().toString() else ""
                    }
                }

                // Bar highlighting on tap
                highLightColor = primaryColor
                highLightAlpha = 100
            }

            // Apply data to the chart
            chart.data = BarData(dataSet).apply {
                barWidth = 0.6f // Slightly wider bars for a more robust look
            }

            // Animate and refresh
            chart.invalidate()
            chart.animateY(800)
        }
    )
}