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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val authResult by authViewModel.authResult.collectAsStateWithLifecycle()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Handle authentication results
    LaunchedEffect(authResult) {
        when (val result = authResult) {
            AuthResult.Success -> {
                errorMessage = null
                currentPassword = ""
                newPassword = ""
                confirmNewPassword = ""
                authViewModel.resetAuthResult()
                navController.popBackStack()
            }
            is AuthResult.Error -> {
                // We no longer need the "re-authenticate-required" check
                // The error message from the VM will be descriptive (e.g., "Wrong password")
                errorMessage = result.message
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

            // Only show the fields if the user is an email/password user
            if (authViewModel.isEmailPasswordUser) {
                // --- 1. ADDED CURRENT PASSWORD FIELD ---
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // New password field
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

                // Confirm new password field
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

                // Change password button
                Button(
                    onClick = {
                        errorMessage = null
                        // --- 2. UPDATED VALIDATION ---
                        if (currentPassword.isBlank() || newPassword.isBlank() || confirmNewPassword.isBlank()) {
                            errorMessage = "Please fill in all password fields."
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

                        // --- 3. UPDATED VIEWMODEL CALL ---
                        authViewModel.changePassword(
                            oldPassword = currentPassword,
                            newPassword = newPassword,
                            onSuccess = { /* Handled by LaunchedEffect */ },
                            onError = { /* Handled by LaunchedEffect */ }
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
            } else {
                // Show this message for Google Sign-In users
                Text(
                    text = "Password management is not available for accounts created with Google Sign-In.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // --- 4. REMOVED THE RE-AUTHENTICATION DIALOG ---
    // (It's no longer needed as we proactively ask for the password)
}