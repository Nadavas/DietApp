package com.nadavariel.dietapp.model

import com.google.firebase.Timestamp
import java.util.Date

// Meal data class
data class Meal(
    val id: String = "", // Document ID from Firestore
    val foodName: String = "",
    val calories: Int = 0,
    val timestamp: Timestamp = Timestamp(Date()) // The time the meal was logged
)