package com.nadavariel.dietapp.repositories

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nadavariel.dietapp.models.FoodNutritionalInfo
import com.nadavariel.dietapp.models.Meal
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

@RequiresApi(Build.VERSION_CODES.O)
class MealRepository {

    private val firestore: FirebaseFirestore = Firebase.firestore
    private val functions: FirebaseFunctions = Firebase.functions("us-central1")

    // --- Live Meals Flow (Daily) ---
    fun getMealsForDateFlow(userId: String, date: LocalDate): Flow<List<Meal>> = callbackFlow {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val listener = firestore.collection("users").document(userId).collection("meals")
            .whereGreaterThanOrEqualTo("timestamp", Timestamp(Date(startOfDay)))
            .whereLessThan("timestamp", Timestamp(Date(endOfDay)))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("MealRepository", "Error listening for meals", e)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val mealList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Meal::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(mealList)
            }
        awaitClose { listener.remove() }
    }

    // --- One-Shot Queries (Weekly Stats) ---
    suspend fun getMealsFromDateRange(userId: String, startDate: Date): List<Meal> {
        val querySnapshot = firestore.collection("users").document(userId).collection("meals")
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .get()
            .await()
        return querySnapshot.toObjects(Meal::class.java)
    }

    // --- CRUD Operations ---

    suspend fun addMeal(userId: String, meal: Meal) {
        firestore.collection("users").document(userId).collection("meals").add(meal).await()
    }

    suspend fun updateMeal(userId: String, mealId: String, data: Map<String, Any?>) {
        firestore.collection("users").document(userId)
            .collection("meals").document(mealId)
            .update(data).await()
    }

    suspend fun deleteMeal(userId: String, mealId: String) {
        firestore.collection("users").document(userId)
            .collection("meals").document(mealId)
            .delete().await()
    }

    // --- Gemini / Cloud Functions ---

    suspend fun getNutritionalInfo(foodName: String, imageB64: String?): List<FoodNutritionalInfo> {
        val data = hashMapOf<String, Any>(
            "foodName" to foodName
        )
        if (imageB64 != null) {
            data["imageB64"] = imageB64
        }

        val result = functions
            .getHttpsCallable("analyzeFoodWithGemini")
            .call(data)
            .await()

        val responseData = result.data as? Map<*, *> ?: throw Exception("Function response data is null.")
        val success = responseData["success"] as? Boolean
        val geminiData = responseData["data"]

        if (success == true && geminiData != null) {
            val gson = Gson()
            val jsonString = gson.toJson(geminiData)
            val listType = object : TypeToken<List<FoodNutritionalInfo>>() {}.type
            return gson.fromJson(jsonString, listType)
        } else {
            val errorMsg = responseData["error"] as? String
            throw Exception(errorMsg ?: "Unknown API error")
        }
    }
}