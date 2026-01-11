package com.nadavariel.dietapp.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.nadavariel.dietapp.constants.QuizConstants
import com.nadavariel.dietapp.models.*
import com.nadavariel.dietapp.repositories.AuthRepository
import com.nadavariel.dietapp.repositories.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

sealed class DietPlanResult {
    data object Idle : DietPlanResult()
    data object Loading : DietPlanResult()
    data class Success(val plan: DietPlan) : DietPlanResult()
    data class Error(val message: String) : DietPlanResult()
}

class QuizViewModel(
    private val authRepository: AuthRepository,
    private val quizRepository: QuizRepository
) : ViewModel() {

    private val _userAnswers = MutableStateFlow<List<UserAnswer>>(emptyList())
    val userAnswers = _userAnswers.asStateFlow()

    private val _dietPlanResult = MutableStateFlow<DietPlanResult>(DietPlanResult.Idle)
    val dietPlanResult = _dietPlanResult.asStateFlow()

    private val dobFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    init {
        fetchUserAnswers()
    }

    private fun fetchUserAnswers() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _userAnswers.value = quizRepository.fetchUserAnswers(userId)
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error fetching user answers", e)
            }
        }
    }

    private suspend fun saveUserAnswersAndUpdateProfile(
        questions: List<Question>,
        answers: List<String?>
    ) {
        val userId = authRepository.currentUser?.uid ?: return

        // 1. Prepare Answers Data
        val userAnswersToSave = questions.mapIndexedNotNull { index, question ->
            answers.getOrNull(index)?.takeIf { it.isNotBlank() }?.let { answer ->
                mapOf("question" to question.text, "answer" to answer)
            }
        }

        // 2. Parse Profile Updates (Parsing Logic stays in VM, it's business logic)
        val profileUpdates = mutableMapOf<String, Any>()
        var targetWeightAnswer: String? = null
        Log.d("QuizViewModel", "Preparing profile updates...")

        userAnswersToSave.forEach { answerMap ->
            val questionText = answerMap["question"]
            val answerText = answerMap["answer"] ?: ""

            when (questionText) {
                QuizConstants.WEIGHT_QUESTION -> {
                    val numericWeight = answerText.filter { it.isDigit() || it == '.' }
                    numericWeight.toFloatOrNull()?.let {
                        profileUpdates["startingWeight"] = it
                    }
                }
                QuizConstants.HEIGHT_QUESTION -> {
                    val numericHeight = answerText.filter { it.isDigit() || it == '.' }
                    numericHeight.toFloatOrNull()?.let {
                        profileUpdates["height"] = it
                    }
                }
                QuizConstants.DOB_QUESTION -> {
                    try {
                        dobFormat.parse(answerText)?.let {
                            val timestamp = Timestamp(it)
                            profileUpdates["dateOfBirth"] = timestamp
                        }
                    } catch (e: Exception) {
                        Log.w("QuizViewModel", "Could not parse DOB", e)
                    }
                }
                QuizConstants.GENDER_QUESTION -> {
                    val genderEnum = Gender.fromString(answerText)
                    profileUpdates["gender"] = genderEnum.name
                }
                QuizConstants.TARGET_WEIGHT_QUESTION -> {
                    targetWeightAnswer = answerText
                }
            }
        }

        try {
            // 3. Perform Saves via Repository
            quizRepository.saveUserAnswers(userId, userAnswersToSave)

            // Update local state
            _userAnswers.value = userAnswersToSave.map {
                UserAnswer(it["question"] ?: "", it["answer"] ?: "")
            }

            if (profileUpdates.isNotEmpty()) {
                quizRepository.updateUserProfileFields(userId, profileUpdates)
                Log.d("QuizViewModel", "Successfully merged user profile fields.")
            }

            targetWeightAnswer?.let { weight ->
                quizRepository.updateTargetWeightGoal(userId, weight)
            }

        } catch (e: Exception) {
            Log.e("QuizViewModel", "Error saving user answers and profile", e)
        }
    }

    fun saveAnswersAndRegeneratePlan(
        authViewModel: AuthViewModel,
        questions: List<Question>,
        answers: List<String?>
    ) {
        viewModelScope.launch {
            _dietPlanResult.value = DietPlanResult.Loading
            try {
                // 1. Ensure User Logic (Delegated to AuthViewModel)
                if (authRepository.currentUser == null) {
                    Log.d("QuizViewModel", "New Email user detected. Creating Email user...")
                    authViewModel.createEmailUserAndProfile()
                } else if (authViewModel.isGoogleSignUp.value) {
                    Log.d("QuizViewModel", "New Google user detected. Creating Google user...")
                    authViewModel.createGoogleUserAndProfile()
                }

                // 2. Save Answers
                saveUserAnswersAndUpdateProfile(questions, answers)

                // 3. Generate Plan
                val updatedAnswersString = _userAnswers.value.joinToString("\n") { "Q: ${it.question}\nA: ${it.answer}" }

                // If user auth dropped, stop
                if (authRepository.currentUser == null) return@launch

                // Call Repository for Cloud Function
                val dietPlan = quizRepository.generateDietPlan(updatedAnswersString)

                // 4. Save Plan & Apply Goals
                val userId = authRepository.currentUser?.uid
                if (userId != null) {
                    quizRepository.saveDietPlan(userId, dietPlan)
                    quizRepository.applyDietPlanToGoals(userId, dietPlan)
                    _dietPlanResult.value = DietPlanResult.Success(dietPlan)
                }

            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error generating diet plan", e)
                if (authRepository.currentUser != null) {
                    _dietPlanResult.value = DietPlanResult.Error(e.message ?: "An unexpected error occurred.")
                }
            }
        }
    }

    fun resetDietPlanResult() {
        _dietPlanResult.value = DietPlanResult.Idle
    }

    fun clearData() {
        _userAnswers.value = emptyList()
        _dietPlanResult.value = DietPlanResult.Idle
        Log.d("QuizViewModel", "Local data cleared.")
    }
}