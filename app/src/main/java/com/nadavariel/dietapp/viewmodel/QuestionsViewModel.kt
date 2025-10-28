package com.nadavariel.dietapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.nadavariel.dietapp.data.DietPlan
import com.nadavariel.dietapp.screens.Question
import com.nadavariel.dietapp.model.Gender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import com.nadavariel.dietapp.model.UserProfile

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

    companion object {
        const val DOB_QUESTION = "What is your date of birth?"
        const val GENDER_QUESTION = "What is your gender?"
        const val HEIGHT_QUESTION = "What is your height?"
        const val CURRENT_WEIGHT_QUESTION = "What is your current weight?"
        const val TARGET_WEIGHT_QUESTION_GOAL = "Do you have a target weight or body composition goal in mind?"
        const val TARGET_WEIGHT_GOAL_TEXT = "What is your target weight (in kg)?"
    }

    // FIX: Changed date format to match input "yyyy-MM-dd"
    private val dobFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

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

    private suspend fun saveUserAnswersAndUpdateProfile(
        questions: List<Question>,
        answers: List<String?>
    ) {
        val userId = auth.currentUser?.uid ?: return

        val userAnswersToSave = questions.mapIndexedNotNull { index, question ->
            answers.getOrNull(index)?.takeIf { it.isNotBlank() }?.let { answer ->
                mapOf("question" to question.text, "answer" to answer)
            }
        }

        val profileUpdates = mutableMapOf<String, Any>()
        var targetWeightAnswer: String? = null
        Log.d("QuestionsViewModel", "Preparing profile updates...")

        userAnswersToSave.forEach { answerMap ->
            val questionText = answerMap["question"] as? String
            val answerText = answerMap["answer"] as? String ?: ""

            when (questionText) {
                CURRENT_WEIGHT_QUESTION -> {
                    Log.d("QuestionsViewModel", "Received weight answer string: '$answerText'") // ADDED log
                    // Attempt to parse after removing potential non-numeric characters (like kg)
                    val numericWeight = answerText.filter { it.isDigit() || it == '.' }
                    numericWeight.toFloatOrNull()?.let {
                        profileUpdates["weight"] = it
                        Log.d("QuestionsViewModel", "Adding weight update: $it")
                    } ?: Log.w("QuestionsViewModel", "Could not parse weight: '$answerText'")
                }
                HEIGHT_QUESTION -> {
                    Log.d("QuestionsViewModel", "Received height answer string: '$answerText'") // ADDED log
                    // Attempt to parse after removing potential non-numeric characters (like cm)
                    val numericHeight = answerText.filter { it.isDigit() || it == '.' }
                    numericHeight.toFloatOrNull()?.let {
                        profileUpdates["height"] = it
                        Log.d("QuestionsViewModel", "Adding height update: $it")
                    } ?: Log.w("QuestionsViewModel", "Could not parse height: '$answerText'")
                }
                DOB_QUESTION -> {
                    try {
                        dobFormat.parse(answerText)?.let {
                            val timestamp = com.google.firebase.Timestamp(it)
                            profileUpdates["dateOfBirth"] = timestamp
                            Log.d("QuestionsViewModel", "Adding dateOfBirth update: $timestamp")
                        }
                    } catch (e: Exception) {
                        // Log format error specifically
                        Log.w("QuestionsViewModel", "Could not parse DOB: '$answerText'. Expected format yyyy-MM-dd", e)
                    }
                }
                GENDER_QUESTION -> {
                    val genderEnum = when (answerText) {
                        "Male" -> Gender.MALE
                        "Female" -> Gender.FEMALE
                        "Other / Prefer not to say" -> Gender.PREFER_NOT_TO_SAY
                        else -> Gender.UNKNOWN
                    }
                    profileUpdates["gender"] = genderEnum.name
                    Log.d("QuestionsViewModel", "Adding gender update: ${genderEnum.name}")
                }
                TARGET_WEIGHT_QUESTION_GOAL -> {
                    targetWeightAnswer = answerText
                    Log.d("QuestionsViewModel", "Found target weight answer: $answerText")
                }
            }
        }

        try {
            // 1. Save all questionnaire answers
            firestore.collection("users").document(userId)
                .collection("user_answers").document("diet_habits")
                .set(mapOf("answers" to userAnswersToSave)).await()
            _userAnswers.value = userAnswersToSave.map { UserAnswer(it["question"] as String, it["answer"] as String) }
            Log.d("QuestionsViewModel", "Saved ${userAnswersToSave.size} questionnaire answers.")

            // 2. Update User Profile document
            if (profileUpdates.isNotEmpty()) {
                Log.d("QuestionsViewModel", "Attempting to merge profile updates: $profileUpdates")
                firestore.collection("users").document(userId)
                    .set(profileUpdates, SetOptions.merge()).await()
                Log.d("QuestionsViewModel", "Successfully merged user profile fields.")
            } else {
                Log.d("QuestionsViewModel", "No profile updates to merge.")
            }

            // 3. Update Target Weight Goal
            targetWeightAnswer?.let { weight ->
                updateTargetWeightGoal(userId, weight)
            }

        } catch (e: Exception) {
            Log.e("QuestionsViewModel", "Error saving user answers and profile", e)
        }
    }

    // Helper to update target weight goal (no changes)
    private suspend fun updateTargetWeightGoal(userId: String, targetWeight: String) {
        val goalsRef = firestore.collection("users").document(userId)
            .collection("user_answers").document("goals")
        try {
            val snapshot = goalsRef.get().await()
            val existingAnswers = if (snapshot.exists()) {
                (snapshot.get("answers") as? List<Map<String, String>>)?.toMutableList() ?: mutableListOf()
            } else { mutableListOf() }
            val aiGenerated = snapshot.getBoolean("aiGenerated") ?: false

            val targetWeightQuestionTextForGoal = TARGET_WEIGHT_GOAL_TEXT
            val targetWeightIndex = existingAnswers.indexOfFirst { it["question"] == targetWeightQuestionTextForGoal }

            if (targetWeightIndex != -1) {
                existingAnswers[targetWeightIndex] = mapOf("question" to targetWeightQuestionTextForGoal, "answer" to targetWeight)
            } else {
                existingAnswers.add(mapOf("question" to targetWeightQuestionTextForGoal, "answer" to targetWeight))
            }
            goalsRef.set(mapOf("answers" to existingAnswers, "aiGenerated" to aiGenerated)).await()
            Log.d("QuestionsViewModel", "Updated target weight goal to $targetWeight.")
        } catch (e: Exception) {
            Log.e("QuestionsViewModel", "Error updating target weight goal", e)
        }
    }

    // saveDietPlanToFirestore (no changes)
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
            Log.e("QuestionsViewModel", "Error saving diet plan", e)
        }
    }

    // saveAnswersAndRegeneratePlan (no changes)
    fun saveAnswersAndRegeneratePlan(questions: List<Question>, answers: List<String?>) {
        viewModelScope.launch {
            saveUserAnswersAndUpdateProfile(questions, answers)
            _dietPlanResult.value = DietPlanResult.Loading
            val updatedAnswersString = _userAnswers.value.joinToString("\n") { "Q: ${it.question}\nA: ${it.answer}" }
            try {
                val data = hashMapOf("userProfile" to updatedAnswersString)
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

    // applyDietPlanToGoals (no changes)
    fun applyDietPlanToGoals(plan: DietPlan) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("QuestionsViewModel", "Cannot apply goals: User not logged in.")
            return
        }
        viewModelScope.launch {
            try {
                val goalsRef = firestore.collection("users").document(userId)
                    .collection("user_answers").document("goals")
                val snapshot = goalsRef.get().await()
                val existingAnswers = if (snapshot.exists()) {
                    (snapshot.get("answers") as? List<Map<String, String>>)?.toMutableList() ?: mutableListOf()
                } else { mutableListOf() }

                val aiGoalsMap = mapOf(
                    "How many calories a day is your target?" to plan.dailyCalories.toString(),
                    "How many grams of protein a day is your target?" to plan.proteinGrams.toString()
                )
                val finalGoalsList = mutableListOf<Map<String, String>>()
                val addedOrUpdatedQuestions = mutableSetOf<String>()

                aiGoalsMap.forEach { (question, answer) ->
                    finalGoalsList.add(mapOf("question" to question, "answer" to answer))
                    addedOrUpdatedQuestions.add(question)
                }
                existingAnswers.forEach { existingGoal ->
                    val question = existingGoal["question"]
                    if (question != null && question !in addedOrUpdatedQuestions) {
                        finalGoalsList.add(existingGoal)
                    }
                }
                val dataToSet = mapOf(
                    "answers" to finalGoalsList,
                    "aiGenerated" to true
                )
                goalsRef.set(dataToSet).await()
                Log.d("QuestionsViewModel", "Successfully applied and merged AI diet plan to goals.")
            } catch (e: Exception) {
                Log.e("QuestionsViewModel", "Error applying/merging diet plan to goals", e)
            }
        }
    }

    // resetDietPlanResult (no changes)
    fun resetDietPlanResult() {
        _dietPlanResult.value = DietPlanResult.Idle
    }
}