package com.nadavariel.dietapp.data

data class DietPlan(
    val dailyCalories: Int = 0,
    val proteinGrams: Int = 0,
    val carbsGrams: Int = 0,
    val fatGrams: Int = 0,
    val recommendations: String = "No recommendations available.",
    val disclaimer: String = "This diet plan is AI-generated. Consult with a healthcare professional before making significant dietary changes."
)