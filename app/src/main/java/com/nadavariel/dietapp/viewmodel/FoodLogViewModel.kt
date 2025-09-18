package com.nadavariel.dietapp.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
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
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.model.MealSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit

// Data class for Gemini's nutritional information. It needs to handle the new fields.
data class FoodNutritionalInfo(
    @SerializedName("food_name") val food_name: String?,
    @SerializedName("serving_unit") val serving_unit: String?,
    @SerializedName("serving_amount") val serving_amount: String?,
    @SerializedName("calories") val calories: String?,
    @SerializedName("protein") val protein: String?,
    @SerializedName("carbohydrates") val carbohydrates: String?,
    @SerializedName("fat") val fat: String?
)

sealed class GeminiResult {
    object Idle : GeminiResult()
    object Loading : GeminiResult()
    // Changed to a list to match the function's return type
    data class Success(val foodInfoList: List<FoodNutritionalInfo>) : GeminiResult()
    data class Error(val message: String) : GeminiResult()
}

@RequiresApi(Build.VERSION_CODES.O)
class FoodLogViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    // Initialize the Firebase functions instance here, it's better practice
    private val functions = Firebase.functions("me-west1")

    // Replaced mutableStateOf with StateFlow for proper observation by Compose UI
    private val _selectedDateState = MutableStateFlow(LocalDate.now())
    val selectedDateState = _selectedDateState.asStateFlow()

    private val _currentWeekStartDateState = MutableStateFlow(LocalDate.now().minusDays(6))
    val currentWeekStartDateState = _currentWeekStartDateState.asStateFlow()

    private val _mealsForSelectedDateState = MutableStateFlow<List<Meal>>(emptyList())
    val mealsForSelectedDate = _mealsForSelectedDateState.asStateFlow()

    private var mealsListenerRegistration: ListenerRegistration? = null

    private val _weeklyCalories = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyCalories = _weeklyCalories.asStateFlow()

    private val _caloriesByTimeOfDay = MutableStateFlow(
        mapOf("Morning" to 0f, "Afternoon" to 0f, "Evening" to 0f, "Night" to 0f)
    )
    val caloriesByTimeOfDay = _caloriesByTimeOfDay.asStateFlow()

    private val _geminiResult = MutableStateFlow<GeminiResult>(GeminiResult.Idle)
    val geminiResult: MutableStateFlow<GeminiResult> = _geminiResult

    init {
        val today = LocalDate.now()
        _currentWeekStartDateState.value = calculateWeekStartDate(today)
        _selectedDateState.value = today

        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    listenForMealsForDate(selectedDateState.value)
                    fetchMealsForLastSevenDays()
                } else {
                    mealsListenerRegistration?.remove()
                    mealsListenerRegistration = null
                    _mealsForSelectedDateState.value = emptyList()
                    _weeklyCalories.value = emptyMap()
                }
            }
        }
    }

    private fun calculateWeekStartDate(date: LocalDate): LocalDate {
        var daysToSubtract = date.dayOfWeek.value
        if (daysToSubtract == 7) {
            daysToSubtract = 0
        }
        return date.minusDays(daysToSubtract.toLong())
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

    fun logMeal(
        foodName: String,
        calories: Int,
        servingAmount: String?,
        servingUnit: String?,
        mealTime: Timestamp,
        // New parameters for nutritional values
        protein: Double?,
        carbohydrates: Double?,
        fat: Double?
    ) {
        val userId = auth.currentUser?.uid ?: return
        val meal = Meal(
            foodName = foodName,
            calories = calories,
            servingAmount = servingAmount,
            servingUnit = servingUnit,
            timestamp = mealTime,
            protein = protein,
            carbohydrates = carbohydrates,
            fat = fat
        )
        viewModelScope.launch {
            try {
                // Remove the extra Firestore update call. It's not necessary.
                firestore.collection("users").document(userId).collection("meals").add(meal).await()
                fetchMealsForLastSevenDays()
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error logging meal: ${e.message}", e)
            }
        }
    }

    fun updateMeal(
        mealId: String,
        newFoodName: String,
        newCalories: Int,
        newServingAmount: String?,
        newServingUnit: String?,
        newTimestamp: Timestamp,
        // New parameters for nutritional values
        newProtein: Double?,
        newCarbohydrates: Double?,
        newFat: Double?
    ) {
        val userId = auth.currentUser?.uid ?: return
        val now = Date()
        if (newTimestamp.toDate().after(now)) {
            return
        }
        val mealRef = firestore.collection("users").document(userId).collection("meals").document(mealId)
        val updatedData = hashMapOf(
            "foodName" to newFoodName,
            "calories" to newCalories,
            "servingAmount" to newServingAmount,
            "servingUnit" to newServingUnit,
            "timestamp" to newTimestamp,
            // Add new nutritional fields to the update map
            "protein" to newProtein,
            "carbohydrates" to newCarbohydrates,
            "fat" to newFat
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
                    _mealsForSelectedDateState.value = emptyList()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val mealList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Meal::class.java)?.copy(id = doc.id)
                    }
                    _mealsForSelectedDateState.value = mealList
                } else {
                    _mealsForSelectedDateState.value = emptyList()
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        mealsListenerRegistration?.remove()
    }

    fun selectDate(date: LocalDate) {
        val newWeekStartDate = calculateWeekStartDate(date)
        _selectedDateState.value = date
        _currentWeekStartDateState.value = newWeekStartDate
        listenForMealsForDate(date)
    }

    fun previousWeek() {
        val newCurrentWeekStartDate = _currentWeekStartDateState.value.minusWeeks(1)
        _currentWeekStartDateState.value = newCurrentWeekStartDate
        _selectedDateState.value = newCurrentWeekStartDate
        listenForMealsForDate(_selectedDateState.value)
    }

    fun nextWeek() {
        val newCurrentWeekStartDate = _currentWeekStartDateState.value.plusWeeks(1)
        _currentWeekStartDateState.value = newCurrentWeekStartDate
        _selectedDateState.value = newCurrentWeekStartDate
        listenForMealsForDate(_selectedDateState.value)
    }

    fun goToToday() {
        selectDate(LocalDate.now())
    }

    fun getMealById(mealId: String): Meal? {
        return mealsForSelectedDate.value.firstOrNull { it.id == mealId }
    }

    fun refreshStatistics() {
        fetchMealsForLastSevenDays()
    }

    fun analyzeImageWithGemini(foodName: String) {
        _geminiResult.value = GeminiResult.Loading
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
                    val geminiData = responseData["data"]

                    if (success == true && geminiData != null) {
                        val gson = Gson()
                        val jsonString = gson.toJson(geminiData)
                        // This line is the key change: parse to a List of objects
                        val listType = object : TypeToken<List<FoodNutritionalInfo>>() {}.type
                        val parsedList: List<FoodNutritionalInfo> = gson.fromJson(jsonString, listType)

                        if (parsedList.isNotEmpty()) {
                            _geminiResult.value = GeminiResult.Success(parsedList)
                        } else {
                            _geminiResult.value = GeminiResult.Error("No food information found.")
                        }
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