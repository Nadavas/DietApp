// viewmodel/FoodLogViewModel.kt
package com.nadavariel.dietapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.model.Meal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class FoodLogViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    // State to hold the list of meals for the currently viewed period (for FoodLogScreen's list part, will be moved later)
    private val _meals = MutableStateFlow<List<Meal>>(emptyList())
    val meals: StateFlow<List<Meal>> = _meals.asStateFlow()

    // State for statistics (will be used by HomeScreen)
    private val _dailyCalories = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val dailyCalories: StateFlow<Map<LocalDate, Int>> = _dailyCalories.asStateFlow()

    init {
        // Initial fetch for statistics, assuming HomeScreen will trigger this
        // Or you might want to call a specific fetch on screen load
        // listenForMealsForDate(LocalDate.now()) // This listener will be managed by HomeScreen/FoodLogScreen's list view
    }

    // --- Meal Logging Operations ---

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
                // Add a new document and let Firestore generate the ID
                val docRef = firestore.collection("users")
                    .document(userId)
                    .collection("meals")
                    .add(meal)
                    .await()

                // Update the meal object with the Firestore-generated ID
                firestore.collection("users")
                    .document(userId)
                    .collection("meals")
                    .document(docRef.id)
                    .update("id", docRef.id) // Save the ID into the document itself
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

    // --- Meal Fetching & Listening (for displaying daily meals in HomeScreen, or a dedicated list) ---

    private var mealsListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun listenForMealsForDate(date: LocalDate) {
        mealsListenerRegistration?.remove() // Remove previous listener if any

        val userId = auth.currentUser?.uid ?: run {
            Log.e("FoodLogViewModel", "Cannot listen for meals: User not signed in.")
            _meals.value = emptyList()
            return
        }

        // Calculate start and end of the day in UTC
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        mealsListenerRegistration = firestore.collection("users")
            .document(userId)
            .collection("meals")
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(Date(startOfDay)))
            .whereLessThan("timestamp", Timestamp(Date(endOfDay)))
            .orderBy("timestamp", Query.Direction.ASCENDING) // Order by time of consumption
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("FoodLogViewModel", "Listen failed.", e)
                    _meals.value = emptyList()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val mealList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Meal::class.java)?.copy(id = doc.id) // Ensure ID is set
                    }
                    _meals.value = mealList
                    Log.d("FoodLogViewModel", "Meals for ${date} updated: ${mealList.size} meals.")
                } else {
                    Log.d("FoodLogViewModel", "Current data: null")
                    _meals.value = emptyList()
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        mealsListenerRegistration?.remove() // Remove listener when ViewModel is cleared
    }

    // --- Statistics Operations (will be called by HomeScreen) ---

    fun fetchDailyCalorieStatistics(daysAgo: Int) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: run {
                Log.e("FoodLogViewModel", "Cannot fetch statistics: User not signed in.")
                _dailyCalories.value = emptyMap()
                return@launch
            }

            val today = LocalDate.now()
            val endDate = today.plusDays(1) // End of today
            val startDate = today.minusDays(daysAgo.toLong()) // Start of (daysAgo) days ago

            val startMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("meals")
                    .whereGreaterThanOrEqualTo("timestamp", Timestamp(Date(startMillis)))
                    .whereLessThan("timestamp", Timestamp(Date(endMillis)))
                    .get()
                    .await()

                val allMealsInPeriod = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Meal::class.java)
                }

                val calculatedCalories = calculateCaloriesPerDay(allMealsInPeriod)
                _dailyCalories.value = calculatedCalories

                Log.d("FoodLogViewModel", "Fetched statistics for last $daysAgo days: $calculatedCalories")

            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error fetching statistics: ${e.message}", e)
                _dailyCalories.value = emptyMap()
            }
        }
    }

    private fun calculateCaloriesPerDay(meals: List<Meal>): Map<LocalDate, Int> {
        return meals.groupBy { meal ->
            meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }.mapValues { (_, mealsPerDay) ->
            mealsPerDay.sumOf { it.calories }
        }
    }
}