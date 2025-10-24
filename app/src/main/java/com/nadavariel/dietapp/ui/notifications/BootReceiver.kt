package com.nadavariel.dietapp.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.model.NotificationPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.nadavariel.dietapp.ui.notifications.NotificationScheduler // Assuming NotificationScheduler is here

/**
 * This BroadcastReceiver is triggered when the device finishes booting up.
 * Its purpose is to fetch all saved notification preferences and reschedule them
 * using the NotificationScheduler, ensuring alarms persist across reboots.
 */
class BootReceiver : BroadcastReceiver() {

    // Use a CoroutineScope tied to the main dispatcher for async database access
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        // We only care about the boot completion action
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val userId = Firebase.auth.currentUser?.uid
            if (userId == null) {
                // If the user hasn't logged in yet, we can't fetch preferences.
                // Notifications will be scheduled when the user opens the app and logs in.
                return
            }

            val firestore = Firebase.firestore
            val preferencesCollection = firestore
                .collection("users")
                .document(userId)
                .collection("notifications")

            // Use a coroutine to handle asynchronous Firestore calls
            coroutineScope.launch {
                try {
                    val snapshot = preferencesCollection.get().await()

                    val preferencesToSchedule = snapshot.documents.mapNotNull { doc ->
                        doc.toObject<NotificationPreference>()?.copy(id = doc.id)
                    }.filter { it.isEnabled } // Only schedule the ones the user enabled

                    val scheduler = NotificationScheduler(context)
                    preferencesToSchedule.forEach { pref ->
                        scheduler.schedule(pref)
                    }

                } catch (e: Exception) {
                    // Log error if fetching/rescheduling fails
                    // e.g., Log.e("BootReceiver", "Failed to reschedule alarms: ${e.message}")
                }
            }
        }
    }
}