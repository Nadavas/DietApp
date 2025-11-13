package com.nadavariel.dietapp.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth // <-- 1. IMPORT
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference // <-- 2. IMPORT
import com.google.firebase.firestore.FirebaseFirestore // <-- 3. IMPORT
import com.google.firebase.firestore.ListenerRegistration // <-- 4. IMPORT
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

    private val auth: FirebaseAuth = Firebase.auth // <-- 5. ADD AUTH INSTANCE
    private val firestore: FirebaseFirestore = Firebase.firestore // <-- 6. ADD FIRESTORE INSTANCE
    private val scheduler = NotificationScheduler(application.applicationContext)

    // --- 7. REMOVE THE LAZY VAL ---
    // private val preferencesCollection by lazy { ... }

    // --- 8. ADD LISTENER REGISTRATION ---
    private var notificationsListener: ListenerRegistration? = null

    // The single source of truth for the UI
    private val _allNotifications = MutableStateFlow<List<NotificationPreference>>(emptyList())
    val allNotifications = _allNotifications.asStateFlow()

    init {
        // --- 9. ADD AUTH STATE LISTENER ---
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                // User is signed in, NOW we fetch
                fetchNotifications()
            } else {
                // User signed out, clear data and listener
                clearAllListenersAndData()
            }
        }
    }

    // --- 10. NEW HELPER FUNCTION TO GET THE *CORRECT* PATH ---
    private fun getPreferencesCollection(): CollectionReference? {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("NotifVM", "User is null, cannot get preferences collection.")
            return null
        }
        return firestore.collection("users").document(userId).collection("notifications")
    }

    private fun fetchNotifications() {
        notificationsListener?.remove() // Clear previous listener
        val collection = getPreferencesCollection() ?: return // Use the new function

        notificationsListener = collection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("NotifVM", "Error listening for notifications: ${e.message}")
                return@addSnapshotListener
            }
            val fetchedList = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<NotificationPreference>()?.copy(id = doc.id)
            } ?: emptyList()
            _allNotifications.value = fetchedList
        }
    }

    private suspend fun writeNotificationToFirestore(preference: NotificationPreference) {
        val collection = getPreferencesCollection() ?: return
        if (preference.id.isBlank()) {
            val newDoc = collection.add(preference).await()
            collection.document(newDoc.id).set(preference.copy(id = newDoc.id)).await()
        } else {
            collection.document(preference.id).set(preference).await()
        }
    }

    fun saveNotification(preference: NotificationPreference) = viewModelScope.launch {
        val collection = getPreferencesCollection() ?: return@launch
        try {
            var prefToSave = preference
            if (preference.id.isBlank()) {
                val newDoc = collection.add(preference).await()
                prefToSave = preference.copy(id = newDoc.id)
                collection.document(prefToSave.id).set(prefToSave).await()
            } else {
                collection.document(preference.id).set(preference).await()
            }

            if (prefToSave.isEnabled) {
                val receiverClass = if (prefToSave.type == "WEIGHT") WeightReminderReceiver::class.java else MealReminderReceiver::class.java
                scheduler.schedule(prefToSave, receiverClass)
            }
        } catch (e: Exception) {
            Log.e("NotifVM", "Error saving notification: ${e.message}")
        }
    }

    fun deleteNotification(preference: NotificationPreference) = viewModelScope.launch {
        val collection = getPreferencesCollection() ?: return@launch
        if (preference.id.isBlank()) return@launch
        try {
            val receiverClass = if (preference.type == "WEIGHT") WeightReminderReceiver::class.java else MealReminderReceiver::class.java
            scheduler.cancel(preference, receiverClass)
            collection.document(preference.id).delete().await()
        } catch (e: Exception) {
            Log.e("NotifVM", "Error deleting notification: ${e.message}")
        }
    }

    fun toggleNotification(preference: NotificationPreference, isEnabled: Boolean) {
        val collection = getPreferencesCollection()
        if (collection == null) {
            Log.e("NotifVM", "Cannot toggle, user not logged in.")
            return
        }

        val updatedPref = preference.copy(isEnabled = isEnabled)
        val receiverClass = if (updatedPref.type == "WEIGHT") WeightReminderReceiver::class.java else MealReminderReceiver::class.java

        _allNotifications.value = _allNotifications.value.map {
            if (it.id == preference.id) updatedPref else it
        }

        if (isEnabled) {
            scheduler.schedule(updatedPref, receiverClass)
        } else {
            scheduler.cancel(updatedPref, receiverClass)
        }

        viewModelScope.launch {
            try {
                writeNotificationToFirestore(updatedPref)
            } catch (e: Exception) {
                Log.e("NotifVM", "Error toggling notification: ${e.message}")
                _allNotifications.value = _allNotifications.value.map {
                    if (it.id == preference.id) preference else it
                }
                if (preference.isEnabled) {
                    scheduler.schedule(preference, receiverClass)
                } else {
                    scheduler.cancel(preference, receiverClass)
                }
            }
        }
    }

    // --- 11. ADD CLEANUP FUNCTIONS ---
    private fun clearAllListenersAndData() {
        notificationsListener?.remove()
        notificationsListener = null
        _allNotifications.value = emptyList()
        Log.d("NotifVM", "Cleared all listeners and data.")
    }

    override fun onCleared() {
        super.onCleared()
        clearAllListenersAndData()
    }
}