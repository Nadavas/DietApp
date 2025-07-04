package com.nadavariel.dietapp.model

import java.util.Date
/**
 * Data class to represent a user's profile information.
 * All fields are initialized with default values for convenience.
 */
data class UserProfile(
    val name: String = "",
    val weight: Float = 0f, // Weight in kilograms, using Float for decimal values
    val dateOfBirth: Date? = null,
    val targetWeight: Float = 0f, // Target weight in kilograms, using Float
    val avatarId: String? = null // ⭐ NEW: Field to store the selected avatar's identifier
)

