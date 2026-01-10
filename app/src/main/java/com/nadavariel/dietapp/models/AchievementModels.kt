package com.nadavariel.dietapp.models

import androidx.compose.ui.graphics.Color

data class BadgeData(
    val threshold: Float,
    val title: String,
    val emoji: String,
    val color: Color
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val color: Color,
    val condition: (
        daysLogged: Int,
        avgCals: Int,
        avgProtein: Int,
        macroMap: Map<String, Float>,
        microMap: Map<String, Float>
    ) -> Boolean
)

