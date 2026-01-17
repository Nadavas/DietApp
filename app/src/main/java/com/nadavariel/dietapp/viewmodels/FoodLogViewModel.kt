package com.nadavariel.dietapp.viewmodels

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.nadavariel.dietapp.models.*
import com.nadavariel.dietapp.repositories.AuthRepository
import com.nadavariel.dietapp.repositories.MealRepository
import com.nadavariel.dietapp.repositories.WeightRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
class FoodLogViewModel(
    private val authRepository: AuthRepository,
    private val mealRepository: MealRepository,
    private val weightRepository: WeightRepository
) : ViewModel() {

    // --- Loading State ---
    private val _isLoadingLogs = MutableStateFlow(true)
    val isLoadingLogs = _isLoadingLogs.asStateFlow()

    // --- Date State ---
    private val _selectedDateState = MutableStateFlow(LocalDate.now())
    val selectedDateState = _selectedDateState.asStateFlow()

    private val _currentWeekStartDateState = MutableStateFlow(LocalDate.now().minusDays(6))
    val currentWeekStartDateState = _currentWeekStartDateState.asStateFlow()

    private val _isFutureTimeSelected = MutableStateFlow(false)
    val isFutureTimeSelected = _isFutureTimeSelected.asStateFlow()

    // --- Data State ---
    private val _mealsForSelectedDateState = MutableStateFlow<List<Meal>>(emptyList())
    val mealsForSelectedDate = _mealsForSelectedDateState.asStateFlow()

    private val _weightHistory = MutableStateFlow<List<WeightEntry>>(emptyList())
    val weightHistory = _weightHistory.asStateFlow()

    private val _targetWeight = MutableStateFlow(0f)
    val targetWeight = _targetWeight.asStateFlow()

    private val _isTargetWeightLoaded = MutableStateFlow(false)
    val isTargetWeightLoaded = _isTargetWeightLoaded.asStateFlow()

    // --- Statistics State ---
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
    private val _weeklyVitaminA = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyVitaminA = _weeklyVitaminA.asStateFlow()
    private val _weeklyVitaminB12 = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyVitaminB12 = _weeklyVitaminB12.asStateFlow()

    private val _weeklyMacroPercentages = MutableStateFlow(
        mapOf("Protein" to 0f, "Carbs" to 0f, "Fat" to 0f)
    )
    val weeklyMacroPercentages = _weeklyMacroPercentages.asStateFlow()

    private val _caloriesByTimeOfDay = MutableStateFlow(
        mapOf("Morning" to 0f, "Afternoon" to 0f, "Evening" to 0f, "Night" to 0f)
    )

    // --- Gemini State ---
    private val _geminiResult = MutableStateFlow<GeminiResult>(GeminiResult.Idle)
    val geminiResult: MutableStateFlow<GeminiResult> = _geminiResult

    private val _shouldResetDateOnResume = MutableStateFlow(true)
    private var tempMealTimestamp: Timestamp? = null

    // --- Jobs and Listeners ---
    private var mealsJob: Job? = null
    private var weightHistoryJob: Job? = null
    private var targetWeightJob: Job? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        val today = LocalDate.now()
        _currentWeekStartDateState.value = calculateWeekStartDate(today)
        _selectedDateState.value = today

        // Use AuthRepository to listen for login state
        authStateListener = authRepository.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _isLoadingLogs.value = true

                // Start listening to data
                listenForMealsForDate(selectedDateState.value)
                listenForWeightHistory(user.uid)
                listenForTargetWeight(user.uid)

                // One-time fetch for stats
                viewModelScope.launch {
                    try {
                        fetchMealsForLastSevenDays(user.uid)
                    } catch (e: Exception) {
                        Log.e("FoodLogViewModel", "Error in initial data fetch: $e")
                    } finally {
                        _isLoadingLogs.value = false
                    }
                }
            } else {
                stopListening()
                resetAllStates()
            }
        }
    }

    private fun stopListening() {
        mealsJob?.cancel()
        weightHistoryJob?.cancel()
        targetWeightJob?.cancel()
    }

    private fun resetAllStates() {
        _isLoadingLogs.value = true
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
        _weeklyVitaminA.value = emptyMap()
        _weeklyVitaminB12.value = emptyMap()
        _caloriesByTimeOfDay.value = mapOf("Morning" to 0f, "Afternoon" to 0f, "Evening" to 0f, "Night" to 0f)
        _isFutureTimeSelected.value = false
        _weightHistory.value = emptyList()
        _targetWeight.value = 0f
        _isTargetWeightLoaded.value = false
    }

    // --- Date & Time Helpers ---
    fun updateDateTimeCheck(selectedTime: Date) {
        _isFutureTimeSelected.value = selectedTime.after(Date())
    }

    private fun calculateWeekStartDate(date: LocalDate): LocalDate {
        var daysToSubtract = date.dayOfWeek.value
        if (daysToSubtract == 7) {
            daysToSubtract = 0
        }
        return date.minusDays(daysToSubtract.toLong())
    }

    // --- Main Logic: Meals ---

    private fun listenForMealsForDate(date: LocalDate) {
        mealsJob?.cancel()
        val userId = authRepository.currentUser?.uid ?: return

        mealsJob = viewModelScope.launch {
            mealRepository.getMealsForDateFlow(userId, date).collect { meals ->
                _mealsForSelectedDateState.value = meals
            }
        }
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

    // --- Main Logic: Stats ---

    fun refreshStatistics() {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            fetchMealsForLastSevenDays(userId)
        }
    }

    private suspend fun fetchMealsForLastSevenDays(userId: String) {
        try {
            val sevenDaysAgo = LocalDate.now().minusDays(6)
            val startOfPeriod = Date.from(sevenDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant())

            val meals = mealRepository.getMealsFromDateRange(userId, startOfPeriod)

            processWeeklyNutrient(meals, Meal::calories, _weeklyCalories)
            processWeeklyNutrient(meals, Meal::protein, _weeklyProtein)
            processWeeklyNutrient(meals, Meal::fiber, _weeklyFiber)
            processWeeklyNutrient(meals, Meal::sugar, _weeklySugar)
            processWeeklyNutrient(meals, Meal::sodium, _weeklySodium)
            processWeeklyNutrient(meals, Meal::potassium, _weeklyPotassium)
            processWeeklyNutrient(meals, Meal::calcium, _weeklyCalcium)
            processWeeklyNutrient(meals, Meal::iron, _weeklyIron)
            processWeeklyNutrient(meals, Meal::vitaminC, _weeklyVitaminC)
            processWeeklyNutrient(meals, Meal::vitaminA, _weeklyVitaminA)
            processWeeklyNutrient(meals, Meal::vitaminB12, _weeklyVitaminB12)
            processCaloriesByTimeOfDay(meals)
            processWeeklyMacroPercentages(meals)

        } catch (e: Exception) {
            Log.e("FoodLogViewModel", "Error fetching weekly meals for stats: ${e.message}", e)
        }
    }

    // --- Helpers for Stats (Unchanged Logic) ---

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

        val proteinCalories = proteinGrams * 4.0
        val carbCalories = carbsGrams * 4.0
        val fatCalories = fatGrams * 9.0
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

    // --- Main Logic: Weight ---

    private fun listenForWeightHistory(userId: String) {
        weightHistoryJob?.cancel()
        weightHistoryJob = viewModelScope.launch {
            weightRepository.getWeightHistoryFlow(userId).collect { list ->
                _weightHistory.value = list
            }
        }
    }

    private fun listenForTargetWeight(userId: String) {
        targetWeightJob?.cancel()
        targetWeightJob = viewModelScope.launch {
            weightRepository.getTargetWeightFlow(userId).collect { weight ->
                _targetWeight.value = weight
                _isTargetWeightLoaded.value = true
            }
        }
    }

    fun setTargetWeightOptimistically(weight: Float) {
        _targetWeight.value = weight
        _isTargetWeightLoaded.value = true
    }

    fun addWeightEntry(weight: Float, date: Calendar) {
        val userId = authRepository.currentUser?.uid ?: return
        if (weight <= 0) return

        viewModelScope.launch {
            try {
                val newEntry = WeightEntry(weight = weight, timestamp = Timestamp(date.time))
                weightRepository.addWeightEntry(userId, newEntry)
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error adding weight entry", e)
            }
        }
    }

    fun updateWeightEntry(id: String, newWeight: Float, newDate: Calendar) {
        val userId = authRepository.currentUser?.uid ?: return
        if (id.isBlank() || newWeight <= 0) return

        viewModelScope.launch {
            try {
                val data = mapOf(
                    "weight" to newWeight,
                    "timestamp" to Timestamp(newDate.time)
                )
                weightRepository.updateWeightEntry(userId, id, data)
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error updating weight entry", e)
            }
        }
    }

    fun deleteWeightEntry(id: String) {
        val userId = authRepository.currentUser?.uid ?: return
        if (id.isBlank()) return

        viewModelScope.launch {
            try {
                weightRepository.deleteWeightEntry(userId, id)
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error deleting weight entry", e)
            }
        }
    }

    // --- Main Logic: CRUD Meals ---

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
        vitaminC: Double?,
        vitaminA: Double?,
        vitaminB12: Double?
    ) {
        val userId = authRepository.currentUser?.uid ?: return
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
            vitaminC = vitaminC,
            vitaminA = vitaminA,
            vitaminB12 = vitaminB12
        )
        viewModelScope.launch {
            try {
                mealRepository.addMeal(userId, meal)
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error logging meal", e)
            }
        }
    }

    // Note: This function calls logMeal repeatedly, so it's already using Repo indirectly.
    fun logMealsFromFoodInfoList(foodInfoList: List<FoodNutritionalInfo>) {
        val mealTime = tempMealTimestamp ?: Timestamp.now()

        for (foodInfo in foodInfoList) {
            logMeal(
                foodName = foodInfo.foodName ?: "Unknown Meal",
                calories = foodInfo.calories?.toIntOrNull() ?: 0,
                servingAmount = foodInfo.servingAmount,
                servingUnit = foodInfo.servingUnit,
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
                vitaminC = foodInfo.vitaminC?.toDoubleOrNull(),
                vitaminA = foodInfo.vitaminA?.toDoubleOrNull(),
                vitaminB12 = foodInfo.vitaminB12?.toDoubleOrNull()
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
        newVitaminC: Double?,
        newVitaminA: Double?,
        newVitaminB12: Double?
    ) {
        val userId = authRepository.currentUser?.uid ?: return

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
            "vitaminC" to newVitaminC,
            "vitaminA" to newVitaminA,
            "vitaminB12" to newVitaminB12
        )
        viewModelScope.launch {
            try {
                mealRepository.updateMeal(userId, mealId, updatedData)
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error updating meal", e)
            }
        }
    }

    fun deleteMeal(mealId: String) {
        val userId = authRepository.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                mealRepository.deleteMeal(userId, mealId)
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error deleting meal", e)
            }
        }
    }

    fun getMealById(mealId: String): Meal? {
        return mealsForSelectedDate.value.firstOrNull { it.id == mealId }
    }

    // --- Gemini Logic ---

    fun analyzeMeal(foodName: String, imageB64: String? = null, mealTime: Timestamp) {
        _geminiResult.value = GeminiResult.Loading
        tempMealTimestamp = mealTime

        viewModelScope.launch {
            try {
                if (foodName.isBlank() && imageB64 == null) {
                    _geminiResult.value = GeminiResult.Error("Please enter a food name or select an image.")
                    return@launch
                }

                val resultList = mealRepository.getNutritionalInfo(foodName, imageB64)

                // Check if user is still logged in before updating UI
                if (authRepository.currentUser == null) return@launch

                if (resultList.isNotEmpty()) {
                    _geminiResult.value = GeminiResult.Success(resultList)
                } else {
                    _geminiResult.value = GeminiResult.Error("No food information found.")
                }

            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Gemini analysis failed: ${e.message}", e)
                if (authRepository.currentUser != null) {
                    _geminiResult.value = GeminiResult.Error("Function call failed: ${e.message}")
                }
            }
        }
    }

    fun resetGeminiResult() {
        _geminiResult.value = GeminiResult.Idle
        tempMealTimestamp = null
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { authRepository.removeAuthStateListener(it) }
        stopListening()
    }
}