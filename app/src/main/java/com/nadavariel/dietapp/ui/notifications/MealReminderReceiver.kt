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

class MealReminderReceiver : BroadcastReceiver() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "meal_reminder_channel"
        const val NOTIFICATION_ID_EXTRA = "notification_id_extra"
        const val NOTIFICATION_MESSAGE_EXTRA = "notification_message_extra"
        const val NOTIFICATION_REPETITION_EXTRA = "notification_repetition_extra"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val notificationId = intent?.getIntExtra(NOTIFICATION_ID_EXTRA, 0) ?: 0
        val message = intent?.getStringExtra(NOTIFICATION_MESSAGE_EXTRA) ?: "Time to log your meal!"
        val repetition = intent?.getStringExtra(NOTIFICATION_REPETITION_EXTRA) ?: "DAILY"
        val firestoreId = intent?.getStringExtra("NOTIFICATION_FIRESTORE_ID")
        val daysOfWeek = intent?.getIntegerArrayListExtra("DAYS_OF_WEEK")
        val hour = intent?.getIntExtra("HOUR", -1) ?: -1
        val minute = intent?.getIntExtra("MINUTE", -1) ?: -1

        if (notificationId == 0) return

        // --- RESCHEDULE LOGIC ---
        if (repetition == "DAILY" && hour != -1 && minute != -1 && intent != null) {
            scheduleNextOccurrence(context, notificationId, intent, hour, minute)
        }

        // --- Check Day of Week ---
        if (repetition == "DAILY" && daysOfWeek != null && daysOfWeek.isNotEmpty()) {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            if (!daysOfWeek.contains(today)) {
                return
            }
        }

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

        createNotificationChannel(context)

        val addMealIntent = Intent(
            Intent.ACTION_VIEW,
            "dietapp://add_meal".toUri(),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            addMealIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Meal Logging Reminder")
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
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val nextIntent = Intent(context, MealReminderReceiver::class.java).apply {
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

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Meal Reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = "Reminders to log your daily meals"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}