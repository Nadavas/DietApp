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
import com.nadavariel.dietapp.ui.notifications.WeightReminderReceiver // Import new receiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val mealScheduler = NotificationScheduler(application.applicationContext)
    // Create a separate scheduler instance for weight, though it points to the same class
    private val weightScheduler = NotificationScheduler(application.applicationContext)

    private val userId: String? get() = auth.currentUser?.uid

    // --- Meal Notifications ---
    private val mealPreferencesCollection by lazy {
        firestore.collection("users").document(userId ?: "no_user").collection("notifications")
    }
    private val _mealNotifications = MutableStateFlow<List<NotificationPreference>>(emptyList())
    val mealNotifications = _mealNotifications.asStateFlow()

    // --- Weight Notifications ---
    private val weightPreferencesCollection by lazy {
        firestore.collection("users").document(userId ?: "no_user").collection("weight_notifications")
    }
    private val _weightNotifications = MutableStateFlow<List<NotificationPreference>>(emptyList())
    val weightNotifications = _weightNotifications.asStateFlow()


    init {
        fetchMealNotifications()
        fetchWeightNotifications()
    }

    private fun fetchMealNotifications() {
        if (userId == null) return
        mealPreferencesCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("NotifVM", "Error listening for MEAL notifications: ${e.message}")
                return@addSnapshotListener
            }
            val fetchedList = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<NotificationPreference>()?.copy(id = doc.id)
            } ?: emptyList()
            _mealNotifications.value = fetchedList
            updateAllMealAlarms(fetchedList)
        }
    }

    private fun fetchWeightNotifications() {
        if (userId == null) return
        weightPreferencesCollection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("NotifVM", "Error listening for WEIGHT notifications: ${e.message}")
                return@addSnapshotListener
            }
            val fetchedList = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject<NotificationPreference>()?.copy(id = doc.id)
            } ?: emptyList()
            _weightNotifications.value = fetchedList
            updateAllWeightAlarms(fetchedList)
        }
    }

    private fun updateAllMealAlarms(preferences: List<NotificationPreference>) {
        preferences.forEach { pref ->
            if (pref.isEnabled) {
                mealScheduler.schedule(pref) // Uses meal scheduler
            } else {
                mealScheduler.cancel(pref)
            }
        }
    }

    private fun updateAllWeightAlarms(preferences: List<NotificationPreference>) {
        preferences.forEach { pref ->
            // Pass the correct receiver class to the scheduler
            if (pref.isEnabled) {
                weightScheduler.schedule(pref, WeightReminderReceiver::class.java)
            } else {
                weightScheduler.cancel(pref, WeightReminderReceiver::class.java)
            }
        }
    }

    private suspend fun writeNotificationToFirestore(preference: NotificationPreference, isMeal: Boolean) {
        val collection = if (isMeal) mealPreferencesCollection else weightPreferencesCollection

        if (preference.id.isBlank()) {
            val newDoc = collection.add(preference).await()
            collection.document(newDoc.id).set(preference.copy(id = newDoc.id)).await()
        } else {
            collection.document(preference.id).set(preference).await()
        }
    }

    fun saveMealNotification(preference: NotificationPreference) = viewModelScope.launch {
        if (userId == null) return@launch
        try {
            var prefToSave = preference
            if (preference.id.isBlank()) {
                val newDoc = mealPreferencesCollection.add(preference).await()
                prefToSave = preference.copy(id = newDoc.id)
                mealPreferencesCollection.document(prefToSave.id).set(prefToSave).await()
            } else {
                mealPreferencesCollection.document(preference.id).set(preference).await()
            }

            if (prefToSave.isEnabled) {
                mealScheduler.schedule(prefToSave)
            }
        } catch (e: Exception) {
            Log.e("NotifVM", "Error saving MEAL notification: ${e.message}")
        }
    }

    fun saveWeightNotification(preference: NotificationPreference) = viewModelScope.launch {
        if (userId == null) return@launch
        try {
            var prefToSave = preference
            if (preference.id.isBlank()) {
                val newDoc = weightPreferencesCollection.add(preference).await()
                prefToSave = preference.copy(id = newDoc.id)
                weightPreferencesCollection.document(prefToSave.id).set(prefToSave).await()
            } else {
                weightPreferencesCollection.document(preference.id).set(preference).await()
            }

            if (prefToSave.isEnabled) {
                weightScheduler.schedule(prefToSave, WeightReminderReceiver::class.java)
            }
        } catch (e: Exception) {
            Log.e("NotifVM", "Error saving WEIGHT notification: ${e.message}")
        }
    }

    fun deleteMealNotification(preference: NotificationPreference) = viewModelScope.launch {
        if (userId == null || preference.id.isBlank()) return@launch
        try {
            mealScheduler.cancel(preference)
            mealPreferencesCollection.document(preference.id).delete().await()
        } catch (e: Exception) {
            Log.e("NotifVM", "Error deleting MEAL notification: ${e.message}")
        }
    }

    fun deleteWeightNotification(preference: NotificationPreference) = viewModelScope.launch {
        if (userId == null || preference.id.isBlank()) return@launch
        try {
            weightScheduler.cancel(preference, WeightReminderReceiver::class.java)
            weightPreferencesCollection.document(preference.id).delete().await()
        } catch (e: Exception) {
            Log.e("NotifVM", "Error deleting WEIGHT notification: ${e.message}")
        }
    }

    fun toggleMealNotification(preference: NotificationPreference, isEnabled: Boolean) {
        val updatedPref = preference.copy(isEnabled = isEnabled)

        _mealNotifications.value = _mealNotifications.value.map {
            if (it.id == preference.id) updatedPref else it
        }

        if (isEnabled) {
            mealScheduler.schedule(updatedPref)
        } else {
            mealScheduler.cancel(updatedPref)
        }

        viewModelScope.launch {
            try {
                writeNotificationToFirestore(updatedPref, isMeal = true)
            } catch (e: Exception) {
                Log.e("NotifVM", "Error toggling MEAL notification: ${e.message}")
                _mealNotifications.value = _mealNotifications.value.map {
                    if (it.id == preference.id) preference else it
                }
                if (preference.isEnabled) {
                    mealScheduler.schedule(preference)
                } else {
                    mealScheduler.cancel(preference)
                }
            }
        }
    }

    fun toggleWeightNotification(preference: NotificationPreference, isEnabled: Boolean) {
        val updatedPref = preference.copy(isEnabled = isEnabled)

        _weightNotifications.value = _weightNotifications.value.map {
            if (it.id == preference.id) updatedPref else it
        }

        if (isEnabled) {
            weightScheduler.schedule(updatedPref, WeightReminderReceiver::class.java)
        } else {
            weightScheduler.cancel(updatedPref, WeightReminderReceiver::class.java)
        }

        viewModelScope.launch {
            try {
                writeNotificationToFirestore(updatedPref, isMeal = false)
            } catch (e: Exception) {
                Log.e("NotifVM", "Error toggling WEIGHT notification: ${e.message}")
                _weightNotifications.value = _weightNotifications.value.map {
                    if (it.id == preference.id) preference else it
                }
                if (preference.isEnabled) {
                    weightScheduler.schedule(preference, WeightReminderReceiver::class.java)
                } else {
                    weightScheduler.cancel(preference, WeightReminderReceiver::class.java)
                }
            }
        }
    }
}