package com.nadavariel.dietapp.ui.stats

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.util.*

// --- DESIGN TOKENS TO MATCH YOUR APP ---
val HealthyGreen = Color(0xFF4CAF50)
private val LightGreyText = Color(0xFF757575)

// --- Shared SectionHeader ---
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(HealthyGreen, CircleShape)
        )
        Text(
            text = title.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = LightGreyText,
            letterSpacing = 1.2.sp
        )
    }
}

// --- Shared FormCard ---
@Composable
fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Apply padding *inside* the card, around the chart content
        Column(modifier = Modifier.padding(16.dp)) {
            content()
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp)
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ChartItem(
    // REMOVED title and icon - handled by SectionHeader in the screen
    weeklyData: Map<LocalDate, Int>,
    target: Int?,
    label: String
) {
    // This composable now *only* contains the logic to show the chart or empty state
    if (weeklyData.isEmpty() || weeklyData.values.all { it == 0 }) {
        EmptyChartState(message = "Log your meals to see your progress here!")
    } else {
        BeautifulBarChart(
            weeklyData = weeklyData,
            target = target,
            label = label
        )
    }
}

@Composable
fun PieChartItem(
    // REMOVED title - handled by SectionHeader in the screen
    data: Map<String, Float>
) {
    // This composable now *only* contains the logic to show the chart or empty state
    val hasMacroData = data.values.any { it > 0f }
    if (!hasMacroData) {
        EmptyChartState(message = "Log meals with nutritional data from yesterday to see your macro distribution!")
    } else {
        BeautifulPieChart(data = data)
    }
}

// --- Reusable button for the bottom sheet ---
@Composable
fun AppSubmitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(50), // Pill shape
        colors = ButtonDefaults.buttonColors(
            containerColor = HealthyGreen,
            contentColor = Color.White,
            disabledContainerColor = HealthyGreen.copy(alpha = 0.3f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}