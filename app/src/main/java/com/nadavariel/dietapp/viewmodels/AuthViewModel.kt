@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ListenerRegistration
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.models.Gender
import com.nadavariel.dietapp.models.UserProfile
import com.nadavariel.dietapp.repositories.AuthRepository
import com.nadavariel.dietapp.repositories.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
    data object Idle : AuthResult()
}

enum class GoogleSignInFlowResult {
    GoToHome,
    GoToSignUp,
    Error
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val nameState = mutableStateOf("")
    val emailState = mutableStateOf("")
    val passwordState = mutableStateOf("")
    val confirmPasswordState = mutableStateOf("")
    val selectedAvatarId = mutableStateOf<String?>(null)
    val rememberMeState = mutableStateOf(false)

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    var currentUser: FirebaseUser? by mutableStateOf(null)
        private set

    private val _googleAccount = mutableStateOf<GoogleSignInAccount?>(null)
    val isGoogleSignUp: State<Boolean> = derivedStateOf { _googleAccount.value != null }

    val isEmailPasswordUser: Boolean
        get() = currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true

    private val _isLoadingProfile = MutableStateFlow(true)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    private var userProfileListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    private val _hasDismissedPlanTip = MutableStateFlow(true)
    val hasDismissedPlanTip: StateFlow<Boolean> = _hasDismissedPlanTip.asStateFlow()

    private val _isPreferencesLoaded = MutableStateFlow(false)
    val isPreferencesLoaded: StateFlow<Boolean> = _isPreferencesLoaded.asStateFlow()

    init {
        authStateListener = authRepository.addAuthStateListener { firebaseAuth ->
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

            preferencesRepository.hasDismissedPlanTipFlow.collect { dismissed ->
                _hasDismissedPlanTip.value = dismissed
                _isPreferencesLoaded.value = true
            }
        }
    }

    fun dismissPlanTip() {
        viewModelScope.launch {
            _hasDismissedPlanTip.value = true
            preferencesRepository.setHasDismissedPlanTip(true)
        }
    }

    private fun attachUserProfileListener() {
        val userId = authRepository.currentUser?.uid
        if (userId != null) {
            _isLoadingProfile.value = true

            userProfileListener = authRepository.addUserProfileListener(
                userId = userId,
                onProfileUpdated = { profile ->
                    _userProfile.value = profile
                    _isLoadingProfile.value = false
                    Log.d("AuthViewModel", "User profile updated from listener.")
                },
                onError = { error ->
                    Log.e("AuthViewModel", "Error listening to user profile", error)
                    _userProfile.value = UserProfile()
                    _isLoadingProfile.value = false
                }
            )
        } else {
            _userProfile.value = UserProfile()
            _isLoadingProfile.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        userProfileListener?.remove()
        authStateListener?.let { authRepository.removeAuthStateListener(it) }
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val userId = authRepository.currentUser?.uid
        if (userId != null) {
            try {
                authRepository.saveUserProfile(userId, profile)
                Log.d("AuthViewModel", "saveUserProfile called.")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Failed to save profile", e)
            }
        }
    }

    fun isUserSignedIn(): Boolean {
        return authRepository.currentUser != null
    }

    suspend fun createEmailUserAndProfile() {
        if (emailState.value.isBlank() || passwordState.value.isBlank()) {
            throw Exception("Email or password was blank.")
        }
        _authResult.value = AuthResult.Loading

        val newName = nameState.value.ifBlank { emailState.value.substringBefore("@") }
        val emailForPrefs = emailState.value

        try {
            authRepository.createUserWithEmailAndPassword(emailState.value.trim(), passwordState.value.trim())

            if (rememberMeState.value) {
                preferencesRepository.saveUserPreferences(emailForPrefs, rememberMeState.value)
            } else {
                preferencesRepository.clearUserPreferences()
            }

            val newProfile = UserProfile(
                name = newName,
                startingWeight = 0f,
                height = 0f,
                dateOfBirth = null,
                avatarId = selectedAvatarId.value,
                gender = Gender.UNKNOWN
            )
            saveUserProfile(newProfile)

            _authResult.value = AuthResult.Success
        } catch (e: Exception) {
            _authResult.value = AuthResult.Error(e.message ?: "Registration failed")
        }
    }

    suspend fun createGoogleUserAndProfile() {
        val account = _googleAccount.value ?: throw Exception("Google account not found.")
        _authResult.value = AuthResult.Loading

        try {
            if (rememberMeState.value) {
                preferencesRepository.saveUserPreferences(account.email ?: "", rememberMeState.value)
            } else {
                preferencesRepository.clearUserPreferences()
            }

            Log.d("AuthViewModel", "createGoogleUserAndProfile: Reading nameState='${nameState.value}', selectedAvatarId='${selectedAvatarId.value}'")

            val newProfile = UserProfile(
                name = nameState.value.ifBlank { account.displayName }.toString(),
                startingWeight = 0f,
                height = 0f,
                dateOfBirth = null,
                avatarId = selectedAvatarId.value,
                gender = Gender.UNKNOWN
            )
            Log.d("AuthViewModel", "Saving Google Profile: Name='${newProfile.name}', Avatar='${newProfile.avatarId}'")
            saveUserProfile(newProfile)

            _authResult.value = AuthResult.Success
        } catch (e: Exception) {
            _authResult.value = AuthResult.Error(e.message ?: "Google Profile Creation failed")
        }
    }


    fun signIn(email: String, password: String, onSuccess: (isNewUser: Boolean) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authResult.value = AuthResult.Error("Email and password cannot be empty.")
            return
        }
        _authResult.value = AuthResult.Loading

        viewModelScope.launch {
            try {
                authRepository.signInWithEmailAndPassword(email.trim(), password.trim())
                _authResult.value = AuthResult.Success

                if (rememberMeState.value) {
                    preferencesRepository.saveUserPreferences(email, rememberMeState.value)
                } else {
                    preferencesRepository.clearUserPreferences()
                }

                onSuccess(false)
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Sign in failed."
                _authResult.value = AuthResult.Error(errorMessage)
            }
        }
    }

