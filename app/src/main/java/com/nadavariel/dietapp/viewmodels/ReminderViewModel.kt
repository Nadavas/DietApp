package com.nadavariel.dietapp.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.models.ReminderPreference
import com.nadavariel.dietapp.scheduling.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val scheduler = ReminderScheduler(application.applicationContext)

    private var notificationsListener: ListenerRegistration? = null

    private val _allNotifications = MutableStateFlow<List<ReminderPreference>>(emptyList())
    val allNotifications = _allNotifications.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                fetchNotifications()
            } else {
                clearAllListenersAndData()
            }
        }
    }

    private fun getPreferencesCollection(): CollectionReference? {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("NotifyVM", "User is null, cannot get preferences collection.")
            return null
        }
        return firestore.collection("users").document(userId).collection("notifications")
    }

    private fun fetchNotifications() {
        notificationsListener?.remove()
        val collection = getPreferencesCollection() ?: return

        notificationsListener = collection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("NotifyVM", "Error listening for notifications: ${e.message}")
                return@addSnapshotListener
            }
            val fetchedList = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<ReminderPreference>()?.copy(id = doc.id)
            } ?: emptyList()
            _allNotifications.value = fetchedList
        }
    }

    fun saveNotification(preference: ReminderPreference) = viewModelScope.launch {
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
                scheduler.schedule(prefToSave)
            }
        } catch (e: Exception) {
            Log.e("NotifyVM", "Error saving notification: ${e.message}")
        }
    }

    fun deleteNotification(preference: ReminderPreference) = viewModelScope.launch {
        val collection = getPreferencesCollection() ?: return@launch
        if (preference.id.isBlank()) return@launch
        try {
            scheduler.cancel(preference)
            collection.document(preference.id).delete().await()
        } catch (e: Exception) {
            Log.e("NotifyVM", "Error deleting notification: ${e.message}")
        }
    }

    fun toggleNotification(preference: ReminderPreference, isEnabled: Boolean) {
        val collection = getPreferencesCollection() ?: return

        val updatedPref = preference.copy(isEnabled = isEnabled)

        // 1. Schedule/Cancel Alarm locally
        try {
            if (isEnabled) {
                scheduler.schedule(updatedPref)
            } else {
                scheduler.cancel(updatedPref)
            }
        } catch (e: Exception) {
            Log.e("NotifyVM", "Error updating alarm: ${e.message}")
        }

        // 2. Update Firestore
        viewModelScope.launch {
            try {
                collection.document(preference.id).update("isEnabled", isEnabled).await()
            } catch (e: Exception) {
                Log.e("NotifyVM", "Error toggling notification in DB: ${e.message}")
            }
        }
    }

    private fun clearAllListenersAndData() {
        notificationsListener?.remove()
        notificationsListener = null
        _allNotifications.value = emptyList()
        Log.d("NotifyVM", "Cleared all listeners and data.")
    }

    override fun onCleared() {
        super.onCleared()
        clearAllListenersAndData()
    }
}