package com.nadavariel.dietapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.data.Goal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Data class for storing answers in Firestore
data class UserGoalSelection(
    val goalId: String = "", // Reference the Goal's ID
    val goalText: String = "",
    val selectedOption: String = ""
)

class GoalsViewModel : ViewModel() {

    private val auth = Firebase.auth
    private val firestore = Firebase.firestore

    private val _userGoalSelections = MutableStateFlow<List<UserGoalSelection>>(emptyList())
    val userGoalSelections = _userGoalSelections.asStateFlow()

    fun saveUserGoals(goals: List<Goal>, selections: List<String?>) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("GoalsViewModel", "Cannot save goals: User not logged in.")
            return
        }

        val goalsToSave = goals.mapIndexedNotNull { index, goal ->
            selections[index]?.let { selectedOption ->
                UserGoalSelection(
                    goalId = goal.id,
                    goalText = goal.text,
                    selectedOption = selectedOption
                )
            }
        }

        if (goalsToSave.isEmpty()) {
            Log.d("GoalsViewModel", "No goals selected to save.")
            return
        }

        viewModelScope.launch {
            try {
                val goalsCollectionRef = firestore.collection("users")
                    .document(userId)
                    .collection("user_goals")

                // Save each goal as a separate document
                for (goalSelection in goalsToSave) {
                    goalsCollectionRef.document(goalSelection.goalId).set(goalSelection).await()
                }

                Log.d("GoalsViewModel", "Successfully saved user goals.")
            } catch (e: Exception) {
                Log.e("GoalsViewModel", "Error saving user goals", e)
            }
        }
    }
}
