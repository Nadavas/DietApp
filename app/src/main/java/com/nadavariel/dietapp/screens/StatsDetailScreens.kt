package com.nadavariel.dietapp.screens

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.withSave
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.buffer.BarBuffer
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.dataprovider.BarDataProvider
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.renderer.PieChartRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.AppTopBar
import com.nadavariel.dietapp.viewmodels.FoodLogViewModel
import com.nadavariel.dietapp.viewmodels.DietPlanViewModel
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyDetailScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    goalViewModel: DietPlanViewModel = viewModel(),
    navController: NavController
) {
    val weeklyCalories by foodLogViewModel.weeklyCalories.collectAsState()
    val weeklyProtein by foodLogViewModel.weeklyProtein.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    DetailScreenScaffold(
        title = "Energy & Protein",
        icon = Icons.Default.LocalFireDepartment,
        color = AppTheme.colors.warmOrange,
        navController = navController
    ) {
        item {
            DetailStatCard(
                title = "Calories",
                weeklyData = weeklyCalories,
                target = goals.getOrNull(0)?.value?.toIntOrNull(),
                label = "kcal",
                color = AppTheme.colors.warmOrange
            )
        }
        item {
            DetailStatCard(
                title = "Protein",
                weeklyData = weeklyProtein,
                target = goals.getOrNull(1)?.value?.toIntOrNull(),
                label = "g",
                color = AppTheme.colors.vividGreen
            )
        }
        item {
            InsightCard(
                insights = buildList {
                    val avgCalories = weeklyCalories.values.average().toInt()
                    val avgProtein = weeklyProtein.values.average().toInt()
                    val calorieTarget = goals.getOrNull(0)?.value?.toIntOrNull()
                    val proteinTarget = goals.getOrNull(1)?.value?.toIntOrNull()

                    if (calorieTarget != null) {
                        val diff = ((avgCalories.toFloat() / calorieTarget) * 100).toInt() - 100
                        add(
                            if (diff >= 0) "You're consuming ${diff}% above your calorie target"
                            else "You're consuming ${-diff}% below your calorie target"
                        )
                    }

                    if (proteinTarget != null && avgProtein >= proteinTarget) {
                        add("Great job hitting your protein goals! 💪")
                    } else if (proteinTarget != null) {
                        add("Try adding more protein-rich foods to meet your goal")
                    }
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacrosDetailScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    navController: NavController
) {
    val weeklyMacroPercentages by foodLogViewModel.weeklyMacroPercentages.collectAsState()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    DetailScreenScaffold(
        title = "Macronutrients",
        icon = Icons.Default.PieChart,
        color = AppTheme.colors.vividGreen,
        navController = navController
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Weekly Balance",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val hasMacroData = weeklyMacroPercentages.values.any { it > 0f }
                    if (!hasMacroData) {
                        EmptyChartState(message = "Log meals to see your macro distribution")
                    } else {
                        BeautifulPieChart(data = weeklyMacroPercentages)
                    }
                }
            }
        }
        item {
            InsightCard(
                insights = listOf(
                    "Aim for 10-35% protein, 45-65% carbs, 20-35% fat to support overall health and energy",
                    "Protein repairs muscle tissue and helps keep you feeling full longer.",
                    "Carbs are your body's primary fuel source for brain function and physical energy.",
                    "Healthy fats are essential for hormone regulation and absorbing vitamins."
                )
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarbsDetailScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    goalViewModel: DietPlanViewModel = viewModel(),
    navController: NavController
) {
    val weeklyFiber by foodLogViewModel.weeklyFiber.collectAsState()
    val weeklySugar by foodLogViewModel.weeklySugar.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    DetailScreenScaffold(
        title = "Fiber & Sugar",
        icon = Icons.Default.Spa,
        color = AppTheme.colors.accentTeal,
        navController = navController
    ) {
        item {
            DetailStatCard(
                title = "Fiber",
                weeklyData = weeklyFiber,
                target = goals.getOrNull(2)?.value?.toIntOrNull(),
                label = "g",
                color = AppTheme.colors.accentTeal
            )
        }
        item {
            DetailStatCard(
                title = "Sugar",
                weeklyData = weeklySugar,
                target = goals.getOrNull(3)?.value?.toIntOrNull(),
                label = "g",
                color = AppTheme.colors.sunsetPink
            )
        }
        item {
            InsightCard(
                recommendedValues = mapOf(
                    "Fiber" to "> 28 g",
                    "Sugar" to "< 50 g"
                ),
                insights = listOf(
                    "Fiber aids digestion and helps maintain stable blood sugar",
                    "Limit added sugars to less than 10% of daily calories"
                )
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineralsDetailScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    goalViewModel: DietPlanViewModel = viewModel(),
    navController: NavController
) {
    val weeklySodium by foodLogViewModel.weeklySodium.collectAsState()
    val weeklyPotassium by foodLogViewModel.weeklyPotassium.collectAsState()
    val weeklyCalcium by foodLogViewModel.weeklyCalcium.collectAsState()
    val weeklyIron by foodLogViewModel.weeklyIron.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    DetailScreenScaffold(
        title = "Minerals",
        icon = Icons.Default.Science,
        color = AppTheme.colors.softBlue,
        navController = navController
    ) {
        item {
            DetailStatCard(
                title = "Sodium",
                weeklyData = weeklySodium,
                target = goals.getOrNull(4)?.value?.toIntOrNull(),
                label = "mg",
                color = AppTheme.colors.softBlue
            )
        }
        item {
            DetailStatCard(
                title = "Potassium",
                weeklyData = weeklyPotassium,
                target = goals.getOrNull(5)?.value?.toIntOrNull(),
                label = "mg",
                color = AppTheme.colors.purple
            )
        }
        item {
            DetailStatCard(
                title = "Calcium",
                weeklyData = weeklyCalcium,
                target = goals.getOrNull(6)?.value?.toIntOrNull(),
                label = "mg",
                color = AppTheme.colors.accentTeal
            )
        }
        item {
            DetailStatCard(
                title = "Iron",
                weeklyData = weeklyIron,
                target = goals.getOrNull(7)?.value?.toIntOrNull(),
                label = "mg",
                color = AppTheme.colors.warmOrange
            )
        }
        item {
            InsightCard(
                recommendedValues = mapOf(
                    "Calcium" to "> 1,000 mg",
                    "Sodium" to "< 2,300 mg",
                    "Potassium" to "M: > 3,400 mg | F: > 2,600 mg",
                    "Iron" to "M: > 8 mg | F: > 18 mg"
                ),
                insights = listOf(
                    "Potassium supports heart health while limiting sodium benefits blood pressure",
                    "Calcium strengthens bones, and iron fuels your daily energy levels"
                )
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitaminsDetailScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    goalViewModel: DietPlanViewModel = viewModel(),
    navController: NavController
) {
    val weeklyVitaminC by foodLogViewModel.weeklyVitaminC.collectAsState()
    val weeklyVitaminA by foodLogViewModel.weeklyVitaminA.collectAsState()
    val weeklyVitaminB12 by foodLogViewModel.weeklyVitaminB12.collectAsState()
    val goals by goalViewModel.goals.collectAsState()

    LaunchedEffect(Unit) {
        foodLogViewModel.refreshStatistics()
    }

    DetailScreenScaffold(
        title = "Vitamins",
        icon = Icons.Default.Favorite,
        color = AppTheme.colors.sunsetPink,
        navController = navController
    ) {
        item {
            DetailStatCard(
                title = "Vitamin C",
                weeklyData = weeklyVitaminC,
                target = goals.getOrNull(8)?.value?.toIntOrNull(),
                label = "mg",
                color = AppTheme.colors.sunsetPink
            )
        }
        item {
            DetailStatCard(
                title = "Vitamin A",
                weeklyData = weeklyVitaminA,
                target = goals.getOrNull(9)?.value?.toIntOrNull(), // Next goal index
                label = "mcg", // Common unit for Vit A
                color = AppTheme.colors.warmOrange
            )
        }
        item {
            DetailStatCard(
                title = "Vitamin B12",
                weeklyData = weeklyVitaminB12,
                target = goals.getOrNull(10)?.value?.toIntOrNull(), // Next goal index
                label = "mcg", // Common unit for Vit B12
                color = AppTheme.colors.softBlue
            )
        }
        item {
            InsightCard(
                recommendedValues = mapOf(
                    "Vitamin C" to "M: > 90 mg | F: > 75 mg",
                    "Vitamin A" to "M: > 900 mcg | F: > 700 mcg",
                    "Vitamin B12" to "> 2.4 mcg"
                ),
                insights = listOf(
                    "Vitamin C supports immune function and collagen production",
                    "Vitamin A is crucial for vision, immune function, and skin health",
                    "Vitamin B12 is essential for nerve function and forming red blood cells"
                )
            )
        }
    }
}

// -------------------------------
// --------- COMPOSABLES ---------
// -------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreenScaffold(
    title: String,
    icon: ImageVector,
    color: Color,
    navController: NavController,
    content: LazyListScope.() -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = title,
                onBack = { navController.popBackStack() },
                icon = icon,
                iconColor = color,
                containerColor = Color.White
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DetailStatCard(
    title: String,
    weeklyData: Map<LocalDate, Int>,
    target: Int?,
    label: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            val average = if (weeklyData.isEmpty()) 0 else weeklyData.values.average().toInt()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )

                    Text(
                        text = "$average $label",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "daily average",
                        fontSize = 13.sp,
                        color = AppTheme.colors.textSecondary
                    )
                }

                target?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Goal",
                            fontSize = 12.sp,
                            color = AppTheme.colors.textSecondary
                        )
                        Text(
                            text = "$it $label",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.textPrimary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        val percentage = ((average.toFloat() / it) * 100).toInt()

                        Text(
                            text = "$percentage%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (weeklyData.isEmpty() || weeklyData.values.all { it == 0 }) {
                EmptyChartState(message = "Log meals to see your progress")
            } else {
                BeautifulBarChart(
                    weeklyData = weeklyData,
                    target = target,
                    label = label,
                    barColor = color,
                    goalColor = color
                )
            }
        }
    }
}

@Composable
private fun InsightCard(
    // New optional parameter for the reference table
    recommendedValues: Map<String, String>? = null,
    insights: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.vividGreen.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = AppTheme.colors.vividGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Insights",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
            }

            // --- NEW: Recommended Values Section ---
            if (recommendedValues != null) {
                Column(
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Recommended daily values (Adults):",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.textPrimary
                    )

                    // Render the map as: Key ..... Value
                    recommendedValues.forEach { (nutrient, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = nutrient,
                                fontSize = 14.sp,
                                color = AppTheme.colors.textPrimary // Slightly lighter for the label
                            )
                            Text(
                                text = value,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold, // Bold for the number
                                color = AppTheme.colors.textPrimary
                            )
                        }
                    }
                }

                // Visual separator between stats and text insights
                HorizontalDivider(
                    color = AppTheme.colors.vividGreen.copy(alpha = 0.2f),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            // ---------------------------------------

            // Existing Bullet Point Loop
            insights.forEach { insight ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "•",
                        fontSize = 14.sp,
                        color = AppTheme.colors.vividGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = insight,
                        fontSize = 14.sp,
                        color = AppTheme.colors.textPrimary,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChartState(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp)
    )
}

// --- CUSTOM CHARTS ---

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun BeautifulBarChart(
    weeklyData: Map<LocalDate, Int>,
    target: Int?,
    label: String,
    barColor: Color = AppTheme.colors.primaryGreen,
    goalColor: Color = AppTheme.colors.warmOrange
) {
    val primaryColor = barColor.toArgb()
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
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
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
                    gridLineWidth = 1.5f
                    gridColor = gridLineColor
                    setLabelCount(5, false)
                    axisMinimum = 0f
                    setDrawAxisLine(false)
                    textColor = axisTextColor
                    textSize = 11f

                    val maxBar = (barEntries.maxOfOrNull { it.y } ?: 0f)
                    val targetValue = target?.toFloat() ?: 0f

                    // Calculate a "clean" maximum value based on the data and label
                    axisMaximum = calculateRoundedAxisMax(maxBar, targetValue) // <-- Label no longer needed

                    target?.let {
                        val targetLine = LimitLine(it.toFloat(), "").apply {
                            lineWidth = 3f
                            lineColor = targetLineColor
                            enableDashedLine(15f, 10f, 0f)
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

            target?.let {
                chart.axisLeft.limitLines.firstOrNull()?.label = "Goal: $it $label"
            }

            chart.invalidate()
            chart.animateY(800, com.github.mikephil.charting.animation.Easing.EaseOutCubic)
        }
    )
}

@Composable
private fun BeautifulPieChart(
    data: Map<String, Float>
) {
    val proteinColor = AppTheme.colors.primaryGreen.toArgb()
    val carbsColor = AppTheme.colors.skyBlue.toArgb()
    val fatColor = AppTheme.colors.tangerine.toArgb()
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
                val myRenderer = ColorPieChartRenderer(this, animator, viewPortHandler)

                myRenderer.customLabelPaint.textSize = Utils.convertDpToPixel(12f)
                myRenderer.customLabelPaint.typeface = Typeface.DEFAULT_BOLD

                renderer = myRenderer

                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                description.isEnabled = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setExtraOffsets(20f, 20f, 20f, 10f)
                setUsePercentValues(true)
                legend.isEnabled = false

                isDrawHoleEnabled = true
                setHoleColor(android.graphics.Color.TRANSPARENT)
                holeRadius = 55f
                transparentCircleRadius = 60f

                setDrawCenterText(true)
                centerText = "Macros"
                setCenterTextSize(18f)
                setCenterTextTypeface(Typeface.DEFAULT_BOLD)
                setCenterTextColor(centerTextColor)

                isRotationEnabled = false
                isHighlightPerTapEnabled = false

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
                valueLineColor = android.graphics.Color.BLACK

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
            val count = ceil((dataSet.entryCount.toFloat() * phaseX).toDouble()).toInt()
                .coerceAtMost(dataSet.entryCount)
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

        mRenderPaint.setShadowLayer(8f, 0f, 4f, android.graphics.Color.argb(40, 0, 0, 0))

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

class ColorPieChartRenderer(
    chart: PieChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : PieChartRenderer(chart, animator, viewPortHandler) {

    val customLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
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

                mValuePaint.color = android.graphics.Color.WHITE
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

                    if (drawXOutside || drawYOutside) {
                        val valueLinePart1OffsetPercentage =
                            dataSet.valueLinePart1OffsetPercentage / 100f
                        val valueLineLength2 = dataSet.valueLinePart2Length

                        val polyline2Radius = dataSet.valueLinePart1Length * radius + radius
                        val pt1x = polyline2Radius * sliceXBase + center.x
                        val pt1y = polyline2Radius * sliceYBase + center.y

                        val line1Radius = radius * valueLinePart1OffsetPercentage
                        val pt0x = line1Radius * sliceXBase + center.x
                        val pt0y = line1Radius * sliceYBase + center.y

                        var pt2x = pt1x
                        var pt2y = pt1y

                        var labelX = pt2x
                        var labelY = pt2y

                        var normAngle = transformedAngle % 360.0
                        if (normAngle < 0) normAngle += 360.0

                        val isHorizontalSlice =
                            (normAngle in 315.0..360.0) || (normAngle in 0.0..45.0) || (normAngle in 135.0..225.0)

                        if (isHorizontalSlice) {
                            if (pt1y < center.y) {
                                pt2y = pt1y - (radius * valueLineLength2) // Go UP
                                labelY = pt2y - 10f // Padding Above
                            }
                            else {
                                pt2y = pt1y + (radius * valueLineLength2) // Go DOWN
                                labelY = pt2y + lineHeight + 5f // Padding Below
                            }

                            mValuePaint.textAlign = Paint.Align.CENTER
                            customLabelPaint.textAlign = Paint.Align.CENTER

                            labelX = pt2x // Center on the vertical line

                        }
                        else {
                            pt2x = if (pt1x < center.x) {
                                pt1x - (radius * valueLineLength2) // Left
                            } else {
                                pt1x + (radius * valueLineLength2) // Right
                            }

                            mValuePaint.textAlign = Paint.Align.CENTER
                            customLabelPaint.textAlign = Paint.Align.CENTER

                            labelX = if (pt1x < center.x) {
                                pt2x - 40f
                            } else {
                                pt2x + 40f
                            }

                            labelY = pt2y
                        }

                        if (dataSet.valueLineColor != android.graphics.Color.TRANSPARENT) {
                            drawLine(pt0x, pt0y, pt1x, pt1y, mRenderPaint) // Radial Part
                            drawLine(pt1x, pt1y, pt2x, pt2y, mRenderPaint) // The 90-degree Turn
                        }

                        if (drawValues && drawYOutside) {
                            drawValue(this, formattedValue, labelX, labelY, specificColor)
                        }

                        if (drawEntryLabels && drawXOutside) {
                            if (isHorizontalSlice) {
                                val offset = if (pt1y < center.y) -lineHeight else lineHeight
                                drawText(entryLabel, labelX, labelY + offset, customLabelPaint)
                            }
                            else {
                                drawText(entryLabel, labelX, labelY - lineHeight, customLabelPaint)
                            }
                        }
                    }

                    if (drawXInside || drawYInside) {
                        val x = (radius * 0.5f * sliceXBase) + center.x
                        val y = (radius * 0.5f * sliceYBase) + center.y

                        mValuePaint.color = android.graphics.Color.WHITE
                        customLabelPaint.color = android.graphics.Color.WHITE
                        customLabelPaint.textAlign = Paint.Align.CENTER

                        if (drawValues && drawYInside) drawValue(
                            this,
                            formattedValue,
                            x,
                            y,
                            android.graphics.Color.WHITE
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