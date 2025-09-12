package com.nadavariel.dietapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ✅ Represents a Goal
data class Goal(
    val text: String = "",
    val options: List<String> = emptyList(),
    val id: String = java.util.UUID.randomUUID().toString(),
    val selectedAnswer: String? = null // <-- pre-marking support
)

class GoalsViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals = _goals.asStateFlow()

    init {
        fetchUserGoals()
    }

    private fun fetchUserGoals() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("GoalsViewModel", "Cannot fetch goals: User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Load all goals (could also come from Firestore, here just an example list)
                val allGoals = listOf(
                    Goal(text = "How often do you eat breakfast?", options = listOf("Never", "Sometimes", "Always")),
                    Goal(text = "Do you drink soda?", options = listOf("Never", "Occasionally", "Daily")),
                    Goal(text = "How many fruits do you eat per day?", options = listOf("0", "1–2", "3+"))
                )

                // 2. Fetch user answers
                val docRef = firestore.collection("users").document(userId)
                    .collection("user_answers").document("diet_habits")

                val snapshot = docRef.get().await()

                val answersMap = if (snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    snapshot.get("answers") as? List<Map<String, String>>
                } else null

                val userAnswers = answersMap?.map {
                    it["question"] to it["answer"]
                }?.toMap() ?: emptyMap()

                // 3. Merge answers into goals
                val mergedGoals = allGoals.map { goal ->
                    goal.copy(selectedAnswer = userAnswers[goal.text])
                }

                _goals.value = mergedGoals
                Log.d("GoalsViewModel", "fetchUserGoals: loaded ${mergedGoals.size} items")
            } catch (e: Exception) {
                Log.e("GoalsViewModel", "Error fetching goals", e)
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
                    mapOf("question" to goal.text, "answer" to (goal.selectedAnswer ?: ""))
                }

                val userAnswersRef = firestore.collection("users").document(userId)
                    .collection("user_answers").document("diet_habits")

                userAnswersRef.set(mapOf("answers" to userAnswersToSave)).await()
                Log.d("GoalsViewModel", "Successfully saved user answers.")
            } catch (e: Exception) {
                Log.e("GoalsViewModel", "Error saving user answers", e)
            }
        }
    }

    fun updateAnswer(goalId: String, answer: String) {
        _goals.value = _goals.value.map { goal ->
            if (goal.id == goalId) goal.copy(selectedAnswer = answer) else goal
        }
    }
}
