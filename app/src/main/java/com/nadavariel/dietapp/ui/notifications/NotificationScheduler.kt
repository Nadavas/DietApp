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

    private fun getPendingIntent(
        preference: NotificationPreference,
        flags: Int,
        receiverClass: Class<*>
    ): PendingIntent {

        val intent = Intent(context, receiverClass).apply {
            // Check the type to set the correct extras
            if (preference.type == "WEIGHT") {
                putExtra(WeightReminderReceiver.WEIGHT_NOTIF_ID_EXTRA, preference.uniqueId)
                putExtra(WeightReminderReceiver.WEIGHT_MESSAGE_EXTRA, preference.message)
            } else {
                // Default to MEAL
                putExtra(MealReminderReceiver.NOTIFICATION_ID_EXTRA, preference.uniqueId)
                putExtra(MealReminderReceiver.NOTIFICATION_MESSAGE_EXTRA, preference.message)
                putExtra(MealReminderReceiver.NOTIFICATION_REPETITION_EXTRA, preference.repetition)
            }
        }

        return PendingIntent.getBroadcast(
            context,
            preference.uniqueId,
            intent,
            flags
        )
    }

    // This default function is no longer safe as we don't know the type.
    // All calls should use the specific schedule/cancel methods.
    // fun schedule(preference: NotificationPreference) { ... }
    // fun cancel(preference: NotificationPreference) { ... }

    fun schedule(preference: NotificationPreference, receiverClass: Class<*>) {
        if (!preference.isEnabled) {
            Log.d(TAG, "Not scheduling alarm for ID ${preference.uniqueId}: Disabled.")
            return
        }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = getPendingIntent(preference, flags, receiverClass)
        val triggerTime = preference.getNextScheduledCalendar().timeInMillis

        val readableTime = preference.getNextScheduledCalendar().time.toString()
        Log.i(TAG, "Scheduling ${receiverClass.simpleName} for ID: ${preference.uniqueId}. Next trigger: $readableTime")

        if (preference.repetition == "DAILY") {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(TAG, "Scheduled DAILY (inexact) alarm.")
        } else { // "ONCE"
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d(TAG, "Scheduled ONCE (non-exact, Doze-safe) alarm.")
        }
    }

    fun cancel(preference: NotificationPreference, receiverClass: Class<*>) {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = getPendingIntent(preference, flags, receiverClass)

        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "Canceled ${receiverClass.simpleName} for ID: ${preference.uniqueId}")
    }
}