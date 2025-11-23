package com.nadavariel.dietapp.ui.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.MainActivity
import com.nadavariel.dietapp.R
import java.util.Calendar

class WeightReminderReceiver : BroadcastReceiver() {

    private val tag = "ALARM_DEBUG_WEIGHT"

    companion object {
        const val WEIGHT_CHANNEL_ID = "weight_reminder_channel"
        const val WEIGHT_NOTIF_ID_EXTRA = "weight_notification_id"
        const val WEIGHT_MESSAGE_EXTRA = "weight_message"
        const val WEIGHT_REPETITION_EXTRA = "weight_repetition"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        Log.d(tag, ">>> WeightReceiver: onReceive TRIGGERED! <<<")

        val notificationId = intent?.getIntExtra(WEIGHT_NOTIF_ID_EXTRA, 2) ?: 2
        val message = intent?.getStringExtra(WEIGHT_MESSAGE_EXTRA) ?: "Time to log your weight!"
        val repetition = intent?.getStringExtra(WEIGHT_REPETITION_EXTRA) ?: "DAILY"
        val firestoreId = intent?.getStringExtra("NOTIFICATION_FIRESTORE_ID")
        val daysOfWeek = intent?.getIntegerArrayListExtra("DAYS_OF_WEEK")
        val hour = intent?.getIntExtra("HOUR", -1) ?: -1
        val minute = intent?.getIntExtra("MINUTE", -1) ?: -1

        Log.d(tag, "WeightReceiver: ID=$notificationId, Repetition=$repetition")

        // --- RESCHEDULE LOGIC ---
        if (repetition == "DAILY" && hour != -1 && minute != -1 && intent != null) {
            scheduleNextOccurrence(context, notificationId, intent, hour, minute)
        } else {
            Log.d(tag, "WeightReceiver: Not rescheduling.")
        }

        // --- Check Day of Week ---
        if (repetition == "DAILY" && daysOfWeek != null && daysOfWeek.isNotEmpty()) {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            if (!daysOfWeek.contains(today)) {
                Log.d(tag, "WeightReceiver: SKIPPING. Today ($today) is not in $daysOfWeek.")
                return
            } else {
                Log.d(tag, "WeightReceiver: Day OK.")
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
                    .addOnFailureListener { Log.e(tag, "WeightReceiver: Firestore update failed", it) }
            }
        }

        createNotificationChannel(context)

        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            "dietapp://weight_tracker?openWeightLog=true".toUri(),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            deepLinkIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, WEIGHT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Weight Log Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
        Log.d(tag, "WeightReceiver: Notification POSTED.")
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

        val nextIntent = Intent(context, WeightReminderReceiver::class.java).apply {
            if (oldIntent.extras != null) putExtras(oldIntent.extras!!)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            nextIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        Log.d(tag, "WeightReceiver: Rescheduling Next for: ${nextAlarm.time}")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarm.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarm.timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            Log.e(tag, "WeightReceiver: Error rescheduling", e)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weight Reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(WEIGHT_CHANNEL_ID, name, importance).apply {
                description = "Reminders to log your weight"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}