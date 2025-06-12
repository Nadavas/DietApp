package com.nadavariel.dietapp

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.nadavariel.dietapp.data.UserPreferencesRepository // Import your new repository
import kotlinx.coroutines.flow.first // Import for .first()
import kotlinx.coroutines.launch

// To represent the result of an auth operation
sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
    data object Idle : AuthResult() // Initial state
}

class AuthViewModel(private val preferencesRepository: UserPreferencesRepository) : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    val emailState = mutableStateOf("")
    val passwordState = mutableStateOf("")
    val confirmPasswordState = mutableStateOf("") // For sign-up

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult

    // New state for "Remember Me" toggle
    val rememberMeState = mutableStateOf(false)

    // Initialize email and rememberMe from DataStore when ViewModel is created
    init {
        viewModelScope.launch {
            emailState.value = preferencesRepository.userEmailFlow.first()
            rememberMeState.value = preferencesRepository.rememberMeFlow.first()
            // If rememberMeState is true, you might want to automatically sign in if Firebase remembers,
            // or just pre-fill fields. For now, we'll just pre-fill.
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
                    // Save email if rememberMe is true after successful signup
                    viewModelScope.launch {
                        if (rememberMeState.value) {
                            preferencesRepository.saveUserPreferences(emailState.value, rememberMeState.value)
                        } else {
                            // If rememberMe is false, clear any stored email
                            preferencesRepository.clearUserPreferences()
                        }
                    }
                    onSuccess() // Navigate on success
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
                    // Save email if rememberMe is true after successful signin
                    viewModelScope.launch {
                        if (rememberMeState.value) {
                            preferencesRepository.saveUserPreferences(emailState.value, rememberMeState.value)
                        } else {
                            // If rememberMe is false, clear any stored email
                            preferencesRepository.clearUserPreferences()
                        }
                    }
                    onSuccess() // Navigate on success
                    clearInputFields()
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Sign in failed.")
                }
            }
    }

    fun signOut() {
        auth.signOut()
        _authResult.value = AuthResult.Idle // Reset state after sign out
        // Clear preferences only if the user explicitly signs out AND rememberMe was false
        // or if you want to force clear on any sign out.
        viewModelScope.launch {
            // preferencesRepository.clearUserPreferences() // Uncomment if you want to clear all on signOut
            // Alternatively, just clear the email if rememberMe is false
            if (!rememberMeState.value) {
                preferencesRepository.saveUserPreferences("", false) // Clear email, keep rememberMe false
            }
        }
        clearInputFields()
    }

    fun clearInputFields() {
        // Only clear email if rememberMe is false, otherwise keep it for pre-filling
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
            .requestIdToken(context.getString(R.string.default_web_client_id)) // this must match the one in Firebase
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    // 🔥 Google Sign-In handler
    fun firebaseAuthWithGoogle(idToken: String) {
        _authResult.value = AuthResult.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authResult.value = AuthResult.Success
                    // For Google Sign-In, Firebase handles session.
                    // If you want to remember Google user's email, you'd save it here too.
                    // preferencesRepository.saveUserPreferences(auth.currentUser?.email ?: "", true) // You can uncomment this if needed
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
                    // Save Google account's email if rememberMe is true (or just for consistency)
                    viewModelScope.launch {
                        if (rememberMeState.value) {
                            preferencesRepository.saveUserPreferences(account.email ?: "", rememberMeState.value)
                        } else {
                            preferencesRepository.clearUserPreferences()
                        }
                    }
                    onSuccess()
                } else {
                    _authResult.value = AuthResult.Error(task.exception?.message ?: "Google sign-in failed.")
                }
            }
    }
}
