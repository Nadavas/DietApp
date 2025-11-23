package com.nadavariel.dietapp.ui.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nadavariel.dietapp.model.NotificationPreference
import java.util.ArrayList
import java.util.Calendar

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun getPendingIntent(
        preference: NotificationPreference,
        receiverClass: Class<*>
    ): PendingIntent {

        val intent = Intent(context, receiverClass).apply {
            putExtra("NOTIFICATION_FIRESTORE_ID", preference.id)
            putIntegerArrayListExtra("DAYS_OF_WEEK", ArrayList(preference.daysOfWeek))

            // Pass Hour/Minute for rescheduling
            putExtra("HOUR", preference.hour)
            putExtra("MINUTE", preference.minute)

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

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        return PendingIntent.getBroadcast(
            context,
            preference.uniqueId,
            intent,
            flags
        )
    }

    fun schedule(preference: NotificationPreference, receiverClass: Class<*>) {
        if (!preference.isEnabled) return

        val pendingIntent = getPendingIntent(preference, receiverClass)

        val calendar = preference.getNextScheduledCalendar()

        // Zero out seconds for exact timing
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var triggerTime = calendar.timeInMillis
        val currentTime = System.currentTimeMillis()

        // Grace period: if time is > 60s in the past, schedule for tomorrow.
        // If it's within 60s (just clicked save), fire immediately.
        if (triggerTime <= currentTime - 60_000) {
            triggerTime += AlarmManager.INTERVAL_DAY
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // Fallback if permission revoked
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancel(preference: NotificationPreference, receiverClass: Class<*>) {
        try {
            val pendingIntent = getPendingIntent(preference, receiverClass)
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}