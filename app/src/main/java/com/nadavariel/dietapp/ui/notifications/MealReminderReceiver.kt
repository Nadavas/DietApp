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

class MealReminderReceiver : BroadcastReceiver() {

    private val tag = "ALARM_DEBUG"

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

        if (notificationId == 0) return

        // Logic to turn off "ONCE" reminders
        if (repetition == "ONCE" && !firestoreId.isNullOrEmpty()) {
            val userId = Firebase.auth.currentUser?.uid
            if (userId != null) {
                Firebase.firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(firestoreId)
                    .update("isEnabled", false)
                    .addOnFailureListener { Log.e(tag, "Failed to disable reminder: ${it.message}") }
            }
        }

        createNotificationChannel(context)

        // FIX: Use standard Intent logic instead of TaskStackBuilder
        val addMealIntent = Intent(
            Intent.ACTION_VIEW,
            "dietapp://add_meal".toUri(),
            context,
            MainActivity::class.java
        ).apply {
            // These flags help ensure we don't create multiple instances of the app
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