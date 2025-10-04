package com.nadavariel.dietapp.data

import com.google.gson.annotations.SerializedName

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