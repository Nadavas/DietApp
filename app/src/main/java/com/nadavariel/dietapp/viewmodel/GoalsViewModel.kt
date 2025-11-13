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

        // 8. SAVE THE LISTENER
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
                    val plan = snapshot.toObject(DietPlan::class.java)
                    _currentDietPlan.value = plan
                    Log.d("GoalsViewModel", "Live diet plan updated.")
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
            Goal(id = "target_weight", text = "Do you have a target weight or body composition goal in mind?")
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