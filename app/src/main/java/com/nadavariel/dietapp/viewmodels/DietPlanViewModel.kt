package com.nadavariel.dietapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.nadavariel.dietapp.models.DietPlan
import com.nadavariel.dietapp.models.Goal
import com.nadavariel.dietapp.repositories.AuthRepository
import com.nadavariel.dietapp.repositories.DietRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DietPlanViewModel(
    private val authRepository: AuthRepository,
    private val dietRepository: DietRepository
) : ViewModel() {

    private val _currentDietPlan = MutableStateFlow<DietPlan?>(null)
    val currentDietPlan = _currentDietPlan.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals = _goals.asStateFlow()

    private val _userWeight = MutableStateFlow(0f)
    val userWeight = _userWeight.asStateFlow()

    private val _hasAiGeneratedGoals = MutableStateFlow(false)
    val hasAiGeneratedGoals = _hasAiGeneratedGoals.asStateFlow()

    private val _isLoadingPlan = MutableStateFlow(true)
    val isLoadingPlan = _isLoadingPlan.asStateFlow()

    // Jobs to track active collectors so we can cancel them on logout
    private var dietPlanJob: Job? = null
    private var goalsJob: Job? = null
    private var weightJob: Job? = null

    // Listener for AuthRepository
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        // FIXED: Use addAuthStateListener (Pragmatic style) instead of authState.collect (Flow style)
        authStateListener = authRepository.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _isLoadingPlan.value = true
                startObservingData(user.uid)
            } else {
                stopObservingData()
            }
        }
    }

    private fun startObservingData(userId: String) {
        // 1. Fetch Diet Plan
        dietPlanJob?.cancel()
        dietPlanJob = viewModelScope.launch {
            dietRepository.getDietPlanFlow(userId).collect { plan ->
                _currentDietPlan.value = plan
                _isLoadingPlan.value = false
                Log.d("DietPlanViewModel", "Diet plan updated from repo.")
            }
        }

        // 2. Fetch Goals
        goalsJob?.cancel()
        goalsJob = viewModelScope.launch {
            dietRepository.getUserGoalsFlow(userId).collect { pair ->
                _goals.value = pair.first
                _hasAiGeneratedGoals.value = pair.second
                Log.d("DietPlanViewModel", "Goals updated from repo.")
            }
        }

        // 3. Fetch Weight
        weightJob?.cancel()
        weightJob = viewModelScope.launch {
            dietRepository.getUserWeightFlow(userId).collect { weight ->
                _userWeight.value = weight
            }
        }
    }

    private fun stopObservingData() {
        dietPlanJob?.cancel()
        goalsJob?.cancel()
        weightJob?.cancel()

        _currentDietPlan.value = null
        _goals.value = emptyList()
        _userWeight.value = 0f
        _hasAiGeneratedGoals.value = false
        _isLoadingPlan.value = false
        Log.d("DietPlanViewModel", "Stopped observing data (User logged out).")
    }

    fun saveUserAnswers() {
        val userId = authRepository.currentUser?.uid
        if (userId == null) {
            Log.e("DietPlanViewModel", "Cannot save answers: User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                dietRepository.saveUserGoals(userId, _goals.value)
                Log.d("DietPlanViewModel", "Successfully saved user answers via repo.")
            } catch (e: Exception) {
                Log.e("DietPlanViewModel", "Error saving user answers", e)
            }
        }
    }

    fun updateAnswer(goalId: String, answer: String) {
        _goals.value = _goals.value.map { goal ->
            if (goal.id == goalId) goal.copy(value = answer) else goal
        }
    }

    override fun onCleared() {
        super.onCleared()
        // FIXED: Clean up the listener using the repo method
        authStateListener?.let { authRepository.removeAuthStateListener(it) }
    }
}