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

class WeightReminderReceiver : BroadcastReceiver() {

    private val tag = "ALARM_DEBUG_WEIGHT"

    companion object {
        const val WEIGHT_CHANNEL_ID = "weight_reminder_channel"
        const val WEIGHT_NOTIF_ID_EXTRA = "weight_notification_id"
        const val WEIGHT_MESSAGE_EXTRA = "weight_message"
        const val WEIGHT_REPETITION_EXTRA = "weight_repetition" // NEW constant
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val notificationId = intent?.getIntExtra(WEIGHT_NOTIF_ID_EXTRA, 2) ?: 2
        val message = intent?.getStringExtra(WEIGHT_MESSAGE_EXTRA) ?: "Time to log your weight!"

        // NEW: Get Firestore ID and Repetition
        val repetition = intent?.getStringExtra(WEIGHT_REPETITION_EXTRA) ?: "DAILY"
        val firestoreId = intent?.getStringExtra("NOTIFICATION_FIRESTORE_ID")

        Log.i(tag, "WeightReminderReceiver: onReceive triggered.")

        // --- NEW LOGIC: Disable One-Time Reminder in Firestore ---
        if (repetition == "ONCE" && !firestoreId.isNullOrEmpty()) {
            val auth = Firebase.auth
            val userId = auth.currentUser?.uid
            if (userId != null) {
                Log.d(tag, "One-time weight reminder fired. Disabling in Firestore: $firestoreId")
                Firebase.firestore.collection("users")
                    .document(userId)
                    .collection("notifications")
                    .document(firestoreId)
                    .update("isEnabled", false)
                    .addOnFailureListener { e ->
                        Log.e(tag, "Failed to disable weight reminder: ${e.message}")
                    }
            }
        }
        // ---------------------------------------------------------

        createNotificationChannel(context)

        val deepLinkIntent = Intent(
            Intent.ACTION_VIEW,
            "dietapp://home?openWeightLog=true".toUri(),
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Weight Log Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
        Log.i(tag, "Weight notification posted with ID: $notificationId")
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