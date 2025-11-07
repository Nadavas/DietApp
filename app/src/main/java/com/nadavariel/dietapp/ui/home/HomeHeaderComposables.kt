package com.nadavariel.dietapp.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import com.nadavariel.dietapp.util.AvatarConstants

// Design tokens
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF6B7280)
private val PrimaryGreen = Color(0xFF00C853)
private val WarmOrange = Color(0xFFFF6E40)

@Composable
fun HeaderSection(userName: String, avatarId: String?, onAvatarClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Welcome back,",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 14.sp
            )
            Text(
                text = userName.ifBlank { "User" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontSize = 24.sp
            )
        }
        Image(
            painter = painterResource(id = AvatarConstants.getAvatarResId(avatarId)),
            contentDescription = "User Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .border(3.dp, PrimaryGreen.copy(alpha = 0.3f), CircleShape)
                .clickable(onClick = onAvatarClick)
        )
    }
}

@Composable
fun MissingGoalsWarning(missingGoals: List<String>, onSetGoalsClick: () -> Unit) {
    val missingListText = missingGoals.joinToString(" and ")
    val message = "Set your $missingListText goal${if (missingGoals.size > 1) "s" else ""} to track your progress"

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = WarmOrange.copy(alpha = 0.1f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSetGoalsClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = WarmOrange,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Set Your Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onSetGoalsClick) {
                Text(
                    "SET",
                    fontWeight = FontWeight.Bold,
                    color = WarmOrange
                )
            }
        }
    }
}