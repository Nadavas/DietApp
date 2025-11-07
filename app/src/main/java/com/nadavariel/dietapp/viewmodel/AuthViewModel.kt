@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.viewmodel

import android.content.Context
import android.util.Log
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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.data.UserPreferencesRepository
import com.nadavariel.dietapp.model.Gender
import com.nadavariel.dietapp.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

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

    private var userProfileListener: ListenerRegistration? = null

    init {
        auth.addAuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            userProfileListener?.remove()
            if (currentUser != null) {
                attachUserProfileListener()
            } else {
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

        _userProfile.combine(snapshotFlow { currentUser }) { profile, user ->
            if (user == null) {
                false
            } else {
                val isNameMissing = profile.name.isBlank()
                val isWeightMissing = profile.startingWeight <= 0f // Changed
                val isHeightMissing = profile.height <= 0f

                isNameMissing || isWeightMissing || isHeightMissing
            }
        }
            .distinctUntilChanged()
            .onEach { hasMissing -> _hasMissingPrimaryProfileDetails.value = hasMissing }
            .launchIn(viewModelScope)
    }

    private fun attachUserProfileListener() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            _isLoadingProfile.value = true
            userProfileListener = firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("AuthViewModel", "Error listening to user profile", error)
                        _userProfile.value = UserProfile()
                        _isLoadingProfile.value = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        try {
                            val name = snapshot.getString("name") ?: ""
                            val startingWeight = (snapshot.get("startingWeight") as? Number)?.toFloat() ?: 0f // Changed
                            val height = (snapshot.get("height") as? Number)?.toFloat() ?: 0f
                            val dateOfBirth = snapshot.getDate("dateOfBirth")
                            val avatarId = snapshot.getString("avatarId")
                            val genderString = snapshot.getString("gender")
                            val gender = try {
                                genderString?.let { Gender.valueOf(it) } ?: Gender.UNKNOWN
                            } catch (_: IllegalArgumentException) {
                                Gender.UNKNOWN
                            }

                            _userProfile.value = UserProfile(
                                name = name,
                                startingWeight = startingWeight, // Changed
                                height = height,
                                dateOfBirth = dateOfBirth,
                                avatarId = avatarId,
                                gender = gender,
                            )
                            Log.d("AuthViewModel", "User profile updated from listener.")
                        } catch (e: Exception) {
                            Log.e("AuthViewModel", "Error parsing profile snapshot", e)
                            _userProfile.value = UserProfile()
                        }
                    } else {
                        Log.d("AuthViewModel", "User profile document does not exist.")
                        _userProfile.value = UserProfile()
                    }
                    _isLoadingProfile.value = false
                }
        } else {
            _userProfile.value = UserProfile()
            _isLoadingProfile.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        userProfileListener?.remove()
    }

    private suspend fun saveUserProfile(profile: UserProfile) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userProfileMap = hashMapOf(
                "name" to profile.name,
                "startingWeight" to profile.startingWeight, // Changed
                "height" to profile.height,
                "dateOfBirth" to profile.dateOfBirth,
                "avatarId" to profile.avatarId,
                "gender" to profile.gender.name
            )
            firestore.collection("users").document(userId).set(userProfileMap).await()
            Log.d("AuthViewModel", "saveUserProfile called.")
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
                        val newProfile = UserProfile(
                            name = auth.currentUser?.displayName ?: emailState.value.substringBefore("@"),
                            startingWeight = 0f, // Changed
                            height = 0f,
                            dateOfBirth = null,
                            avatarId = null,
                            gender = Gender.UNKNOWN
                        )
                        saveUserProfile(newProfile)
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
        userProfileListener?.remove()
        userProfileListener = null
        auth.signOut()
        _authResult.value = AuthResult.Idle
        viewModelScope.launch {
            if (!rememberMeState.value) {
                preferencesRepository.saveUserPreferences("", false)
            }
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
                                val newProfile = UserProfile(
                                    name = task.result.user?.displayName ?: ""
                                    // other defaults are set in UserProfile
                                )
                                saveUserProfile(newProfile)
                            }
                        }
                        onSuccess()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Google sign-in failed."
                    _authResult.value = AuthResult.Error(errorMessage)
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
                                val newProfile = UserProfile(
                                    name = account.displayName ?: ""
                                    // other defaults are set in UserProfile
                                )
                                saveUserProfile(newProfile)
                            }
                        }
                        onSuccess()
                    }
                } else {
                    val errorMessage = task.exception?.message ?: "Google sign-in failed."
                    _authResult.value = AuthResult.Error(errorMessage)
                }
            }
    }


    fun updateProfile(
        name: String,
        weight: String, // This string now represents STARTING weight
        height: String,
        dateOfBirth: Date?,
        avatarId: String?,
        gender: Gender,
    ) {
        viewModelScope.launch {
            val parsedStartingWeight = weight.toFloatOrNull() ?: _userProfile.value.startingWeight // Changed
            val parsedHeight = height.toFloatOrNull() ?: _userProfile.value.height

            val updatedProfile = _userProfile.value.copy(
                name = name,
                startingWeight = parsedStartingWeight, // Changed
                height = parsedHeight,
                dateOfBirth = dateOfBirth,
                avatarId = avatarId,
                gender = gender,
            )
            saveUserProfile(updatedProfile)
        }
    }

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
            val userId = user.uid
            _authResult.value = AuthResult.Loading

            viewModelScope.launch {
                try {
                    // 1. DELETE FIRESTORE DATA FIRST (while user is still authenticated)
                    // Note: This won't delete sub-collections.
                    firestore.collection("users").document(userId).delete().await()
                    Log.d("AuthViewModel", "Successfully deleted user data from Firestore.")

                    // 2. NOW DELETE THE AUTH USER
                    user.delete().await()
                    Log.d("AuthViewModel", "Successfully deleted user from Firebase Auth.")

                    // 3. Clean up local preferences
                    preferencesRepository.clearUserPreferences()
                    _authResult.value = AuthResult.Success
                    onSuccess() // AuthStateListener will handle resetting the user profile

                } catch (e: Exception) {
                    val errorMessage = e.message ?: "Failed to delete account."
                    Log.e("AuthViewModel", "Error deleting account: $errorMessage", e)
                    _authResult.value = AuthResult.Error(errorMessage)

                    if (e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                        onError("re-authenticate-required")
                    } else if (e is com.google.firebase.firestore.FirebaseFirestoreException && e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        // This error might still happen if your rules are very strict
                        onError("Permission denied. Could not delete user data.")
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