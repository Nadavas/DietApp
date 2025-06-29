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

// A data class to represent the structure of the answers in Firestorm
data class UserAnswer(
    val question: String = "", // Add default values for Firestorm deserialization
    val answer: String = ""
)

class QuestionsViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _userAnswers = MutableStateFlow<List<UserAnswer>>(emptyList())
    val userAnswers = _userAnswers.asStateFlow()

    init {
        fetchUserAnswers()
    }

    private fun fetchUserAnswers() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("QuestionsViewModel", "Cannot fetch answers: User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                val docRef = firestore.collection("users").document(userId)
                    .collection("user_answers").document("diet_habits")

                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val answersMap = snapshot.get("answers") as? List<Map<String, String>>
                    if (answersMap != null) {
                        _userAnswers.value = answersMap.map { UserAnswer(it["question"] ?: "", it["answer"] ?: "") }
                        Log.d("QuestionsViewModel", "Successfully fetched user answers.")
                    }
                } else {
                    Log.d("QuestionsViewModel", "No saved answers found for the user.")
                }
            } catch (e: Exception) {
                Log.e("QuestionsViewModel", "Error fetching user answers", e)
            }
        }
    }

    fun saveUserAnswers(questions: List<com.nadavariel.dietapp.screens.Question>, answers: List<String?>) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("QuestionsViewModel", "Cannot save answers: User not logged in.")
            return
        }

        val userAnswersToSave = questions.mapIndexed { index, question ->
            UserAnswer(question = question.text, answer = answers[index] ?: "")
        }

        viewModelScope.launch {
            try {
                val userAnswersRef = firestore.collection("users").document(userId)
                    .collection("user_answers").document("diet_habits")

                userAnswersRef.set(mapOf("answers" to userAnswersToSave)).await()
                Log.d("QuestionsViewModel", "Successfully saved user answers.")
            } catch (e: Exception)
            {
                Log.e("QuestionsViewModel", "Error saving user answers", e)
            }
        }
    }
}