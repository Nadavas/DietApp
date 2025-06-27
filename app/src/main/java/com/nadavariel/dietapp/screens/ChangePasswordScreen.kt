package com.nadavariel.dietapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nadavariel.dietapp.AuthResult
import com.nadavariel.dietapp.AuthViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") } // For re-authentication dialog

    val authResult by authViewModel.authResult.collectAsStateWithLifecycle()

    LaunchedEffect(authResult) {
        when (authResult) {
            AuthResult.Success -> {
                errorMessage = null
                newPassword = ""
                confirmNewPassword = ""
                authViewModel.resetAuthResult() // Reset for next operation
                navController.popBackStack() // Go back to settings on success
            }
            is AuthResult.Error -> {
                val error = (authResult as AuthResult.Error).message
                if (error == "re-authenticate-required") {
                    showReauthDialog = true
                    errorMessage = "Please re-enter your current password to change your password."
                } else {
                    errorMessage = error
                }
            }
            AuthResult.Loading -> {
                errorMessage = null
            }
            AuthResult.Idle -> {
                errorMessage = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmNewPassword,
                onValueChange = { confirmNewPassword = it },
                label = { Text("Confirm New Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    errorMessage = null
                    if (newPassword.isBlank() || confirmNewPassword.isBlank()) {
                        errorMessage = "Please fill in both password fields."
                        return@Button
                    }
                    if (newPassword != confirmNewPassword) {
                        errorMessage = "New passwords do not match."
                        return@Button
                    }
                    if (newPassword.length < 6) {
                        errorMessage = "Password must be at least 6 characters long."
                        return@Button
                    }
                    authViewModel.changePassword(
                        newPassword = newPassword,
                        onSuccess = { /* Handled by LaunchedEffect */ },
                        onError = { errorMsg -> errorMessage = errorMsg }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Change Password")
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    // Re-authentication dialog (similar to the one in AccountScreen for delete)
    if (showReauthDialog) {
        AlertDialog(
            onDismissRequest = {
                showReauthDialog = false
                reauthPassword = ""
                authViewModel.resetAuthResult()
            },
            title = { Text("Re-authentication Required") },
            text = {
                Column {
                    Text(errorMessage ?: "Please re-enter your current password to proceed.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        label = { Text("Current Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentEmail = authViewModel.currentUser?.email
                        if (currentEmail != null && reauthPassword.isNotBlank()) {
                            errorMessage = null
                            authViewModel.signIn(currentEmail, reauthPassword) {
                                showReauthDialog = false
                                reauthPassword = ""
                                // After re-authentication, immediately retry password change
                                authViewModel.changePassword(
                                    newPassword = newPassword,
                                    onSuccess = { /* Handled by LaunchedEffect */ },
                                    onError = { errorMsg -> errorMessage = errorMsg }
                                )
                            }
                        } else {
                            errorMessage = "Please enter your current password."
                        }
                    },
                    enabled = reauthPassword.isNotBlank()
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showReauthDialog = false
                    reauthPassword = ""
                    authViewModel.resetAuthResult()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}