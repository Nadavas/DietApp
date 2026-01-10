package com.nadavariel.dietapp.models

import java.util.Date
import java.util.UUID

data class UserProfile(
    val name: String = "",
    val startingWeight: Float = 0f,
    val height: Float = 0f,
    val dateOfBirth: Date? = null,
    val avatarId: String? = null,
    val gender: Gender = Gender.UNKNOWN
)

enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    PREFER_NOT_TO_SAY("Other / Prefer not to say"),
    UNKNOWN("Not Set");

    companion object {
        /**
         * Dynamic lookup: Finds the Gender where the displayName matches the answer.
         * This ensures Single Source of Truth.
         */
        fun fromString(answerText: String): Gender {
            return entries.find { it.displayName.equals(answerText, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

data class Goal(
    val text: String,
    val value: String? = null,
    val id: String = UUID.randomUUID().toString()
)

