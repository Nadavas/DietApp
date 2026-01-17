package com.nadavariel.dietapp.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.constants.QuizConstants
import com.nadavariel.dietapp.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DietRepository {

    private val firestore: FirebaseFirestore = Firebase.firestore

    // --- Diet Plan Flow ---
    fun getDietPlanFlow(userId: String): Flow<DietPlan?> = callbackFlow {
        val docRef = firestore.collection("users").document(userId)
            .collection("diet_plans").document("current_plan")

        val listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("DietRepository", "Error listening to diet plan", e)
                trySend(null)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                try {
                    fun getListField(fieldName: String): List<String> {
                        val rawValue = snapshot.get(fieldName)
                        return when (rawValue) {
                            is List<*> -> rawValue.mapNotNull { it?.toString() }
                            is String -> listOf(rawValue)
                            else -> emptyList()
                        }
                    }

                    val healthOverview = getListField("healthOverview")
                    val goalStrategy = getListField("goalStrategy")
                    val disclaimer = snapshot.getString("disclaimer") ?: ""
                    val concretePlanMap = snapshot.get("concretePlan") as? Map<String, Any> ?: emptyMap()
                    val targetsMap = concretePlanMap["targets"] as? Map<String, Any> ?: emptyMap()
                    val guidelinesMap = concretePlanMap["mealGuidelines"] as? Map<String, Any> ?: emptyMap()

                    val concretePlan = ConcretePlan(
                        targets = Targets(
                            dailyCalories = (targetsMap["dailyCalories"] as? Long)?.toInt() ?: 0,
                            proteinGrams = (targetsMap["proteinGrams"] as? Long)?.toInt() ?: 0,
                            carbsGrams = (targetsMap["carbsGrams"] as? Long)?.toInt() ?: 0,
                            fatGrams = (targetsMap["fatGrams"] as? Long)?.toInt() ?: 0
                        ),
                        mealGuidelines = MealGuidelines(
                            mealFrequency = guidelinesMap["mealFrequency"] as? String ?: "",
                            foodsToEmphasize = (guidelinesMap["foodsToEmphasize"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                            foodsToLimit = (guidelinesMap["foodsToLimit"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                        ),
                        trainingAdvice = when (val advice = concretePlanMap["trainingAdvice"]) {
                            is List<*> -> advice.mapNotNull { it?.toString() }
                            is String -> listOf(advice)
                            else -> emptyList()
                        }
                    )

                    val mealPlanMap = snapshot.get("exampleMealPlan") as? Map<String, Any> ?: emptyMap()

                    fun mapMeal(key: String): ExampleMeal {
                        val m = mealPlanMap[key] as? Map<String, Any> ?: return ExampleMeal()
                        return ExampleMeal(
                            description = m["description"] as? String ?: "",
                            estimatedCalories = (m["estimatedCalories"] as? Long)?.toInt() ?: 0
                        )
                    }

                    val exampleMealPlan = ExampleMealPlan(
                        breakfast = mapMeal("breakfast"),
                        lunch = mapMeal("lunch"),
                        dinner = mapMeal("dinner"),
                        snacks = mapMeal("snacks")
                    )

                    val plan = DietPlan(
                        healthOverview = healthOverview,
                        goalStrategy = goalStrategy,
                        concretePlan = concretePlan,
                        exampleMealPlan = exampleMealPlan,
                        disclaimer = disclaimer
                    )

                    trySend(plan)
                    Log.d("DietRepository", "Diet plan parsed and emitted.")

                } catch (e: Exception) {
                    Log.e("DietRepository", "Error parsing diet plan", e)
                    trySend(null)
                }
            } else {
                trySend(null)
            }
        }
        awaitClose { listener.remove() }
    }

    // --- User Goals Flow ---
    // Returns a Pair: List of Goals AND the Boolean isAiGenerated
    fun getUserGoalsFlow(userId: String): Flow<Pair<List<Goal>, Boolean>> = callbackFlow {
        val allGoals = listOf(
            Goal(id = "calories", text = QuizConstants.CALORIES_GOAL_QUESTION),
            Goal(id = "protein", text = QuizConstants.PROTEIN_GOAL_QUESTION),
            Goal(id = "target_weight", text = QuizConstants.TARGET_WEIGHT_QUESTION)
        )

        val docRef = firestore.collection("users").document(userId)
            .collection("user_answers").document("goals")

        val listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                trySend(Pair(allGoals, false))
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val answersMap = snapshot.get("answers") as? List<Map<String, String>>
                val aiGenerated = snapshot.getBoolean("aiGenerated") == true

                val userAnswers = answersMap?.associate {
                    it["question"] to it["answer"]
                } ?: emptyMap()

                val mergedGoals = allGoals.map { goal ->
                    val answer = userAnswers[goal.text]
                    if (goal.id == "target_weight" && answer != null) {
                        val parsedValue = answer.split(" ").firstOrNull() ?: ""
                        goal.copy(value = parsedValue)
                    } else {
                        goal.copy(value = answer)
                    }
                }
                trySend(Pair(mergedGoals, aiGenerated))
            } else {
                trySend(Pair(allGoals, false))
            }
        }
        awaitClose { listener.remove() }
    }

    // --- User Weight Flow ---
    fun getUserWeightFlow(userId: String): Flow<Float> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val weight = (snapshot.get("startingWeight") as? Number)?.toFloat() ?: 0f
                    trySend(weight)
                } else {
                    trySend(0f)
                }
            }
        awaitClose { listener.remove() }
    }

    // --- Save Actions ---
    suspend fun saveUserGoals(userId: String, goals: List<Goal>) {
        val userAnswersToSave = goals.map { goal ->
            mapOf("question" to goal.text, "answer" to (goal.value ?: ""))
        }

        val userAnswersRef = firestore.collection("users").document(userId)
            .collection("user_answers").document("goals")

        // We check if it was previously AI generated to preserve that flag, or default to false
        val currentData = userAnswersRef.get().await()
        val aiGenerated = currentData.getBoolean("aiGenerated") == true

        userAnswersRef.set(mapOf(
            "answers" to userAnswersToSave,
            "aiGenerated" to aiGenerated
        )).await()
    }
}