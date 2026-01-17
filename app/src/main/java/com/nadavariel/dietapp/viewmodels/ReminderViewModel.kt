package com.nadavariel.dietapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nadavariel.dietapp.models.ReminderPreference
import com.nadavariel.dietapp.repositories.AuthRepository
import com.nadavariel.dietapp.repositories.ReminderRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReminderViewModel(
    private val authRepository: AuthRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _allNotifications = MutableStateFlow<List<ReminderPreference>>(emptyList())
    val allNotifications = _allNotifications.asStateFlow()

    private var notificationsJob: Job? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        authStateListener = authRepository.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                startListening(user.uid)
            } else {
                stopListening()
            }
        }
    }

    private fun startListening(userId: String) {
        notificationsJob?.cancel()
        notificationsJob = viewModelScope.launch {
            reminderRepository.getNotificationsFlow(userId).collect { list ->
                _allNotifications.value = list
            }
        }
    }

    private fun stopListening() {
        notificationsJob?.cancel()
        _allNotifications.value = emptyList()
        Log.d("ReminderViewModel", "Stopped listening and cleared data.")
    }

    fun saveNotification(preference: ReminderPreference) = viewModelScope.launch {
        val userId = authRepository.currentUser?.uid ?: return@launch
        try {
            reminderRepository.saveNotification(userId, preference)
        } catch (e: Exception) {
            Log.e("ReminderViewModel", "Error saving notification", e)
        }
    }

    fun deleteNotification(preference: ReminderPreference) = viewModelScope.launch {
        val userId = authRepository.currentUser?.uid ?: return@launch
        try {
            reminderRepository.deleteNotification(userId, preference)
        } catch (e: Exception) {
            Log.e("ReminderViewModel", "Error deleting notification", e)
        }
    }

    fun toggleNotification(preference: ReminderPreference, isEnabled: Boolean) = viewModelScope.launch {
        val userId = authRepository.currentUser?.uid ?: return@launch
        try {
            reminderRepository.toggleNotification(userId, preference, isEnabled)
        } catch (e: Exception) {
            Log.e("ReminderViewModel", "Error toggling notification", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { authRepository.removeAuthStateListener(it) }
        stopListening()
    }
}