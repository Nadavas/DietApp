package com.nadavariel.dietapp.model

import java.util.Date

/**
 * Data class to represent a user's profile information.
 * All fields are initialized with default values for convenience.
 */
data class UserProfile(
    val name: String = "",
    val weight: Float = 0f, // Current weight in kilograms
    val height: Float = 0f, // ⭐ NEW: Height in centimeters
    val dateOfBirth: Date? = null,
    val targetWeight: Float = 0f, // Target weight in kilograms
    val avatarId: String? = null, // Field to store the selected avatar's identifier
    val gender: Gender = Gender.UNKNOWN, // ⭐ MODIFIED: Default to UNKNOWN
    val activityLevel: ActivityLevel = ActivityLevel.NOT_SET // User's activity level
)

// ⭐ MODIFIED: Enum for Gender with more inclusive options
enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    NON_BINARY("Non-binary"),
    PREFER_NOT_TO_SAY("Prefer not to say"),
    UNKNOWN("Not Set") // Default or not specified, indicating no selection yet
}

// Enum for Activity Level (remains unchanged)
enum class ActivityLevel(val displayName: String, val multiplier: Double) {
    NOT_SET("Not Set", 1.0), // Default value, will typically indicate missing data
    SEDENTARY("Sedentary (little to no exercise)", 1.2),
    LIGHTLY_ACTIVE("Lightly Active (light exercise/sports 1-3 days/week)", 1.375),
    MODERATELY_ACTIVE("Moderately Active (moderate exercise/sports 3-5 days/week)", 1.55),
    VERY_ACTIVE("Very Active (hard exercise/sports 6-7 days a week)", 1.725),
    EXTRA_ACTIVE("Extra Active (very hard exercise/physical job)", 1.9)
}