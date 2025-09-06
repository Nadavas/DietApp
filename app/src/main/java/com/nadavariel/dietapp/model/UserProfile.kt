package com.nadavariel.dietapp.model

import java.util.Date

// Profile data class
data class UserProfile(
    val name: String = "",
    val weight: Float = 0f,
    val height: Float = 0f,
    val dateOfBirth: Date? = null,
    val targetWeight: Float = 0f,
    val avatarId: String? = null,
    val gender: Gender = Gender.UNKNOWN,
    val activityLevel: ActivityLevel = ActivityLevel.NOT_SET
)

// Enum for multiple options for gender
enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    NON_BINARY("Non-binary"),
    PREFER_NOT_TO_SAY("Prefer not to say"),
    UNKNOWN("Not Set")
}

// Enum for activity level
enum class ActivityLevel(val displayName: String, val multiplier: Double) {
    NOT_SET("Not Set", 1.0),
    SEDENTARY("Sedentary (little to no exercise)", 1.2),
    LIGHTLY_ACTIVE("Lightly Active (light exercise/sports 1-3 days/week)", 1.375),
    MODERATELY_ACTIVE("Moderately Active (moderate exercise/sports 3-5 days/week)", 1.55),
    VERY_ACTIVE("Very Active (hard exercise/sports 6-7 days a week)", 1.725),
    EXTRA_ACTIVE("Extra Active (very hard exercise/physical job)", 1.9)
}