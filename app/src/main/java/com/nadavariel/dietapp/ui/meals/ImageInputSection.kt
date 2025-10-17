package com.nadavariel.dietapp.ui.meals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val HealthyGreen = Color(0xFF4CAF50)

@Composable
fun ImageInputSection(
    onTakePhotoClick: () -> Unit,
    onUploadPhotoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically // Center the divider vertically
            ) {
                ImagePickerOption(
                    icon = Icons.Outlined.PhotoLibrary,
                    text = "From Gallery",
                    onClick = onUploadPhotoClick,
                    modifier = Modifier.weight(1f)
                )

                // The new beautiful separator line
                VerticalDivider(
                    modifier = Modifier.height(40.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                ImagePickerOption(
                    icon = Icons.Outlined.CameraAlt,
                    text = "Use Camera",
                    onClick = onTakePhotoClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ImagePickerOption(
    icon: ImageVector, text: String, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    // REMOVED the Card wrapper. Now it's a simple, clickable Column.
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)) // Clip to give it a shape for the ripple effect
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp), // Add some padding for a better touch target
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = HealthyGreen,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = HealthyGreen
        )
    }
}
