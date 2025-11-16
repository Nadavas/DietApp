package com.nadavariel.dietapp.ui.meals

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.viewmodel.GeminiResult

@Composable
fun SubmitMealButton(
    isEditMode: Boolean,
    geminiResult: GeminiResult,
    isButtonEnabled: Boolean,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        enabled = isButtonEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(50), // Pill shape
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.vibrantGreen,
            contentColor = Color.White,
            disabledContainerColor = AppTheme.colors.vibrantGreen.copy(alpha = 0.3f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        when {
            geminiResult is GeminiResult.Loading -> {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.width(12.dp))
                Text("Analyzing...", fontWeight = FontWeight.Bold)
            }
            isEditMode -> {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Update Meal", fontWeight = FontWeight.Bold)
            }
            else -> {
                Icon(Icons.Default.CloudUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Analyze and Add Meal", fontWeight = FontWeight.Bold)
            }
        }
    }
}