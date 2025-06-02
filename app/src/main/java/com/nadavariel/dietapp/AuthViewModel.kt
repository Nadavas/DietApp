package com.nadavariel.dietapp

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// To represent the result of an auth operation
sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Loading : AuthResult()
    data object Idle : AuthResult() // Initial state
}

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    val emailState = mutableStateOf("")
    val passwordState = mutableStateOf("")
    val confirmPasswordState = mutableStateOf("") // For sign-up

    private val _authResult = MutableStateFlow<AuthResult>(AuthResult.Idle)
    val authResult: StateFlow<AuthResult> = _authResult

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
        clearInputFields()
    }

    fun clearInputFields() {
        emailState.value = ""
        passwordState.value = ""
        confirmPasswordState.value = ""
    }

    fun resetAuthResult() {
        _authResult.value = AuthResult.Idle
    }
}