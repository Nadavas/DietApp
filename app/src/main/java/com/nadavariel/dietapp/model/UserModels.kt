package com.nadavariel.dietapp.model // You can place this in your 'model' package

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Calendar
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

enum class InputType { DOB, HEIGHT, WEIGHT, TARGET_WEIGHT, EXERCISE_TYPE }
data class Question(
    val text: String,
    val options: List<String>? = null,
    val inputType: InputType? = null
)

// --- NotificationPreference ---

@IgnoreExtraProperties
data class NotificationPreference(
    @DocumentId
    val id: String = "",
    val hour: Int = 12,
    val minute: Int = 0,
    val repetition: String = "DAILY",
    val message: String = "",

    @get:PropertyName("isEnabled")
    @set:PropertyName("isEnabled")
    var isEnabled: Boolean = true,

    val type: String = "MEAL",

    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)
) {
    @get:Exclude
    val uniqueId: Int
        get() = id.hashCode()

    @Exclude
    fun getNextScheduledCalendar(): Calendar {
        val now = Calendar.getInstance()
        val scheduled = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (scheduled.before(now)) {
            scheduled.add(Calendar.DAY_OF_YEAR, 1)
        }
        return scheduled
    }
}