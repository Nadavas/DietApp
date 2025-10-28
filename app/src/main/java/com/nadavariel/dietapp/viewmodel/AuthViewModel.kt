@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration // Import ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.data.UserPreferencesRepository
import com.nadavariel.dietapp.model.Gender
import com.nadavariel.dietapp.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import android.util.Log // Import Log

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
    val authResult: StateFlow<AuthResult> = _authResult.asStateFlow()

    val rememberMeState = mutableStateOf(false)

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    var currentUser: FirebaseUser? by mutableStateOf(null)
        private set

    val isEmailPasswordUser: Boolean
        get() = currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true

    private val _isDarkModeEnabled = MutableStateFlow(false)
    val isDarkModeEnabled: StateFlow<Boolean> = _isDarkModeEnabled.asStateFlow()

    private val _hasMissingPrimaryProfileDetails = MutableStateFlow(false)
    val hasMissingPrimaryProfileDetails: StateFlow<Boolean> = _hasMissingPrimaryProfileDetails.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(true)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    val currentAvatarId: String? get() = userProfile.value.avatarId

    // Variable to hold the listener registration so we can remove it later
    private var userProfileListener: ListenerRegistration? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            // Remove previous listener if user changes or logs out
            userProfileListener?.remove()
            if (currentUser != null) {
                // If user is logged in, attach the persistent listener
                attachUserProfileListener()
            } else {
                // If user logged out, reset profile and loading state
                _userProfile.value = UserProfile()
                _isLoadingProfile.value = false
            }
        }

        viewModelScope.launch {
            emailState.value = preferencesRepository.userEmailFlow.first()
            rememberMeState.value = preferencesRepository.rememberMeFlow.first()

            preferencesRepository.darkModeEnabledFlow.collect { isEnabled ->
                _isDarkModeEnabled.value = isEnabled
            }
        }

        // Combine flow for missing details (no changes needed here)
        _userProfile.combine(snapshotFlow { currentUser }) { profile, user ->
            if (user == null) {
                false
            } else {
                profile.name.isBlank() || profile.weight <= 0f || profile.height <= 0f
            }
        }
            .distinctUntilChanged()
            .onEach { hasMissing -> _hasMissingPrimaryProfileDetails.value = hasMissing }
            .launchIn(viewModelScope)
    }

    // FIX: Replaced loadUserProfile with a persistent listener
    private fun attachUserProfileListener() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            _isLoadingProfile.value = true // Start loading
            userProfileListener = firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("AuthViewModel", "Error listening to user profile", error)
                        _userProfile.value = UserProfile() // Reset on error
                        _isLoadingProfile.value = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        try {
                            // Parse data from snapshot
                            val name = snapshot.getString("name") ?: ""
                            val weight = (snapshot.get("weight") as? Number)?.toFloat() ?: 0f
                            val height = (snapshot.get("height") as? Number)?.toFloat() ?: 0f
                            val dateOfBirth = snapshot.getDate("dateOfBirth")
                            val avatarId = snapshot.getString("avatarId")
                            val genderString = snapshot.getString("gender")
                            val gender = try {
                                genderString?.let { Gender.valueOf(it) } ?: Gender.UNKNOWN
                            } catch (e: IllegalArgumentException) {
                                Gender.UNKNOWN
                            }

                            // Update the StateFlow
                            _userProfile.value = UserProfile(
                                name = name,
                                weight = weight,
                                height = height,
                                dateOfBirth = dateOfBirth,
                                avatarId = avatarId,
                                gender = gender,
                            )
                            Log.d("AuthViewModel", "User profile updated from listener.")
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error parsing profile snapshot", e)
                            _userProfile.value = UserProfile() // Reset on parsing error
                        }
                    } else {
                        Log.d("AuthViewModel", "User profile document does not exist.")
                        _userProfile.value = UserProfile() // Reset if document deleted
                    }
                    _isLoadingProfile.value = false // Stop loading
                }
        } else {
            // Should not happen if currentUser is not null, but handle anyway
            _userProfile.value = UserProfile()
            _isLoadingProfile.value = false
        }
    }

    // Call this when the ViewModel is cleared to prevent leaks
    override fun onCleared() {
        super.onCleared()
        userProfileListener?.remove() // Detach the listener
    }

    // saveUserProfile is still needed for initial profile creation and manual updates
    private suspend fun saveUserProfile(profile: UserProfile) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userProfileMap = hashMapOf(
                "name" to profile.name,
                "weight" to profile.weight,
                "height" to profile.height,
                "dateOfBirth" to profile.dateOfBirth,
                "avatarId" to profile.avatarId,
                "gender" to profile.gender.name // Save enum name as string
            )
            // Use set without merge here, assuming we want to overwrite fully on save
            firestore.collection("users").document(userId).set(userProfileMap).await()
            // No need to manually update _userProfile.value here, listener will handle it
            // _userProfile.value = profile
            Log.d("AuthViewModel", "saveUserProfile called.")
        }
    }

    // --- Other functions (signIn, signUp, signOut, etc.) remain unchanged ---
    // Make sure they call saveUserProfile or rely on the listener appropriately.
    // Specifically, ensure that after Google Sign-In, if loadUserProfile was called,
    // it's now handled by the listener being attached.

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
                        // Create initial profile
                        val newProfile = UserProfile(
                            name = auth.currentUser?.displayName ?: emailState.value.substringBefore("@"),
                            // Set other fields to defaults or leave unset
                            weight = 0f,
                            height = 0f,
                            dateOfBirth = null,
                            avatarId = null,
                            gender = Gender.UNKNOWN
                        )
                        saveUserProfile(newProfile) // This write will be picked up by the listener
                    }
                    onSuccess()
                    clearInputFields()
                } else {
                    val errorMessage = task.exception?.message ?: "Sign up failed."
                    _authResult.value = AuthResult.Error(errorMessage)
                }
            }
    }

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
                        // Listener will handle profile loading via AuthStateListener
                    }
                    onSuccess()
                    clearInputFields()
                } else {
                    val errorMessage = task.exception?.message ?: "Sign in failed."
                    _authResult.value = AuthResult.Error(errorMessage)
                }
            }
    }
    fun signIn(onSuccess: () -> Unit) {
        signIn(emailState.value, passwordState.value, onSuccess)
    }

    fun signOut() {
        userProfileListener?.remove() // Detach listener on sign out
        userProfileListener = null
        auth.signOut()
        _authResult.value = AuthResult.Idle
        viewModelScope.launch {
            if (!rememberMeState.value) {
                preferencesRepository.saveUserPreferences("", false)
            }
            // _userProfile is reset by AuthStateListener
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

    // Simplified Google Sign In - relies on AuthStateListener to trigger listener attachment
    fun firebaseAuthWithGoogle(idToken: String, onSuccess: () -> Unit) {
        _authResult.value = AuthResult.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = AuthResult.Success
                    viewModelScope.launch {
                        // Check if profile exists, create if not
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            val userDoc = firestore.collection("users").document(userId).get().await()
                            if (!userDoc.exists()) {
                                val newProfile = UserProfile(
                                    name = task.result.user?.displayName ?: "",
                                    // other defaults
                                )
                                saveUserProfile(newProfile) // Listener will pick this up
                            }
                            // Listener already attached by AuthStateListener, no need to load manually
                        }
                        onSuccess() // Call onSuccess after checking/creating profile
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Google sign-in failed."
                    _authResult.value = AuthResult.Error(errorMessage)
                }
            }
    }

    // Simplified Google Sign In Result Handler
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
                        // Check if profile exists, create if not
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            val userDoc = firestore.collection("users").document(userId).get().await()
                            if (!userDoc.exists()) {
                                val newProfile = UserProfile(
                                    name = account.displayName ?: "",
                                    // other defaults
                                )
                                saveUserProfile(newProfile) // Listener will pick this up
                            }
                            // Listener already attached by AuthStateListener, no need to load manually
                        }
                        onSuccess() // Call onSuccess after checking/creating profile
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Google sign-in failed."
                    _authResult.value = AuthResult.Error(errorMessage)
                }
            }
    }


    fun updateProfile(
        name: String,
        weight: String,
        height: String,
        dateOfBirth: Date?,
        avatarId: String?,
        gender: Gender,
    ) {
        viewModelScope.launch {
            val parsedWeight = weight.toFloatOrNull() ?: _userProfile.value.weight // Keep old if invalid
            val parsedHeight = height.toFloatOrNull() ?: _userProfile.value.height // Keep old if invalid

            val updatedProfile = _userProfile.value.copy(
                name = name,
                weight = parsedWeight,
                height = parsedHeight,
                dateOfBirth = dateOfBirth,
                avatarId = avatarId,
                gender = gender,
            )
            // Save using the existing method, listener will update the state flow
            saveUserProfile(updatedProfile)
        }
    }

    // changePassword, deleteCurrentUser, toggleDarkMode remain unchanged

    fun changePassword(newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            if (newPassword.isBlank() || newPassword.length < 6) {
                onError("Password must be at least 6 characters long.")
                return
            }
            _authResult.value = AuthResult.Loading
            user.updatePassword(newPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _authResult.value = AuthResult.Success
                        onSuccess()
                    } else {
                        val errorMessage = task.exception?.message ?: "Failed to change password."
                        _authResult.value = AuthResult.Error(errorMessage)
                        if (task.exception is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                            onError("re-authenticate-required")
                        } else {
                            onError(errorMessage)
                        }
                    }
                }
        } else {
            val noUserError = "No user is currently signed in to change password."
            _authResult.value = AuthResult.Error(noUserError)
            onError(noUserError)
        }
    }

    fun deleteCurrentUser(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid // Capture UID before deletion attempt
            _authResult.value = AuthResult.Loading
            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        viewModelScope.launch {
                            try {
                                firestore.collection("users").document(userId).delete().await()
                                // No need to reset _userProfile, AuthStateListener handles it
                                preferencesRepository.clearUserPreferences()
                                _authResult.value = AuthResult.Success // Set success after Firestore delete
                                onSuccess()
                            } catch (e: Exception) {
                                val firestoreError = "Account deleted, but failed to delete associated data: ${e.message}"
                                _authResult.value = AuthResult.Error(firestoreError)
                                onError(firestoreError)
                            }
                        }
                    } else {
                        val errorMessage = task.exception?.message ?: "Failed to delete account."
                        _authResult.value = AuthResult.Error(errorMessage)

                        if (task.exception is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                            onError("re-authenticate-required")
                        } else {
                            onError(errorMessage)
                        }
                    }
                }
        } else {
            val noUserError = "No user is currently signed in to delete."
            _authResult.value = AuthResult.Error(noUserError)
            onError(noUserError)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.saveDarkModePreference(enabled)
        }
    }
}