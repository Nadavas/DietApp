package com.nadavariel.dietapp.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.gson.annotations.SerializedName
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

data class Meal(
    val id: String = "", // Document ID from Firestore
    val foodName: String = "",
    val calories: Int = 0,
    val protein: Double? = null,
    val carbohydrates: Double? = null,
    val fat: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val sodium: Double? = null,
    val potassium: Double? = null,
    val calcium: Double? = null,
    val iron: Double? = null,
    val vitaminC: Double? = null,
    val vitaminA: Double? = null,
    val vitaminB12: Double? = null,
    val servingAmount: String? = null,
    val servingUnit: String? = null,
    val timestamp: Timestamp = Timestamp(Date())
)

val MorningSectionColor = Color(0xFFFBC02D)
val NoonSectionColor = Color(0xFF8BC34A)
val EveningSectionColor = Color(0xFF40C4FF)
val NightSectionColor = Color(0xFF607D8B)

enum class MealSection(val sectionName: String, val color: Color) {
    MORNING("Morning", MorningSectionColor),
    NOON("Noon", NoonSectionColor),
    EVENING("Evening", EveningSectionColor),
    NIGHT("Night", NightSectionColor);

    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun getMealSection(mealTimestamp: Date): MealSection {
            val localTime = mealTimestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
            return when {
                localTime.isBefore(LocalTime.of(12, 0)) -> MORNING
                localTime.isBefore(LocalTime.of(18, 0)) -> NOON
                localTime.isBefore(LocalTime.of(22, 0)) -> EVENING
                else -> NIGHT
            }
        }
    }
}

data class CalculatedStats(
    val daysLogged: Int,
    val avgCals: Int,
    val avgProtein: Int
)

data class DietPlan(
    val healthOverview: List<String> = emptyList(),
    val goalStrategy: List<String> = emptyList(),
    val concretePlan: ConcretePlan = ConcretePlan(),
    val exampleMealPlan: ExampleMealPlan = ExampleMealPlan(),
    val disclaimer: String = ""
)

data class ConcretePlan(
    val targets: Targets = Targets(),
    val mealGuidelines: MealGuidelines = MealGuidelines(),
    val trainingAdvice: List<String> = emptyList()
)

data class Targets(
    val dailyCalories: Int = 0,
    val proteinGrams: Int = 0,
    val carbsGrams: Int = 0,
    val fatGrams: Int = 0
)

data class MealGuidelines(
    val mealFrequency: String = "",
    val foodsToEmphasize: List<String> = emptyList(),
    val foodsToLimit: List<String> = emptyList()
)

data class ExampleMealPlan(
    val breakfast: ExampleMeal = ExampleMeal(),
    val lunch: ExampleMeal = ExampleMeal(),
    val dinner: ExampleMeal = ExampleMeal(),
    val snacks: ExampleMeal = ExampleMeal()
)

data class ExampleMeal(
    val description: String = "",
    val estimatedCalories: Int = 0
)

data class NutrientDef(
    val label: String,
    val unit: String,
    val color: Color,
    val selector: (Meal) -> Double? // Function to get the value from a Meal
)

data class NutrientData(
    val label: String,
    val value: String,
    val onChange: (String) -> Unit
)

data class FoodNutritionalInfo(
    @SerializedName("food_name") val foodName: String?,
    @SerializedName("serving_unit") val servingUnit: String?,
    @SerializedName("serving_amount") val servingAmount: String?,
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
    @SerializedName("vitamin_c") val vitaminC: String?,
    @SerializedName("vitamin_a") val vitaminA: String?,
    @SerializedName("vitamin_b12") val vitaminB12: String?
)

data class WeightEntry(
    @DocumentId
    val id: String = "",
    val weight: Float = 0f,
    @ServerTimestamp
    val timestamp: Timestamp? = null
)