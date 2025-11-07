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
import com.nadavariel.dietapp.ui.notifications.MealReminderReceiver
import com.nadavariel.dietapp.ui.notifications.NotificationScheduler
import com.nadavariel.dietapp.ui.notifications.WeightReminderReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val scheduler = NotificationScheduler(application.applicationContext)

    private val userId: String? get() = auth.currentUser?.uid

    // The single collection for all reminders
    private val preferencesCollection by lazy {
        firestore.collection("users").document(userId ?: "no_user").collection("notifications")
    }

    // The single source of truth for the UI
    private val _allNotifications = MutableStateFlow<List<NotificationPreference>>(emptyList())
    val allNotifications = _allNotifications.asStateFlow()

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
                // Make sure to update your NotificationPreference model to include 'type'
                doc.toObject<NotificationPreference>()?.copy(id = doc.id)
            } ?: emptyList()
            _allNotifications.value = fetchedList
            // No scheduling from here, to prevent race conditions
        }
    }

    private suspend fun writeNotificationToFirestore(preference: NotificationPreference) {
        if (preference.id.isBlank()) {
            val newDoc = preferencesCollection.add(preference).await()
            preferencesCollection.document(newDoc.id).set(preference.copy(id = newDoc.id)).await()
        } else {
            preferencesCollection.document(preference.id).set(preference).await()
        }
    }

    // Single save function for both types
    fun saveNotification(preference: NotificationPreference) = viewModelScope.launch {
        if (userId == null) return@launch
        try {
            var prefToSave = preference
            if (preference.id.isBlank()) {
                val newDoc = preferencesCollection.add(preference).await()
                prefToSave = preference.copy(id = newDoc.id)
                preferencesCollection.document(prefToSave.id).set(prefToSave).await()
            } else {
                preferencesCollection.document(preference.id).set(preference).await()
            }

            if (prefToSave.isEnabled) {
                val receiverClass = if (prefToSave.type == "WEIGHT") WeightReminderReceiver::class.java else MealReminderReceiver::class.java
                scheduler.schedule(prefToSave, receiverClass)
            }
        } catch (e: Exception) {
            Log.e("NotifVM", "Error saving notification: ${e.message}")
        }
    }

    // Single delete function
    fun deleteNotification(preference: NotificationPreference) = viewModelScope.launch {
        if (userId == null || preference.id.isBlank()) return@launch
        try {
            val receiverClass = if (preference.type == "WEIGHT") WeightReminderReceiver::class.java else MealReminderReceiver::class.java
            scheduler.cancel(preference, receiverClass)
            preferencesCollection.document(preference.id).delete().await()
        } catch (e: Exception) {
            Log.e("NotifVM", "Error deleting notification: ${e.message}")
        }
    }

    // Single toggle function
    fun toggleNotification(preference: NotificationPreference, isEnabled: Boolean) {
        val updatedPref = preference.copy(isEnabled = isEnabled)
        val receiverClass = if (updatedPref.type == "WEIGHT") WeightReminderReceiver::class.java else MealReminderReceiver::class.java

        // 1. Optimistic UI Update
        _allNotifications.value = _allNotifications.value.map {
            if (it.id == preference.id) updatedPref else it
        }

        // 2. Immediate Alarm Action
        if (isEnabled) {
            scheduler.schedule(updatedPref, receiverClass)
        } else {
            scheduler.cancel(updatedPref, receiverClass)
        }

        // 3. Persist change
        viewModelScope.launch {
            try {
                writeNotificationToFirestore(updatedPref)
            } catch (e: Exception) {
                Log.e("NotifVM", "Error toggling notification: ${e.message}")
                // Revert optimistic update on failure
                _allNotifications.value = _allNotifications.value.map {
                    if (it.id == preference.id) preference else it
                }
                // Revert alarm action
                if (preference.isEnabled) {
                    scheduler.schedule(preference, receiverClass)
                } else {
                    scheduler.cancel(preference, receiverClass)
                }
            }
        }
    }
}