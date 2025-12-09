package com.nadavariel.dietapp.ui.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.MainActivity
import com.nadavariel.dietapp.R
import java.util.Calendar

class UniversalReminderReceiver : BroadcastReceiver() {

    companion object {
        // Shared Keys
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_MESSAGE = "notification_message"
        const val EXTRA_REPETITION = "notification_repetition"
        const val EXTRA_TYPE = "notification_type" // "MEAL" or "WEIGHT"
        const val EXTRA_FIRESTORE_ID = "notification_firestore_id"
        const val EXTRA_DAYS_OF_WEEK = "days_of_week"
        const val EXTRA_HOUR = "hour"
        const val EXTRA_MINUTE = "minute"

        // Channel IDs
        const val CHANNEL_ID_MEAL = "meal_reminder_channel"
        const val CHANNEL_ID_WEIGHT = "weight_reminder_channel"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Time to log!"
        val repetition = intent.getStringExtra(EXTRA_REPETITION) ?: "DAILY"
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "MEAL"
        val firestoreId = intent.getStringExtra(EXTRA_FIRESTORE_ID)
        val daysOfWeek = intent.getIntegerArrayListExtra(EXTRA_DAYS_OF_WEEK)
        val hour = intent.getIntExtra(EXTRA_HOUR, -1)
        val minute = intent.getIntExtra(EXTRA_MINUTE, -1)

        if (notificationId == 0) return

        // --- 1. RESCHEDULE LOGIC (For Daily) ---
        // We reschedule immediately to ensure the next alarm is set even if the app isn't opened
        if (repetition == "DAILY" && hour != -1 && minute != -1) {
            scheduleNextOccurrence(context, notificationId, intent, hour, minute)
        }

        // --- 2. CHECK DAY OF WEEK ---
        if (repetition == "DAILY" && daysOfWeek != null && daysOfWeek.isNotEmpty()) {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            // Calendar.SUNDAY is 1, SATURDAY is 7. Ensure your saving logic matches this.
            if (!daysOfWeek.contains(today)) {
                return // Skip showing notification today, but alarm was already rescheduled for tomorrow above
            }
        }

        // --- 3. DISABLE "ONCE" ALARMS IN FIRESTORE ---
        if (repetition == "ONCE" && !firestoreId.isNullOrEmpty()) {
            val userId = Firebase.auth.currentUser?.uid
            if (userId != null) {
                Firebase.firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(firestoreId)
                    .update("isEnabled", false)
            }
        }

        // --- 4. DETERMINE CHANNEL AND DEEP LINK BASED ON TYPE ---
        val channelId: String
        val deepLinkUri: String
        val title: String

        if (type == "WEIGHT") {
            channelId = CHANNEL_ID_WEIGHT
            deepLinkUri = "dietapp://weight_tracker?openWeightLog=true"
            title = "Weight Log Reminder"
        } else {
            channelId = CHANNEL_ID_MEAL
            deepLinkUri = "dietapp://add_meal"
            title = "Meal Logging Reminder"
        }

        createNotificationChannel(context, type)

        // --- 5. SHOW NOTIFICATION ---
        val appIntent = Intent(
            Intent.ACTION_VIEW,
            deepLinkUri.toUri(),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun scheduleNextOccurrence(context: Context, notificationId: Int, oldIntent: Intent, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val nextAlarm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, 1) // Add 1 day
        }

        val nextIntent = Intent(context, UniversalReminderReceiver::class.java).apply {
            if (oldIntent.extras != null) putExtras(oldIntent.extras!!)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarm.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarm.timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel(context: Context, type: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val id = if (type == "WEIGHT") CHANNEL_ID_WEIGHT else CHANNEL_ID_MEAL
            val name = if (type == "WEIGHT") "Weight Reminders" else "Meal Reminders"
            val desc = if (type == "WEIGHT") "Reminders to log your weight" else "Reminders to log your daily meals"

            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(id, name, importance).apply {
                description = desc
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}