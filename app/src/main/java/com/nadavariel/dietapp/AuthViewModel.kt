@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser // Add this import
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.data.UserPreferencesRepository
import com.nadavariel.dietapp.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
    data object Idle : AuthResult()
}

class AuthViewModel(private val preferencesRepository: UserPreferencesRepository) : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    val emailState = mutableStateOf("")
    val passwordState = mutableStateOf("")
    val confirmPasswordState = mutableStateOf("")

    // AuthResult will remain a StateFlow as it's typically observed once per event
    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult.asStateFlow()

    val rememberMeState = mutableStateOf(false)

    // ⭐ CHANGE: UserProfile is now Compose mutableStateOf directly
    var userProfile: UserProfile by mutableStateOf(UserProfile())
        private set

    // ⭐ CHANGE: currentUser is now Compose mutableStateOf directly
    var currentUser: FirebaseUser? by mutableStateOf(null)
        private set

    init {
        // Observe Firebase Auth state changes and update the Compose State directly
        auth.addAuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            Log.d("AuthViewModel", "Auth state changed. Current user: ${currentUser?.uid}")
            // Trigger loading user profile whenever auth state changes (e.g., after login/logout)
            viewModelScope.launch {
                if (currentUser != null) {
                    loadUserProfile()
                } else {
                    userProfile = UserProfile() // Clear profile if no user
                }
            }
        }

        viewModelScope.launch {
            emailState.value = preferencesRepository.userEmailFlow.first()
            rememberMeState.value = preferencesRepository.rememberMeFlow.first()

            // Initial load of profile should now be handled by the authStateListener
            // if (isUserSignedIn()) { loadUserProfile() } // Removed: Listener handles this
        }
    }

    private suspend fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                if (userDoc.exists()) {
                    val name = userDoc.getString("name") ?: ""
                    val weight = (userDoc.get("weight") as? Number)?.toFloat() ?: 0f
                    val age = (userDoc.get("age") as? Number)?.toInt() ?: 0
                    val targetWeight = (userDoc.get("targetWeight") as? Number)?.toFloat() ?: 0f

                    userProfile = UserProfile(name, weight, age, targetWeight) // Update Compose State directly
                    Log.d("AuthViewModel", "Loaded profile from firestore: $userProfile for UID: $userId")
                } else {
                    userProfile = UserProfile() // Set to default empty profile
                    Log.d("AuthViewModel", "No profile found in firestore for UID: $userId. Setting empty.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error loading user profile from firestore: ${e.message}", e)
                userProfile = UserProfile() // Set to default empty profile on error
            }
        } else {
            userProfile = UserProfile() // No user signed in, clear profile states
            Log.d("AuthViewModel", "No user signed in. Clearing profile states.")
        }
    }

    private suspend fun saveUserProfile(profile: UserProfile) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userProfileMap = hashMapOf(
                "name" to profile.name,
                "weight" to profile.weight,
                "age" to profile.age,
                "targetWeight" to profile.targetWeight
            )
            try {
                firestore.collection("users").document(userId).set(userProfileMap).await()
                Log.d("AuthViewModel", "Profile saved to firestore for UID: $userId: $profile")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error saving user profile to firestore: ${e.message}", e)
            }
        } else {
            Log.w("AuthViewModel", "Cannot save profile: No user signed in.")
        }
    }

    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    fun signUp(onSuccess: () -> Unit) {
        if (emailState.value.isBlank() || passwordState.value.isBlank() || confirmPasswordState.value.isBlank()) {
            _authResult.value = AuthResult.Error("Email and passwords cannot be empty.")
            return
        }
        if (passwordState.value != confirmPasswordState.value) {
            _authResult.value = AuthResult.Error("Passwords do not match.")
            return
        }
        _authResult.value = AuthResult.Loading
        auth.createUserWithEmailAndPassword(emailState.value.trim(), passwordState.value.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = AuthResult.Success
                    viewModelScope.launch {
                        if (rememberMeState.value) {
                            preferencesRepository.saveUserPreferences(emailState.value, rememberMeState.value)
                        } else {
                            preferencesRepository.clearUserPreferences()
                        }
                        val newProfile = UserProfile(name = emailState.value.substringBefore("@"))
                        saveUserProfile(newProfile)
                        userProfile = newProfile // Update Compose State directly
                    }
                    onSuccess()
                    clearInputFields()
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Sign up failed.")
                }
            }
    }

    // ⭐ MODIFIED: signIn now accepts email and password explicitly for re-authentication use case
    fun signIn(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authResult.value = AuthResult.Error("Email and password cannot be empty.")
            return
        }
        _authResult.value = AuthResult.Loading
        auth.signInWithEmailAndPassword(email.trim(), password.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = AuthResult.Success
                    viewModelScope.launch {
                        if (rememberMeState.value) {
                            preferencesRepository.saveUserPreferences(email, rememberMeState.value)
                        } else {
                            preferencesRepository.clearUserPreferences()
                        }
                    }
                    onSuccess()
                    clearInputFields()
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Sign in failed.")
                }
            }
    }

    // Overload for signIn that uses existing email/passwordState (for standard login screen)
    fun signIn(onSuccess: () -> Unit) {
        signIn(emailState.value, passwordState.value, onSuccess)
    }

    fun signOut() {
        auth.signOut()
        _authResult.value = AuthResult.Idle
        viewModelScope.launch {
            if (!rememberMeState.value) {
                preferencesRepository.saveUserPreferences("", false)
            }
            // userProfile will be cleared by authStateListener setting currentUser to null
        }
        clearInputFields()
    }

    fun clearInputFields() {
        if (!rememberMeState.value) {
            emailState.value = ""
        }
        passwordState.value = ""
        confirmPasswordState.value = ""
    }

    fun resetAuthResult() {
        _authResult.value = AuthResult.Idle
    }

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun firebaseAuthWithGoogle(idToken: String, onSuccess: () -> Unit) {
        _authResult.value = AuthResult.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = AuthResult.Success
                    viewModelScope.launch {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            val userDoc = firestore.collection("users").document(userId).get().await()
                            if (!userDoc.exists()) {
                                val newProfile = UserProfile(name = task.result.user?.displayName ?: "")
                                saveUserProfile(newProfile)
                            }
                        }
                        // loadUserProfile() is handled by authStateListener
                        onSuccess()
                    }
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Google sign-in failed.")
                }
            }
    }

    fun handleGoogleSignInResult(account: GoogleSignInAccount, onSuccess: () -> Unit) {
        _authResult.value = AuthResult.Loading
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = AuthResult.Success
                    viewModelScope.launch {
                        if (rememberMeState.value) {
                            preferencesRepository.saveUserPreferences(account.email ?: "", rememberMeState.value)
                        } else {
                            preferencesRepository.clearUserPreferences()
                        }
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            val userDoc = firestore.collection("users").document(userId).get().await()
                            if (!userDoc.exists()) {
                                val newProfile = UserProfile(name = account.displayName ?: "")
                                saveUserProfile(newProfile)
                            }
                        }
                        // loadUserProfile() is handled by authStateListener
                        onSuccess()
                    }
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Google sign-in failed.")
                }
            }
    }

    fun updateProfile(name: String, weight: String, age: String, targetWeight: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val parsedWeight = weight.toFloatOrNull() ?: 0f
            val parsedAge = age.toIntOrNull() ?: 0
            val parsedTargetWeight = targetWeight.toFloatOrNull() ?: 0f

            val updatedProfile = userProfile.copy( // Access directly
                name = name,
                weight = parsedWeight,
                age = parsedAge,
                targetWeight = parsedTargetWeight
            )
            saveUserProfile(updatedProfile)
            userProfile = updatedProfile // Update Compose State directly
            onSuccess()
        }
    }

    // ⭐ NEW: Function to delete current user and their firestore data
    fun deleteCurrentUser(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            _authResult.value = AuthResult.Loading // Set loading state
            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("AuthViewModel", "User account deleted from Firebase Auth for UID: ${user.uid}")
                        // Now delete their firestore profile data
                        viewModelScope.launch {
                            try {
                                firestore.collection("users").document(user.uid).delete().await()
                                Log.d("AuthViewModel", "User data deleted from firestore for UID: ${user.uid}")
                                // Reset auth result to success after both operations
                                _authResult.value = AuthResult.Success
                                // Clear local states, as user is now gone
                                userProfile = UserProfile()
                                preferencesRepository.clearUserPreferences()
                                onSuccess()
                            } catch (e: Exception) {
                                val firestoreError = "Account deleted, but failed to delete associated data: ${e.message}"
                                Log.e("AuthViewModel", firestoreError, e)
                                _authResult.value = AuthResult.Error(firestoreError) // Report firestore deletion error
                                onError(firestoreError) // Callback with error
                            }
                        }
                    } else {
                        val errorMessage = task.exception?.message ?: "Failed to delete account."
                        Log.e("AuthViewModel", "Failed to delete user account: $errorMessage", task.exception)
                        _authResult.value = AuthResult.Error(errorMessage) // Report Auth deletion error

                        // Handle re-authentication requirement
                        if (task.exception is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                            onError("re-authenticate-required") // Use a specific code for UI to interpret
                        } else {
                            onError(errorMessage)
                        }
                    }
                }
        } else {
            val noUserError = "No user is currently signed in to delete."
            Log.w("AuthViewModel", noUserError)
            _authResult.value = AuthResult.Error(noUserError)
            onError(noUserError)
        }
    }
}