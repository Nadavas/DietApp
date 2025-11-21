package com.nadavariel.dietapp.ui.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nadavariel.dietapp.model.NotificationPreference

class NotificationScheduler(private val context: Context) {

    private val tag = "ALARM_DEBUG"
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun getPendingIntent(
        preference: NotificationPreference,
        flags: Int,
        receiverClass: Class<*>
    ): PendingIntent {

        val intent = Intent(context, receiverClass).apply {
            // Pass the Firestore Document ID so the receiver can update the DB
            putExtra("NOTIFICATION_FIRESTORE_ID", preference.id)

            // Check the type to set the correct extras
            if (preference.type == "WEIGHT") {
                putExtra(WeightReminderReceiver.WEIGHT_NOTIF_ID_EXTRA, preference.uniqueId)
                putExtra(WeightReminderReceiver.WEIGHT_MESSAGE_EXTRA, preference.message)
                // NEW: Pass repetition so WeightReceiver knows if it's ONCE
                putExtra(WeightReminderReceiver.WEIGHT_REPETITION_EXTRA, preference.repetition)
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

    fun schedule(preference: NotificationPreference, receiverClass: Class<*>) {
        if (!preference.isEnabled) {
            Log.d(tag, "Not scheduling alarm for ID ${preference.uniqueId}: Disabled.")
            return
        }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = getPendingIntent(preference, flags, receiverClass)
        val triggerTime = preference.getNextScheduledCalendar().timeInMillis

        val readableTime = preference.getNextScheduledCalendar().time.toString()
        Log.i(tag, "Scheduling ${receiverClass.simpleName} for ID: ${preference.uniqueId}. Next trigger: $readableTime")

        if (preference.repetition == "DAILY") {
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(tag, "Scheduled DAILY (inexact) alarm.")
        } else { // "ONCE"
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d(tag, "Scheduled ONCE (non-exact, Doze-safe) alarm.")
        }
    }

    fun cancel(preference: NotificationPreference, receiverClass: Class<*>) {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = getPendingIntent(preference, flags, receiverClass)

        alarmManager.cancel(pendingIntent)
        Log.i(tag, "Canceled ${receiverClass.simpleName} for ID: ${preference.uniqueId}")
    }
}