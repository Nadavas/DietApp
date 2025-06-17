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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class FoodLogViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    var selectedDate: LocalDate by mutableStateOf(LocalDate.now())
        private set

    // Initial value will be set in init block using the helper function
    var currentWeekStartDate: LocalDate by mutableStateOf(LocalDate.now()) // Temporary value, will be set correctly below
        private set

    var mealsForSelectedDate: List<Meal> by mutableStateOf(emptyList())
        private set

    private var mealsListenerRegistration: ListenerRegistration? = null

    // ⭐ NEW HELPER FUNCTION: Correctly finds the Sunday that begins the current week
    private fun getSundayOfCurrentWeek(date: LocalDate): LocalDate {
        var result = date
        // Keep subtracting days until we reach Sunday.
        // If 'date' is already Sunday, this loop won't run.
        while (result.dayOfWeek != DayOfWeek.SUNDAY) {
            result = result.minusDays(1)
        }
        return result
    }

    init {
        val today = LocalDate.now()
        // ⭐ Use the new helper function for currentWeekStartDate
        val sundayOfThisWeekCorrect = getSundayOfCurrentWeek(today)

        Log.d("FoodLogViewModel", "DEBUG_CALC: LocalDate.now() reports: $today")
        // Keeping this for comparison, it should show the next Sunday
        Log.d("FoodLogViewModel", "DEBUG_CALC: today.with(DayOfWeek.SUNDAY) (direct call) yields: ${today.with(DayOfWeek.SUNDAY)}")
        Log.d("FoodLogViewModel", "DEBUG_CALC: getSundayOfCurrentWeek() calculated as: $sundayOfThisWeekCorrect")

        selectedDate = today
        currentWeekStartDate = sundayOfThisWeekCorrect // Assign the correctly calculated value

        Log.d("FoodLogViewModel", "VM Init: Initial selectedDate=${selectedDate}, currentWeekStartDate=${currentWeekStartDate} (Sunday of its week).")

        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    Log.d("FoodLogViewModel", "Auth state changed: User ${currentUser.uid} signed in. Fetching meals for selected date: ${selectedDate}.")
                    listenForMealsForDate(selectedDate)
                } else {
                    Log.d("FoodLogViewModel", "Auth state changed: User signed out. Clearing meals and stopping listener.")
                    mealsListenerRegistration?.remove()
                    mealsListenerRegistration = null
                    mealsForSelectedDate = emptyList()
                }
            }
        }
    }

    // --- Meal Logging Operations --- (No changes here)

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
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error deleting meal '$mealId': ${e.message}", e)
            }
        }
    }

    // --- Meal Fetching & Listening --- (No changes here)

    private fun listenForMealsForDate(date: LocalDate) {
        mealsListenerRegistration?.remove()

        val userId = auth.currentUser?.uid ?: run {
            Log.e("FoodLogViewModel", "Cannot listen for meals: User not signed in.")
            mealsForSelectedDate = emptyList()
            return
        }

        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        Log.d("FoodLogViewModel", "Listening for meals for date: $date ($startOfDay - $endOfDay)")

        mealsListenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("meals")
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(Date(startOfDay)))
            .whereLessThan("timestamp", Timestamp(Date(endOfDay)))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FoodLogViewModel", "Listen failed.", e)
                    mealsForSelectedDate = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val mealList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Meal::class.java)?.copy(id = doc.id)
                    }
                    mealsForSelectedDate = mealList
                    Log.d("FoodLogViewModel", "Meals for $date updated: ${mealList.size} meals.")
                } else {
                    Log.d("FoodLogViewModel", "Current data: null")
                    mealsForSelectedDate = emptyList()
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
        if (selectedDate != date) {
            val oldSelectedDate = selectedDate
            selectedDate = date
            // Ensure currentWeekStartDate encompasses the newly selected date
            if (date.isBefore(currentWeekStartDate) || date.isAfter(currentWeekStartDate.plusDays(6))) {
                val oldWeekStart = currentWeekStartDate
                currentWeekStartDate = getSundayOfCurrentWeek(date) // ⭐ Use the new helper here
                Log.d("FoodLogViewModel", "selectDate: Week changed from $oldWeekStart to $currentWeekStartDate for new selectedDate $selectedDate.")
            }
            Log.d("FoodLogViewModel", "selectDate: Selected date changed from $oldSelectedDate to $selectedDate. Re-listening for meals.")
            listenForMealsForDate(date)
        } else {
            Log.d("FoodLogViewModel", "selectDate: Date $date already selected. No change needed.")
        }
    }

    // Navigate between weeks
    fun previousWeek() {
        val oldWeekStart = currentWeekStartDate
        currentWeekStartDate = currentWeekStartDate.minusWeeks(1)
        Log.d("FoodLogViewModel", "Navigating to previous week from $oldWeekStart to $currentWeekStartDate.")
        selectDate(currentWeekStartDate) // This will call selectDate and trigger getSundayOfCurrentWeek if week changes
    }

    fun nextWeek() {
        val oldWeekStart = currentWeekStartDate
        currentWeekStartDate = currentWeekStartDate.plusWeeks(1)
        Log.d("FoodLogViewModel", "Navigating to next week from $oldWeekStart to $currentWeekStartDate.")
        selectDate(currentWeekStartDate) // This will call selectDate and trigger getSundayOfCurrentWeek if week changes
    }
}