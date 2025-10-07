package com.nadavariel.dietapp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nadavariel.dietapp.util.AvatarConstants

import androidx.compose.ui.unit.Dp

/**
 * A custom modifier to apply a "glassmorphism" effect
 * with a semi-transparent fill and a subtle border.
 */
fun Modifier.glassmorphism(
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    color: Color = Color.White.copy(alpha = 0.1f), // The fill color
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(color) // Apply the fill
    .border(borderWidth, borderColor, shape)

@Composable
fun HeaderSection(userName: String, avatarId: String?, onAvatarClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp), // Adjusted padding for TopAppBar
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Welcome,",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f) // Updated color
            )
            Text(
                text = userName.ifBlank { "Guest" },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White // Updated color
            )
        }
        Image(
            painter = painterResource(id = AvatarConstants.getAvatarResId(avatarId)),
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape) // Replaced shadow with border
                .clickable(onClick = onAvatarClick)
        )
    }
}

@Composable
fun MissingGoalsWarning(missingGoals: List<String>, onSetGoalsClick: () -> Unit) {
    val missingListText = missingGoals.joinToString(" and ")
    val message = "Your $missingListText goal${if (missingGoals.size > 1) "s" else ""} are missing."

    // FIX: Use a Box to separate the blurred background from the sharp content.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)) // Clip the entire Box
    ) {
        // 1. BACKGROUND LAYER: A blurred, colored Box that sits behind the content.
        Box(
            modifier = Modifier
                .fillMaxSize() // Match the size of the parent Box
                .glassmorphism(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
        )

        // 2. CONTENT LAYER: The Row with your text and buttons, which is NOT blurred.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Warning",
                    tint = Color.White, // Updated color
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White // Updated color
                )
            }
            TextButton(onClick = onSetGoalsClick) {
                Text("SET GOALS", color = Color.White, fontWeight = FontWeight.Bold) // Updated color
            }
        }
    }
}
