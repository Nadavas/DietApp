package com.nadavariel.dietapp.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.model.Meal
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class FoodLogViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    var selectedDate: LocalDate by mutableStateOf(LocalDate.now())
        private set

    var mealsForSelectedDate: List<Meal> by mutableStateOf(emptyList())
        private set

    private var mealsListenerRegistration: ListenerRegistration? = null

    init {
        // ⭐ CHANGED: Observe authentication state within FoodLogViewModel
        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    // If a user is signed in, start listening for meals for the currently selected date
                    Log.d("FoodLogViewModel", "Auth state changed: User ${currentUser.uid} signed in. Re-listening for meals for selected date: ${selectedDate}.")
                    listenForMealsForDate(selectedDate)
                } else {
                    // If no user is signed in (e.g., signed out), clear meals and stop listening
                    Log.d("FoodLogViewModel", "Auth state changed: User signed out. Clearing meals and stopping listener.")
                    mealsListenerRegistration?.remove()
                    mealsListenerRegistration = null // Ensure listener is truly gone
                    mealsForSelectedDate = emptyList() // Clear the displayed meals
                }
            }
        }
        // Removed the direct call to listenForMealsForDate(selectedDate) from here,
        // as the auth listener will now handle the initial load.
    }

    // --- Meal Logging Operations (no changes needed here from your previous version) ---

    fun logMeal(foodName: String, calories: Int, mealTime: Date = Date()) {
        val userId = auth.currentUser?.uid ?: run {
            Log.e("FoodLogViewModel", "Cannot log meal: User not signed in.")
            return
        }

        val meal = Meal(
            foodName = foodName,
            calories = calories,
            timestamp = Timestamp(mealTime)
        )

        viewModelScope.launch {
            try {
                val docRef = firestore.collection("users")
                    .document(userId)
                    .collection("meals")
                    .add(meal)
                    .await()

                firestore.collection("users")
                    .document(userId)
                    .collection("meals")
                    .document(docRef.id)
                    .update("id", docRef.id)
                    .await()

                Log.d("FoodLogViewModel", "Meal '${meal.foodName}' logged successfully with ID: ${docRef.id}")
                // Refresh meals for the currently selected date after logging
                // The real-time listener will automatically update mealsForSelectedDate.
                // listenForMealsForDate(selectedDate) // Removed: Listener does this automatically
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error logging meal: ${e.message}", e)
            }
        }
    }

    fun updateMeal(mealId: String, newFoodName: String, newCalories: Int, newTimestamp: Timestamp) {
        val userId = auth.currentUser?.uid ?: run {
            Log.e("FoodLogViewModel", "Cannot update meal: User not signed in.")
            return
        }

        val mealRef = firestore.collection("users")
            .document(userId)
            .collection("meals")
            .document(mealId)

        val updatedData = hashMapOf(
            "foodName" to newFoodName,
            "calories" to newCalories,
            "timestamp" to newTimestamp
        )

        viewModelScope.launch {
            try {
                mealRef.update(updatedData as Map<String, Any>).await()
                Log.d("FoodLogViewModel", "Meal '$mealId' updated successfully.")
                // The real-time listener will automatically update mealsForSelectedDate.
                // listenForMealsForDate(selectedDate) // Removed: Listener does this automatically
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error updating meal '$mealId': ${e.message}", e)
            }
        }
    }

    fun deleteMeal(mealId: String) {
        val userId = auth.currentUser?.uid ?: run {
            Log.e("FoodLogViewModel", "Cannot delete meal: User not signed in.")
            return
        }

        val mealRef = firestore.collection("users")
            .document(userId)
            .collection("meals")
            .document(mealId)

        viewModelScope.launch {
            try {
                mealRef.delete().await()
                Log.d("FoodLogViewModel", "Meal '$mealId' deleted successfully.")
                // The real-time listener will automatically update mealsForSelectedDate.
                // listenForMealsForDate(selectedDate) // Removed: Listener does this automatically
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error deleting meal '$mealId': ${e.message}", e)
            }
        }
    }

    // --- Meal Fetching & Listening (now specifically for the selected date) ---

    // This function updates the Compose mutableStateOf directly
    private fun listenForMealsForDate(date: LocalDate) {
        mealsListenerRegistration?.remove() // Remove previous listener if any

        val userId = auth.currentUser?.uid ?: run {
            Log.e("FoodLogViewModel", "Cannot listen for meals: User not signed in. This block should ideally not be reached if called by auth listener.")
            mealsForSelectedDate = emptyList() // Update Compose State directly
            return
        }

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        mealsListenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("meals")
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(Date(startOfDay)))
            .whereLessThan("timestamp", Timestamp(Date(endOfDay)))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FoodLogViewModel", "Listen failed.", e)
                    mealsForSelectedDate = emptyList() // Update Compose State directly
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val mealList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Meal::class.java)?.copy(id = doc.id)
                    }
                    mealsForSelectedDate = mealList // Update Compose State directly
                    Log.d("FoodLogViewModel", "Meals for ${date} updated: ${mealList.size} meals.")
                } else {
                    Log.d("FoodLogViewModel", "Current data: null")
                    mealsForSelectedDate = emptyList() // Update Compose State directly
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        mealsListenerRegistration?.remove()
        Log.d("FoodLogViewModel", "ViewModel cleared, Firestore listener removed.")
    }

    // Function to change the selected date from UI, updates Compose State directly
    fun selectDate(date: LocalDate) {
        if (selectedDate != date) { // Only update and re-listen if the date actually changes
            selectedDate = date
            Log.d("FoodLogViewModel", "Date selected: $date. Re-listening for meals.")
            listenForMealsForDate(date)
        }
    }
}