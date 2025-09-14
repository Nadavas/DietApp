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
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.model.MealSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

// Data class for Gemini's nutritional information. No date or time fields as they are handled manually.
data class FoodNutritionalInfo(
    val food_name: String?,
    val serving_unit: String?,
    val serving_amount: String?,
    val calories: String?,
    val protein: String?,
    val carbohydrates: String?,
    val fat: String?
)

sealed class GeminiResult {
    object Idle : GeminiResult()
    object Loading : GeminiResult()
    data class Success(val foodInfo: FoodNutritionalInfo) : GeminiResult()
    data class Error(val message: String) : GeminiResult()
}

@RequiresApi(Build.VERSION_CODES.O)
class FoodLogViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    var selectedDate: LocalDate by mutableStateOf(LocalDate.now())
        private set

    var currentWeekStartDate: LocalDate by mutableStateOf(LocalDate.now().minusDays(6))
        private set

    var mealsForSelectedDate: List<Meal> by mutableStateOf(emptyList())
        private set
    private var mealsListenerRegistration: ListenerRegistration? = null

    private val _weeklyCalories = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyCalories = _weeklyCalories.asStateFlow()

    private val _caloriesByTimeOfDay = MutableStateFlow(
        mapOf("Morning" to 0f, "Afternoon" to 0f, "Evening" to 0f, "Night" to 0f)
    )
    val caloriesByTimeOfDay = _caloriesByTimeOfDay.asStateFlow()

    private val _geminiResult = MutableStateFlow<GeminiResult>(GeminiResult.Idle)
    val geminiResult: MutableStateFlow<GeminiResult> = _geminiResult

    private fun calculateWeekStartEndingOnDate(date: LocalDate): LocalDate {
        return date.minusDays(6)
    }

    init {
        val today = LocalDate.now()
        currentWeekStartDate = calculateWeekStartEndingOnDate(today)
        selectedDate = today

        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    listenForMealsForDate(selectedDate)
                    fetchMealsForLastSevenDays()
                } else {
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
                val querySnapshot = firestore.collection("users").document(userId).collection("meals")
                    .whereGreaterThanOrEqualTo("timestamp", startOfPeriod)
                    .get()
                    .await()
                val meals = querySnapshot.toObjects(Meal::class.java)
                processWeeklyCalories(meals)
                processCaloriesByTimeOfDay(meals)
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
        _weeklyCalories.value = caloriesByDay
    }

    private fun processCaloriesByTimeOfDay(meals: List<Meal>) {
        val rawTimeBuckets = mutableMapOf(
            MealSection.MORNING.sectionName to 0f,
            MealSection.NOON.sectionName to 0f,
            MealSection.EVENING.sectionName to 0f,
            MealSection.NIGHT.sectionName to 0f
        )
        for (meal in meals) {
            val section = MealSection.getMealSection(meal.timestamp.toDate())
            rawTimeBuckets[section.sectionName] = (rawTimeBuckets[section.sectionName] ?: 0f) + meal.calories
        }
        val finalTimeBuckets = mutableMapOf<String, Float>()
        finalTimeBuckets["Morning"] = rawTimeBuckets[MealSection.MORNING.sectionName] ?: 0f
        finalTimeBuckets["Afternoon"] = rawTimeBuckets[MealSection.NOON.sectionName] ?: 0f
        finalTimeBuckets["Evening"] = rawTimeBuckets[MealSection.EVENING.sectionName] ?: 0f
        finalTimeBuckets["Night"] = rawTimeBuckets[MealSection.NIGHT.sectionName] ?: 0f
        _caloriesByTimeOfDay.value = finalTimeBuckets
    }

    fun logMeal(foodName: String, calories: Int, mealTime: Timestamp) {
        val userId = auth.currentUser?.uid ?: return
        val meal = Meal(foodName = foodName, calories = calories, timestamp = mealTime)
        viewModelScope.launch {
            try {
                val docRef = firestore.collection("users").document(userId).collection("meals").add(meal).await()
                firestore.collection("users").document(userId).collection("meals").document(docRef.id).update("id", docRef.id).await()
                fetchMealsForLastSevenDays()
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error logging meal: ${e.message}", e)
            }
        }
    }

    fun updateMeal(mealId: String, newFoodName: String, newCalories: Int, newTimestamp: Timestamp) {
        val userId = auth.currentUser?.uid ?: return
        val now = Date()
        if (newTimestamp.toDate().after(now)) {
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
                fetchMealsForLastSevenDays()
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error updating meal '$mealId': ${e.message}", e)
            }
        }
    }

    fun deleteMeal(mealId: String) {
        val userId = auth.currentUser?.uid ?: return
        val mealRef = firestore.collection("users").document(userId).collection("meals").document(mealId)
        viewModelScope.launch {
            try {
                mealRef.delete().await()
                fetchMealsForLastSevenDays()
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error deleting meal '$mealId': ${e.message}", e)
            }
        }
    }

    private fun listenForMealsForDate(date: LocalDate) {
        mealsListenerRegistration?.remove()
        val userId = auth.currentUser?.uid ?: return
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        mealsListenerRegistration = firestore.collection("users").document(userId).collection("meals")
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(Date(startOfDay)))
            .whereLessThan("timestamp", Timestamp(Date(endOfDay)))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    mealsForSelectedDate = emptyList()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val mealList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Meal::class.java)?.copy(id = doc.id)
                    }
                    mealsForSelectedDate = mealList
                } else {
                    mealsForSelectedDate = emptyList()
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        mealsListenerRegistration?.remove()
    }

    fun selectDate(date: LocalDate) {
        if (selectedDate != date) {
            selectedDate = date
            listenForMealsForDate(date)
        } else {
            listenForMealsForDate(date)
        }
    }

    fun previousWeek() {
        val newCurrentWeekStartDate = currentWeekStartDate.minusWeeks(1)
        currentWeekStartDate = newCurrentWeekStartDate
        selectedDate = newCurrentWeekStartDate.plusDays(6)
        listenForMealsForDate(selectedDate)
    }

    fun nextWeek() {
        val newCurrentWeekStartDate = currentWeekStartDate.plusWeeks(1)
        currentWeekStartDate = newCurrentWeekStartDate
        selectedDate = newCurrentWeekStartDate.plusDays(6)
        listenForMealsForDate(selectedDate)
    }

    fun getMealById(mealId: String): Meal? {
        return mealsForSelectedDate.firstOrNull { it.id == mealId }
    }

    fun refreshStatistics() {
        fetchMealsForLastSevenDays()
    }

    fun analyzeImageWithGemini(foodName: String) {
        _geminiResult.value = GeminiResult.Loading
        val functions = Firebase.functions("me-west1")
        viewModelScope.launch {
            try {
                val data = hashMapOf("foodName" to foodName)
                val result = functions
                    .getHttpsCallable("analyzeFoodWithGemini")
                    .call(data)
                    .await()
                val responseData = result.data as? Map<String, Any>

                if (responseData != null) {
                    val success = responseData["success"] as? Boolean
                    val geminiData = responseData["data"] as? Map<String, Any>

                    if (success == true && geminiData != null) {
                        val gson = Gson()
                        val jsonString = gson.toJson(geminiData)
                        val parsedInfo = gson.fromJson(jsonString, FoodNutritionalInfo::class.java)

                        Log.d("ViewModel", "Food Name: ${parsedInfo.food_name}")
                        Log.d("ViewModel", "Serving Amount: ${parsedInfo.serving_amount}")
                        Log.d("ViewModel", "Serving Unit: ${parsedInfo.serving_unit}")
                        Log.d("ViewModel", "Calories: ${parsedInfo.calories}")
                        Log.d("ViewModel", "Protein: ${parsedInfo.protein}")
                        Log.d("ViewModel", "Carbohydrates: ${parsedInfo.carbohydrates}")
                        Log.d("ViewModel", "Fat: ${parsedInfo.fat}")

                        _geminiResult.value = GeminiResult.Success(parsedInfo)
                    } else {
                        val errorMsg = responseData["error"] as? String
                        _geminiResult.value = GeminiResult.Error(errorMsg ?: "Unknown API error")
                    }
                } else {
                    _geminiResult.value = GeminiResult.Error("Function response data is null.")
                }
            } catch (e: Exception) {
                _geminiResult.value = GeminiResult.Error("Function call failed: ${e.message}")
            }
        }
    }

    fun resetGeminiResult() {
        _geminiResult.value = GeminiResult.Idle
    }
}