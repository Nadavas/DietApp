package com.nadavariel.dietapp.ui.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nadavariel.dietapp.model.NotificationPreference

class NotificationScheduler(private val context: Context) {

    private val TAG = "ALARM_DEBUG"
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun getPendingIntent(preference: NotificationPreference, flags: Int): PendingIntent {
        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            putExtra(MealReminderReceiver.NOTIFICATION_ID_EXTRA, preference.uniqueId)
            putExtra(MealReminderReceiver.NOTIFICATION_MESSAGE_EXTRA, preference.message)
            putExtra(MealReminderReceiver.NOTIFICATION_REPETITION_EXTRA, preference.repetition)
        }

        return PendingIntent.getBroadcast(
            context,
            preference.uniqueId,
            intent,
            flags
        )
    }

    fun schedule(preference: NotificationPreference) {
        if (!preference.isEnabled) {
            Log.d(TAG, "Not scheduling alarm for ID ${preference.uniqueId}: Disabled.")
            return
        }

        // Flags must match for scheduling and canceling
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = getPendingIntent(preference, flags)
        val triggerTime = preference.getNextScheduledCalendar().timeInMillis

        val readableTime = preference.getNextScheduledCalendar().time.toString()
        Log.i(TAG, "Scheduling alarm for ID: ${preference.uniqueId}. Next trigger: $readableTime")

        if (preference.repetition == "DAILY") {
            // BEST PRACTICE: Use setInexactRepeating for recurring non-critical alarms.
            // This is battery-friendly and does not require special permissions.
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(TAG, "Scheduled DAILY (inexact) alarm.")
        } else { // "ONCE"
            // FIX: Replaced setExactAndAllowWhileIdle (which crashes) with setAndAllowWhileIdle.
            // setAndAllowWhileIdle is inexact but allows the alarm to fire even in Doze mode,
            // which is suitable for a single-time meal reminder without requiring the special exact alarm permission.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d(TAG, "Scheduled ONCE (non-exact, Doze-safe) alarm.")
        }
    }

    fun cancel(preference: NotificationPreference) {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = getPendingIntent(preference, flags)

        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "Canceled alarm for ID: ${preference.uniqueId}")
    }
}