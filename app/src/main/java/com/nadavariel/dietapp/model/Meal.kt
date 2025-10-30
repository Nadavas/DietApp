package com.nadavariel.dietapp.model // You can place this in your 'model' package

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import com.google.firebase.Timestamp
import com.google.gson.annotations.SerializedName
import com.nadavariel.dietapp.ui.theme.EveningSectionColor
import com.nadavariel.dietapp.ui.theme.MorningSectionColor
import com.nadavariel.dietapp.ui.theme.NightSectionColor
import com.nadavariel.dietapp.ui.theme.NoonSectionColor
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

// --- Meal ---

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
    val servingAmount: String? = null,
    val servingUnit: String? = null,
    val timestamp: Timestamp = Timestamp(Date())
)

// --- MealSection ---

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

// --- DietPlan ---

data class DietPlan(
    val dailyCalories: Int = 0,
    val proteinGrams: Int = 0,
    val carbsGrams: Int = 0,
    val fatGrams: Int = 0,
    val recommendations: String = "No recommendations available.",
    val disclaimer: String = "This diet plan is AI-generated. Consult with a healthcare professional before making significant dietary changes."
)

// --- FoodNutritionalInfo (from Gemini) ---

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