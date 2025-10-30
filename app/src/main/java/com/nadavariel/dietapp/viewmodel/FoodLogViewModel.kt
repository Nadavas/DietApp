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
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nadavariel.dietapp.model.FoodNutritionalInfo
import com.nadavariel.dietapp.model.GraphPreference
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.model.MealSection
import com.nadavariel.dietapp.model.WeightEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
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

    private val _selectedDateState = MutableStateFlow(LocalDate.now())
    val selectedDateState = _selectedDateState.asStateFlow()

    private val _currentWeekStartDateState = MutableStateFlow(LocalDate.now().minusDays(6))
    val currentWeekStartDateState = _currentWeekStartDateState.asStateFlow()

    private val _mealsForSelectedDateState = MutableStateFlow<List<Meal>>(emptyList())
    val mealsForSelectedDate = _mealsForSelectedDateState.asStateFlow()

    private var mealsListenerRegistration: ListenerRegistration? = null

    private var weightHistoryListener: ListenerRegistration? = null
    private var targetWeightListener: ListenerRegistration? = null

    private val _graphPreferences = MutableStateFlow<List<GraphPreference>>(emptyList())
    val graphPreferences = _graphPreferences.asStateFlow()

    private val _weightHistory = MutableStateFlow<List<WeightEntry>>(emptyList())
    val weightHistory = _weightHistory.asStateFlow()

    private val _targetWeight = MutableStateFlow(0f)
    val targetWeight = _targetWeight.asStateFlow()


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

    private val _weeklyMacroPercentages = MutableStateFlow(
        mapOf("Protein" to 0f, "Carbs" to 0f, "Fat" to 0f)
    )
    val weeklyMacroPercentages = _weeklyMacroPercentages.asStateFlow()

    private val _caloriesByTimeOfDay = MutableStateFlow(
        mapOf("Morning" to 0f, "Afternoon" to 0f, "Evening" to 0f, "Night" to 0f)
    )


    private val _geminiResult = MutableStateFlow<GeminiResult>(GeminiResult.Idle)
    val geminiResult: MutableStateFlow<GeminiResult> = _geminiResult

    private val _shouldResetDateOnResume = MutableStateFlow(true)

    private val _isFutureTimeSelected = MutableStateFlow(false)
    val isFutureTimeSelected = _isFutureTimeSelected.asStateFlow()


    init {
        val today = LocalDate.now()
        _currentWeekStartDateState.value = calculateWeekStartDate(today)
        _selectedDateState.value = today

        viewModelScope.launch {
            auth.addAuthStateListener { firebaseAuth ->
                if (firebaseAuth.currentUser != null) {
                    listenForMealsForDate(selectedDateState.value)
                    fetchMealsForLastSevenDays()
                    fetchGraphPreferences()
                    listenForWeightHistory()
                    listenForTargetWeight()
                } else {
                    mealsListenerRegistration?.remove()
                    mealsListenerRegistration = null
                    weightHistoryListener?.remove()
                    weightHistoryListener = null
                    targetWeightListener?.remove()
                    targetWeightListener = null
                    resetAllStates()
                }
            }
        }
    }

    private fun resetAllStates() {
        _mealsForSelectedDateState.value = emptyList()
        _weeklyCalories.value = emptyMap()
        _weeklyProtein.value = emptyMap()
        _weeklyMacroPercentages.value = mapOf("Protein" to 0f, "Carbs" to 0f, "Fat" to 0f)
        _weeklyFiber.value = emptyMap()
        _weeklySugar.value = emptyMap()
        _weeklySodium.value = emptyMap()
        _weeklyPotassium.value = emptyMap()
        _weeklyCalcium.value = emptyMap()
        _weeklyIron.value = emptyMap()
        _weeklyVitaminC.value = emptyMap()
        _graphPreferences.value = emptyList()
        _caloriesByTimeOfDay.value = mapOf("Morning" to 0f, "Afternoon" to 0f, "Evening" to 0f, "Night" to 0f)
        _isFutureTimeSelected.value = false
        _weightHistory.value = emptyList()
        _targetWeight.value = 0f
    }

    fun updateDateTimeCheck(selectedTime: Date) {
        _isFutureTimeSelected.value = selectedTime.after(Date())
    }

    private fun getDefaultGraphPreferences(): List<GraphPreference> = listOf(
        GraphPreference("calories", "Weekly Calorie Intake", 0, true, true),
        GraphPreference("protein", "Weekly Protein Intake", 1, true, true),
        GraphPreference("weekly_macros_pie", "Weekly Macronutrient Distribution", 2, true, true),
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
                        GraphPreference(
                            id = map["id"] as? String ?: return@mapNotNull null,
                            title = map["title"] as? String ?: return@mapNotNull null,
                            order = (map["order"] as? Long)?.toInt() ?: 0,
                            isVisible = map["isVisible"] as? Boolean != false,
                            isMacro = map["isMacro"] as? Boolean == true
                        )
                    } ?: getDefaultGraphPreferences()

                    val storedMap = storedPreferences.associateBy { it.id }

                    defaultMap.keys.mapNotNull { id ->
                        if (storedMap.containsKey(id)) {
                            storedMap[id]?.copy(title = defaultMap[id]?.title ?: storedMap[id]!!.title)
                        } else {
                            defaultMap[id]?.copy(order = defaultMap.size + storedMap.size)
                        }
                    }.sortedBy { it.order }

                } else {
                    getDefaultGraphPreferences()
                }

                _graphPreferences.value = preferences
                if (!snapshot.exists()) {
                    saveGraphPreferences(preferences)
                }
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error fetching graph preferences: ${e.message}")
                _graphPreferences.value = getDefaultGraphPreferences()
            }
        }
    }

    fun saveGraphPreferences(preferences: List<GraphPreference>) {
        auth.currentUser?.uid ?: return
        _graphPreferences.value = preferences
        viewModelScope.launch {
            try {
                val dataToSave = hashMapOf("list" to preferences.map { it.toMap() })
                preferencesCollection.document("graph_order").set(dataToSave).await()
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error saving graph preferences: ${e.message}")
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

    fun refreshStatistics() {
        fetchMealsForLastSevenDays()
        fetchGraphPreferences()
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
                processWeeklyMacroPercentages(meals)

            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error fetching weekly meals for stats: ${e.message}", e)
            }
        }
    }

    private fun processWeeklyNutrient(
        meals: List<Meal>,
        nutrientProperty: KProperty1<Meal, Number?>,
        stateFlow: MutableStateFlow<Map<LocalDate, Int>>
    ) {
        val today = LocalDate.now()
        val nutrientByDay = (0..6).associate {
            today.minusDays(it.toLong()) to 0
        }.toMutableMap()

        for (meal in meals) {
            val mealDate = meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
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

    private fun processWeeklyMacroPercentages(meals: List<Meal>) {
        val proteinGrams = meals.sumOf { it.protein ?: 0.0 }
        val carbsGrams = meals.sumOf { it.carbohydrates ?: 0.0 }
        val fatGrams = meals.sumOf { it.fat ?: 0.0 }

        val proteinCaloriesPerGram = 4.0
        val carbCaloriesPerGram = 4.0
        val fatCaloriesPerGram = 9.0

        val proteinCalories = proteinGrams * proteinCaloriesPerGram
        val carbCalories = carbsGrams * carbCaloriesPerGram
        val fatCalories = fatGrams * fatCaloriesPerGram

        val totalMacroCalories = proteinCalories + carbCalories + fatCalories

        _weeklyMacroPercentages.value = if (totalMacroCalories > 0) {
            mapOf(
                "Protein" to (proteinCalories / totalMacroCalories * 100).toFloat(),
                "Carbs" to (carbCalories / totalMacroCalories * 100).toFloat(),
                "Fat" to (fatCalories / totalMacroCalories * 100).toFloat()
            )
        } else {
            mapOf("Protein" to 0f, "Carbs" to 0f, "Fat" to 0f)
        }
    }

    // --- Weight Logging ---

    private fun listenForWeightHistory() {
        weightHistoryListener?.remove()
        val userId = auth.currentUser?.uid ?: return

        weightHistoryListener = firestore.collection("users").document(userId)
            .collection("weight_history")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FoodLogViewModel", "Error listening for weight history", e)
                    _weightHistory.value = emptyList()
                    return@addSnapshotListener
                }
                // Updated to map document ID
                val historyList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject<WeightEntry>()?.copy(id = doc.id)
                } ?: emptyList()
                _weightHistory.value = historyList
            }
    }

    private fun listenForTargetWeight() {
        targetWeightListener?.remove()
        val userId = auth.currentUser?.uid ?: return

        targetWeightListener = firestore.collection("users").document(userId)
            .collection("user_answers").document("goals")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("FoodLogViewModel", "Error listening to target weight", e)
                    _targetWeight.value = 0f
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val answersMap = snapshot.get("answers") as? List<Map<String, String>>
                    val targetWeightAnswer = answersMap?.firstOrNull {
                        it["question"] == "What is your target weight (in kg)?"
                    }?.get("answer")

                    _targetWeight.value = targetWeightAnswer?.toFloatOrNull() ?: 0f
                } else {
                    _targetWeight.value = 0f
                }
            }
    }

    fun addWeightEntry(weight: Float, date: Calendar) {
        val userId = auth.currentUser?.uid ?: return
        if (weight <= 0) return

        viewModelScope.launch {
            try {
                val timestamp = Timestamp(date.time)
                // We set timestamp here manually so it's not null,
                // but @ServerTimestamp in WeightEntry will overwrite it on the server.
                val newEntry = WeightEntry(weight = weight, timestamp = timestamp)

                firestore.collection("users").document(userId)
                    .collection("weight_history")
                    .add(newEntry)
                    .await()
                Log.d("FoodLogViewModel", "Successfully added new weight entry: $weight at $timestamp")
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error adding weight entry", e)
            }
        }
    }

    // New function to update a weight entry
    fun updateWeightEntry(id: String, newWeight: Float, newDate: Calendar) {
        val userId = auth.currentUser?.uid ?: return
        if (id.isBlank()) {
            Log.e("FoodLogViewModel", "Cannot update weight entry: ID is blank.")
            return
        }
        if (newWeight <= 0) {
            Log.e("FoodLogViewModel", "Cannot update weight entry: Invalid weight.")
            return
        }

        val updatedData = mapOf(
            "weight" to newWeight,
            "timestamp" to Timestamp(newDate.time)
        )

        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId)
                    .collection("weight_history").document(id)
                    .update(updatedData).await()
                Log.d("FoodLogViewModel", "Successfully updated weight entry: $id")
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error updating weight entry", e)
            }
        }
    }

    // New function to delete a weight entry
    fun deleteWeightEntry(id: String) {
        val userId = auth.currentUser?.uid ?: return
        if (id.isBlank()) {
            Log.e("FoodLogViewModel", "Cannot delete weight entry: ID is blank.")
            return
        }
        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId)
                    .collection("weight_history").document(id)
                    .delete().await()
                Log.d("FoodLogViewModel", "Successfully deleted weight entry: $id")
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error deleting weight entry", e)
            }
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
                listenForMealsForDate(selectedDateState.value)
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error logging meal: ${e.message}", e)
            }
        }
    }

    fun logMealsFromFoodInfoList(
        foodInfoList: List<FoodNutritionalInfo>,
        mealTime: Timestamp
    ) {
        for (foodInfo in foodInfoList) {
            logMeal(
                foodName = foodInfo.food_name ?: "Unknown Meal",
                calories = foodInfo.calories?.toIntOrNull() ?: 0,
                servingAmount = foodInfo.serving_amount,
                servingUnit = foodInfo.serving_unit,
                mealTime = mealTime,
                protein = foodInfo.protein?.toDoubleOrNull(),
                carbohydrates = foodInfo.carbohydrates?.toDoubleOrNull(),
                fat = foodInfo.fat?.toDoubleOrNull(),
                fiber = foodInfo.fiber?.toDoubleOrNull(),
                sugar = foodInfo.sugar?.toDoubleOrNull(),
                sodium = foodInfo.sodium?.toDoubleOrNull(),
                potassium = foodInfo.potassium?.toDoubleOrNull(),
                calcium = foodInfo.calcium?.toDoubleOrNull(),
                iron = foodInfo.iron?.toDoubleOrNull(),
                vitaminC = foodInfo.vitaminC?.toDoubleOrNull()
            )
        }
        resetGeminiResult()
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
                listenForMealsForDate(selectedDateState.value)
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
                listenForMealsForDate(selectedDateState.value)
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
        weightHistoryListener?.remove()
        targetWeightListener?.remove()
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

    fun analyzeImageWithGemini(foodName: String, imageB64: String? = null) {
        _geminiResult.value = GeminiResult.Loading
        viewModelScope.launch {
            try {
                val data = hashMapOf<String, Any>(
                    "foodName" to foodName
                )
                if (imageB64 != null) {
                    data["imageB64"] = imageB64
                }

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

private fun GraphPreference.toMap(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "title" to title,
        "order" to order,
        "isVisible" to isVisible,
        "isMacro" to isMacro
    )
}