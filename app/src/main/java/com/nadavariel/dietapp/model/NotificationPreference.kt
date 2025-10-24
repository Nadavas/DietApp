package com.nadavariel.dietapp.model

import com.google.firebase.firestore.DocumentId
import java.util.Calendar

data class NotificationPreference(
    // Firestore Document ID (for saving/deleting from the database)
    @DocumentId
    val id: String = "",

    // The hour of the day (0-23)
    val hour: Int = 12,

    // The minute of the hour (0-59)
    val minute: Int = 0,

    // Type of repetition: "DAILY" or "ONCE"
    val repetition: String = "DAILY",

    // Text for the notification (e.g., "Time to log breakfast!")
    val message: String = "Time to log your meal!",

    // Whether the notification is currently enabled by the user
    val isEnabled: Boolean = true
) {
    // Helper to get a unique integer ID for the PendingIntent, derived from the string ID
    val uniqueId: Int
        get() = id.hashCode() // Simple way to generate a unique int from the string ID

    // Helper to set a Calendar instance to the scheduled time (for use with AlarmManager)
    fun getNextScheduledCalendar(): Calendar {
        val now = Calendar.getInstance()
        val scheduled = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the scheduled time is in the past, set it for tomorrow (Daily or Once-Daily setup)
        if (scheduled.before(now)) {
            scheduled.add(Calendar.DAY_OF_YEAR, 1)
        }
        return scheduled
    }
}