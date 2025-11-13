package com.nadavariel.dietapp.model // You can place this in your 'model' package

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.UUID

// --- UserProfile ---

data class UserProfile(
    val name: String = "",
    val startingWeight: Float = 0f,
    val height: Float = 0f,
    val dateOfBirth: Date? = null,
    val avatarId: String? = null,
    val gender: Gender = Gender.UNKNOWN
)

// --- Gender ---
enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    PREFER_NOT_TO_SAY("Prefer not to say"),
    UNKNOWN("Not Set"); // Removed NON_BINARY

    companion object {
        /**
         * Parses a string answer from the questionnaire into a Gender enum.
         */
        fun fromString(answerText: String): Gender {
            return when (answerText) {
                "Male" -> MALE
                "Female" -> FEMALE
                "Other / Prefer not to say" -> PREFER_NOT_TO_SAY
                else -> UNKNOWN
            }
        }
    }
}

// --- WeightEntry ---

data class WeightEntry(
    @DocumentId
    val id: String = "",
    val weight: Float = 0f,
    @ServerTimestamp
    val timestamp: Timestamp? = null
)

// --- Goal ---

data class Goal(
    val text: String,
    val value: String? = null,
    val id: String = UUID.randomUUID().toString()
)

// --- Questions ---

enum class InputType { DOB, HEIGHT, WEIGHT, TEXT, TARGET_WEIGHT, EXERCISE_TYPE }
data class Question(
    val text: String,
    val options: List<String>? = null,
    val inputType: InputType? = null
)