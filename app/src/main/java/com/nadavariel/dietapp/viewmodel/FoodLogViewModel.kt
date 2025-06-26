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

    // This will represent the start of the currently displayed 7-day period.
    var currentWeekStartDate: LocalDate by mutableStateOf(LocalDate.now().minusDays(6))
        private set

    var mealsForSelectedDate: List<Meal> by mutableStateOf(emptyList())
        private set
    private var mealsListenerRegistration: ListenerRegistration? = null

    private val _weeklyCalories = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyCalories = _weeklyCalories.asStateFlow()

    // Helper function to get the start date of a 7-day period ending on 'date'
    private fun calculateWeekStartEndingOnDate(date: LocalDate): LocalDate {
        return date.minusDays(6)
    }

    init {
        val today = LocalDate.now()
        // Initialize currentWeekStartDate so that 'today' is the end of the week
        currentWeekStartDate = calculateWeekStartEndingOnDate(today)
        selectedDate = today // Ensure selectedDate is today initially

        Log.d("FoodLogViewModel", "VM Init: Initial selectedDate=$selectedDate, currentWeekStartDate=$currentWeekStartDate (Week ending on selectedDate).")

        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    Log.d("FoodLogViewModel", "Auth state changed: User ${currentUser.uid} signed in.")
                    listenForMealsForDate(selectedDate)
                    fetchMealsForLastSevenDays()
                } else {
                    Log.d("FoodLogViewModel", "Auth state changed: User signed out.")
                    mealsListenerRegistration?.remove()
                    mealsListenerRegistration = null
                    mealsForSelectedDate = emptyList()
                    _weeklyCalories.value = emptyMap()
                }
            }
        }
    }

    private fun fetchMealsForLastSevenDays() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
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

    private fun processWeeklyCalories(meals: List<Meal>) {
        val today = LocalDate.now()
        val caloriesByDay = (0..6).associate {
            today.minusDays(it.toLong()) to 0
        }.toMutableMap()

        for (meal in meals) {
            val mealDate = meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            if (caloriesByDay.containsKey(mealDate)) {
                caloriesByDay[mealDate] = (caloriesByDay[mealDate] ?: 0) + meal.calories
            }
        }

        Log.d("FoodLogViewModel", "Processed weekly calories: $caloriesByDay")
        _weeklyCalories.value = caloriesByDay
    }

    fun logMeal(foodName: String, calories: Int, mealTime: Date = Date()) {
        val userId = auth.currentUser?.uid ?: run {
            Log.e("FoodLogViewModel", "Cannot log meal: User not signed in.")
            return
        }

        val now = Date()
        if (mealTime.after(now)) {
            Log.e("FoodLogViewModel", "Attempted to log a meal in the future. Meal not logged.")
            return
        }

        val meal = Meal(foodName = foodName, calories = calories, timestamp = Timestamp(mealTime))
        viewModelScope.launch {
            try {
                val docRef = firestore.collection("users").document(userId).collection("meals").add(meal).await()
                firestore.collection("users").document(userId).collection("meals").document(docRef.id).update("id", docRef.id).await()
                Log.d("FoodLogViewModel", "Meal '${meal.foodName}' logged successfully.")
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

        val now = Date()
        if (newTimestamp.toDate().after(now)) {
            Log.e("FoodLogViewModel", "Attempted to update meal '$mealId' to a future time. Update not performed.")
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
                fetchMealsForLastSevenDays()
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error deleting meal '$mealId': ${e.message}", e)
            }
        }
    }

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

    // ⭐ MODIFIED: This function now only updates selectedDate and fetches meals,
    // it does NOT change currentWeekStartDate when called from UI day clicks.
    fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            selectedDate = date
            Log.d("FoodLogViewModel", "selectDate: Selected date changed to $selectedDate. Re-listening for meals.")
            listenForMealsForDate(date)
        } else {
            Log.d("FoodLogViewModel", "selectDate: Date $date already selected. No change needed, ensuring listener is active.")
            listenForMealsForDate(date) // Ensure listener is active even if same date is re-tapped
        }
    }

    // ⭐ MODIFIED: These functions now explicitly update currentWeekStartDate
    // and then set selectedDate to the end of that *new* week.
    fun previousWeek() {
        val newCurrentWeekStartDate = currentWeekStartDate.minusWeeks(1)
        currentWeekStartDate = newCurrentWeekStartDate // Update the week range
        selectedDate = newCurrentWeekStartDate.plusDays(6) // Set selected date to the new end of the week
        Log.d("FoodLogViewModel", "Navigating to previous week. New week starts: $currentWeekStartDate. New selected date: $selectedDate.")
        listenForMealsForDate(selectedDate) // Fetch meals for the new selected date
    }

    fun nextWeek() {
        val newCurrentWeekStartDate = currentWeekStartDate.plusWeeks(1)
        currentWeekStartDate = newCurrentWeekStartDate // Update the week range
        selectedDate = newCurrentWeekStartDate.plusDays(6) // Set selected date to the new end of the week
        Log.d("FoodLogViewModel", "Navigating to next week. New week starts: $currentWeekStartDate. New selected date: $selectedDate.")
        listenForMealsForDate(selectedDate) // Fetch meals for the new selected date
    }

    fun getMealById(mealId: String): Meal? {
        return mealsForSelectedDate.firstOrNull { it.id == mealId }
    }
}