    fun signOut(context: Context) {
        userProfileListener?.remove()
        userProfileListener = null

        try {
            getGoogleSignInClient(context).signOut()
            Log.d("AuthViewModel", "Successfully signed out of Google Client")
        } catch (e: Exception) {
            Log.w("AuthViewModel", "Could not sign out of Google Client: ${e.message}")
        }

        authRepository.signOut()
        _authResult.value = AuthResult.Idle
        viewModelScope.launch {
            if (!rememberMeState.value) {
                preferencesRepository.saveUserPreferences("", false)
            }
        }
        clearInputFields()
    }

    fun clearInputFields() {
        Log.d("AuthViewModel", "clearInputFields: Clearing all input fields.")
        if (!rememberMeState.value) {
            emailState.value = ""
        }
        nameState.value = ""
        selectedAvatarId.value = null
        passwordState.value = ""
        confirmPasswordState.value = ""
        _googleAccount.value = null
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

    fun handleGoogleSignIn(
        account: GoogleSignInAccount,
        onFlowResult: (GoogleSignInFlowResult) -> Unit
    ) {
        _authResult.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                // Step 1: Sign in to Auth via Repository
                val authResultTask = authRepository.signInWithGoogleCredential(account)

                val userId = authResultTask.user?.uid
                if (userId == null) throw Exception("Failed to get user ID.")

                // Step 2: Check Firestore for existing profile via Repository
                val userExists = authRepository.getUserDocument(userId)

                if (userExists) {
                    // --- EXISTING USER ---
                    Log.d("AuthViewModel", "Google user exists. Logging in.")
                    _authResult.value = AuthResult.Success
                    if (rememberMeState.value) {
                        preferencesRepository.saveUserPreferences(account.email ?: "", rememberMeState.value)
                    }
                    onFlowResult(GoogleSignInFlowResult.GoToHome)
                } else {
                    // --- NEW USER ---
                    Log.d("AuthViewModel", "New Google user. Setting state for Questionnaire.")

                    // 1. Set the state so QuestionsViewModel knows this is a Google Sign Up
                    _googleAccount.value = account

                    // 2. Signal UI to navigate
                    _authResult.value = AuthResult.Idle // Keep Idle so it doesn't trigger 'Success' toast yet
                    onFlowResult(GoogleSignInFlowResult.GoToSignUp)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google Sign-In failed", e)
                _authResult.value = AuthResult.Error(e.message ?: "Sign-In failed.")
                _googleAccount.value = null
                onFlowResult(GoogleSignInFlowResult.Error)
            }
        }
    }

    fun isEmailVerified(): Boolean {
        return authRepository.isEmailVerified()
    }

    fun sendVerificationEmail(onSuccess: () -> Unit, onError: (String) -> Unit) {
        authRepository.sendVerificationEmail(onSuccess, onError)
    }

    suspend fun checkEmailVerificationStatus(): Boolean {
        return try {
            val user = authRepository.currentUser
            if (user == null) {
                Log.d("AuthViewModel", "CheckStatus: No user signed in.")
                return false
            }

            // 1. Force reload from server via Repository
            authRepository.reloadUser()

            // 2. Log the result for debugging
            val isVerified = user.isEmailVerified
            Log.d("AuthViewModel", "CheckStatus: ${user.email} verified=$isVerified")

            return isVerified
        } catch (e: Exception) {
            Log.e("AuthViewModel", "CheckStatus: Error reloading user", e)
            false
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
            val userId = authRepository.currentUser?.uid
            if (userId != null) {
                val finalAvatarId = if (!avatarId.isNullOrBlank()) avatarId else _userProfile.value.avatarId
                val parsedStartingWeight = weight.toFloatOrNull() ?: _userProfile.value.startingWeight
                val parsedHeight = height.toFloatOrNull() ?: _userProfile.value.height

                val updatedProfile = _userProfile.value.copy(
                    name = name,
                    startingWeight = parsedStartingWeight,
                    height = parsedHeight,
                    dateOfBirth = dateOfBirth,
                    avatarId = finalAvatarId,
                    gender = gender,
                )
                saveUserProfile(updatedProfile)
            }
        }
    }

