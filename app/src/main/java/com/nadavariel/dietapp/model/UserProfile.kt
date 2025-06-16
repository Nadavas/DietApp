package com.nadavariel.dietapp.model // Or com.nadavariel.dietapp.data, or similar

/**
 * Data class to represent a user's profile information.
 * All fields are initialized with default values for convenience.
 */
data class UserProfile(
    val name: String = "",
    val weight: Float = 0f, // Weight in kilograms, using Float for decimal values
    val age: Int = 0,
    val targetWeight: Float = 0f // Target weight in kilograms, using Float
)