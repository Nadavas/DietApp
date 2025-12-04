package com.nadavariel.dietapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.components.UserAvatar

@Composable
fun ModernHomeHeader(
    userName: String,
    avatarId: String?,
    onAvatarClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HeaderSection(
                userName = userName,
                avatarId = avatarId,
                onAvatarClick = onAvatarClick
            )
        }
    }
}

@Composable
fun HeaderSection(userName: String, avatarId: String?, onAvatarClick: () -> Unit) {
    Row(
        // FIX 1: Added fillMaxWidth() to ensure SpaceBetween pushes elements to edges
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
                fontSize = 14.sp
            )
            Text(
                text = userName.ifBlank { "User" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                fontSize = 24.sp
            )
        }

        // Spacer is less critical now with SpaceBetween + fillMaxWidth, but keeps safe distance
        Spacer(Modifier.width(16.dp))

        // --- UPDATED AVATAR CODE ---
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(60.dp) // FIX 2: Increased from 52.dp to 60.dp
                .clickable(onClick = onAvatarClick)
        ) {
            // Background Circle
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f), CircleShape)
            )
            // FIX: Replaced manual Image with UserAvatar to support URLs/URIs
            UserAvatar(
                avatarId = avatarId,
                size = 60.dp,
                modifier = Modifier.clickable(onClick = onAvatarClick)
            )
        }
        // --- END OF UPDATED CODE ---
    }
}