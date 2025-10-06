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

data class UserAnswer(
    val question: String = "",
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

    /** Fetch user's saved answers from Firestore **/
    private fun fetchUserAnswers() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("QuestionsViewModel", "Cannot fetch answers: User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                val docRef = firestore.collection("users")
                    .document(userId)
                    .collection("user_answers")
                    .document("diet_habits")

                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    val answersList = snapshot.get("answers") as? List<Map<String, Any>>
                    if (answersList != null) {
                        val parsedAnswers = answersList.map {
                            UserAnswer(
                                question = it["question"]?.toString() ?: "",
                                answer = it["answer"]?.toString() ?: ""
                            )
                        }
                        _userAnswers.value = parsedAnswers
                        Log.d("QuestionsViewModel", "Successfully fetched ${parsedAnswers.size} answers.")
                    } else {
                        Log.d("QuestionsViewModel", "Answers field missing or empty.")
                    }
                } else {
                    Log.d("QuestionsViewModel", "No saved answers found for the user.")
                }
            } catch (e: Exception) {
                Log.e("QuestionsViewModel", "Error fetching user answers", e)
            }
        }
    }

    /** Save user's answers to Firestore **/
    fun saveUserAnswers(
        questions: List<com.nadavariel.dietapp.screens.Question>,
        answers: List<String?>
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("QuestionsViewModel", "Cannot save answers: User not logged in.")
            return
        }

        val userAnswersToSave = questions.mapIndexed { index, question ->
            mapOf(
                "question" to question.text,
                "answer" to (answers.getOrNull(index) ?: "")
            )
        }

        viewModelScope.launch {
            try {
                val userAnswersRef = firestore.collection("users")
                    .document(userId)
                    .collection("user_answers")
                    .document("diet_habits")

                userAnswersRef.set(mapOf("answers" to userAnswersToSave)).await()
                _userAnswers.value = userAnswersToSave.map {
                    UserAnswer(it["question"] as String, it["answer"] as String)
                }

                Log.d("QuestionsViewModel", "Successfully saved ${userAnswersToSave.size} user answers.")
            } catch (e: Exception) {
                Log.e("QuestionsViewModel", "Error saving user answers", e)
            }
        }
    }
}
