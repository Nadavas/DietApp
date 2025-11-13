package com.nadavariel.dietapp.ui.account

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter


// Helper for image icons that need tinting for consistency
@Composable
fun ImageIcon(painter: Painter, contentDescription: String?) {
    Image(
        painter = painter,
        contentDescription = contentDescription,
        colorFilter = ColorFilter.tint(Color.White)
    )
}