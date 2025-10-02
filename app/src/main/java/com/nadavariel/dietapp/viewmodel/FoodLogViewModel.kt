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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.TimeUnit

// 🌟 NEW: Data class for Graph Preferences
data class GraphPreference(
    val id: String, // Unique ID (e.g., "calories", "protein", "fiber")
    val title: String, // Display name
    val order: Int, // User-defined display order (0, 1, 2, ...)
    val isVisible: Boolean, // Whether the user wants to see it
    val isMacro: Boolean // Helper for potential future UI grouping
)

// Data class for Gemini's nutritional information
data class FoodNutritionalInfo(
    @SerializedName("food_name") val food_name: String?,
    @SerializedName("serving_unit") val serving_unit: String?,
    @SerializedName("serving_amount") val serving_amount: String?,
    @SerializedName("calories") val calories: String?,
    @SerializedName("protein") val protein: String?,
    @SerializedName("carbohydrates") val carbohydrates: String?,
    @SerializedName("fat") val fat: String?,
    @SerializedName("fiber") val fiber: String?,
    @SerializedName("sugar") val sugar: String?,
    @SerializedName("sodium") val sodium: String?,
    @SerializedName("potassium") val potassium: String?,
    @SerializedName("calcium") val calcium: String?,
    @SerializedName("iron") val iron: String?,
    @SerializedName("vitamin_c") val vitaminC: String?
)

sealed class GeminiResult {
    object Idle : GeminiResult()
    object Loading : GeminiResult()
    data class Success(val foodInfoList: List<FoodNutritionalInfo>) : GeminiResult()
    data class Error(val message: String) : GeminiResult()
}

