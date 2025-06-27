package com.nadavariel.dietapp.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.AuthResult
import com.nadavariel.dietapp.AuthViewModel
import com.nadavariel.dietapp.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    authViewModel: AuthViewModel // ⭐ MODIFIED: Removed = viewModel()
) {
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentUser = authViewModel.currentUser
    val authResult by authViewModel.authResult.collectAsStateWithLifecycle()

    LaunchedEffect(authResult) {
        when (authResult) {
            AuthResult.Success -> {
                errorMessage = null
                authViewModel.signOut()
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
                    errorMessage = "Please re-enter your password to confirm deletion."
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
                title = { Text("Account") },
                navigationIcon = {
                    // Back button is generally not shown for primary bottom navigation screens.
                    // If you intend for this screen to also be reachable via other means where a back button is appropriate, uncomment this:
                    /*
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    */
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile List Item
            ListItem(
                headlineContent = { Text("Profile") },
                leadingContent = {
                    Icon(Icons.Filled.Person, contentDescription = "My Profile")
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go to My Profile")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(NavRoutes.MY_PROFILE) }
            )
            HorizontalDivider()

            // Settings List Item
            ListItem(
                headlineContent = { Text("Settings") },
                leadingContent = {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go to Settings")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(NavRoutes.SETTINGS) }
            )
            HorizontalDivider()

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { showDeleteConfirmationDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Account")
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmationDialog = false
                authViewModel.resetAuthResult()
            },
            title = { Text("Confirm Account Deletion") },
            text = { Text("Are you sure you want to permanently delete your account and all associated data? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirmationDialog = false
                    errorMessage = null
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
                            errorMessage = null
                            authViewModel.signIn(currentEmail, reauthPassword) {
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
                    authViewModel.resetAuthResult()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Keep ListItemWithArrow composable as is
@Composable
fun ListItemWithArrow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Go to $text",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}