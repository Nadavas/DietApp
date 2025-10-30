package com.nadavariel.dietapp.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.nadavariel.dietapp.MainActivity
import com.nadavariel.dietapp.R

class WeightReminderReceiver : BroadcastReceiver() {

    private val TAG = "ALARM_DEBUG_WEIGHT"

    companion object {
        // Use a different channel ID for weight reminders
        const val WEIGHT_CHANNEL_ID = "weight_reminder_channel"
        const val WEIGHT_NOTIF_ID_EXTRA = "weight_notification_id"
        const val WEIGHT_MESSAGE_EXTRA = "weight_message"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val notificationId = intent?.getIntExtra(WEIGHT_NOTIF_ID_EXTRA, 2) ?: 2 // Use a different base ID (e.g., 2)
        val message = intent?.getStringExtra(WEIGHT_MESSAGE_EXTRA) ?: "Time to log your weight!"

        Log.i(TAG, "WeightReminderReceiver: onReceive triggered.")

        createNotificationChannel(context)

        // This deep link will open the home screen and tell it to open the log weight dialog
        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("dietapp://home?openWeightLog=true"), // New deep link URI
            context,
            MainActivity::class.java
        )

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(deepLinkIntent)
            getPendingIntent(
                notificationId,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notification = NotificationCompat.Builder(context, WEIGHT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use your icon
            .setContentTitle("Weight Log Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
        Log.i(TAG, "Weight notification posted with ID: $notificationId")
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Weight Reminders"
            val descriptionText = "Reminders to log your weight"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(WEIGHT_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}