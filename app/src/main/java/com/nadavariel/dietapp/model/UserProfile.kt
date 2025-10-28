package com.nadavariel.dietapp.model

import java.util.Date

// Profile data class
data class UserProfile(
    val name: String = "",
    val weight: Float = 0f,
    val height: Float = 0f,
    val dateOfBirth: Date? = null,
    val avatarId: String? = null,
    val gender: Gender = Gender.UNKNOWN
)

// Enum for multiple options for gender
enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    NON_BINARY("Non-binary"),
    PREFER_NOT_TO_SAY("Prefer not to say"),
    UNKNOWN("Not Set")
}