package com.nadavariel.dietapp.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.util.Calendar

@IgnoreExtraProperties
data class ReminderPreference(
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