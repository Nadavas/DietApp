package com.nadavariel.dietapp.data

// Data class for Graph Preferences
data class GraphPreference(
    val id: String, // Unique ID (e.g., "calories", "protein", "fiber")
    val title: String, // Display name
    val order: Int, // User-defined display order (0, 1, 2, ...)
    val isVisible: Boolean, // Whether the user wants to see it
    val isMacro: Boolean // Helper for potential future UI grouping
)