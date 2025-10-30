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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.nadavariel.dietapp.data.DietPlan

class GoalsViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _currentDietPlan = MutableStateFlow<DietPlan?>(null)
    val currentDietPlan = _currentDietPlan.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals = _goals.asStateFlow()

    private val _userWeight = MutableStateFlow(0f)
    val userWeight = _userWeight.asStateFlow()

    private val _hasAiGeneratedGoals = MutableStateFlow(false)
    val hasAiGeneratedGoals = _hasAiGeneratedGoals.asStateFlow()

    init {
        fetchDietPlan()
        fetchUserGoals()
        fetchUserProfile()
    }

    private fun fetchDietPlan() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId)
                    .collection("diet_plans").document("current_plan")
                    .get().await()

                if (snapshot.exists()) {
                    val plan = DietPlan(
                        dailyCalories = (snapshot.getLong("dailyCalories") ?: 0).toInt(),
                        proteinGrams = (snapshot.getLong("proteinGrams") ?: 0).toInt(),
                        carbsGrams = (snapshot.getLong("carbsGrams") ?: 0).toInt(),
                        fatGrams = (snapshot.getLong("fatGrams") ?: 0).toInt(),
                        recommendations = snapshot.getString("recommendations") ?: "",
                        disclaimer = snapshot.getString("disclaimer") ?: "Consult a healthcare professional before making dietary changes."
                    )
                    _currentDietPlan.value = plan
                }
            } catch (e: Exception) {
                Log.e("GoalsViewModel", "Error fetching diet plan", e)
            }
        }
    }

    private fun fetchUserGoals() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("GoalsViewModel", "Cannot fetch goals: User not logged in.")
            return
        }

        // Added "Target Weight" goal with a unique ID
        val allGoals = listOf(
            Goal(id = "calories", text = "How many calories a day is your target?"),
            Goal(id = "protein", text = "How many grams of protein a day is your target?"),
            Goal(id = "target_weight", text = "What is your target weight (in kg)?") // Added new goal
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
                    val aiGenerated = snapshot.getBoolean("aiGenerated") == true

                    val userAnswers = answersMap?.associate {
                        it["question"] to it["answer"]
                    } ?: emptyMap()

                    // Ensure all defined goals are present, merged with saved answers
                    val mergedGoals = allGoals.map { goal ->
                        goal.copy(value = userAnswers[goal.text])
                    }

                    _goals.value = mergedGoals
                    _hasAiGeneratedGoals.value = aiGenerated
                    Log.d("GoalsViewModel", "Live goals updated: ${mergedGoals.size} items, AI-generated: $aiGenerated")
                } else {
                    // If no saved data, ensure all base goals are shown
                    _goals.value = allGoals
                    _hasAiGeneratedGoals.value = false
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
                    _userWeight.value = profile?.startingWeight ?: 0f
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
                // Ensure we save using the current state of goals
                val userAnswersToSave = _goals.value.map { goal ->
                    mapOf("question" to goal.text, "answer" to (goal.value ?: ""))
                }

                val userAnswersRef = firestore.collection("users").document(userId)
                    .collection("user_answers").document("goals")

                val currentData = userAnswersRef.get().await()
                val aiGenerated = currentData.getBoolean("aiGenerated") == true

                userAnswersRef.set(mapOf(
                    "answers" to userAnswersToSave,
                    "aiGenerated" to aiGenerated
                )).await()

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