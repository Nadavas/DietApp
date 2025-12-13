package com.nadavariel.dietapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.nadavariel.dietapp.model.Achievement
import com.nadavariel.dietapp.model.AchievementRepository
import com.nadavariel.dietapp.ui.AppTheme
import java.time.LocalDate

@Composable
fun AchievementsScreen(
    navController: NavController,
    weeklyCalories: Map<LocalDate, Int>,
    weeklyProtein: Map<LocalDate, Float>,
    weeklyMacroPercentages: Map<String, Float>
) {
    val stats = remember(weeklyCalories, weeklyProtein) {
        calculateStats(weeklyCalories, weeklyProtein)
    }

    val weeklyMicros = emptyMap<String, Float>()
    val allBadges = AchievementRepository.allAchievements

    var selectedBadge by remember { mutableStateOf<Achievement?>(null) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "All Achievements",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(allBadges) { badge ->
                    val isUnlocked = badge.condition(stats.daysLogged, stats.avgCals, stats.avgProtein, weeklyMacroPercentages, weeklyMicros)
                    AchievementGridItem(
                        badge = badge,
                        isUnlocked = isUnlocked,
                        onClick = { selectedBadge = badge }
                    )
                }
            }
        }

        if (selectedBadge != null) {
            val isUnlocked = selectedBadge!!.condition(stats.daysLogged, stats.avgCals, stats.avgProtein, weeklyMacroPercentages, weeklyMicros)
            AchievementDetailDialog(
                badge = selectedBadge!!,
                isUnlocked = isUnlocked,
                onDismiss = { selectedBadge = null }
            )
        }
    }
}

@Composable
private fun AchievementGridItem(
    badge: Achievement,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(
                if (isUnlocked)
                    Brush.radialGradient(listOf(badge.color.copy(alpha = 0.4f), badge.color.copy(alpha = 0.1f)))
                else
                    SolidColor(Color.Gray.copy(alpha = 0.1f))
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge.emoji,
                fontSize = 32.sp,
                modifier = Modifier.graphicsLayer { alpha = if (isUnlocked) 1f else 0.3f }
            )
            Box(
                modifier = Modifier.fillMaxSize().padding(6.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                if (isUnlocked) {
                    Text("✓", color = badge.color, fontWeight = FontWeight.Bold)
                } else {
                    Text("🔒", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = badge.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (isUnlocked) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary
        )
    }
}

@Composable
private fun AchievementDetailDialog(
    badge: Achievement,
    isUnlocked: Boolean,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        modifier = Modifier.clickable(onClick = onDismiss)
                    )
                }
                Box(
                    modifier = Modifier.size(120.dp).clip(CircleShape).background(
                        if (isUnlocked)
                            Brush.radialGradient(listOf(badge.color.copy(alpha = 0.4f), badge.color.copy(alpha = 0.1f)))
                        else
                            SolidColor(Color.Gray.copy(alpha = 0.1f))
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge.emoji,
                        fontSize = 60.sp,
                        modifier = Modifier.graphicsLayer { alpha = if (isUnlocked) 1f else 0.3f }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) AppTheme.colors.textPrimary else Color.Gray
                )
                if (!isUnlocked) {
                    Text(
                        text = "(Locked)",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Red.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}