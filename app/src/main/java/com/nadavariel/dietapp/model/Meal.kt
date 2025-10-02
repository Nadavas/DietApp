package com.nadavariel.dietapp.model

import com.google.firebase.Timestamp
import java.util.Date

// Meal data class
data class Meal(
    val id: String = "", // Document ID from Firestore
    val foodName: String = "",
    val calories: Int = 0,
    val protein: Double? = null,
    val carbohydrates: Double? = null,
    val fat: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val sodium: Double? = null,       // Typically measured in milligrams (mg), but using Double for consistency with others
    val potassium: Double? = null,
    val calcium: Double? = null,
    val iron: Double? = null,
    val vitaminC: Double? = null,

    val servingAmount: String? = null,
    val servingUnit: String? = null,
    val timestamp: Timestamp = Timestamp(Date()) // The time the meal was logged
)