package com.nadavariel.dietapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.data.Goal
import com.nadavariel.dietapp.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class GoalsViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals = _goals.asStateFlow()

    private val _userWeight = MutableStateFlow(0f)
    val userWeight = _userWeight.asStateFlow()

    // 🌟 New State: Tracks which goals are missing (e.g., ["Calorie", "Protein"])
    private val _missingGoals = MutableStateFlow<List<String>>(emptyList())
    val missingGoals = _missingGoals.asStateFlow()

    init {
        fetchUserGoals()
        fetchUserProfile()
        // 🌟 Start monitoring for missing goals immediately
        viewModelScope.launch {
            goals.collect { currentGoals ->
                updateMissingGoals(currentGoals)
            }
        }
    }

    private fun updateMissingGoals(currentGoals: List<Goal>) {
        val missing = currentGoals
            .filter { it.value.isNullOrBlank() || it.value == "0" }
            .map { goal ->
                when {
                    goal.text.contains("calorie", ignoreCase = true) -> "Calorie"
                    goal.text.contains("protein", ignoreCase = true) -> "Protein"
                    // Add other goal types here if needed (e.g., Weight)
                    else -> "Goal"
                }
            }
            .distinct() // Ensure no duplicate names
        _missingGoals.value = missing
        Log.d("GoalsViewModel", "Missing goals updated: $missing")
    }

    private fun fetchUserGoals() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("GoalsViewModel", "Cannot fetch goals: User not logged in.")
            return
        }

        val allGoals = listOf(
            Goal(text = "How many calories a day is your target?"),
            Goal(text = "How many grams of protein a day is your target?"),
        )

        firestore.collection("users").document(userId)
            .collection("user_answers").document("goals")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("GoalsViewModel", "Error listening to user answers", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val answersMap = snapshot.get("answers") as? List<Map<String, String>>

                    val userAnswers = answersMap?.map {
                        it["question"] to it["answer"]
                    }?.toMap() ?: emptyMap()

                    val mergedGoals = allGoals.map { goal ->
                        goal.copy(value = userAnswers[goal.text])
                    }

                    _goals.value = mergedGoals
                    Log.d("GoalsViewModel", "Live goals updated: ${mergedGoals.size} items")
                } else {
                    _goals.value = allGoals
                    Log.d("GoalsViewModel", "No saved answers yet, using base goals.")
                }
            }
    }

    private fun fetchUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("GoalsViewModel", "Cannot fetch profile: User not logged in.")
            return
        }

        firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("GoalsViewModel", "Error listening to user profile", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    _userWeight.value = profile?.weight ?: 0f
                } else {
                    Log.d("GoalsViewModel", "User profile does not exist.")
                }
            }
    }


    fun saveUserAnswers() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("GoalsViewModel", "Cannot save answers: User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                val userAnswersToSave = _goals.value.map { goal ->
                    mapOf("question" to goal.text, "answer" to (goal.value ?: ""))
                }

                val userAnswersRef = firestore.collection("users").document(userId)
                    .collection("user_answers").document("goals")

                userAnswersRef.set(mapOf("answers" to userAnswersToSave)).await()
                Log.d("GoalsViewModel", "Successfully saved user answers.")
            } catch (e: Exception) {
                Log.e("GoalsViewModel", "Error saving user answers", e)
            }
        }
    }

    fun updateAnswer(goalId: String, answer: String) {
        _goals.value = _goals.value.map { goal ->
            if (goal.id == goalId) goal.copy(value = answer) else goal
        }
    }
}