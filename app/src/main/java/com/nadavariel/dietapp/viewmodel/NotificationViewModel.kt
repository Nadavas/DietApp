package com.nadavariel.dietapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.model.NotificationPreference
import com.nadavariel.dietapp.ui.notifications.NotificationScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val scheduler = NotificationScheduler(application.applicationContext)

    private val userId: String? get() = auth.currentUser?.uid
    private val preferencesCollection by lazy {
        firestore.collection("users").document(userId ?: "no_user").collection("notifications")
    }

    private val _notifications = MutableStateFlow<List<NotificationPreference>>(emptyList())
    val notifications = _notifications.asStateFlow()

    init {
        fetchNotifications()
    }

    private fun fetchNotifications() {
        if (userId == null) return

        preferencesCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("NotifVM", "Error listening for notifications: ${e.message}")
                return@addSnapshotListener
            }

            val fetchedList = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<NotificationPreference>()?.copy(id = doc.id)
            } ?: emptyList()

            _notifications.value = fetchedList
            // Reschedule/cancel all alarms based on the fetched state
            updateAllAlarms(fetchedList)
        }
    }

    private fun updateAllAlarms(preferences: List<NotificationPreference>) {
        preferences.forEach { pref ->
            if (pref.isEnabled) {
                scheduler.schedule(pref)
            } else {
                scheduler.cancel(pref)
            }
        }
    }

    fun saveNotification(preference: NotificationPreference) = viewModelScope.launch {
        if (userId == null) return@launch

        try {
            if (preference.id.isBlank()) {
                // New notification: Firestore assigns ID
                val newDoc = preferencesCollection.add(preference).await()
                // Update the scheduler with the new ID
                scheduler.schedule(preference.copy(id = newDoc.id))
            } else {
                // Existing notification: Update
                preferencesCollection.document(preference.id).set(preference).await()

                // Always reschedule after update to pick up new time/status
                if (preference.isEnabled) {
                    scheduler.schedule(preference)
                } else {
                    scheduler.cancel(preference)
                }
            }
        } catch (e: Exception) {
            Log.e("NotifVM", "Error saving notification: ${e.message}")
        }
    }

    fun deleteNotification(preference: NotificationPreference) = viewModelScope.launch {
        if (userId == null || preference.id.isBlank()) return@launch

        try {
            // 1. Cancel the active alarm
            scheduler.cancel(preference)

            // 2. Delete from Firestore
            preferencesCollection.document(preference.id).delete().await()
        } catch (e: Exception) {
            Log.e("NotifVM", "Error deleting notification: ${e.message}")
        }
    }

    fun toggleNotification(preference: NotificationPreference, isEnabled: Boolean) {
        val updatedPref = preference.copy(isEnabled = isEnabled)
        saveNotification(updatedPref) // Save will trigger fetch, which will call updateAllAlarms
    }
}