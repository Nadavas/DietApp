package com.nadavariel.dietapp.repositories

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.nadavariel.dietapp.constants.QuizConstants
import com.nadavariel.dietapp.models.DietPlan
import com.nadavariel.dietapp.models.UserAnswer
import kotlinx.coroutines.tasks.await

class QuizRepository {

    private val firestore: FirebaseFirestore = Firebase.firestore
    private val functions: FirebaseFunctions = Firebase.functions("us-central1")

    // --- Fetch Data ---
    suspend fun fetchUserAnswers(userId: String): List<UserAnswer> {
        val snapshot = firestore.collection("users").document(userId)
            .collection("user_answers").document("diet_habits").get().await()

        if (snapshot.exists()) {
            @Suppress("UNCHECKED_CAST")
            val answersList = snapshot.get("answers") as? List<Map<String, Any>>
            return answersList?.map {
                UserAnswer(it["question"].toString(), it["answer"].toString())
            } ?: emptyList()
        }
        return emptyList()
    }

    // --- Save Logic ---
    suspend fun saveUserAnswers(userId: String, answers: List<Map<String, String>>) {
        firestore.collection("users").document(userId)
            .collection("user_answers").document("diet_habits")
            .set(mapOf("answers" to answers)).await()
    }

    suspend fun updateUserProfileFields(userId: String, updates: Map<String, Any>) {
        if (updates.isNotEmpty()) {
            firestore.collection("users").document(userId)
                .set(updates, SetOptions.merge()).await()
        }
    }

    suspend fun updateTargetWeightGoal(userId: String, targetWeight: String) {
        val goalsRef = firestore.collection("users").document(userId)
            .collection("user_answers").document("goals")

        val snapshot = goalsRef.get().await()
        @Suppress("UNCHECKED_CAST")
        val existingAnswers = if (snapshot.exists()) {
            (snapshot.get("answers") as? List<Map<String, String>>)?.toMutableList() ?: mutableListOf()
        } else {
            mutableListOf()
        }
        val aiGenerated = snapshot.getBoolean("aiGenerated") == true
        val targetWeightQuestionText = QuizConstants.TARGET_WEIGHT_QUESTION

        val targetWeightIndex = existingAnswers.indexOfFirst { it["question"] == targetWeightQuestionText }

        if (targetWeightIndex != -1) {
            existingAnswers[targetWeightIndex] = mapOf("question" to targetWeightQuestionText, "answer" to targetWeight)
        } else {
            existingAnswers.add(mapOf("question" to targetWeightQuestionText, "answer" to targetWeight))
        }

        goalsRef.set(mapOf("answers" to existingAnswers, "aiGenerated" to aiGenerated)).await()
    }

    suspend fun saveDietPlan(userId: String, dietPlan: DietPlan) {
        val planData = mapOf(
            "healthOverview" to dietPlan.healthOverview,
            "goalStrategy" to dietPlan.goalStrategy,
            "concretePlan" to mapOf(
                "targets" to dietPlan.concretePlan.targets,
                "mealGuidelines" to dietPlan.concretePlan.mealGuidelines,
                "trainingAdvice" to dietPlan.concretePlan.trainingAdvice
            ),
            "exampleMealPlan" to dietPlan.exampleMealPlan,
            "disclaimer" to dietPlan.disclaimer,
            "generatedAt" to Timestamp.now()
        )
        firestore.collection("users").document(userId)
            .collection("diet_plans").document("current_plan")
            .set(planData).await()
    }

    suspend fun applyDietPlanToGoals(userId: String, plan: DietPlan) {
        val goalsRef = firestore.collection("users").document(userId)
            .collection("user_answers").document("goals")

        val snapshot = goalsRef.get().await()
        @Suppress("UNCHECKED_CAST")
        val existingAnswers = if (snapshot.exists()) {
            (snapshot.get("answers") as? List<Map<String, String>>)?.toMutableList() ?: mutableListOf()
        } else {
            mutableListOf()
        }

        val aiGoalsMap = mapOf(
            "How many calories a day is your target?" to plan.concretePlan.targets.dailyCalories.toString(),
            "How many grams of protein a day is your target?" to plan.concretePlan.targets.proteinGrams.toString()
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
    }

    // --- Cloud Function ---
    suspend fun generateDietPlan(userProfileString: String): DietPlan {
        val data = hashMapOf("userProfile" to userProfileString)
        val result = functions.getHttpsCallable("generateDietPlan").call(data).await()

        @Suppress("UNCHECKED_CAST")
        val responseData = result.data as? Map<String, Any> ?: throw Exception("Function response is invalid")

        if (responseData["success"] == true) {
            val gson = Gson()
            return gson.fromJson(gson.toJson(responseData["data"]), DietPlan::class.java)
        } else {
            throw Exception(responseData["error"]?.toString() ?: "Unknown error from function")
        }
    }
}