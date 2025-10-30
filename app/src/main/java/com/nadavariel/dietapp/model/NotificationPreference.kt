package com.nadavariel.dietapp.model // You can place this in your 'model' package

import com.google.firebase.firestore.DocumentId
import java.util.Calendar
import java.util.UUID

// --- Goal ---

data class Goal(
    val text: String,
    val value: String? = null,
    val id: String = UUID.randomUUID().toString()
)

// --- GraphPreference ---

data class GraphPreference(
    val id: String,
    val title: String,
    val order: Int,
    val isVisible: Boolean,
    val isMacro: Boolean
)

// --- NotificationPreference ---

data class NotificationPreference(
    @DocumentId
    val id: String = "",
    val hour: Int = 12,
    val minute: Int = 0,
    val repetition: String = "DAILY",
    val message: String = "Time to log your meal!",
    val isEnabled: Boolean = true
) {
    val uniqueId: Int
        get() = id.hashCode()

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