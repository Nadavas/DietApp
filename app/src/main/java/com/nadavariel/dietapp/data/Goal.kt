package com.nadavariel.dietapp.data

import java.util.UUID

data class Goal(
    val text: String,
    val options: List<String>,
    val id: String = UUID.randomUUID().toString() // Unique ID for each goal
)