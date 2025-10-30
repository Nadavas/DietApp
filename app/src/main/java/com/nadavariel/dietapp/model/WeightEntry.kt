package com.nadavariel.dietapp.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Represents a single weight entry in the user's history.
 * Includes the document ID to allow for editing and deleting.
 */
data class WeightEntry(
    @DocumentId
    val id: String = "", // <-- ADDED THIS FIELD
    val weight: Float = 0f,
    @ServerTimestamp
    val timestamp: Timestamp? = null
)