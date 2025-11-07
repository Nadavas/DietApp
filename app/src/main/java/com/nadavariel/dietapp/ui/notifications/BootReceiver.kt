package com.nadavariel.dietapp.ui.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.model.NotificationPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BootReceiver : BroadcastReceiver() {

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val userId = Firebase.auth.currentUser?.uid
            if (userId == null) {
                Log.d("BootReceiver", "User not logged in, skipping alarm reschedule.")
                return
            }

            val firestore = Firebase.firestore
            // Reads from the single "notifications" collection
            val preferencesCollection = firestore
                .collection("users")
                .document(userId)
                .collection("notifications")

            coroutineScope.launch {
                try {
                    val snapshot = preferencesCollection.get().await()

                    val preferencesToSchedule = snapshot.documents.mapNotNull { doc ->
                        doc.toObject<NotificationPreference>()?.copy(id = doc.id)
                    }.filter { it.isEnabled }

                    val scheduler = NotificationScheduler(context)

                    // Check the type and schedule with the correct receiver
                    preferencesToSchedule.forEach { pref ->
                        if (pref.type == "WEIGHT") {
                            scheduler.schedule(pref, WeightReminderReceiver::class.java)
                        } else {
                            // Default to Meal
                            scheduler.schedule(pref, MealReminderReceiver::class.java)
                        }
                    }
                    Log.d("BootReceiver", "Rescheduled ${preferencesToSchedule.size} alarms.")

                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to reschedule alarms: ${e.message}")
                }
            }
        }
    }
}