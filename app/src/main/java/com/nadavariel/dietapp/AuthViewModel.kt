@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp

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
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.data.UserPreferencesRepository
import com.nadavariel.dietapp.model.ActivityLevel
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
        get() = currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } ?: false

    private val _isDarkModeEnabled = MutableStateFlow(false)
    val isDarkModeEnabled: StateFlow<Boolean> = _isDarkModeEnabled.asStateFlow()

    private val _hasMissingPrimaryProfileDetails = MutableStateFlow(false)
    val hasMissingPrimaryProfileDetails: StateFlow<Boolean> = _hasMissingPrimaryProfileDetails.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            viewModelScope.launch {
                if (currentUser != null) {
                    loadUserProfile()
                } else {
                    _userProfile.value = UserProfile() // Reset user profile state on sign out
                }
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
            val missing: Boolean = if (user == null) {
                false // If no user, no profile to be missing
            } else {
                val isNameMissing = profile.name.isBlank()
                val isWeightMissing = profile.weight <= 0f
                val isTargetWeightMissing = profile.targetWeight <= 0f
                val isHeightMissing = profile.height <= 0f // Check for missing height

                // ⭐ MODIFIED: Removed isGenderMissing and isActivityLevelMissing from this check
                val detailsMissing = isNameMissing || isWeightMissing || isTargetWeightMissing || isHeightMissing

                detailsMissing
            }
            missing
        }
            .distinctUntilChanged()
            .onEach { hasMissing ->
                _hasMissingPrimaryProfileDetails.value = hasMissing
            }
            .launchIn(viewModelScope)
    }

    private suspend fun loadUserProfile() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            try {
                val userDoc = firestore.collection("users").document(userId).get().await()
                if (userDoc.exists()) {
                    val name = userDoc.getString("name") ?: ""
                    val weight = (userDoc.get("weight") as? Number)?.toFloat() ?: 0f
                    val height = (userDoc.get("height") as? Number)?.toFloat() ?: 0f
                    val dateOfBirth = userDoc.getDate("dateOfBirth")
                    val targetWeight = (userDoc.get("targetWeight") as? Number)?.toFloat() ?: 0f
                    val avatarId = userDoc.getString("avatarId")
                    val genderString = userDoc.getString("gender")
                    val activityLevelString = userDoc.getString("activityLevel")

                    val gender = try {
                        genderString?.let { Gender.valueOf(it) } ?: Gender.UNKNOWN
                    } catch (e: IllegalArgumentException) {
                        Gender.UNKNOWN
                    }
                    val activityLevel = try {
                        activityLevelString?.let { ActivityLevel.valueOf(it) } ?: ActivityLevel.NOT_SET
                    } catch (e: IllegalArgumentException) {
                        ActivityLevel.NOT_SET
                    }

                    _userProfile.value = UserProfile(
                        name = name,
                        weight = weight,
                        height = height,
                        dateOfBirth = dateOfBirth,
                        targetWeight = targetWeight,
                        avatarId = avatarId,
                        gender = gender,
                        activityLevel = activityLevel
                    )
                } else {
                    _userProfile.value = UserProfile()
                }
            } catch (e: Exception) {
                _userProfile.value = UserProfile()
                // Log the exception if needed: Log.e("AuthViewModel", "Error loading user profile", e)
            }
        } else {
            _userProfile.value = UserProfile()
        }
    }

    private suspend fun saveUserProfile(profile: UserProfile) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userProfileMap = hashMapOf(
                "name" to profile.name,
                "weight" to profile.weight,
                "height" to profile.height,
                "dateOfBirth" to profile.dateOfBirth,
                "targetWeight" to profile.targetWeight,
                "avatarId" to profile.avatarId,
                "gender" to profile.gender.name,
                "activityLevel" to profile.activityLevel.name
            )
            firestore.collection("users").document(userId).set(userProfileMap).await()
            _userProfile.value = profile
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
                            dateOfBirth = null,
                            avatarId = null,
                            height = 0f,
                            gender = Gender.UNKNOWN,
                            activityLevel = ActivityLevel.NOT_SET
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
                                    name = task.result.user?.displayName ?: "",
                                    dateOfBirth = null,
                                    avatarId = null,
                                    height = 0f,
                                    gender = Gender.UNKNOWN,
                                    activityLevel = ActivityLevel.NOT_SET
                                )
                                saveUserProfile(newProfile)
                            } else {
                                loadUserProfile()
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
                                    name = account.displayName ?: "",
                                    dateOfBirth = null,
                                    avatarId = null,
                                    height = 0f,
                                    gender = Gender.UNKNOWN,
                                    activityLevel = ActivityLevel.NOT_SET
                                )
                                saveUserProfile(newProfile)
                            } else {
                                loadUserProfile()
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
        weight: String,
        height: String,
        dateOfBirth: Date?,
        targetWeight: String,
        avatarId: String?,
        gender: Gender,
        activityLevel: ActivityLevel
    ) {
        viewModelScope.launch {
            val parsedWeight = weight.toFloatOrNull() ?: 0f
            val parsedHeight = height.toFloatOrNull() ?: 0f
            val parsedTargetWeight = targetWeight.toFloatOrNull() ?: 0f

            val updatedProfile = _userProfile.value.copy(
                name = name,
                weight = parsedWeight,
                height = parsedHeight,
                dateOfBirth = dateOfBirth,
                targetWeight = parsedTargetWeight,
                avatarId = avatarId,
                gender = gender,
                activityLevel = activityLevel
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
            _authResult.value = AuthResult.Loading
            user.delete()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        viewModelScope.launch {
                            try {
                                firestore.collection("users").document(user.uid).delete().await()
                                _authResult.value = AuthResult.Success
                                _userProfile.value = UserProfile()
                                preferencesRepository.clearUserPreferences()
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