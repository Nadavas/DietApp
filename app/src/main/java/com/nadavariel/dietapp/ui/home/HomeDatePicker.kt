package com.nadavariel.dietapp.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadavariel.dietapp.ui.HomeColors.CardBackground
import com.nadavariel.dietapp.ui.HomeColors.PrimaryGreen
import com.nadavariel.dietapp.ui.HomeColors.TextPrimary
import com.nadavariel.dietapp.ui.HomeColors.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DatePickerSection(
    currentWeekStartDate: LocalDate,
    selectedDate: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onGoToToday: () -> Unit
) {
    val weekDays = remember(currentWeekStartDate) {
        (0..6).map { currentWeekStartDate.plusDays(it.toLong()) }
    }
    val today = LocalDate.now()
    val isTodayVisible = weekDays.any { it.isEqual(today) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        // Re-adjusted padding for a balanced look
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPreviousWeek,
                    modifier = Modifier
                        .size(36.dp) // Slightly larger button
                        .clip(CircleShape)
                        .background(PrimaryGreen.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        "Previous Week",
                        tint = PrimaryGreen
                    )
                }
                val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
                Text(
                    text = selectedDate.format(formatter),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(
                    onClick = onNextWeek,
                    modifier = Modifier
                        .size(36.dp) // Slightly larger button
                        .clip(CircleShape)
                        .background(PrimaryGreen.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        "Next Week",
                        tint = PrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // More space

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { date ->
                    DayOfWeekItem(
                        date = date,
                        isSelected = date.isEqual(selectedDate),
                        isToday = date.isEqual(today),
                        onClick = { onDateSelected(date) }
                    )
                }
            }

            if (!isTodayVisible || !selectedDate.isEqual(today)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp), // More padding
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = onGoToToday,
                        contentPadding = PaddingValues(vertical = 8.dp) // More padding
                    ) {
                        Text(
                            "Go to Today",
                            color = PrimaryGreen,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp // Reset to default legible size
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DayOfWeekItem(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        isSelected -> PrimaryGreen
        else -> Color.Transparent
    }
    val contentColor = when {
        isSelected -> Color.White
        else -> TextPrimary
    }
    val borderModifier = if (isToday && !isSelected) {
        Modifier.border(2.dp, PrimaryGreen.copy(alpha = 0.5f), CircleShape) // Thicker border
    } else Modifier

    Column(
        modifier = Modifier
            .widthIn(min = 44.dp) // Use widthIn for flexibility
            .height(44.dp)     // Set height
            .clip(CircleShape)
            .background(backgroundColor)
            .then(borderModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp), // Add horizontal padding for safety
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            fontSize = 11.sp, // Legible small size
            fontWeight = FontWeight.Medium,
            color = if (isSelected) contentColor else TextSecondary
        )
        Spacer(modifier = Modifier.height(2.dp)) // More space
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 16.sp, // Legible size
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
    }
}