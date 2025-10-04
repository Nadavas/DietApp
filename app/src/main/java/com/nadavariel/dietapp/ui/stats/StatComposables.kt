package com.nadavariel.dietapp.ui.stats

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.toArgb
import com.nadavariel.dietapp.data.GraphPreference
import java.time.LocalDate

@Composable
fun StatisticsHeader(onEditClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
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
        IconButton(onClick = onEditClicked) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Edit Graphs Order and Visibility",
                tint = MaterialTheme.colorScheme.primary
            )
        }
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ChartItem(
    title: String,
    icon: ImageVector,
    weeklyData: Map<LocalDate, Int>,
    target: Int?,
    label: String
) {
    StatisticCard(title = title, icon = icon) {
        if (weeklyData.isEmpty() || weeklyData.values.all { it == 0 }) {
            EmptyChartState(message = "Log your meals to see your progress here!")
        } else {
            BeautifulBarChart(
                weeklyData = weeklyData,
                primaryColor = MaterialTheme.colorScheme.primary.toArgb(),
                target = target,
                label = label
            )
        }
    }
}

@Composable
fun PieChartItem(
    title: String,
    data: Map<String, Float>,
    primaryColor: Int,
    onSurfaceColor: Int
) {
    StatisticCard(title = title, icon = Icons.Default.PieChart) {
        val hasMacroData = data.values.any { it > 0f }
        if (!hasMacroData) {
            EmptyChartState(message = "Log meals with nutritional data from yesterday to see your macro distribution!")
        } else {
            BeautifulPieChart(
                data = data,
                primaryColor = primaryColor,
                onSurfaceColor = onSurfaceColor
            )
        }
    }
}