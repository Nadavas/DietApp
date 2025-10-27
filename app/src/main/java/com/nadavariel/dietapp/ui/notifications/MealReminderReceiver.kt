package com.nadavariel.dietapp.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log // NEW: Import for debugging
import androidx.core.app.NotificationCompat
import com.nadavariel.dietapp.MainActivity // Assuming your main activity is named MainActivity
import com.nadavariel.dietapp.R // Make sure you have an R file with drawables/strings

class MealReminderReceiver : BroadcastReceiver() {

    private val TAG = "ALARM_DEBUG"

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "meal_reminder_channel"
        const val NOTIFICATION_ID_EXTRA = "notification_id_extra"
        const val NOTIFICATION_MESSAGE_EXTRA = "notification_message_extra"
        const val NOTIFICATION_REPETITION_EXTRA = "notification_repetition_extra"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        // STEP 2 DEBUGGING: Log immediately to confirm the receiver was triggered by the system
        Log.i(TAG, "MealReminderReceiver: onReceive triggered. Intent action: ${intent?.action}")

        val notificationId = intent?.getIntExtra(NOTIFICATION_ID_EXTRA, 0) ?: 0
        val message = intent?.getStringExtra(NOTIFICATION_MESSAGE_EXTRA) ?: "Time to log your meal!"
        val repetition = intent?.getStringExtra(NOTIFICATION_REPETITION_EXTRA) ?: "DAILY"

        Log.d(TAG, "Receiver details - ID: $notificationId, Message: $message, Repetition: $repetition")

        if (notificationId == 0) {
            Log.e(TAG, "Notification ID is 0. Cannot post notification.")
            return
        }

        createNotificationChannel(context)

        // Create an intent to launch your app when the notification is tapped
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = try {
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your app's notification icon here
                .setContentTitle("Meal Logging Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        } catch (e: Exception) {
            // STEP 3 DEBUGGING: Catch common errors like missing icon resource (R.drawable.ic_launcher_foreground)
            Log.e(TAG, "Error building notification: ${e.message}", e)
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)

        // NEW LOG: Confirm notification has been posted to the manager
        Log.i(TAG, "Notification successfully posted with ID: $notificationId")

        // Note on "ONCE" alarms:
        // If the alarm was set using setExact... (for ONCE), you should also explicitly cancel it here,
        // as exact alarms don't automatically cancel themselves after firing once on newer APIs.
        // However, for now, we leave this logic out to focus on the initial failure.
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
            Log.d(TAG, "Notification channel created/updated.")
        }
    }
}