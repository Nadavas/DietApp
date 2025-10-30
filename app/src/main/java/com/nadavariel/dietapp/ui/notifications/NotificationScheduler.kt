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

    // Updated to accept a generic receiver class
    private fun getPendingIntent(
        preference: NotificationPreference,
        flags: Int,
        receiverClass: Class<*> // e.g., MealReminderReceiver::class.java
    ): PendingIntent {

        // Use the correct receiver class
        val intent = Intent(context, receiverClass).apply {
            // Use different extras for different receivers to avoid conflicts
            if (receiverClass == MealReminderReceiver::class.java) {
                putExtra(MealReminderReceiver.NOTIFICATION_ID_EXTRA, preference.uniqueId)
                putExtra(MealReminderReceiver.NOTIFICATION_MESSAGE_EXTRA, preference.message)
                putExtra(MealReminderReceiver.NOTIFICATION_REPETITION_EXTRA, preference.repetition)
            } else {
                putExtra(WeightReminderReceiver.WEIGHT_NOTIF_ID_EXTRA, preference.uniqueId)
                putExtra(WeightReminderReceiver.WEIGHT_MESSAGE_EXTRA, preference.message)
            }
        }

        return PendingIntent.getBroadcast(
            context,
            preference.uniqueId,
            intent,
            flags
        )
    }

    // Overload the old schedule function to default to MealReminderReceiver (keeps old code working)
    fun schedule(preference: NotificationPreference) {
        schedule(preference, MealReminderReceiver::class.java)
    }

    // Overload the old cancel function
    fun cancel(preference: NotificationPreference) {
        cancel(preference, MealReminderReceiver::class.java)
    }

    // New schedule function that accepts the receiver class
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

    // New cancel function that accepts the receiver class
    fun cancel(preference: NotificationPreference, receiverClass: Class<*>) {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = getPendingIntent(preference, flags, receiverClass)

        alarmManager.cancel(pendingIntent)
        Log.i(TAG, "Canceled ${receiverClass.simpleName} for ID: ${preference.uniqueId}")
    }
}