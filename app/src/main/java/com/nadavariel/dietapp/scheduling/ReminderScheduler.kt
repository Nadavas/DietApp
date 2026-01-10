package com.nadavariel.dietapp.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nadavariel.dietapp.models.ReminderPreference
import java.util.ArrayList
import java.util.Calendar

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun getPendingIntent(preference: ReminderPreference): PendingIntent {
        // ALWAYS use UniversalReminderReceiver
        val intent = Intent(context, UniversalReminderReceiver::class.java).apply {
            putExtra(UniversalReminderReceiver.EXTRA_REMINDER_ID, preference.uniqueId)
            putExtra(UniversalReminderReceiver.EXTRA_MESSAGE, preference.message)
            putExtra(UniversalReminderReceiver.EXTRA_REPETITION, preference.repetition)
            // Pass the type ("MEAL" or "WEIGHT")
            putExtra(UniversalReminderReceiver.EXTRA_TYPE, preference.type)
            putExtra(UniversalReminderReceiver.EXTRA_FIRESTORE_ID, preference.id)
            putIntegerArrayListExtra(UniversalReminderReceiver.EXTRA_DAYS_OF_WEEK, ArrayList(preference.daysOfWeek))
            putExtra(UniversalReminderReceiver.EXTRA_HOUR, preference.hour)
            putExtra(UniversalReminderReceiver.EXTRA_MINUTE, preference.minute)
        }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getBroadcast(
            context,
            preference.uniqueId,
            intent,
            flags
        )
    }

    // Removed the 'receiverClass' parameter
    fun schedule(preference: ReminderPreference) {
        if (!preference.isEnabled) return

        val pendingIntent = getPendingIntent(preference)
        val calendar = preference.getNextScheduledCalendar()

        // Zero out seconds
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var triggerTime = calendar.timeInMillis
        val currentTime = System.currentTimeMillis()

        // Grace period logic
        if (triggerTime <= currentTime - 60_000) {
            triggerTime += AlarmManager.INTERVAL_DAY
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    // Removed the 'receiverClass' parameter
    fun cancel(preference: ReminderPreference) {
        try {
            val pendingIntent = getPendingIntent(preference)
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}