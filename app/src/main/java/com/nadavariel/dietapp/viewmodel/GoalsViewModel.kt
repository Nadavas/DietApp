package com.nadavariel.dietapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth // <-- 1. IMPORT
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore // <-- 2. IMPORT
import com.google.firebase.firestore.ListenerRegistration // <-- 3. IMPORT
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.model.Goal
import com.nadavariel.dietapp.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.nadavariel.dietapp.model.DietPlan

class GoalsViewModel : ViewModel() {

    private val auth: FirebaseAuth = Firebase.auth // <-- 4. ADD AUTH INSTANCE
    private val firestore: FirebaseFirestore = Firebase.firestore // <-- 5. ADD FIRESTORE INSTANCE

    // --- 6. ADD LISTENER REGISTRATIONS ---
    private var dietPlanListener: ListenerRegistration? = null
    private var goalsListener: ListenerRegistration? = null
    private var profileListener: ListenerRegistration? = null

    private val _currentDietPlan = MutableStateFlow<DietPlan?>(null)
    val currentDietPlan = _currentDietPlan.asStateFlow()

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    val goals = _goals.asStateFlow()

    private val _userWeight = MutableStateFlow(0f)
    val userWeight = _userWeight.asStateFlow()

    private val _hasAiGeneratedGoals = MutableStateFlow(false)
    val hasAiGeneratedGoals = _hasAiGeneratedGoals.asStateFlow()

    private val _isLoadingPlan = MutableStateFlow(true)
    val isLoadingPlan = _isLoadingPlan.asStateFlow()

