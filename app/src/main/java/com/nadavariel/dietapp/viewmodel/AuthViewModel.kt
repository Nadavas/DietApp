@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State // <-- This import is critical
import androidx.compose.runtime.derivedStateOf
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

enum class GoogleSignInFlowResult {
    GoToHome,
    GoToSignUp,
    Error
}

class AuthViewModel(private val preferencesRepository: UserPreferencesRepository) : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore: FirebaseFirestore = Firebase.firestore

    val nameState = mutableStateOf("")
    val emailState = mutableStateOf("")
    val passwordState = mutableStateOf("")
    val confirmPasswordState = mutableStateOf("")
    val selectedAvatarId = mutableStateOf<String?>(null)

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult.asStateFlow()

    val rememberMeState = mutableStateOf(false)

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    var currentUser: FirebaseUser? by mutableStateOf(null)
        private set

    private val _googleAccount = mutableStateOf<GoogleSignInAccount?>(null)
    // This is the correct definition that fixes your errors
    val isGoogleSignUp: State<Boolean> = derivedStateOf { _googleAccount.value != null }

    val isEmailPasswordUser: Boolean
        get() = currentUser?.providerData?.any { it.providerId == EmailAuthProvider.PROVIDER_ID } == true

    private val _hasMissingPrimaryProfileDetails = MutableStateFlow(false)
    val hasMissingPrimaryProfileDetails: StateFlow<Boolean> = _hasMissingPrimaryProfileDetails.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(true)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

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
        }

        _userProfile.combine(snapshotFlow { currentUser }) { profile, user ->
            if (user == null) {
                false
            } else {
                val isNameMissing = profile.name.isBlank()
                val isWeightMissing = profile.startingWeight <= 0f
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

                            _userProfile.value = UserProfile(
                                name = name,
                                startingWeight = startingWeight,
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

    suspend fun saveUserProfile(profile: UserProfile) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userProfileMap = hashMapOf(
                "name" to profile.name,
                "startingWeight" to profile.startingWeight,
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
        if (isGoogleSignUp.value) { // Use .value
            if (nameState.value.isBlank()) {
                _authResult.value = AuthResult.Error("Name cannot be empty.")
                return
            }
        } else {
            if (nameState.value.isBlank() || emailState.value.isBlank() || passwordState.value.isBlank() || confirmPasswordState.value.isBlank()) {
                _authResult.value = AuthResult.Error("Name, email and passwords cannot be empty.")
                return
            }
            if (passwordState.value != confirmPasswordState.value) {
                _authResult.value = AuthResult.Error("Passwords do not match.")
                return
            }
        }

        _authResult.value = AuthResult.Idle
        onSuccess()
    }

    suspend fun createEmailUserAndProfile() {
        if (emailState.value.isBlank() || passwordState.value.isBlank()) {
            throw Exception("Email or password was blank.")
        }
        _authResult.value = AuthResult.Loading

        val newName = nameState.value.ifBlank { emailState.value.substringBefore("@") }
        val newAvatarId = selectedAvatarId.value
        val emailForPrefs = emailState.value

        auth.createUserWithEmailAndPassword(emailState.value.trim(), passwordState.value.trim()).await()

        _authResult.value = AuthResult.Success

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
            avatarId = newAvatarId,
            gender = Gender.UNKNOWN
        )
        saveUserProfile(newProfile)

        // Clear the temporary sign-up state
        clearInputFields()
    }

    suspend fun createGoogleUserAndProfile() {
        // Get the account details we stored in handleGoogleSignIn
        val account = _googleAccount.value ?: throw Exception("Google account not found.")
        _authResult.value = AuthResult.Loading

        // The user was already signed in to Firebase Auth in handleGoogleSignIn.
        // We just need to create their Firestore profile document.

        _authResult.value = AuthResult.Success

        if (rememberMeState.value) {
            preferencesRepository.saveUserPreferences(account.email ?: "", rememberMeState.value)
        } else {
            preferencesRepository.clearUserPreferences()
        }

        Log.d("AuthViewModel", "createGoogleUserAndProfile: Reading nameState='${nameState.value}', selectedAvatarId='${selectedAvatarId.value}'")

        // Create the profile using the name and avatar from the ViewModel state
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

        // Clear the temporary sign-up state (this sets _googleAccount.value to null)
        clearInputFields()
    }


    fun signIn(email: String, password: String, onSuccess: (isNewUser: Boolean) -> Unit) {
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
                    onSuccess(false)
                    clearInputFields()
                } else {
                    val errorMessage = task.exception?.message ?: "Sign in failed."
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
                // Step 1: Sign in to Auth
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                val authResultTask = auth.signInWithCredential(credential).await()

                val userId = authResultTask.user?.uid
                if (userId == null) throw Exception("Failed to get user ID.")

                // Step 2: Check Firestore for existing profile
                val userDoc = firestore.collection("users").document(userId).get().await()

                if (userDoc.exists()) {
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
                    nameState.value = account.displayName ?: ""
                    emailState.value = account.email ?: ""

                    // 2. DO NOT create profile yet. QuestionsViewModel will do it later.

                    // 3. Signal UI to navigate
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


    fun updateProfile(
        name: String,
        weight: String,
        height: String,
        dateOfBirth: Date?,
        avatarId: String?,
        gender: Gender,
    ) {
        viewModelScope.launch {
            val parsedStartingWeight = weight.toFloatOrNull() ?: _userProfile.value.startingWeight
            val parsedHeight = height.toFloatOrNull() ?: _userProfile.value.height

            val updatedProfile = _userProfile.value.copy(
                name = name,
                startingWeight = parsedStartingWeight,
                height = parsedHeight,
                dateOfBirth = dateOfBirth,
                avatarId = avatarId,
                gender = gender,
            )
            saveUserProfile(updatedProfile)
        }
    }

    fun changePassword(
        oldPassword: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser
        val email = user?.email

        if (user == null || email == null) {
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

        val credential = EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                    if (updateTask.isSuccessful) {
                        _authResult.value = AuthResult.Success
                        onSuccess()
                    } else {
                        val errorMessage = updateTask.exception?.message ?: "Failed to update password."
                        _authResult.value = AuthResult.Error(errorMessage)
                        onError(errorMessage)
                    }
                }
            } else {
                val errorMessage = reauthTask.exception?.message ?: "Incorrect current password."
                _authResult.value = AuthResult.Error(errorMessage)
                onError(errorMessage)
            }
        }
    }

    private suspend fun deleteSubCollection(userId: String, collectionName: String) {
        try {
            val collectionRef = firestore.collection("users").document(userId).collection(collectionName)
            val querySnapshot = collectionRef.get().await()
            val batch = firestore.batch()
            for (document in querySnapshot.documents) {
                batch.delete(document.reference)
            }
            batch.commit().await()
            Log.d("AuthViewModel", "Successfully deleted sub-collection: $collectionName")
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error deleting sub-collection $collectionName", e)
        }
    }

    fun deleteCurrentUser(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            _authResult.value = AuthResult.Loading

            viewModelScope.launch {
                try {
                    Log.d("AuthViewModel", "Starting Firestore data deletion for user: $userId")

                    deleteSubCollection(userId, "meals")
                    deleteSubCollection(userId, "weight_history")
                    deleteSubCollection(userId, "notifications")

                    firestore.collection("users").document(userId)
                        .collection("user_answers").document("diet_habits")
                        .delete().await()
                    Log.d("AuthViewModel", "Deleted user_answers/diet_habits")

                    firestore.collection("users").document(userId)
                        .collection("user_answers").document("goals")
                        .delete().await()
                    Log.d("AuthViewModel", "Deleted user_answers/goals")

                    firestore.collection("users").document(userId)
                        .collection("diet_plans").document("current_plan")
                        .delete().await()
                    Log.d("AuthViewModel", "Deleted diet_plans/current_plan")

                    firestore.collection("users").document(userId)
                        .collection("preferences").document("graph_order")
                        .delete().await()
                    Log.d("AuthViewModel", "Deleted preferences/graph_order")

                    firestore.collection("users").document(userId).delete().await()
                    Log.d("AuthViewModel", "Successfully deleted main user document.")

                    user.delete().await()
                    Log.d("AuthViewModel","Successfully deleted user from Firebase Auth.")

                    preferencesRepository.clearUserPreferences()
                    _authResult.value = AuthResult.Success
                    onSuccess()

                } catch (e: Exception) {
                    val errorMessage = e.message ?: "Failed to delete account."
                    Log.e("AuthViewModel", "Error deleting account: $errorMessage", e)
                    _authResult.value = AuthResult.Error(errorMessage)

                    if (e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                        onError("re-authenticate-required")
                    } else if (e is com.google.firebase.firestore.FirebaseFirestoreException && e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
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
}