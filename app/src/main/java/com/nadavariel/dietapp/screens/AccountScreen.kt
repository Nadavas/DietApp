package com.nadavariel.dietapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val currentUser = authViewModel.currentUser
    val authResult by authViewModel.authResult.collectAsStateWithLifecycle()
    val hasMissingDetails by authViewModel.hasMissingPrimaryProfileDetails.collectAsStateWithLifecycle()

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }

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
            else -> errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Account") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Email card
            AccountCard(
                title = "Email Address",
                subtitle = currentUser?.email ?: "N/A",
                leading = { Icon(Icons.Filled.Email, contentDescription = "Email") }
            )

            // Profile
            AccountCard(
                title = "Profile",
                leading = { Icon(Icons.Filled.Person, contentDescription = "My Profile") },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (hasMissingDetails) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go to My Profile")
                    }
                },
                onClick = { navController.navigate(NavRoutes.MY_PROFILE) }
            )

            // Questions
            AccountCard(
                title = "Questions",
                leading = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_query_filled),
                        contentDescription = "Questions",
                        colorFilter = ColorFilter.tint(LocalContentColor.current)
                    )
                },
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go to Questions")
                },
                onClick = { navController.navigate(NavRoutes.QUESTIONS) }
            )

            // Goals
            AccountCard(
                title = "Goals",
                leading = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_goals),
                        contentDescription = "Goals",
                        colorFilter = ColorFilter.tint(LocalContentColor.current)
                    )
                },
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go to Goals")
                },
                onClick = { navController.navigate(NavRoutes.GOALS) }
            )

            // Threads
            AccountCard(
                title = "Threads",
                leading = {
                    Image(
                        painter = painterResource(id = R.drawable.ic_forum),
                        contentDescription = "Threads",
                        colorFilter = ColorFilter.tint(LocalContentColor.current)
                    )
                },
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go to Threads")
                },
                onClick = { navController.navigate(NavRoutes.THREADS) }
            )

            // Settings
            AccountCard(
                title = "Settings",
                leading = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                trailing = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go to Settings")
                },
                onClick = { navController.navigate(NavRoutes.SETTINGS) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Out button
            Button(
                onClick = { showSignOutDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }

            // Delete Account button
            Button(
                onClick = { showDeleteConfirmationDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Account", color = MaterialTheme.colorScheme.onError)
            }

            // Error message
            errorMessage?.let { msg ->
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
        DeleteAccountDialog(
            onConfirm = {
                showDeleteConfirmationDialog = false
                errorMessage = null
                authViewModel.deleteCurrentUser(
                    onSuccess = {},
                    onError = {}
                )
            },
            onDismiss = {
                showDeleteConfirmationDialog = false
                authViewModel.resetAuthResult()
            }
        )
    }

    if (showReauthDialog) {
        ReauthDialog(
            errorMessage = errorMessage,
            password = reauthPassword,
            onPasswordChange = { reauthPassword = it },
            onConfirm = {
                val currentEmail = currentUser?.email
                if (currentEmail != null && reauthPassword.isNotBlank()) {
                    errorMessage = null
                    authViewModel.signIn(currentEmail, reauthPassword) {
                        showReauthDialog = false
                        reauthPassword = ""
                        authViewModel.deleteCurrentUser(
                            onSuccess = {},
                            onError = {}
                        )
                    }
                } else {
                    errorMessage = "Please enter your password."
                }
            },
            onDismiss = {
                showReauthDialog = false
                reauthPassword = ""
                authViewModel.resetAuthResult()
            }
        )
    }

    if (showSignOutDialog) {
        ConfirmDialog(
            title = "Confirm Sign Out",
            text = "Are you sure you want to sign out?",
            onConfirm = {
                showSignOutDialog = false
                authViewModel.signOut()
                navController.navigate(NavRoutes.LANDING) {
                    popUpTo(NavRoutes.HOME) { inclusive = true }
                    launchSingleTop = true
                }
            },
            onDismiss = { showSignOutDialog = false }
        )
    }
}

@Composable
fun AccountCard(
    title: String,
    subtitle: String? = null,
    leading: @Composable () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(16.dp)
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) } },
            leadingContent = { leading() },
            trailingContent = { trailing?.invoke() }
        )
    }
}

@Composable
fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Account Deletion") },
        text = { Text("Are you sure you want to permanently delete your account and all associated data? This action cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ReauthDialog(
    errorMessage: String?,
    password: String,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Re-authentication Required") },
        text = {
            Column {
                Text(errorMessage ?: "Please re-enter your password.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Confirm & Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Yes") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("No") } }
    )
}
