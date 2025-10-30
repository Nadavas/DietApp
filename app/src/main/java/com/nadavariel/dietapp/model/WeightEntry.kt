package com.nadavariel.dietapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a single weight entry in the user's history.
 * This will be stored in the 'weight_history' sub-collection.
 */
data class WeightEntry(
    val weight: Float = 0f,
    @ServerTimestamp
    val timestamp: Timestamp? = null
)