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

            // FIX: The listener ONLY updates the UI state.
            // It no longer triggers any alarm logic. This stops the race condition.
            _notifications.value = fetchedList
            // REMOVED: updateAllAlarms(fetchedList)
        }
    }

    // This function is no longer called by the listener,
    // but can be used by other parts of the app if needed (e.g., BootReceiver).
    private fun updateAllAlarms(preferences: List<NotificationPreference>) {
        preferences.forEach { pref ->
            if (pref.isEnabled) {
                scheduler.schedule(pref)
            } else {
                scheduler.cancel(pref)
            }
        }
    }

    // Encapsulated save logic
    private suspend fun writeNotificationToFirestore(preference: NotificationPreference) {
        if (preference.id.isBlank()) {
            val newDoc = preferencesCollection.add(preference).await()
            // Immediately update the Firestore document with its own ID
            preferencesCollection.document(newDoc.id).set(preference.copy(id = newDoc.id)).await()
        } else {
            preferencesCollection.document(preference.id).set(preference).await()
        }
    }

    // FIX: saveNotification (from the dialog) must now handle its own scheduling.
    fun saveNotification(preference: NotificationPreference) = viewModelScope.launch {
        if (userId == null) return@launch
        try {
            var prefToSave = preference
            if (preference.id.isBlank()) {
                val newDoc = preferencesCollection.add(preference).await()
                prefToSave = preference.copy(id = newDoc.id)
                // Write again to set the ID
                preferencesCollection.document(prefToSave.id).set(prefToSave).await()
            } else {
                preferencesCollection.document(preference.id).set(preference).await()
            }

            // Manually schedule the new/edited item
            if (prefToSave.isEnabled) {
                scheduler.schedule(prefToSave)
            }
        } catch (e: Exception) {
            Log.e("NotifVM", "Error saving notification: ${e.message}")
        }
    }

    fun deleteNotification(preference: NotificationPreference) = viewModelScope.launch {
        if (userId == null || preference.id.isBlank()) return@launch

        try {
            // Manually cancel the alarm
            scheduler.cancel(preference)
            preferencesCollection.document(preference.id).delete().await()
        } catch (e: Exception) {
            Log.e("NotifVM", "Error deleting notification: ${e.message}")
        }
    }

    // FIX: toggleNotification now handles the UI update and scheduling *once*.
    fun toggleNotification(preference: NotificationPreference, isEnabled: Boolean) {
        val updatedPref = preference.copy(isEnabled = isEnabled)

        // 1. OPTIMISTIC UI UPDATE:
        // Immediately update the local StateFlow. This makes the Switch
        // in the UI respond instantly, preventing the "snap back".
        _notifications.value = _notifications.value.map {
            if (it.id == preference.id) updatedPref else it
        }

        // 2. IMMEDIATE ALARM ACTION (This is now the *only* call):
        if (isEnabled) {
            scheduler.schedule(updatedPref)
        } else {
            scheduler.cancel(updatedPref)
        }

        // 3. PERSISTENCE (Async):
        // Launch a coroutine to save this change to Firestore. The listener
        // will get this update but will *only* update the state, not
        // re-run the alarm logic.
        viewModelScope.launch {
            try {
                writeNotificationToFirestore(updatedPref)
            } catch (e: Exception) {
                Log.e("NotifVM", "Error toggling notification: ${e.message}")
                // If the save fails, revert the optimistic update
                _notifications.value = _notifications.value.map {
                    if (it.id == preference.id) preference else it
                }
                // And revert the alarm action
                if (preference.isEnabled) {
                    scheduler.schedule(preference)
                } else {
                    scheduler.cancel(preference)
                }
            }
        }
    }
}