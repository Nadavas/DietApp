// model/Meal.kt
package com.nadavariel.dietapp.model

import com.google.firebase.Timestamp
import java.util.Date

// Represents a single food item logged by the user
data class Meal(
    val id: String = "", // Unique ID for the meal (will be firestore document ID)
    val foodName: String = "",
    val calories: Int = 0,
    val timestamp: Timestamp = Timestamp(Date()) // When the meal was consumed
)