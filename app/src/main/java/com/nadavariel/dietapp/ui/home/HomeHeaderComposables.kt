package com.nadavariel.dietapp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.util.AvatarConstants

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
        // Note: This was fillMaxWidth(), but ModernHomeHeader's Row is now responsible for spacing.
        // Kept it as is, but this modifier might not be necessary if it's always used inside ModernHomeHeader
        // modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.TextSecondary,
                fontSize = 14.sp
            )
            Text(
                text = userName.ifBlank { "User" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.TextPrimary,
                fontSize = 24.sp
            )
        }
        Spacer(Modifier.width(16.dp)) // Add spacer for cases where column doesn't fill width

        // --- THIS IS THE UPDATED AVATAR CODE ---
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp) // This is the total size including the background
                .clickable(onClick = onAvatarClick) // Clickable is now on the outer box
        ) {
            // Background Circle
            Box(
                modifier = Modifier
                    .fillMaxSize() // Fills the 52.dp
                    .background(AppTheme.colors.PrimaryGreen.copy(alpha = 0.1f), CircleShape)
            )
            // Avatar Image (slightly smaller to show background)
            Image(
                painter = painterResource(id = AvatarConstants.getAvatarResId(avatarId)),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
        }
        // --- END OF UPDATED CODE ---
    }
}