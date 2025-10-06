package com.nadavariel.dietapp.ui.meals

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White.copy(alpha = 0.9f),
            contentColor = Color(0xFF1644A0),
            disabledContainerColor = Color.White.copy(alpha = 0.3f),
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        )
    ) {
        when {
            geminiResult is GeminiResult.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color(0xFF1644A0)
                )
                Spacer(Modifier.width(12.dp))
                Text("Analyzing with AI...")
            }
            isEditMode -> {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Update Meal")
            }
            else -> {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add Meal with AI")
            }
        }
    }
}