package com.nadavariel.dietapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions // <-- ADD THIS IMPORT
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson // <-- ADD THIS IMPORT
import com.nadavariel.dietapp.data.DietPlan // <-- ADD THIS IMPORT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserAnswer(
    val question: String = "",
    val answer: String = ""
)

// STEP 1: Create a sealed class for managing Gemini state
sealed class DietPlanResult {
    data object Idle : DietPlanResult()
    data object Loading : DietPlanResult()
    data class Success(val plan: DietPlan) : DietPlanResult()
    data class Error(val message: String) : DietPlanResult()
}


class QuestionsViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val functions = Firebase.functions("me-west1") // <-- ADD FIREBASE FUNCTIONS INSTANCE

    private val _userAnswers = MutableStateFlow<List<UserAnswer>>(emptyList())
    val userAnswers = _userAnswers.asStateFlow()

    // STEP 2: Add StateFlow for the diet plan result
    private val _dietPlanResult = MutableStateFlow<DietPlanResult>(DietPlanResult.Idle)
    val dietPlanResult = _dietPlanResult.asStateFlow()


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

    // STEP 3: Create the function to call Gemini
    fun generateDietPlan() {
        if (_userAnswers.value.isEmpty()) {
            _dietPlanResult.value = DietPlanResult.Error("User answers are not available.")
            return
        }

        _dietPlanResult.value = DietPlanResult.Loading
        viewModelScope.launch {
            // Format the answers into a single string prompt
            val userProfile = _userAnswers.value.joinToString("\n") {
                "Q: ${it.question}\nA: ${it.answer}"
            }

            try {
                val data = hashMapOf("userProfile" to userProfile)

                // Call the new cloud function
                val result = functions
                    .getHttpsCallable("generateDietPlan") // <-- IMPORTANT: This is a new function you'll create
                    .call(data)
                    .await()

                val responseData = result.data as? Map<String, Any> ?: throw Exception("Function response data is null or invalid.")
                val success = responseData["success"] as? Boolean
                val geminiData = responseData["data"]

                if (success == true && geminiData != null) {
                    val gson = Gson()
                    val jsonString = gson.toJson(geminiData) // Convert the map to a JSON string
                    val dietPlan = gson.fromJson(jsonString, DietPlan::class.java) // Parse into your data class
                    _dietPlanResult.value = DietPlanResult.Success(dietPlan)
                } else {
                    val errorMsg = responseData["error"] as? String
                    _dietPlanResult.value = DietPlanResult.Error(errorMsg ?: "Unknown API error occurred.")
                }

            } catch (e: Exception) {
                Log.e("QuestionsViewModel", "Gemini diet plan generation failed: ${e.message}", e)
                _dietPlanResult.value = DietPlanResult.Error("Failed to generate diet plan: ${e.message}")
            }
        }
    }

    // STEP 4: Add a function to reset the state (good practice)
    fun resetDietPlanResult() {
        _dietPlanResult.value = DietPlanResult.Idle
    }
}