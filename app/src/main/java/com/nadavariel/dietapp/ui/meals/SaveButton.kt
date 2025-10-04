package com.nadavariel.dietapp.ui.meals

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = isButtonEnabled,
        shape = RoundedCornerShape(12.dp)
    ) {
        AnimatedContent(
            targetState = geminiResult is GeminiResult.Loading,
            label = "button_state_animation"
        ) { isLoading ->
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = LocalContentColor.current,
                        strokeWidth = 2.dp
                    )
                    Text("Analyzing...")
                }
            } else {
                Text(if (isEditMode) "Save Changes" else "Add Meal with AI", fontSize = 18.sp)
            }
        }
    }
}