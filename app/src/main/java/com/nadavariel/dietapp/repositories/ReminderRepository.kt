package com.nadavariel.dietapp.repositories

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.models.ReminderPreference
import com.nadavariel.dietapp.scheduling.ReminderScheduler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReminderRepository(context: Context) {

    private val firestore: FirebaseFirestore = Firebase.firestore
    private val scheduler = ReminderScheduler(context)

    // --- Fetch Notifications Flow ---
    fun getNotificationsFlow(userId: String): Flow<List<ReminderPreference>> = callbackFlow {
        val collection = firestore.collection("users").document(userId).collection("notifications")

        val listener = collection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("ReminderRepo", "Error listening for notifications", e)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val fetchedList = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<ReminderPreference>()?.copy(id = doc.id)
            } ?: emptyList()
            trySend(fetchedList)
        }
        awaitClose { listener.remove() }
    }

    // --- Save & Schedule ---
    suspend fun saveNotification(userId: String, preference: ReminderPreference) {
        val collection = firestore.collection("users").document(userId).collection("notifications")

        // 1. Save to DB
        var prefToSave = preference
        if (preference.id.isBlank()) {
            val newDoc = collection.add(preference).await()
            prefToSave = preference.copy(id = newDoc.id)
            // Update the doc with the generated ID so local and remote match
            collection.document(prefToSave.id).set(prefToSave).await()
        } else {
            collection.document(preference.id).set(preference).await()
        }

        // 2. Schedule Alarm
        if (prefToSave.isEnabled) {
            scheduler.schedule(prefToSave)
            Log.d("ReminderRepo", "Scheduled alarm for: ${prefToSave.id}")
        }
    }

    // --- Delete & Cancel ---
    suspend fun deleteNotification(userId: String, preference: ReminderPreference) {
        if (preference.id.isBlank()) return

        // 1. Cancel Alarm
        scheduler.cancel(preference)
        Log.d("ReminderRepo", "Cancelled alarm for: ${preference.id}")

        // 2. Delete from DB
        firestore.collection("users").document(userId)
            .collection("notifications").document(preference.id)
            .delete().await()
    }

    // --- Toggle ---
    suspend fun toggleNotification(userId: String, preference: ReminderPreference, isEnabled: Boolean) {
        val updatedPref = preference.copy(isEnabled = isEnabled)

        // 1. Schedule or Cancel
        if (isEnabled) {
            scheduler.schedule(updatedPref)
        } else {
            scheduler.cancel(updatedPref)
        }

        // 2. Update DB
        firestore.collection("users").document(userId)
            .collection("notifications").document(preference.id)
            .update("isEnabled", isEnabled).await()
    }
}