@RequiresApi(Build.VERSION_CODES.O)
class FoodLogViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore
    private val functions = Firebase.functions("me-west1")

    // 🌟 NEW: Firebase Collection Reference for preferences
    private val preferencesCollection = firestore.collection("users").document(auth.currentUser?.uid ?: "no_user").collection("preferences")

    private val _selectedDateState = MutableStateFlow(LocalDate.now())
    val selectedDateState = _selectedDateState.asStateFlow()

    private val _currentWeekStartDateState = MutableStateFlow(LocalDate.now().minusDays(6))
    val currentWeekStartDateState = _currentWeekStartDateState.asStateFlow()

    private val _mealsForSelectedDateState = MutableStateFlow<List<Meal>>(emptyList())
    val mealsForSelectedDate = _mealsForSelectedDateState.asStateFlow()

    private var mealsListenerRegistration: ListenerRegistration? = null

    // 🌟 NEW: Graph Preference State Flow
    private val _graphPreferences = MutableStateFlow<List<GraphPreference>>(emptyList())
    val graphPreferences = _graphPreferences.asStateFlow()

    // --- EXISTING WEEKLY STATES ---
    private val _weeklyCalories = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyCalories = _weeklyCalories.asStateFlow()

    private val _weeklyProtein = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyProtein = _weeklyProtein.asStateFlow()

    private val _yesterdayMacroPercentages = MutableStateFlow(
        mapOf("Protein" to 0f, "Carbs" to 0f, "Fat" to 0f)
    )
    val yesterdayMacroPercentages = _yesterdayMacroPercentages.asStateFlow()

    private val _caloriesByTimeOfDay = MutableStateFlow(
        mapOf("Morning" to 0f, "Afternoon" to 0f, "Evening" to 0f, "Night" to 0f)
    )
    val caloriesByTimeOfDay = _caloriesByTimeOfDay.asStateFlow()

    // 🌟 STATE FLOWS FOR ALL NEW WEEKLY NUTRIENTS (g/mg, so Int)
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

    private val _geminiResult = MutableStateFlow<GeminiResult>(GeminiResult.Idle)
    val geminiResult: MutableStateFlow<GeminiResult> = _geminiResult

    private val _shouldResetDateOnResume = MutableStateFlow(true)
    val shouldResetDateOnResume = _shouldResetDateOnResume.asStateFlow()


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
                    fetchGraphPreferences() // 🌟 NEW: Fetch preferences on sign-in
                } else {
                    mealsListenerRegistration?.remove()
                    mealsListenerRegistration = null
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
                    _graphPreferences.value = emptyList() // 🌟 NEW: Clear preferences on logout
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // |                    NEW GRAPH PREFERENCE METHODS                       |
    // -------------------------------------------------------------------------

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
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                // Fetch the document containing the list of graph preferences
                val snapshot = preferencesCollection.document("graph_order").get().await()

                if (snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val storedList = snapshot.get("list") as? List<Map<String, Any>>

                    val preferences = storedList?.mapNotNull { map ->
                        // Safely map Firestore data to data class
                        GraphPreference(
                            id = map["id"] as? String ?: return@mapNotNull null,
                            title = map["title"] as? String ?: return@mapNotNull null,
                            order = (map["order"] as? Long)?.toInt() ?: 0,
                            isVisible = map["isVisible"] as? Boolean ?: true,
                            isMacro = map["isMacro"] as? Boolean ?: false
                        )
                    } ?: getDefaultGraphPreferences()

                    // CRITICAL: Merge default with stored list to handle new graphs
                    val defaultMap = getDefaultGraphPreferences().associateBy { it.id }
                    val storedMap = preferences.associateBy { it.id }

                    // Create the final list by prioritizing stored preferences but including all defaults
                    // (and ensuring new graphs appear at the end)
                    val finalPreferences = defaultMap.keys.mapNotNull { id ->
                        if (storedMap.containsKey(id)) {
                            // Use stored preference but ensure it has the correct, up-to-date title
                            storedMap[id]?.copy(title = defaultMap[id]?.title ?: storedMap[id]!!.title)
                        } else {
                            // New graph not in storage, add it to the end
                            defaultMap[id]?.copy(order = defaultMap.size + storedMap.size)
                        }
                    }.sortedBy { it.order }

                    _graphPreferences.value = finalPreferences

                } else {
                    // No preferences found, use and save default order
                    val defaultList = getDefaultGraphPreferences()
                    _graphPreferences.value = defaultList
                    saveGraphPreferences(defaultList) // Save default for the first time
                }
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error fetching graph preferences: ${e.message}")
                _graphPreferences.value = getDefaultGraphPreferences()
            }
        }
    }

    fun saveGraphPreferences(preferences: List<GraphPreference>) {
        val userId = auth.currentUser?.uid ?: return
        _graphPreferences.value = preferences // Optimistic update
        viewModelScope.launch {
            try {
                // Prepare data for Firestore
                val dataToSave = hashMapOf("list" to preferences.map { it.toMap() })
                preferencesCollection.document("graph_order").set(dataToSave).await()
            } catch (e: Exception) {
                Log.e("FoodLogViewModel", "Error saving graph preferences: ${e.message}")
            }
        }
    }

    // Helper function to convert GraphPreference to Map for Firestore
    private fun GraphPreference.toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "title" to title,
            "order" to order,
            "isVisible" to isVisible,
            "isMacro" to isMacro
        )
    }

    // -------------------------------------------------------------------------
    // |                    EXISTING LOGIC                                     |
    // -------------------------------------------------------------------------

    private fun calculateWeekStartDate(date: LocalDate): LocalDate {
        var daysToSubtract = date.dayOfWeek.value
        if (daysToSubtract == 7) {
            daysToSubtract = 0
        }
        return date.minusDays(daysToSubtract.toLong())
    }

    // Updated to call fetchGraphPreferences
    fun refreshStatistics() {
        fetchMealsForLastSevenDays()
        fetchGraphPreferences() // Ensure preferences are also refreshed
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

                processWeeklyCalories(meals)
                processCaloriesByTimeOfDay(meals)
                processWeeklyProtein(meals)

                // CALL ALL NEW WEEKLY PROCESSORS
                processWeeklyFiber(meals)
                processWeeklySugar(meals)
                processWeeklySodium(meals)
                processWeeklyPotassium(meals)
                processWeeklyCalcium(meals)
                processWeeklyIron(meals)
                processWeeklyVitaminC(meals)

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

    private fun processWeeklyProtein(meals: List<Meal>) {
        val today = LocalDate.now()
        val proteinByDay = (0..6).associate {
            today.minusDays(it.toLong()) to 0
        }.toMutableMap()
        for (meal in meals) {
            val mealDate = meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val proteinValue = meal.protein?.toInt() ?: 0
            if (proteinByDay.containsKey(mealDate)) {
                proteinByDay[mealDate] = (proteinByDay[mealDate] ?: 0) + proteinValue
            }
        }
        _weeklyProtein.value = proteinByDay
    }

    private fun processWeeklyFiber(meals: List<Meal>) {
        val today = LocalDate.now()
        val fiberByDay = (0..6).associate {
            today.minusDays(it.toLong()) to 0
        }.toMutableMap()
        for (meal in meals) {
            val mealDate = meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val fiberValue = meal.fiber?.toInt() ?: 0
            if (fiberByDay.containsKey(mealDate)) {
                fiberByDay[mealDate] = (fiberByDay[mealDate] ?: 0) + fiberValue
            }
        }
        _weeklyFiber.value = fiberByDay
    }

    private fun processWeeklySugar(meals: List<Meal>) {
        val today = LocalDate.now()
        val sugarByDay = (0..6).associate {
            today.minusDays(it.toLong()) to 0
        }.toMutableMap()
        for (meal in meals) {
            val mealDate = meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val sugarValue = meal.sugar?.toInt() ?: 0
            if (sugarByDay.containsKey(mealDate)) {
                sugarByDay[mealDate] = (sugarByDay[mealDate] ?: 0) + sugarValue
            }
        }
        _weeklySugar.value = sugarByDay
    }

    private fun processWeeklySodium(meals: List<Meal>) {
        val today = LocalDate.now()
        val sodiumByDay = (0..6).associate {
            today.minusDays(it.toLong()) to 0
        }.toMutableMap()
        for (meal in meals) {
            val mealDate = meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val sodiumValue = meal.sodium?.toInt() ?: 0
            if (sodiumByDay.containsKey(mealDate)) {
                sodiumByDay[mealDate] = (sodiumByDay[mealDate] ?: 0) + sodiumValue
            }
        }
        _weeklySodium.value = sodiumByDay
    }

    private fun processWeeklyPotassium(meals: List<Meal>) {
        val today = LocalDate.now()
        val potassiumByDay = (0..6).associate {
            today.minusDays(it.toLong()) to 0
        }.toMutableMap()
        for (meal in meals) {
            val mealDate = meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val potassiumValue = meal.potassium?.toInt() ?: 0
            if (potassiumByDay.containsKey(mealDate)) {
                potassiumByDay[mealDate] = (potassiumByDay[mealDate] ?: 0) + potassiumValue
            }
        }
        _weeklyPotassium.value = potassiumByDay
    }

    private fun processWeeklyCalcium(meals: List<Meal>) {
        val today = LocalDate.now()
        val calciumByDay = (0..6).associate {
            today.minusDays(it.toLong()) to 0
        }.toMutableMap()
        for (meal in meals) {
            val mealDate = meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val calciumValue = meal.calcium?.toInt() ?: 0
            if (calciumByDay.containsKey(mealDate)) {
                calciumByDay[mealDate] = (calciumByDay[mealDate] ?: 0) + calciumValue
            }
        }
        _weeklyCalcium.value = calciumByDay
    }

    private fun processWeeklyIron(meals: List<Meal>) {
        val today = LocalDate.now()
        val ironByDay = (0..6).associate {
            today.minusDays(it.toLong()) to 0
        }.toMutableMap()
        for (meal in meals) {
            val mealDate = meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val ironValue = meal.iron?.toInt() ?: 0
            if (ironByDay.containsKey(mealDate)) {
                ironByDay[mealDate] = (ironByDay[mealDate] ?: 0) + ironValue
            }
        }
        _weeklyIron.value = ironByDay
    }

    private fun processWeeklyVitaminC(meals: List<Meal>) {
        val today = LocalDate.now()
        val vitaminCByDay = (0..6).associate {
            today.minusDays(it.toLong()) to 0
        }.toMutableMap()
        for (meal in meals) {
            val mealDate = meal.timestamp.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            val vitaminCValue = meal.vitaminC?.toInt() ?: 0
            if (vitaminCByDay.containsKey(mealDate)) {
                vitaminCByDay[mealDate] = (vitaminCByDay[mealDate] ?: 0) + vitaminCValue
            }
        }
        _weeklyVitaminC.value = vitaminCByDay
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
        var totalProtein = 0.0
        var totalCarbs = 0.0
        var totalFat = 0.0

        val PROTEIN_CALORIES_PER_GRAM = 4.0
        val CARB_CALORIES_PER_GRAM = 4.0
        val FAT_CALORIES_PER_GRAM = 9.0

        for (meal in meals) {
            totalProtein += meal.protein ?: 0.0
            totalCarbs += meal.carbohydrates ?: 0.0
            totalFat += meal.fat ?: 0.0
        }

        val proteinCalories = totalProtein * PROTEIN_CALORIES_PER_GRAM
        val carbCalories = totalCarbs * CARB_CALORIES_PER_GRAM
        val fatCalories = totalFat * FAT_CALORIES_PER_GRAM

        val totalMacroCalories = proteinCalories + carbCalories + fatCalories

        if (totalMacroCalories > 0) {
            val proteinPct = (proteinCalories / totalMacroCalories * 100).toFloat()
            val carbPct = (carbCalories / totalMacroCalories * 100).toFloat()
            val fatPct = (fatCalories / totalMacroCalories * 100).toFloat()

            _yesterdayMacroPercentages.value = mapOf(
                "Protein" to proteinPct,
                "Carbs" to carbPct,
                "Fat" to fatPct
            )
        } else {
            _yesterdayMacroPercentages.value = mapOf("Protein" to 0f, "Carbs" to 0f, "Fat" to 0f)
        }
    }


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

    fun resetDateToTodayIfNeeded() {
        if (_shouldResetDateOnResume.value) {
            selectDate(LocalDate.now())
            _shouldResetDateOnResume.value = false
        }
    }

    fun setShouldResetDateOnResume() {
        _shouldResetDateOnResume.value = true
    }

    fun getMealById(mealId: String): Meal? {
        return mealsForSelectedDate.value.firstOrNull { it.id == mealId }
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