    init {
        // --- 7. ADD AUTH STATE LISTENER ---
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser != null) {
                // User is signed in, NOW we fetch data
                fetchDietPlan()
                fetchUserGoals()
                fetchUserProfile()
            } else {
                // User signed out, clear all data and listeners
                clearAllListenersAndData()
            }
        }
    }

    private fun fetchDietPlan() {
        dietPlanListener?.remove() // Clear previous listener
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("GoalsViewModel", "Cannot fetch diet plan: User not logged in.")
            _isLoadingPlan.value = false
            return
        }

        _isLoadingPlan.value = true

        dietPlanListener = firestore.collection("users").document(userId)
            .collection("diet_plans").document("current_plan")
            .addSnapshotListener { snapshot, e ->
                _isLoadingPlan.value = false

                if (e != null) {
                    Log.e("GoalsViewModel", "Error listening to diet plan", e)
                    _currentDietPlan.value = null
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        // --- MANUAL MAPPING TO PREVENT CRASH ON OLD DATA ---

                        // 1. Helper to safely get List<String> from String OR List
                        fun getListField(fieldName: String): List<String> {
                            val rawValue = snapshot.get(fieldName)
                            return when (rawValue) {
                                is List<*> -> rawValue.mapNotNull { it?.toString() }
                                is String -> listOf(rawValue) // Fallback for old data
                                else -> emptyList()
                            }
                        }

                        // 2. Helper to safely get List<String> from nested ConcretePlan
                        fun getNestedListField(path: String): List<String> {
                            val rawValue = snapshot.get(path) // e.g. "concretePlan.trainingAdvice"
                            return when (rawValue) {
                                is List<*> -> rawValue.mapNotNull { it?.toString() }
                                is String -> listOf(rawValue)
                                else -> emptyList()
                            }
                        }

                        // 3. Map top-level fields
                        val healthOverview = getListField("healthOverview")
                        val goalStrategy = getListField("goalStrategy")
                        val disclaimer = snapshot.getString("disclaimer") ?: ""

                        // 4. Map ConcretePlan (Manually to handle trainingAdvice list)
                        val concretePlanMap = snapshot.get("concretePlan") as? Map<String, Any> ?: emptyMap()
                        val targetsMap = concretePlanMap["targets"] as? Map<String, Any> ?: emptyMap()
                        val guidelinesMap = concretePlanMap["mealGuidelines"] as? Map<String, Any> ?: emptyMap()

                        // Reconstruct ConcretePlan manually
                        val concretePlan = com.nadavariel.dietapp.model.ConcretePlan(
                            targets = com.nadavariel.dietapp.model.Targets(
                                dailyCalories = (targetsMap["dailyCalories"] as? Long)?.toInt() ?: 0,
                                proteinGrams = (targetsMap["proteinGrams"] as? Long)?.toInt() ?: 0,
                                carbsGrams = (targetsMap["carbsGrams"] as? Long)?.toInt() ?: 0,
                                fatGrams = (targetsMap["fatGrams"] as? Long)?.toInt() ?: 0
                            ),
                            mealGuidelines = com.nadavariel.dietapp.model.MealGuidelines(
                                mealFrequency = guidelinesMap["mealFrequency"] as? String ?: "",
                                foodsToEmphasize = (guidelinesMap["foodsToEmphasize"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                                foodsToLimit = (guidelinesMap["foodsToLimit"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                            ),
                            // Handle trainingAdvice list vs string manually from the map
                            trainingAdvice = when (val advice = concretePlanMap["trainingAdvice"]) {
                                is List<*> -> advice.mapNotNull { it?.toString() }
                                is String -> listOf(advice)
                                else -> emptyList()
                            }
                        )

                        // 5. Map ExampleMealPlan (Standard mapping is safe here usually, but manual for consistency)
                        // Using toObject for this part is generally safe as structure didn't change drastically,
                        // but let's stick to the manual reconstruction to be 100% safe.
                        val mealPlanMap = snapshot.get("exampleMealPlan") as? Map<String, Any> ?: emptyMap()

                        fun mapMeal(key: String): com.nadavariel.dietapp.model.ExampleMeal {
                            val m = mealPlanMap[key] as? Map<String, Any> ?: return com.nadavariel.dietapp.model.ExampleMeal()
                            return com.nadavariel.dietapp.model.ExampleMeal(
                                description = m["description"] as? String ?: "",
                                estimatedCalories = (m["estimatedCalories"] as? Long)?.toInt() ?: 0
                            )
                        }

                        val exampleMealPlan = com.nadavariel.dietapp.model.ExampleMealPlan(
                            breakfast = mapMeal("breakfast"),
                            lunch = mapMeal("lunch"),
                            dinner = mapMeal("dinner"),
                            snacks = mapMeal("snacks")
                        )

                        // 6. Build final object
                        val safePlan = DietPlan(
                            healthOverview = healthOverview,
                            goalStrategy = goalStrategy,
                            concretePlan = concretePlan,
                            exampleMealPlan = exampleMealPlan,
                            disclaimer = disclaimer
                        )

                        _currentDietPlan.value = safePlan
                        Log.d("GoalsViewModel", "Live diet plan updated (Safe Mapping).")

                    } catch (e: Exception) {
                        Log.e("GoalsViewModel", "Error parsing diet plan manually", e)
                        _currentDietPlan.value = null
                    }
                } else {
                    Log.d("GoalsViewModel", "No diet plan document found.")
                    _currentDietPlan.value = null
                }
            }
    }

    private fun fetchUserGoals() {
        goalsListener?.remove() // Clear previous listener
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("GoalsViewModel", "Cannot fetch goals: User not logged in.")
            return
        }

        val allGoals = listOf(
            Goal(id = "calories", text = "How many calories a day is your target?"),
            Goal(id = "protein", text = "How many grams of protein a day is your target?"),
            Goal(id = "target_weight", text = "Do you have a target weight?")
        )

        // 9. SAVE THE LISTENER
        goalsListener = firestore.collection("users").document(userId)
            .collection("user_answers").document("goals")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("GoalsViewModel", "Error listening to user answers", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
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

                    _goals.value = mergedGoals
                    _hasAiGeneratedGoals.value = aiGenerated
                    Log.d("GoalsViewModel", "Live goals updated: ${mergedGoals.size} items, AI-generated: $aiGenerated")
                } else {
                    _goals.value = allGoals
                    _hasAiGeneratedGoals.value = false
                    Log.d("GoalsViewModel", "No saved answers yet, using base goals.")
                }
            }
    }

    private fun fetchUserProfile() {
        profileListener?.remove() // Clear previous listener
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("GoalsViewModel", "Cannot fetch profile: User not logged in.")
            return
        }

        // 10. SAVE THE LISTENER
        profileListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("GoalsViewModel", "Error listening to user profile", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    _userWeight.value = profile?.startingWeight ?: 0f
                } else {
                    Log.d("GoalsViewModel", "User profile does not exist.")
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
                    mapOf("question" to goal.text, "answer" to (goal.value ?: ""))
                }

                val userAnswersRef = firestore.collection("users").document(userId)
                    .collection("user_answers").document("goals")

                val currentData = userAnswersRef.get().await()
                val aiGenerated = currentData.getBoolean("aiGenerated") == true

                userAnswersRef.set(mapOf(
                    "answers" to userAnswersToSave,
                    "aiGenerated" to aiGenerated
                )).await()

                Log.d("GoalsViewModel", "Successfully saved user answers.")
            } catch (e: Exception) {
                Log.e("GoalsViewModel", "Error saving user answers", e)
            }
        }
    }

    fun updateAnswer(goalId: String, answer: String) {
        _goals.value = _goals.value.map { goal ->
            if (goal.id == goalId) goal.copy(value = answer) else goal
        }
    }

    // --- 11. ADD CLEANUP FUNCTIONS ---
    private fun clearAllListenersAndData() {
        dietPlanListener?.remove()
        dietPlanListener = null
        goalsListener?.remove()
        goalsListener = null
        profileListener?.remove()
        profileListener = null

        _currentDietPlan.value = null
        _goals.value = emptyList()
        _userWeight.value = 0f
        _hasAiGeneratedGoals.value = false
        _isLoadingPlan.value = false // Set to false, not true
        Log.d("GoalsViewModel", "Cleared all listeners and data.")
    }

    override fun onCleared() {
        super.onCleared()
        clearAllListenersAndData()
    }
}