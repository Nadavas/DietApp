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
import com.google.gson.reflect.TypeToken
import com.nadavariel.dietapp.data.FoodNutritionalInfo
import com.nadavariel.dietapp.data.GraphPreference
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.model.MealSection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import kotlin.reflect.KProperty1

sealed class GeminiResult {
    data object Idle : GeminiResult()
    data object Loading : GeminiResult()
    data class Success(val foodInfoList: List<FoodNutritionalInfo>) : GeminiResult()
    data class Error(val message: String) : GeminiResult()
}

@RequiresApi(Build.VERSION_CODES.O)
class FoodLogViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val functions = Firebase.functions("me-west1")
    private val preferencesCollection = firestore.collection("users").document(auth.currentUser?.uid ?: "no_user").collection("preferences")

    // --- State Flows ---

    private val _selectedDateState = MutableStateFlow(LocalDate.now())
    val selectedDateState = _selectedDateState.asStateFlow()

    private val _currentWeekStartDateState = MutableStateFlow(LocalDate.now().minusDays(6))
    val currentWeekStartDateState = _currentWeekStartDateState.asStateFlow()

    private val _mealsForSelectedDateState = MutableStateFlow<List<Meal>>(emptyList())
    val mealsForSelectedDate = _mealsForSelectedDateState.asStateFlow()

    private var mealsListenerRegistration: ListenerRegistration? = null

    private val _graphPreferences = MutableStateFlow<List<GraphPreference>>(emptyList())
    val graphPreferences = _graphPreferences.asStateFlow()

    // Weekly Nutrient States (Int for grams/milligrams, or calories)
    private val _weeklyCalories = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyCalories = _weeklyCalories.asStateFlow()

    private val _weeklyProtein = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyProtein = _weeklyProtein.asStateFlow()

    private val _weeklyFiber = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyFiber = _weeklyFiber.asStateFlow()

    private val _weeklySugar = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklySugar = _weeklySugar.asStateFlow()

    private val _weeklySodium = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklySodium = _weeklySodium.asStateFlow()

    private val _weeklyPotassium = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyPotassium = _weeklyPotassium.asStateFlow()

    private val _weeklyCalcium = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyCalcium = _weeklyCalcium.asStateFlow()

    private val _weeklyIron = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyIron = _weeklyIron.asStateFlow()

    private val _weeklyVitaminC = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyVitaminC = _weeklyVitaminC.asStateFlow()

    private val _yesterdayMacroPercentages = MutableStateFlow(
        mapOf("Protein" to 0f, "Carbs" to 0f, "Fat" to 0f)
    )
    val yesterdayMacroPercentages = _yesterdayMacroPercentages.asStateFlow()

    private val _caloriesByTimeOfDay = MutableStateFlow(
        mapOf("Morning" to 0f, "Afternoon" to 0f, "Evening" to 0f, "Night" to 0f)
    )


    private val _geminiResult = MutableStateFlow<GeminiResult>(GeminiResult.Idle)
    val geminiResult: MutableStateFlow<GeminiResult> = _geminiResult

    private val _shouldResetDateOnResume = MutableStateFlow(true)


    init {
        val today = LocalDate.now()
        _currentWeekStartDateState.value = calculateWeekStartDate(today)
        _selectedDateState.value = today

        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                if (firebaseAuth.currentUser != null) {
                    // User signed in
                    listenForMealsForDate(selectedDateState.value)
                    fetchMealsForLastSevenDays()
                    fetchGraphPreferences()
                } else {
                    // User signed out
                    mealsListenerRegistration?.remove()
                    mealsListenerRegistration = null
                    resetAllStates()
                }
            }
        }
    }

    private fun resetAllStates() {
        _mealsForSelectedDateState.value = emptyList()
        _weeklyCalories.value = emptyMap()
        _weeklyProtein.value = emptyMap()
        _yesterdayMacroPercentages.value = mapOf("Protein" to 0f, "Carbs" to 0f, "Fat" to 0f)
        _weeklyFiber.value = emptyMap()
        _weeklySugar.value = emptyMap()
        _weeklySodium.value = emptyMap()
        _weeklyPotassium.value = emptyMap()
        _weeklyCalcium.value = emptyMap()
        _weeklyIron.value = emptyMap()
        _weeklyVitaminC.value = emptyMap()
        _graphPreferences.value = emptyList()
    }

    // --- Graph Preference Methods ---

    private fun getDefaultGraphPreferences(): List<GraphPreference> = listOf(
        GraphPreference("calories", "Weekly Calorie Intake", 0, true, true),
        GraphPreference("protein", "Weekly Protein Intake", 1, true, true),
        GraphPreference("macros_pie", "Yesterday's Macronutrient Distribution", 2, true, true),
        GraphPreference("fiber", "Weekly Fiber Intake", 3, true, false),
        GraphPreference("sugar", "Weekly Sugar Intake", 4, true, false),
        GraphPreference("sodium", "Weekly Sodium Intake", 5, true, false),
        GraphPreference("potassium", "Weekly Potassium Intake", 6, true, false),
        GraphPreference("calcium", "Weekly Calcium Intake", 7, true, false),
        GraphPreference("iron", "Weekly Iron Intake", 8, true, false),
        GraphPreference("vitamin_c", "Weekly Vitamin C Intake", 9, true, false)
    )

    fun fetchGraphPreferences() {
        auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = preferencesCollection.document("graph_order").get().await()

                val defaultMap = getDefaultGraphPreferences().associateBy { it.id }

                val preferences = if (snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val storedList = snapshot.get("list") as? List<Map<String, Any>>

                    val storedPreferences = storedList?.mapNotNull { map ->
                        // Safely map Firestore data to data class
                        GraphPreference(
                            id = map["id"] as? String ?: return@mapNotNull null,
                            title = map["title"] as? String ?: return@mapNotNull null,
                            order = (map["order"] as? Long)?.toInt() ?: 0,
                            isVisible = map["isVisible"] as? Boolean ?: true,
                            isMacro = map["isMacro"] as? Boolean ?: false
                        )
                    } ?: getDefaultGraphPreferences()

                    // Merge: prioritize stored preferences, but include all defaults (for new graphs)
                    val storedMap = storedPreferences.associateBy { it.id }

                    defaultMap.keys.mapNotNull { id ->
                        if (storedMap.containsKey(id)) {
                            // Use stored preference but ensure it has the correct, up-to-date title
                            storedMap[id]?.copy(title = defaultMap[id]?.title ?: storedMap[id]!!.title)
                        } else {
                            // New graph not in storage, add it with a high order value
                            defaultMap[id]?.copy(order = defaultMap.size + storedMap.size)
                        }
                    }.sortedBy { it.order }

                } else {
                    // No preferences found, use default order
                    getDefaultGraphPreferences()
                }

                _graphPreferences.value = preferences
                if (!snapshot.exists()) {
                    saveGraphPreferences(preferences) // Save default for the first time
                }
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error fetching graph preferences: ${e.message}")
                _graphPreferences.value = getDefaultGraphPreferences()
            }
        }
    }

    fun saveGraphPreferences(preferences: List<GraphPreference>) {
        auth.currentUser?.uid ?: return
        _graphPreferences.value = preferences // Optimistic update
        viewModelScope.launch {
            try {
                // Prepare data for Firestore using the helper function
                val dataToSave = hashMapOf("list" to preferences.map { it.toMap() })
                preferencesCollection.document("graph_order").set(dataToSave).await()
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error saving graph preferences: ${e.message}")
            }
        }
    }

    // --- Data Fetching & Processing ---

    private fun calculateWeekStartDate(date: LocalDate): LocalDate {
        var daysToSubtract = date.dayOfWeek.value
        if (daysToSubtract == 7) { // Assuming Sunday is the start of the week (value 7)
            daysToSubtract = 0
        }
        return date.minusDays(daysToSubtract.toLong())
    }

    fun refreshStatistics() {
        fetchMealsForLastSevenDays()
        fetchGraphPreferences()
    }

    private fun fetchMealsForLastSevenDays() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // 1. Fetch Meals for Last 7 Days
                val sevenDaysAgo = LocalDate.now().minusDays(6)
                val startOfPeriod = Date.from(sevenDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant())
                val querySnapshot = firestore.collection("users").document(userId).collection("meals")
                    .whereGreaterThanOrEqualTo("timestamp", startOfPeriod)
                    .get()
                    .await()
                val meals = querySnapshot.toObjects(Meal::class.java)

                // Process all weekly stats using the single, generic function
                processWeeklyNutrient(meals, Meal::calories, _weeklyCalories)
                processWeeklyNutrient(meals, Meal::protein, _weeklyProtein)
                processWeeklyNutrient(meals, Meal::fiber, _weeklyFiber)
                processWeeklyNutrient(meals, Meal::sugar, _weeklySugar)
                processWeeklyNutrient(meals, Meal::sodium, _weeklySodium)
                processWeeklyNutrient(meals, Meal::potassium, _weeklyPotassium)
                processWeeklyNutrient(meals, Meal::calcium, _weeklyCalcium)
                processWeeklyNutrient(meals, Meal::iron, _weeklyIron)
                processWeeklyNutrient(meals, Meal::vitaminC, _weeklyVitaminC)

                processCaloriesByTimeOfDay(meals)

                // 2. Fetch Meals for Yesterday (for Macro Pie Chart)
                val yesterday = LocalDate.now().minusDays(1)
                val yesterdayStart = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant())
                val todayStart = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant())

                val yesterdaySnapshot = firestore.collection("users").document(userId).collection("meals")
                    .whereGreaterThanOrEqualTo("timestamp", yesterdayStart)
                    .whereLessThan("timestamp", todayStart)
                    .get()
                    .await()
                val yesterdayMeals = yesterdaySnapshot.toObjects(Meal::class.java)
                processYesterdayMacroPercentages(yesterdayMeals)
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error fetching weekly meals for stats: ${e.message}", e)
            }
        }
    }

    /**
     * Replaces all individual weekly nutrient processing functions.
     * Uses Kotlin Reflection (KProperty1) to access the correct nutrient field dynamically.
     */
    private fun processWeeklyNutrient(
        meals: List<Meal>,
        nutrientProperty: KProperty1<Meal, Number?>,
        stateFlow: MutableStateFlow<Map<LocalDate, Int>>
    ) {
        val today = LocalDate.now()
        // Initialize the map with the last 7 days and zero value
        val nutrientByDay = (0..6).associate {
            today.minusDays(it.toLong()) to 0
        }.toMutableMap()

        for (meal in meals) {
            val mealDate = meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

            // Get the nutrient value using reflection, safely converting to Int
            // Calories is Int, others are Double?
            val nutrientValue = nutrientProperty.get(meal)?.toInt() ?: 0

            if (nutrientByDay.containsKey(mealDate)) {
                nutrientByDay[mealDate] = (nutrientByDay[mealDate] ?: 0) + nutrientValue
            }
        }
        stateFlow.value = nutrientByDay
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

    private fun processYesterdayMacroPercentages(meals: List<Meal>) {
        val proteinGrams = meals.sumOf { it.protein ?: 0.0 }
        val carbsGrams = meals.sumOf { it.carbohydrates ?: 0.0 }
        val fatGrams = meals.sumOf { it.fat ?: 0.0 }

        val PROTEIN_CALORIES_PER_GRAM = 4.0
        val CARB_CALORIES_PER_GRAM = 4.0
        val FAT_CALORIES_PER_GRAM = 9.0

        val proteinCalories = proteinGrams * PROTEIN_CALORIES_PER_GRAM
        val carbCalories = carbsGrams * CARB_CALORIES_PER_GRAM
        val fatCalories = fatGrams * FAT_CALORIES_PER_GRAM

        val totalMacroCalories = proteinCalories + carbCalories + fatCalories

        _yesterdayMacroPercentages.value = if (totalMacroCalories > 0) {
            mapOf(
                "Protein" to (proteinCalories / totalMacroCalories * 100).toFloat(),
                "Carbs" to (carbCalories / totalMacroCalories * 100).toFloat(),
                "Fat" to (fatCalories / totalMacroCalories * 100).toFloat()
            )
        } else {
            mapOf("Protein" to 0f, "Carbs" to 0f, "Fat" to 0f)
        }
    }

    // --- Meal CRUD Operations ---

    fun logMeal(
        foodName: String,
        calories: Int,
        servingAmount: String?,
        servingUnit: String?,
        mealTime: Timestamp,
        protein: Double?,
        carbohydrates: Double?,
        fat: Double?,
        fiber: Double?,
        sugar: Double?,
        sodium: Double?,
        potassium: Double?,
        calcium: Double?,
        iron: Double?,
        vitaminC: Double?
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
            fat = fat,
            fiber = fiber,
            sugar = sugar,
            sodium = sodium,
            potassium = potassium,
            calcium = calcium,
            iron = iron,
            vitaminC = vitaminC
        )
        viewModelScope.launch {
            try {
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
        newProtein: Double?,
        newCarbohydrates: Double?,
        newFat: Double?,
        newFiber: Double?,
        newSugar: Double?,
        newSodium: Double?,
        newPotassium: Double?,
        newCalcium: Double?,
        newIron: Double?,
        newVitaminC: Double?
    ) {
        val userId = auth.currentUser?.uid ?: return
        if (newTimestamp.toDate().after(Date())) return // Prevent logging future meals

        val mealRef = firestore.collection("users").document(userId).collection("meals").document(mealId)
        val updatedData = mapOf(
            "foodName" to newFoodName,
            "calories" to newCalories,
            "servingAmount" to newServingAmount,
            "servingUnit" to newServingUnit,
            "timestamp" to newTimestamp,
            "protein" to newProtein,
            "carbohydrates" to newCarbohydrates,
            "fat" to newFat,
            "fiber" to newFiber,
            "sugar" to newSugar,
            "sodium" to newSodium,
            "potassium" to newPotassium,
            "calcium" to newCalcium,
            "iron" to newIron,
            "vitaminC" to newVitaminC
        )
        viewModelScope.launch {
            try {
                mealRef.update(updatedData).await()
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

    // --- Date/UI Methods ---

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
                    Log.e("FoodLogViewModel", "Error listening for meals: ${e.message}")
                    return@addSnapshotListener
                }
                val mealList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Meal::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                _mealsForSelectedDateState.value = mealList
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

    fun resetDateToTodayIfNeeded() {
        if (_shouldResetDateOnResume.value) {
            selectDate(LocalDate.now())
            _shouldResetDateOnResume.value = false
        }
    }

    fun getMealById(mealId: String): Meal? {
        return mealsForSelectedDate.value.firstOrNull { it.id == mealId }
    }

    // --- Gemini API Methods ---

    /**
     * Calls the Firebase function to analyze food, using text name or image data.
     * @param foodName The food description (used for text input, or a fallback).
     * @param imageB64 Optional Base64 string of the image for multimodal analysis.
     */
    fun analyzeImageWithGemini(foodName: String, imageB64: String? = null) { // <-- PARAMETER ADDED
        _geminiResult.value = GeminiResult.Loading
        viewModelScope.launch {
            try {
                // Construct data payload, only including imageB64 if it's not null
                val data = hashMapOf<String, Any>(
                    "foodName" to foodName
                )
                if (imageB64 != null) {
                    data["imageB64"] = imageB64
                }

                // Validate input before API call
                if (foodName.isBlank() && imageB64 == null) {
                    _geminiResult.value = GeminiResult.Error("Please enter a food name or select an image.")
                    return@launch
                }

                val result = functions
                    .getHttpsCallable("analyzeFoodWithGemini")
                    .call(data)
                    .await()

                val responseData = result.data as? Map<String, Any> ?: throw Exception("Function response data is null.")
                val success = responseData["success"] as? Boolean
                val geminiData = responseData["data"]

                if (success == true && geminiData != null) {
                    val gson = Gson()
                    val jsonString = gson.toJson(geminiData)
                    val listType = object : TypeToken<List<FoodNutritionalInfo>>() {}.type
                    val parsedList: List<FoodNutritionalInfo> = gson.fromJson(jsonString, listType)

                    _geminiResult.value = if (parsedList.isNotEmpty()) {
                        GeminiResult.Success(parsedList)
                    } else {
                        GeminiResult.Error("No food information found.")
                    }
                } else {
                    val errorMsg = responseData["error"] as? String
                    _geminiResult.value = GeminiResult.Error(errorMsg ?: "Unknown API error")
                }
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Gemini analysis failed: ${e.message}", e)
                _geminiResult.value = GeminiResult.Error("Function call failed: ${e.message}")
            }
        }
    }

    fun resetGeminiResult() {
        _geminiResult.value = GeminiResult.Idle
    }
}

/**
 * Helper function to convert GraphPreference to Map for Firestore.
 */
private fun GraphPreference.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "title" to title,
        "order" to order,
        "isVisible" to isVisible,
        "isMacro" to isMacro
    )
}