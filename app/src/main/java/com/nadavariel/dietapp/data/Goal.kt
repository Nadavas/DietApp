package com.nadavariel.dietapp.data

import java.util.UUID

data class Goal(
    val text: String,
    val value: String? = null, // Holds the user's text input. It can be null initially.
    val id: String = UUID.randomUUID().toString() // Unique ID for each goal
)