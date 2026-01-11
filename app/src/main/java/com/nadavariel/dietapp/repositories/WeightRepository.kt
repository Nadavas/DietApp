package com.nadavariel.dietapp.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.constants.QuizConstants
import com.nadavariel.dietapp.models.WeightEntry
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WeightRepository {

    private val firestore: FirebaseFirestore = Firebase.firestore

    // --- Weight History Flow ---
    fun getWeightHistoryFlow(userId: String): Flow<List<WeightEntry>> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("weight_history")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("WeightRepository", "Error listening for weight history", e)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val historyList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(WeightEntry::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(historyList)
            }
        awaitClose { listener.remove() }
    }

    // --- Target Weight Flow ---
    fun getTargetWeightFlow(userId: String): Flow<Float> = callbackFlow {
        val listener = firestore.collection("users").document(userId)
            .collection("user_answers").document("goals")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    trySend(0f)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val answersMap = snapshot.get("answers") as? List<Map<String, String>>

                    val targetWeightAnswer = answersMap?.firstOrNull {
                        it["question"] == QuizConstants.TARGET_WEIGHT_QUESTION
                    }?.get("answer")

                    val parsedWeight = targetWeightAnswer?.split(" ")?.firstOrNull()?.toFloatOrNull() ?: 0f
                    trySend(parsedWeight)
                } else {
                    trySend(0f)
                }
            }
        awaitClose { listener.remove() }
    }

    // --- CRUD ---

    suspend fun addWeightEntry(userId: String, entry: WeightEntry) {
        firestore.collection("users").document(userId)
            .collection("weight_history")
            .add(entry)
            .await()
    }

    suspend fun updateWeightEntry(userId: String, id: String, data: Map<String, Any>) {
        firestore.collection("users").document(userId)
            .collection("weight_history").document(id)
            .update(data).await()
    }

    suspend fun deleteWeightEntry(userId: String, id: String) {
        firestore.collection("users").document(userId)
            .collection("weight_history").document(id)
            .delete().await()
    }
}