package com.nadavariel.dietapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.nadavariel.dietapp.data.DietPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserAnswer(
    val question: String = "",
    val answer: String = ""
)

sealed class DietPlanResult {
    data object Idle : DietPlanResult()
    data object Loading : DietPlanResult()
    data class Success(val plan: DietPlan) : DietPlanResult()
    data class Error(val message: String) : DietPlanResult()
}

class QuestionsViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore
    private val functions = Firebase.functions("me-west1")

    private val _userAnswers = MutableStateFlow<List<UserAnswer>>(emptyList())
    val userAnswers = _userAnswers.asStateFlow()

    private val _dietPlanResult = MutableStateFlow<DietPlanResult>(DietPlanResult.Idle)
    val dietPlanResult = _dietPlanResult.asStateFlow()

    init {
        fetchUserAnswers()
        fetchSavedDietPlan() // Load saved diet plan on init
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

    /** NEW: Fetch saved diet plan from Firestore **/
    private fun fetchSavedDietPlan() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("QuestionsViewModel", "Cannot fetch diet plan: User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                val docRef = firestore.collection("users")
                    .document(userId)
                    .collection("diet_plans")
                    .document("current_plan")

                val snapshot = docRef.get().await()
                if (snapshot.exists()) {
                    val gson = Gson()
                    val dietPlan = snapshot.toObject(DietPlan::class.java)

                    if (dietPlan != null) {
                        _dietPlanResult.value = DietPlanResult.Success(dietPlan)
                        Log.d("QuestionsViewModel", "Successfully fetched saved diet plan.")
                    }
                } else {
                    Log.d("QuestionsViewModel", "No saved diet plan found.")
                }
            } catch (e: Exception) {
                Log.e("QuestionsViewModel", "Error fetching saved diet plan", e)
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

    /** NEW: Save diet plan to Firestore **/
    private suspend fun saveDietPlanToFirestore(dietPlan: DietPlan) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("QuestionsViewModel", "Cannot save diet plan: User not logged in.")
            return
        }

        try {
            val dietPlanRef = firestore.collection("users")
                .document(userId)
                .collection("diet_plans")
                .document("current_plan")

            // Add timestamp for tracking when the plan was generated
            val planData = hashMapOf(
                "dailyCalories" to dietPlan.dailyCalories,
                "proteinGrams" to dietPlan.proteinGrams,
                "carbsGrams" to dietPlan.carbsGrams,
                "fatGrams" to dietPlan.fatGrams,
                "recommendations" to dietPlan.recommendations,
                "disclaimer" to dietPlan.disclaimer,
                "generatedAt" to com.google.firebase.Timestamp.now()
            )

            dietPlanRef.set(planData).await()
            Log.d("QuestionsViewModel", "Successfully saved diet plan to Firestore.")
        } catch (e: Exception) {
            Log.e("QuestionsViewModel", "Error saving diet plan to Firestore", e)
        }
    }

    /** Generate or load cached diet plan **/
    fun generateDietPlan() {
        if (_userAnswers.value.isEmpty()) {
            _dietPlanResult.value = DietPlanResult.Error("User answers are not available.")
            return
        }

        // Check if we already have a saved plan
        if (_dietPlanResult.value is DietPlanResult.Success) {
            Log.d("QuestionsViewModel", "Using cached diet plan.")
            return
        }

        _dietPlanResult.value = DietPlanResult.Loading
        viewModelScope.launch {
            val userProfile = _userAnswers.value.joinToString("\n") {
                "Q: ${it.question}\nA: ${it.answer}"
            }

            try {
                val data = hashMapOf("userProfile" to userProfile)

                val result = functions
                    .getHttpsCallable("generateDietPlan")
                    .call(data)
                    .await()

                val responseData = result.data as? Map<String, Any> ?: throw Exception("Function response data is null or invalid.")
                val success = responseData["success"] as? Boolean
                val geminiData = responseData["data"]

                if (success == true && geminiData != null) {
                    val gson = Gson()
                    val jsonString = gson.toJson(geminiData)
                    val dietPlan = gson.fromJson(jsonString, DietPlan::class.java)

                    // Save to Firestore
                    saveDietPlanToFirestore(dietPlan)

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

    /** NEW: Force regenerate diet plan (ignoring cache) **/
    fun regenerateDietPlan() {
        _dietPlanResult.value = DietPlanResult.Idle
        generateDietPlan()
    }

    /** Reset the state **/
    fun resetDietPlanResult() {
        _dietPlanResult.value = DietPlanResult.Idle
    }
}