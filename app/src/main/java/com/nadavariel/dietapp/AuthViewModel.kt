package com.nadavariel.dietapp

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore // NEW: Import Firestore
import com.google.firebase.firestore.ktx.firestore // NEW: Import Firestore KTX
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await // NEW: For await() on Firestore tasks

// To represent the result of an auth operation
sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
    data object Idle : AuthResult() // Initial state
}

class AuthViewModel(private val preferencesRepository: UserPreferencesRepository) : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore // NEW: Initialize Firestore

    val emailState = mutableStateOf("")
    val passwordState = mutableStateOf("")
    val confirmPasswordState = mutableStateOf("") // For sign-up

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult

    val rememberMeState = mutableStateOf(false)

    val nameState = mutableStateOf("")
    val weightState = mutableStateOf("") // Stored as String, convert to Float/Int for calculations

    // REMOVED: ProfileKeys as profile data will be in Firestore
    // private object ProfileKeys {
    //     val USER_NAME = stringPreferencesKey("user_name")
    //     val USER_WEIGHT = stringPreferencesKey("user_weight")
    // }

    init {
        viewModelScope.launch {
            emailState.value = preferencesRepository.userEmailFlow.first()
            rememberMeState.value = preferencesRepository.rememberMeFlow.first()

            // Only load profile data if a user is already signed in on app launch
            if (isUserSignedIn()) { // NEW: Conditional load
                loadUserProfile() // Call the new Firestore loading function
            }
        }
    }

    // NEW: Function to load profile data from Firestore
    private suspend fun loadUserProfile() {
        val userId = auth.currentUser?.uid // Get the current user's UID
        if (userId != null) {
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                if (userDoc.exists()) {
                    nameState.value = userDoc.getString("name") ?: ""
                    weightState.value = userDoc.getString("weight") ?: ""
                    Log.d("AuthViewModel", "Loaded profile from Firestore: Name='${nameState.value}', Weight='${weightState.value}' for UID: $userId")
                } else {
                    // User exists in Auth but no profile in Firestore yet (e.g., new user)
                    nameState.value = ""
                    weightState.value = ""
                    Log.d("AuthViewModel", "No profile found in Firestore for UID: $userId. Setting empty.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error loading user profile from Firestore: ${e.message}", e)
                // Optionally, show an error to the user or set default values
                nameState.value = ""
                weightState.value = ""
            }
        } else {
            // No user signed in, clear profile states
            nameState.value = ""
            weightState.value = ""
            Log.d("AuthViewModel", "No user signed in. Clearing profile states.")
        }
    }

    // NEW: Function to save profile data to Firestore
    private suspend fun saveUserProfile(name: String, weight: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userProfile = hashMapOf(
                "name" to name,
                "weight" to weight
            )
            try {
                firestore.collection("users").document(userId).set(userProfile).await()
                Log.d("AuthViewModel", "Profile saved to Firestore for UID: $userId")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error saving user profile to Firestore: ${e.message}", e)
                // Optionally, show an error to the user
            }
        } else {
            Log.w("AuthViewModel", "Cannot save profile: No user signed in.")
        }
    }

    // Function to check if a user is currently signed in
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
                        // NEW: Load profile data for the newly signed up user
                        loadUserProfile()
                    }
                    onSuccess()
                    clearInputFields()
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Sign up failed.")
                }
            }
    }

    fun signIn(onSuccess: () -> Unit) {
        if (emailState.value.isBlank() || passwordState.value.isBlank()) {
            _authResult.value = AuthResult.Error("Email and password cannot be empty.")
            return
        }
        _authResult.value = AuthResult.Loading
        auth.signInWithEmailAndPassword(emailState.value.trim(), passwordState.value.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = AuthResult.Success
                    viewModelScope.launch {
                        if (rememberMeState.value) {
                            preferencesRepository.saveUserPreferences(emailState.value, rememberMeState.value)
                        } else {
                            preferencesRepository.clearUserPreferences()
                        }
                        // NEW: Load profile data for the newly signed in user
                        loadUserProfile()
                    }
                    onSuccess()
                    clearInputFields()
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Sign in failed.")
                }
            }
    }

    fun signOut() {
        auth.signOut()
        _authResult.value = AuthResult.Idle
        viewModelScope.launch {
            // Keep email and rememberMe if user chose, clear otherwise
            if (!rememberMeState.value) {
                preferencesRepository.saveUserPreferences("", false)
            }
            // NEW: Clear profile states in ViewModel when signing out
            // The data itself remains in Firestore, ready for the user to sign back in later.
            nameState.value = ""
            weightState.value = ""
            // No need to clear from DataStore as it's no longer storing profile data.
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

    fun firebaseAuthWithGoogle(idToken: String) {
        _authResult.value = AuthResult.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = AuthResult.Success
                    // NEW: Load data after successful Google sign-in
                    viewModelScope.launch {
                        loadUserProfile()
                    }
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Google sign-in failed.")
                }
            }
    }

    fun handleGoogleSignInResult(account: GoogleSignInAccount?, onSuccess: () -> Unit) {
        if (account == null) {
            _authResult.value = AuthResult.Error("Google sign-in failed.")
            return
        }
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
                        // NEW: Load profile data after successful Google sign-in
                        loadUserProfile()
                    }
                    onSuccess()
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Google sign-in failed.")
                }
            }
    }

    // Functions to update and save profile data
    fun updateProfile(name: String, weight: String, onSuccess: () -> Unit) {
        // NEW: Save to Firestore instead of DataStore
        viewModelScope.launch {
            saveUserProfile(name, weight) // Call the new Firestore saving function
            // Update the states in the ViewModel immediately
            nameState.value = name
            weightState.value = weight
            onSuccess() // Indicate success, e.g., for navigation
        }
    }
}