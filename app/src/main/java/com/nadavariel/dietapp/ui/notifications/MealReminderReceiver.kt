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
import androidx.core.app.TaskStackBuilder
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
        Log.i(tag, "MealReminderReceiver: onReceive triggered. Intent action: ${intent?.action}")

        val notificationId = intent?.getIntExtra(NOTIFICATION_ID_EXTRA, 0) ?: 0
        val message = intent?.getStringExtra(NOTIFICATION_MESSAGE_EXTRA) ?: "Time to log your meal!"
        val repetition = intent?.getStringExtra(NOTIFICATION_REPETITION_EXTRA) ?: "DAILY"

        // NEW: Get the Firestore Document ID we added in the Scheduler
        val firestoreId = intent?.getStringExtra("NOTIFICATION_FIRESTORE_ID")

        Log.d(tag, "Receiver details - ID: $notificationId, Message: $message, Repetition: $repetition")

        if (notificationId == 0) {
            Log.e(tag, "Notification ID is 0. Cannot post notification.")
            return
        }

        // --- NEW LOGIC: If this is a One-Time reminder, turn it off in Firestore ---
        if (repetition == "ONCE" && !firestoreId.isNullOrEmpty()) {
            val auth = Firebase.auth
            val userId = auth.currentUser?.uid
            if (userId != null) {
                Log.d(tag, "One-time reminder fired. Updating Firestore to disable: $firestoreId")
                Firebase.firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(firestoreId)
                    .update("isEnabled", false)
                    .addOnFailureListener { e ->
                        Log.e(tag, "Failed to disable one-time reminder in Firestore: ${e.message}")
                    }
            }
        }
        // --------------------------------------------------------------------------

        createNotificationChannel(context)

        val addMealIntent = Intent(
            Intent.ACTION_VIEW,
            "dietapp://add_meal".toUri(),
            context,
            MainActivity::class.java
        )

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(addMealIntent)
            getPendingIntent(
                notificationId,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = try {
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Meal Logging Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        } catch (e: Exception) {
            Log.e(tag, "Error building notification: ${e.message}", e)
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)

        Log.i(tag, "Notification successfully posted with ID: $notificationId")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Meal Reminders"
            val descriptionText = "Reminders to log your daily meals"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(tag, "Notification channel created/updated.")
        }
    }
}