package com.nadavariel.dietapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.ColorFilter
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.NavRoutes
import androidx.compose.ui.res.painterResource
import com.nadavariel.dietapp.R
import androidx.compose.material.icons.filled.Email

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    // Dialog state management
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Get state from the viewmodel
    val currentUser = authViewModel.currentUser
    val authResult by authViewModel.authResult.collectAsStateWithLifecycle()
    val hasMissingDetails by authViewModel.hasMissingPrimaryProfileDetails.collectAsStateWithLifecycle()

    // Handle authentication results
    LaunchedEffect(authResult) {
        when (authResult) {
            AuthResult.Success -> {
                errorMessage = null
                if (showDeleteConfirmationDialog) {
                    authViewModel.signOut()
                    navController.navigate(NavRoutes.LANDING) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
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

    // Main screen layout
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account") }
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

            // Email
            ListItem(
                headlineContent = { Text("Email Address") },
                supportingContent = { Text(currentUser?.email ?: "N/A") },
                leadingContent = {
                    Icon(Icons.Filled.Email, contentDescription = "Email")
                },
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider()

            // Profile
            ListItem(
                headlineContent = { Text("Profile") },
                leadingContent = {
                    Icon(Icons.Filled.Person, contentDescription = "My Profile")
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Red dot for incomplete profile
                        if (hasMissingDetails) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Red, CircleShape)
                                    .align(Alignment.CenterVertically)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go to My Profile")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(NavRoutes.MY_PROFILE) }
            )
            HorizontalDivider()

            // Questions
            ListItem(
                headlineContent = { Text("Questions") },
                leadingContent = {
                    val currentIconColor = LocalContentColor.current
                    Image(
                        painter = painterResource(id = R.drawable.ic_query_filled),
                        contentDescription = "Questions",
                        colorFilter = ColorFilter.tint(currentIconColor)
                    )
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Go to Questions"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(NavRoutes.QUESTIONS) }
            )
            HorizontalDivider()

            // Settings
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

            // Sign out button
            Button(
                onClick = { showSignOutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Sign Out",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Delete account button (in red)
            Button(
                onClick = { showDeleteConfirmationDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Account")
            }

            // Display error messages
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

    // Dialogs
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

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = {
                showSignOutDialog = false
            },
            title = {
                Text(text = "Confirm Sign Out")
            },
            text = {
                Text(text = "Are you sure you want to sign out?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        authViewModel.signOut()
                        navController.navigate(NavRoutes.LANDING) {
                            popUpTo(NavRoutes.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                ) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                    }
                ) {
                    Text("No")
                }
            }
        )
    }

}