    fun changePassword(
        oldPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isUserSignedIn()) {
            val noUserError = "No user is currently signed in."
            _authResult.value = AuthResult.Error(noUserError)
            onError(noUserError)
            return
        }
        if (oldPassword.isBlank() || newPassword.isBlank()) {
            val emptyError = "Passwords cannot be blank."
            _authResult.value = AuthResult.Error(emptyError)
            onError(emptyError)
            return
        }
        if (newPassword.length < 6) {
            val lengthError = "Password must be at least 6 characters long."
            _authResult.value = AuthResult.Error(lengthError)
            onError(lengthError)
            return
        }

        _authResult.value = AuthResult.Loading

        val credential = authRepository.getEmailCredential(oldPassword)
        if (credential == null) {
            val error = "Could not verify credentials."
            _authResult.value = AuthResult.Error(error)
            onError(error)
            return
        }

        authRepository.reauthenticateUser(credential,
            onSuccess = {
                authRepository.updatePassword(newPassword,
                    onSuccess = {
                        _authResult.value = AuthResult.Success
                        onSuccess()
                    },
                    onError = { msg ->
                        _authResult.value = AuthResult.Error(msg)
                        onError(msg)
                    }
                )
            },
            onError = { msg ->
                _authResult.value = AuthResult.Error(msg)
                onError(msg)
            }
        )
    }

    fun reauthenticateWithGoogle(
        account: GoogleSignInAccount,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val credential = authRepository.getGoogleCredential(account.idToken!!)

        authRepository.reauthenticateUser(credential,
            onSuccess = {
                Log.d("AuthViewModel", "Google re-auth successful.")
                onSuccess()
            },
            onError = { msg ->
                Log.e("AuthViewModel", "Google re-auth failed: $msg")
                onError(msg)
            }
        )
    }

    fun resetUserData(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = authRepository.currentUser
        val userId = user?.uid
        if (userId == null) {
            onError("User not logged in.")
            return
        }

        _authResult.value = AuthResult.Loading
        viewModelScope.launch {
            try {
                // 1. Delete Subcollections (Meals, Weight, Notifications)
                authRepository.deleteUserSubCollections(userId)

                // 2. Delete Plan Data (Habits, Current Plan)
                authRepository.deleteUserPlanData(userId)

                // NOTE: We do NOT delete Goals or the Main Document here.

                Log.d("AuthViewModel", "Successfully reset user data and plan.")
                _authResult.value = AuthResult.Success
                onSuccess()

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error resetting data", e)
                _authResult.value = AuthResult.Error(e.message ?: "Reset failed.")
                onError(e.message ?: "Reset failed.")
            }
        }
    }

    fun deleteCurrentUser(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = authRepository.currentUser
        if (user != null) {
            val userId = user.uid
            _authResult.value = AuthResult.Loading

            viewModelScope.launch {
                try {
                    Log.d("AuthViewModel", "Starting account deletion for: $userId")

                    // 1. Shared Logic (Same as Reset)
                    authRepository.deleteUserSubCollections(userId)
                    authRepository.deleteUserPlanData(userId)

                    // 2. Account Specific Logic
                    authRepository.deleteUserGoals(userId)      // Delete goals
                    authRepository.deleteUserMainDocument(userId) // Delete profile

                    // 3. Delete Auth
                    authRepository.deleteAuthUser()

                    Log.d("AuthViewModel", "Successfully deleted user account completely.")

                    preferencesRepository.clearUserPreferences()
                    _authResult.value = AuthResult.Success
                    onSuccess()

                } catch (e: Exception) {
                    val rawErrorMessage = e.message ?: "Failed to delete account."
                    Log.e("AuthViewModel", "Error deleting account", e)

                    if (e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                        _authResult.value = AuthResult.Error("re-authenticate-required")
                        onError("re-authenticate-required")
                    } else if (e is com.google.firebase.firestore.FirebaseFirestoreException && e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        _authResult.value = AuthResult.Error("Permission denied.")
                        onError("Permission denied. Could not delete user data.")
                    } else {
                        _authResult.value = AuthResult.Error(rawErrorMessage)
                        onError(rawErrorMessage)
                    }
                }
            }
        } else {
            val noUserError = "No user is currently signed in to delete."
            _authResult.value = AuthResult.Error(noUserError)
            onError(noUserError)
        }
    }
}