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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.data.UserPreferencesRepository
import com.nadavariel.dietapp.model.UserProfile // Import the UserProfile data class
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

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult

    val rememberMeState = mutableStateOf(false)

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    val currentUser get() = auth.currentUser

    init {
        viewModelScope.launch {
            emailState.value = preferencesRepository.userEmailFlow.first()
            rememberMeState.value = preferencesRepository.rememberMeFlow.first()

            if (isUserSignedIn()) {
                loadUserProfile()
            }
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

                    _userProfile.value = UserProfile(name, weight, age, targetWeight)
                    Log.d("AuthViewModel", "Loaded profile from Firestore: ${_userProfile.value} for UID: $userId")
                } else {
                    _userProfile.value = UserProfile() // Set to default empty profile
                    Log.d("AuthViewModel", "No profile found in Firestore for UID: $userId. Setting empty.")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error loading user profile from Firestore: ${e.message}", e)
                _userProfile.value = UserProfile() // Set to default empty profile on error
            }
        } else {
            _userProfile.value = UserProfile() // No user signed in, clear profile states
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
                Log.d("AuthViewModel", "Profile saved to Firestore for UID: $userId: $profile")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error saving user profile to Firestore: ${e.message}", e)
            }
        } else {
            Log.w("AuthViewModel", "Cannot save profile: No user signed in.")
        }
    }

    fun isUserSignedIn(): Boolean {
        return auth.currentUser != null
    }

    fun signUp(onSuccess: () -> Unit) { // Correct signature: takes onSuccess
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
                        _userProfile.value = newProfile
                    }
                    onSuccess() // Call the success callback
                    clearInputFields()
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Sign up failed.")
                }
            }
    }

    fun signIn(onSuccess: () -> Unit) { // Correct signature: takes onSuccess
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
                        loadUserProfile()
                    }
                    onSuccess() // Call the success callback
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
            if (!rememberMeState.value) {
                preferencesRepository.saveUserPreferences("", false)
            }
            _userProfile.value = UserProfile()
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

    fun firebaseAuthWithGoogle(idToken: String, onSuccess: () -> Unit) { // Corrected: takes onSuccess
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
                        loadUserProfile()
                        onSuccess() // Call the success callback
                    }
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Google sign-in failed.")
                }
            }
    }

    fun handleGoogleSignInResult(account: GoogleSignInAccount, onSuccess: () -> Unit) { // Corrected: takes onSuccess
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
                        loadUserProfile()
                        onSuccess() // Call the success callback
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

            val updatedProfile = _userProfile.value.copy(
                name = name,
                weight = parsedWeight,
                age = parsedAge,
                targetWeight = parsedTargetWeight
            )
            saveUserProfile(updatedProfile)
            _userProfile.value = updatedProfile
            onSuccess()
        }
    }
}