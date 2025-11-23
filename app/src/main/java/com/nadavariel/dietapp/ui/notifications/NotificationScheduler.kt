package com.nadavariel.dietapp.ui.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nadavariel.dietapp.model.NotificationPreference
import java.util.ArrayList

class NotificationScheduler(private val context: Context) {

    private val tag = "ALARM_DEBUG"
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun getPendingIntent(
        preference: NotificationPreference,
        flags: Int,
        receiverClass: Class<*>
    ): PendingIntent {

        val intent = Intent(context, receiverClass).apply {
            putExtra("NOTIFICATION_FIRESTORE_ID", preference.id)
            putIntegerArrayListExtra("DAYS_OF_WEEK", ArrayList(preference.daysOfWeek))

            if (preference.type == "WEIGHT") {
                putExtra(WeightReminderReceiver.WEIGHT_NOTIF_ID_EXTRA, preference.uniqueId)
                putExtra(WeightReminderReceiver.WEIGHT_MESSAGE_EXTRA, preference.message)
                putExtra(WeightReminderReceiver.WEIGHT_REPETITION_EXTRA, preference.repetition)
            } else {
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

        // Ensure trigger time is in the future
        var triggerTime = preference.getNextScheduledCalendar().timeInMillis
        if (triggerTime <= System.currentTimeMillis()) {
            triggerTime += AlarmManager.INTERVAL_DAY
        }

        Log.i(tag, "Scheduling ${receiverClass.simpleName} for ID: ${preference.uniqueId}. Trigger: $triggerTime")

        if (preference.repetition == "DAILY") {
            // Changed to setRepeating for better accuracy than setInexactRepeating
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
            Log.d(tag, "Scheduled DAILY alarm.")
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
            Log.d(tag, "Scheduled ONCE alarm.")
        }
    }

    fun cancel(preference: NotificationPreference, receiverClass: Class<*>) {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = getPendingIntent(preference, flags, receiverClass)

        alarmManager.cancel(pendingIntent)
        Log.i(tag, "Canceled ${receiverClass.simpleName} for ID: ${preference.uniqueId}")
    }
}