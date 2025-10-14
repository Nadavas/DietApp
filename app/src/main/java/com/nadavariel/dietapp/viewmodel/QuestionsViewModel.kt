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
import com.nadavariel.dietapp.screens.Question
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
    }

    private fun fetchUserAnswers() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(userId)
                    .collection("user_answers").document("diet_habits").get().await()
                if (snapshot.exists()) {
                    val answersList = snapshot.get("answers") as? List<Map<String, Any>>
                    _userAnswers.value = answersList?.map {
                        UserAnswer(it["question"].toString(), it["answer"].toString())
                    } ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("QuestionsViewModel", "Error fetching user answers", e)
            }
        }
    }

    private suspend fun saveUserAnswers(
        questions: List<Question>,
        answers: List<String?>
    ) {
        val userId = auth.currentUser?.uid ?: return

        val userAnswersToSave = questions.mapIndexed { index, question ->
            mapOf("question" to question.text, "answer" to (answers.getOrNull(index) ?: ""))
        }

        try {
            firestore.collection("users").document(userId)
                .collection("user_answers").document("diet_habits")
                .set(mapOf("answers" to userAnswersToSave)).await()

            _userAnswers.value = userAnswersToSave.map {
                UserAnswer(it["question"] as String, it["answer"] as String)
            }
            Log.d("QuestionsViewModel", "Successfully saved ${userAnswersToSave.size} user answers.")
        } catch (e: Exception) {
            Log.e("QuestionsViewModel", "Error saving user answers", e)
        }
    }

    private suspend fun saveDietPlanToFirestore(dietPlan: DietPlan) {
        val userId = auth.currentUser?.uid ?: return
        try {
            val planData = mapOf(
                "dailyCalories" to dietPlan.dailyCalories,
                "proteinGrams" to dietPlan.proteinGrams,
                "carbsGrams" to dietPlan.carbsGrams,
                "fatGrams" to dietPlan.fatGrams,
                "recommendations" to dietPlan.recommendations,
                "disclaimer" to dietPlan.disclaimer,
                "generatedAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users").document(userId)
                .collection("diet_plans").document("current_plan")
                .set(planData).await()
        } catch (e: Exception) {
            Log.e("QuestionsViewModel", "Error saving diet plan to Firestore", e)
        }
    }

    fun saveAnswersAndRegeneratePlan(questions: List<Question>, answers: List<String?>) {
        viewModelScope.launch {
            saveUserAnswers(questions, answers)

            _dietPlanResult.value = DietPlanResult.Loading
            val userProfile = _userAnswers.value.joinToString("\n") { "Q: ${it.question}\nA: ${it.answer}" }

            try {
                val data = hashMapOf("userProfile" to userProfile)
                val result = functions.getHttpsCallable("generateDietPlan").call(data).await()
                val responseData = result.data as? Map<String, Any> ?: throw Exception("Function response is invalid")

                if (responseData["success"] == true) {
                    val gson = Gson()
                    val dietPlan = gson.fromJson(gson.toJson(responseData["data"]), DietPlan::class.java)
                    saveDietPlanToFirestore(dietPlan)
                    _dietPlanResult.value = DietPlanResult.Success(dietPlan)
                } else {
                    throw Exception(responseData["error"]?.toString() ?: "Unknown error from function")
                }
            } catch (e: Exception) {
                Log.e("QuestionsViewModel", "Error generating diet plan", e)
                _dietPlanResult.value = DietPlanResult.Error(e.message ?: "An unexpected error occurred.")
            }
        }
    }

    /**
     * ✅ CORRECTED: This function now saves goals to the correct path and in the correct format
     * that GoalsViewModel is listening to.
     */
    fun applyDietPlanToGoals(plan: DietPlan) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("QuestionsViewModel", "Cannot apply goals: User not logged in.")
            return
        }

        viewModelScope.launch {
            try {
                // 1. Point to the correct Firestore document
                val goalsRef = firestore.collection("users").document(userId)
                    .collection("user_answers").document("goals")

                // 2. Create the data in the format GoalsViewModel expects
                // The "question" text MUST match the text in GoalsViewModel.
                val goalsToSave = listOf(
                    mapOf(
                        "question" to "How many calories a day is your target?",
                        "answer" to plan.dailyCalories.toString()
                    ),
                    mapOf(
                        "question" to "How many grams of protein a day is your target?",
                        "answer" to plan.proteinGrams.toString()
                    )
                )

                // 3. Set the data along with the AI flag
                val dataToSet = mapOf(
                    "answers" to goalsToSave,
                    "aiGenerated" to true
                )

                goalsRef.set(dataToSet).await()

                Log.d("QuestionsViewModel", "Successfully applied AI diet plan to goals.")

            } catch (e: Exception) {
                Log.e("QuestionsViewModel", "Error applying diet plan to goals", e)
            }
        }
    }

    // This helper function is no longer needed with the new logic
    // private fun updateOrAddGoal(...)

    fun resetDietPlanResult() {
        _dietPlanResult.value = DietPlanResult.Idle
    }
}