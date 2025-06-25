package com.nadavariel.dietapp.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

@RequiresApi(Build.VERSION_CODES.O)
class FoodLogViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    // State for daily meal view
    var selectedDate: LocalDate by mutableStateOf(LocalDate.now())
        private set
    var currentWeekStartDate: LocalDate by mutableStateOf(LocalDate.now())
        private set
    var mealsForSelectedDate: List<Meal> by mutableStateOf(emptyList())
        private set
    private var mealsListenerRegistration: ListenerRegistration? = null

    // --- NEW: State for Statistics Screen ---
    private val _weeklyCalories = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyCalories = _weeklyCalories.asStateFlow()
    // --- End of New State ---


    private fun getSundayOfCurrentWeek(date: LocalDate): LocalDate {
        var result = date
        while (result.dayOfWeek != DayOfWeek.SUNDAY) {
            result = result.minusDays(1)
        }
        return result
    }

    init {
        val today = LocalDate.now()
        currentWeekStartDate = getSundayOfCurrentWeek(today)
        selectedDate = today

        Log.d("FoodLogViewModel", "VM Init: Initial selectedDate=$selectedDate, currentWeekStartDate=$currentWeekStartDate.")

        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    Log.d("FoodLogViewModel", "Auth state changed: User ${currentUser.uid} signed in.")
                    // Listen for daily meals
                    listenForMealsForDate(selectedDate)
                    // Fetch data for statistics
                    fetchMealsForLastSevenDays()
                } else {
                    Log.d("FoodLogViewModel", "Auth state changed: User signed out.")
                    mealsListenerRegistration?.remove()
                    mealsListenerRegistration = null
                    mealsForSelectedDate = emptyList()
                    // Clear weekly data as well
                    _weeklyCalories.value = emptyMap()
                }
            }
        }
    }


    // --- NEW: Functions for Statistics Screen ---

    /**
     * Fetches meals from the last 7 days and processes them to calculate daily calorie totals.
     * This is designed to be called once or when a refresh is needed for the stats screen.
     */
    private fun fetchMealsForLastSevenDays() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Get the date 6 days ago to form a 7-day period including today
                val sevenDaysAgo = LocalDate.now().minusDays(6)
                val startOfPeriod = Date.from(sevenDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant())

                Log.d("FoodLogViewModel", "Fetching meals for stats from: $startOfPeriod")

                val querySnapshot = firestore.collection("users").document(userId).collection("meals")
                    .whereGreaterThanOrEqualTo("timestamp", startOfPeriod)
                    .get()
                    .await()

                val meals = querySnapshot.toObjects(Meal::class.java)
                Log.d("FoodLogViewModel", "Fetched ${meals.size} meals for the last 7 days.")
                processWeeklyCalories(meals)

            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error fetching weekly meals for stats: ${e.message}", e)
            }
        }
    }

    /**
     * Processes a list of meals into a map of daily total calories for the last 7 days.
     */
    private fun processWeeklyCalories(meals: List<Meal>) {
        val today = LocalDate.now()
        // Create a mutable map initialized with the last 7 days, each with 0 calories.
        // This ensures all 7 days are present in the map, even if no meals were logged.
        val caloriesByDay = (0..6).associate {
            today.minusDays(it.toLong()) to 0
        }.toMutableMap()

        // Sum up calories for each meal on its respective day
        for (meal in meals) {
            val mealDate = meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            if (caloriesByDay.containsKey(mealDate)) {
                caloriesByDay[mealDate] = (caloriesByDay[mealDate] ?: 0) + meal.calories
            }
        }

        Log.d("FoodLogViewModel", "Processed weekly calories: $caloriesByDay")
        _weeklyCalories.value = caloriesByDay // Update the StateFlow to notify the UI
    }

    // --- End of New Functions ---


    // --- Meal Logging Operations ---

    fun logMeal(foodName: String, calories: Int, mealTime: Date = Date()) {
        val userId = auth.currentUser?.uid ?: run {
            Log.e("FoodLogViewModel", "Cannot log meal: User not signed in.")
            return
        }
        val meal = Meal(foodName = foodName, calories = calories, timestamp = Timestamp(mealTime))
        viewModelScope.launch {
            try {
                val docRef = firestore.collection("users").document(userId).collection("meals").add(meal).await()
                firestore.collection("users").document(userId).collection("meals").document(docRef.id).update("id", docRef.id).await()
                Log.d("FoodLogViewModel", "Meal '${meal.foodName}' logged successfully.")
                // Refresh weekly data after logging a new meal
                fetchMealsForLastSevenDays()
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
        val mealRef = firestore.collection("users").document(userId).collection("meals").document(mealId)
        val updatedData = hashMapOf(
            "foodName" to newFoodName,
            "calories" to newCalories,
            "timestamp" to newTimestamp
        )
        viewModelScope.launch {
            try {
                mealRef.update(updatedData as Map<String, Any>).await()
                Log.d("FoodLogViewModel", "Meal '$mealId' updated successfully.")
                // Refresh weekly data after updating a meal
                fetchMealsForLastSevenDays()
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
        val mealRef = firestore.collection("users").document(userId).collection("meals").document(mealId)
        viewModelScope.launch {
            try {
                mealRef.delete().await()
                Log.d("FoodLogViewModel", "Meal '$mealId' deleted successfully.")
                // Refresh weekly data after deleting a meal
                fetchMealsForLastSevenDays()
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error deleting meal '$mealId': ${e.message}", e)
            }
        }
    }

    // --- Meal Fetching & Listening ---

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
        mealsListenerRegistration = firestore.collection("users").document(userId).collection("meals")
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
        Log.d("FoodLogViewModel", "ViewModel cleared, firestore listener removed.")
    }

    // --- Date Navigation ---

    fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            selectedDate = date
            if (date.isBefore(currentWeekStartDate) || date.isAfter(currentWeekStartDate.plusDays(6))) {
                currentWeekStartDate = getSundayOfCurrentWeek(date)
            }
            Log.d("FoodLogViewModel", "selectDate: Selected date changed to $selectedDate. Re-listening for meals.")
            listenForMealsForDate(date)
        }
    }

    fun previousWeek() {
        currentWeekStartDate = currentWeekStartDate.minusWeeks(1)
        Log.d("FoodLogViewModel", "Navigating to previous week. New week starts: $currentWeekStartDate.")
        selectDate(currentWeekStartDate)
    }

    fun nextWeek() {
        currentWeekStartDate = currentWeekStartDate.plusWeeks(1)
        Log.d("FoodLogViewModel", "Navigating to next week. New week starts: $currentWeekStartDate.")
        selectDate(currentWeekStartDate)
    }

    fun getMealById(mealId: String): Meal? {
        return mealsForSelectedDate.firstOrNull { it.id == mealId }
    }
}