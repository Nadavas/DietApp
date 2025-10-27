package com.nadavariel.dietapp.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri // NEW: Import
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder // NEW: Import
import com.nadavariel.dietapp.MainActivity
import com.nadavariel.dietapp.R

class MealReminderReceiver : BroadcastReceiver() {

    private val TAG = "ALARM_DEBUG"

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "meal_reminder_channel"
        const val NOTIFICATION_ID_EXTRA = "notification_id_extra"
        const val NOTIFICATION_MESSAGE_EXTRA = "notification_message_extra"
        const val NOTIFICATION_REPETITION_EXTRA = "notification_repetition_extra"
    }

    override fun onReceive(context: Context, intent: Intent?) {
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

        // --- NEW: Create a deep link intent with a back stack ---

        // 1. Create the deep link Intent for the AddEditMealScreen
        val addMealIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("dietapp://add_meal"), // The custom URI we defined
            context,
            MainActivity::class.java // The activity that hosts the NavGraph
        )

        // 2. Use TaskStackBuilder to create a proper back stack (so "back" goes to Home)
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            // This reads the manifest and adds the parent (MainActivity) automatically
            addNextIntentWithParentStack(addMealIntent)

            // Create the PendingIntent
            getPendingIntent(
                notificationId,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        // --- End of new intent logic ---


        val notification = try {
            NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Meal Logging Reminder")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent) // Use the new deep link pendingIntent
                .setAutoCancel(true)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Error building notification: ${e.message}", e)
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)

        Log.i(TAG, "Notification successfully posted with ID: $notificationId")
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