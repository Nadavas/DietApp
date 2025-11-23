package com.nadavariel.dietapp.ui.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.nadavariel.dietapp.model.NotificationPreference
import java.util.ArrayList
import java.util.Calendar
import java.util.Date

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

        return PendingIntent.getBroadcast(
            context,
            preference.uniqueId,
            intent,
            flags
        )
    }

    fun schedule(preference: NotificationPreference, receiverClass: Class<*>) {
        if (!preference.isEnabled) {
            Log.d(tag, "Scheduler: Skipped ID ${preference.uniqueId} (Disabled)")
            return
        }

        Log.d(tag, "--------------------------------------------------")
        Log.d(tag, "Scheduler: Preparing to schedule ID ${preference.uniqueId} (${preference.type})")

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = getPendingIntent(preference, flags, receiverClass)

        val calendar = preference.getNextScheduledCalendar()

        // Zero out seconds for exact timing
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var triggerTime = calendar.timeInMillis
        val currentTime = System.currentTimeMillis()

        Log.d(tag, "Scheduler: Target Original: ${Date(triggerTime)}")
        Log.d(tag, "Scheduler: Current Time : ${Date(currentTime)}")

        // Grace period logic (60 seconds)
        if (triggerTime <= currentTime - 60_000) {
            Log.d(tag, "Scheduler: Target is in the past (> 60s). Adding 1 Day.")
            triggerTime += AlarmManager.INTERVAL_DAY
        } else {
            Log.d(tag, "Scheduler: Target is valid (Future or within 60s grace).")
        }

        Log.d(tag, "Scheduler: FINAL Trigger : ${Date(triggerTime)}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    Log.d(tag, "Scheduler: Permission GRANTED. Setting ExactAndAllowWhileIdle.")
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    Log.e(tag, "Scheduler: Permission DENIED (canScheduleExactAlarms=false). Using Inexact fallback.")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                Log.d(tag, "Scheduler: Android < S. Setting ExactAndAllowWhileIdle.")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e(tag, "Scheduler: CRASH/SECURITY ERROR! Missing Permission?", e)
        }
    }

    fun cancel(preference: NotificationPreference, receiverClass: Class<*>) {
        try {
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            val pendingIntent = getPendingIntent(preference, flags, receiverClass)
            alarmManager.cancel(pendingIntent)
            Log.i(tag, "Scheduler: Canceled alarm for ID: ${preference.uniqueId}")
        } catch (e: Exception) {
            Log.e(tag, "Scheduler: Error cancelling alarm", e)
        }
    }
}