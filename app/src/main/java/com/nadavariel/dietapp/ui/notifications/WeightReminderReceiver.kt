package com.nadavariel.dietapp.ui.notifications

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
        val notificationId = intent?.getIntExtra(WEIGHT_NOTIF_ID_EXTRA, 2) ?: 2
        val message = intent?.getStringExtra(WEIGHT_MESSAGE_EXTRA) ?: "Time to log your weight!"
        val repetition = intent?.getStringExtra(WEIGHT_REPETITION_EXTRA) ?: "DAILY"
        val firestoreId = intent?.getStringExtra("NOTIFICATION_FIRESTORE_ID")

        // NEW: Get the allowed days
        val daysOfWeek = intent?.getIntegerArrayListExtra("DAYS_OF_WEEK")

        // --- NEW LOGIC: Check Day of Week ---
        if (repetition == "DAILY" && daysOfWeek != null && daysOfWeek.isNotEmpty()) {
            val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            if (!daysOfWeek.contains(today)) {
                Log.d(tag, "Weight Alarm woke up, but today ($today) is not in selected days $daysOfWeek. Skipping.")
                return
            }
        }
        // ------------------------------------

        if (repetition == "ONCE" && !firestoreId.isNullOrEmpty()) {
            val userId = Firebase.auth.currentUser?.uid
            if (userId != null) {
                Firebase.firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(firestoreId)
                    .update("isEnabled", false)
                    .addOnFailureListener { Log.e(tag, "Failed to disable weight reminder: ${it.message}") }
            }
        }

        createNotificationChannel(context)

        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            "dietapp://weight_tracker?openWeightLog=true".toUri(),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
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