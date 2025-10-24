package com.nadavariel.dietapp.ui.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nadavariel.dietapp.model.NotificationPreference
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(preference: NotificationPreference) {
        if (!preference.isEnabled) return

        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            putExtra(MealReminderReceiver.NOTIFICATION_ID_EXTRA, preference.uniqueId)
            putExtra(MealReminderReceiver.NOTIFICATION_MESSAGE_EXTRA, preference.message)
            putExtra(MealReminderReceiver.NOTIFICATION_REPETITION_EXTRA, preference.repetition)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            preference.uniqueId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val triggerTime = preference.getNextScheduledCalendar().timeInMillis

        if (preference.repetition == "DAILY") {
            // Use setInexactRepeating for daily alarms to save battery, using a full day interval
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } else { // "ONCE"
            // Use setExactAndAllowWhileIdle for one-time, time-critical alarms
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancel(preference: NotificationPreference) {
        val intent = Intent(context, MealReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            preference.uniqueId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // You will need a function in a BootReceiver to call this after device reboot
    // fun rescheduleAll(preferences: List<NotificationPreference>) {
    //    preferences.forEach { schedule(it) }
    // }
}