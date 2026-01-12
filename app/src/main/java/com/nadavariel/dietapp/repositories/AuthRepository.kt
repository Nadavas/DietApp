@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.repositories

import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.models.Gender
import com.nadavariel.dietapp.models.UserProfile
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    // --- Auth State ---

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun addAuthStateListener(listener: (FirebaseAuth) -> Unit): FirebaseAuth.AuthStateListener {
        val authListener = FirebaseAuth.AuthStateListener { auth -> listener(auth) }
        auth.addAuthStateListener(authListener)
        return authListener
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified == true
    }

    suspend fun reloadUser() {
        auth.currentUser?.reload()?.await()
    }

    // --- Sign In / Sign Up Actions ---

    suspend fun createUserWithEmailAndPassword(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signInWithGoogleCredential(account: GoogleSignInAccount): com.google.firebase.auth.AuthResult {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        return auth.signInWithCredential(credential).await()
    }

    fun signOut() {
        auth.signOut()
    }

    fun sendVerificationEmail(onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                onError(task.exception?.message ?: "Failed to send verification email.")
            }
        }
    }

    // --- Re-authentication & Password Management ---

    fun reauthenticateUser(credential: AuthCredential, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            onError("No user signed in.")
            return
        }
        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                onError(task.exception?.message ?: "Re-authentication failed.")
            }
        }
    }

    fun updatePassword(newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onSuccess()
            } else {
                onError(task.exception?.message ?: "Failed to update password.")
            }
        }
    }

    fun getEmailCredential(password: String): AuthCredential? {
        val email = auth.currentUser?.email ?: return null
        return EmailAuthProvider.getCredential(email, password)
    }

    fun getGoogleCredential(idToken: String): AuthCredential {
        return GoogleAuthProvider.getCredential(idToken, null)
    }

    // --- Firestore: User Profile ---

    suspend fun getUserDocument(userId: String): Boolean {
        val snapshot = firestore.collection("users").document(userId).get().await()
        return snapshot.exists()
    }

    fun addUserProfileListener(
        userId: String,
        onProfileUpdated: (UserProfile) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        val name = snapshot.getString("name") ?: ""
                        val startingWeight = (snapshot.get("startingWeight") as? Number)?.toFloat() ?: 0f
                        val height = (snapshot.get("height") as? Number)?.toFloat() ?: 0f
                        val dateOfBirth = snapshot.getDate("dateOfBirth")
                        val avatarId = snapshot.getString("avatarId")
                        val genderString = snapshot.getString("gender")
                        val gender = try {
                            genderString?.let { Gender.valueOf(it) } ?: Gender.UNKNOWN
                        } catch (_: IllegalArgumentException) {
                            Gender.UNKNOWN
                        }

                        val profile = UserProfile(
                            name = name,
                            startingWeight = startingWeight,
                            height = height,
                            dateOfBirth = dateOfBirth,
                            avatarId = avatarId,
                            gender = gender,
                        )
                        onProfileUpdated(profile)
                    } catch (e: Exception) {
                        onError(e)
                    }
                } else {
                    // Document doesn't exist, return empty profile
                    onProfileUpdated(UserProfile())
                }
            }
    }

    suspend fun saveUserProfile(userId: String, profile: UserProfile) {
        val userProfileMap = hashMapOf(
            "name" to profile.name,
            "startingWeight" to profile.startingWeight,
            "height" to profile.height,
            "dateOfBirth" to profile.dateOfBirth,
            "avatarId" to profile.avatarId,
            "gender" to profile.gender.name
        )
        firestore.collection("users").document(userId).set(userProfileMap).await()
    }

    // --- Account Deletion Logic ---

    suspend fun deleteUserSubCollections(userId: String) {
        // Helper to delete specific collections
        val collections = listOf("meals", "weight_history", "notifications")

        for (collectionName in collections) {
            try {
                val collectionRef = firestore.collection("users")
                    .document(userId).collection(collectionName)
                val querySnapshot = collectionRef.get().await()
                val batch = firestore.batch()
                for (document in querySnapshot.documents) {
                    batch.delete(document.reference)
                }
                batch.commit().await()
                Log.d("AuthRepository", "Deleted sub-collection: $collectionName")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Error deleting sub-collection $collectionName", e)
            }
        }
    }

    suspend fun deleteUserDocuments(userId: String) {
        // Delete individual documents in sub-collections
        firestore.collection("users").document(userId)
            .collection("user_answers").document("diet_habits")
            .delete().await()

        firestore.collection("users").document(userId)
            .collection("user_answers").document("goals")
            .delete().await()

        firestore.collection("users").document(userId)
            .collection("diet_plans").document("current_plan")
            .delete().await()

        // Delete main user document
        firestore.collection("users").document(userId).delete().await()
    }

    suspend fun deleteAuthUser() {
        auth.currentUser?.delete()?.await()
    }
}