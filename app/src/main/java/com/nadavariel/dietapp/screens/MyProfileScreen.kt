package com.nadavariel.dietapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nadavariel.dietapp.AuthViewModel
import com.nadavariel.dietapp.AuthResult
import com.nadavariel.dietapp.NavRoutes
// ⭐ Use this import:
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Correct import for StateFlow in Compose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    authViewModel: AuthViewModel,
    navController: NavController
) {
    // ⭐ These are already `mutableStateOf` in AuthViewModel, so direct access is correct.
    val currentUser = authViewModel.currentUser
    val userProfile = authViewModel.userProfile

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) } // To show errors on the screen directly

    // ⭐ Correct way to collect StateFlow from ViewModel
    val authResult by authViewModel.authResult.collectAsStateWithLifecycle()


    LaunchedEffect(authResult) {
        when (authResult) {
            AuthResult.Success -> {
                errorMessage = null // Clear any previous error
                // Account deleted successfully, navigate to landing
                authViewModel.signOut() // Ensure ViewModel state is reset
                navController.navigate(NavRoutes.LANDING) {
                    popUpTo(NavRoutes.HOME) { inclusive = true }
                    launchSingleTop = true
                }
                authViewModel.resetAuthResult()
            }
            is AuthResult.Error -> {
                val error = (authResult as AuthResult.Error).message
                if (error == "re-authenticate-required") {
                    showReauthDialog = true
                    errorMessage = "Please re-enter your password to confirm deletion." // Specific message for user
                } else {
                    errorMessage = error // Display generic error
                }
            }
            AuthResult.Loading -> {
                errorMessage = null // Clear errors when loading
            }
            AuthResult.Idle -> {
                errorMessage = null // Clear errors when idle
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Profile Details",
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // ⭐ Ensure these properties are correctly accessed from userProfile object
            ProfileDetailRow("Name:", userProfile.name.ifEmpty { "Not set" })
            ProfileDetailRow("Email:", currentUser?.email ?: "N/A")
            ProfileDetailRow("Current Weight:", if (userProfile.weight > 0f) "${userProfile.weight} kg" else "Not set")
            ProfileDetailRow("Age:", if (userProfile.age > 0) userProfile.age.toString() else "Not set")
            ProfileDetailRow("Target Weight:", if (userProfile.targetWeight > 0f) "${userProfile.targetWeight} kg" else "Not set")

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate(NavRoutes.UPDATE_PROFILE) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Profile")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delete Account Button
            Button(
                onClick = { showDeleteConfirmationDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Account")
            }

            // Display Error Message
            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmationDialog = false
                authViewModel.resetAuthResult() // Reset if user cancels dialog
            },
            title = { Text("Confirm Account Deletion") },
            text = { Text("Are you sure you want to permanently delete your account and all associated data? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirmationDialog = false
                    errorMessage = null // Clear error when attempting delete
                    authViewModel.deleteCurrentUser(
                        onSuccess = { /* Handled by LaunchedEffect */ },
                        onError = { /* Handled by LaunchedEffect */ }
                    )
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Re-authentication Dialog
    if (showReauthDialog) {
        AlertDialog(
            onDismissRequest = {
                showReauthDialog = false
                reauthPassword = ""
                authViewModel.resetAuthResult() // Reset if user dismisses reauth dialog
            },
            title = { Text("Re-authentication Required") },
            text = {
                Column {
                    Text(errorMessage ?: "Please re-enter your password.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        label = { Text("Password") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentEmail = currentUser?.email
                        if (currentEmail != null && reauthPassword.isNotBlank()) {
                            errorMessage = null // Clear any input-related errors
                            // ⭐ This is the call that might be problematic:
                            authViewModel.signIn(currentEmail, reauthPassword) {
                                // If signIn (re-auth) is successful, retry deletion
                                showReauthDialog = false
                                reauthPassword = ""
                                authViewModel.deleteCurrentUser(
                                    onSuccess = { /* Handled by LaunchedEffect */ },
                                    onError = { /* Handled by LaunchedEffect */ }
                                )
                            }
                        } else {
                            errorMessage = "Please enter your password."
                        }
                    },
                    enabled = reauthPassword.isNotBlank()
                ) {
                    Text("Confirm & Delete")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showReauthDialog = false
                    reauthPassword = ""
                    authViewModel.resetAuthResult() // Reset if user cancels reauth
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.6f)
        )
    }
}