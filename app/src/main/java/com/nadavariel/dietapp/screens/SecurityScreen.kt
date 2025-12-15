@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.AppTopBar
import com.nadavariel.dietapp.ui.StyledAlertDialog
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    quizViewModel: com.nadavariel.dietapp.viewmodel.QuizViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current
    val projectId = "dietapp-c5e7e"

    val authResult by authViewModel.authResult.collectAsStateWithLifecycle()
    val currentUser = authViewModel.currentUser

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var isPasswordSectionExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showPasswordReauthDialog by remember { mutableStateOf(false) }
    var showGoogleReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetConfirmationDialog by remember { mutableStateOf(false) }

    // --- GOOGLE LAUNCHER FOR RE-AUTH ---
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember(context) { GoogleSignIn.getClient(context, gso) }

    val reAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                // Perform Re-auth with the fresh account
                authViewModel.reauthenticateWithGoogle(
                    account = account,
                    onSuccess = {
                        // If re-auth succeeds, try deleting again immediately
                        authViewModel.deleteCurrentUser(
                            onSuccess = { navController.navigate(NavRoutes.LANDING) },
                            onError = { /* Error is handled by LaunchedEffect below */ }
                        )
                    },
                    onError = { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                )
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Google Re-auth failed: ${e.message}") }
            }
        }
    }

    // Handle authentication results
    LaunchedEffect(authResult) {
        when (val result = authResult) {
            AuthResult.Success -> {
                // If success came from changing password
                if (isPasswordSectionExpanded) {
                    isPasswordSectionExpanded = false
                    currentPassword = ""
                    newPassword = ""
                    confirmNewPassword = ""
                    errorMessage = null
                }
                // If success came from deleting account, the VM usually signs out
                authViewModel.resetAuthResult()
            }
            is AuthResult.Error -> {
                if (result.message == "re-authenticate-required") {
                    // Logic to show correct dialog
                    if (authViewModel.isEmailPasswordUser) {
                        showPasswordReauthDialog = true
                        errorMessage = "Please re-enter your password to confirm deletion."
                    } else {
                        showGoogleReauthDialog = true
                    }
                    authViewModel.resetAuthResult()
                } else {
                    errorMessage = result.message
                    // Show generic errors in snackbar too
                    scope.launch { snackbarHostState.showSnackbar(result.message) }
                }
            }
            else -> { /* Idle or Loading */ }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppTheme.colors.screenBackground,
        topBar = {
            AppTopBar(
                title = "Security & Privacy",
                onBack = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- SECTION 1: ACCOUNT SECURITY (Manual Users Only) ---
            if (authViewModel.isEmailPasswordUser) {
                Text(
                    "ACCOUNT SECURITY",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.lightGreyText,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Collapsible Change Password Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Header Row (Always visible)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isPasswordSectionExpanded = !isPasswordSectionExpanded }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(AppTheme.colors.softBlue.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = AppTheme.colors.softBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Change Password",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.darkGreyText,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isPasswordSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = AppTheme.colors.lightGreyText
                            )
                        }

                        // Expanded Content
                        AnimatedVisibility(
                            visible = isPasswordSectionExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 20.dp)
                            ) {
                                HorizontalDivider(color = AppTheme.colors.divider, modifier = Modifier.padding(bottom = 16.dp))

                                OutlinedTextField(
                                    value = currentPassword,
                                    onValueChange = { currentPassword = it },
                                    label = { Text("Current Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppTheme.colors.primaryGreen,
                                        focusedLabelColor = AppTheme.colors.primaryGreen,
                                        cursorColor = AppTheme.colors.primaryGreen
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = newPassword,
                                    onValueChange = { newPassword = it },
                                    label = { Text("New Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppTheme.colors.primaryGreen,
                                        focusedLabelColor = AppTheme.colors.primaryGreen,
                                        cursorColor = AppTheme.colors.primaryGreen
                                    )
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = confirmNewPassword,
                                    onValueChange = { confirmNewPassword = it },
                                    label = { Text("Confirm New Password") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppTheme.colors.primaryGreen,
                                        focusedLabelColor = AppTheme.colors.primaryGreen,
                                        cursorColor = AppTheme.colors.primaryGreen
                                    )
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        errorMessage = null
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
                                        authViewModel.changePassword(
                                            oldPassword = currentPassword,
                                            newPassword = newPassword,
                                            onSuccess = { /* Handled by LaunchedEffect */ },
                                            onError = { /* Handled by LaunchedEffect */ }
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.softBlue)
                                ) {
                                    if (authResult is AuthResult.Loading) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                    } else {
                                        Text("Update Password", fontWeight = FontWeight.Bold)
                                    }
                                }

                                errorMessage?.let {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = it,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.align(Alignment.CenterHorizontally)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- SECTION 2: LEGAL ---
            Text(
                "LEGAL",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.lightGreyText,
                modifier = Modifier.padding(top = 8.dp)
            )

            SecurityOptionRow(
                title = "Privacy Policy",
                icon = Icons.Default.PrivacyTip,
                iconColor = AppTheme.colors.softBlue,
                onClick = {
                    uriHandler.openUri("https://$projectId.web.app/privacy.html")
                }
            )

            SecurityOptionRow(
                title = "Terms of Service",
                icon = Icons.Default.Description,
                iconColor = AppTheme.colors.softBlue,
                onClick = {
                    uriHandler.openUri("https://$projectId.web.app/terms.html")
                }
            )

            // --- SECTION 3: DANGER ZONE ---
            Text(
                "DANGER ZONE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )

            // 1. Reset Data Button
            OutlinedButton(
                onClick = { showResetConfirmationDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AppTheme.colors.darkGreyText
                ),
                border = BorderStroke(1.dp, AppTheme.colors.lightGreyText.copy(alpha = 0.3f))
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = AppTheme.colors.softBlue
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Reset Diet Journey",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // 2. Delete Account Button
            Button(
                onClick = { showDeleteConfirmationDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Delete Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    // --- Dialogs ---

    if (showResetConfirmationDialog) {
        StyledAlertDialog(
            onDismissRequest = { showResetConfirmationDialog = false },
            title = "Reset Journey?",
            text = "This will delete all your diet plan, logged meals, weight history, and reminders. Your profile and settings will be kept.\n\nThis cannot be undone.",
            confirmButtonText = "Reset Data",
            dismissButtonText = "Cancel",
            onConfirm = {
                showResetConfirmationDialog = false

                authViewModel.resetUserData(
                    onSuccess = {
                        quizViewModel.clearData()
                    },
                    onError = {}
                )
            }
        )
    }

    if (showDeleteConfirmationDialog) {
        StyledAlertDialog(
            onDismissRequest = {
                showDeleteConfirmationDialog = false
                authViewModel.resetAuthResult()
            },
            title = "Delete Account",
            text = "Are you sure you want to permanently delete your account? This will erase all your data and cannot be undone.",
            confirmButtonText = "Delete Forever",
            dismissButtonText = "Cancel",
            onConfirm = {
                showDeleteConfirmationDialog = false
                authViewModel.deleteCurrentUser(onSuccess = {}, onError = {})
            }
        )
    }

    if (showPasswordReauthDialog) {
        ReauthDialog(
            errorMessage = errorMessage,
            password = reauthPassword,
            onPasswordChange = { reauthPassword = it },
            onConfirm = {
                val currentEmail = currentUser?.email
                if (currentEmail != null && reauthPassword.isNotBlank()) {
                    errorMessage = null
                    authViewModel.signIn(currentEmail, reauthPassword) {
                        showPasswordReauthDialog = false
                        reauthPassword = ""
                        // After re-auth, try deleting again
                        authViewModel.deleteCurrentUser(
                            onSuccess = { navController.navigate(NavRoutes.LANDING) },
                            onError = { /* Handled by LaunchedEffect */ }
                        )
                    }
                } else {
                    errorMessage = "Please enter your password."
                }
            },
            onDismiss = {
                showPasswordReauthDialog = false
                reauthPassword = ""
                authViewModel.resetAuthResult()
            }
        )
    }

    if (showGoogleReauthDialog) {
        StyledAlertDialog(
            onDismissRequest = { showGoogleReauthDialog = false },
            title = "Verify it's you",
            text = "For security, please sign in with Google again to confirm account deletion.",
            confirmButtonText = "Verify with Google",
            dismissButtonText = "Cancel",
            onConfirm = {
                showGoogleReauthDialog = false
                scope.launch {
                    try { googleSignInClient.signOut().await() } catch (_: Exception) {}
                    reAuthLauncher.launch(googleSignInClient.signInIntent)
                }
            }
        )
    }
}

@Composable
private fun SecurityOptionRow(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.darkGreyText,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReauthDialog(
    errorMessage: String?,
    password: String,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = { Text("Re-authentication Required", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    errorMessage ?: "Please re-enter your password to proceed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (errorMessage != null) MaterialTheme.colorScheme.error else AppTheme.colors.textSecondary
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primaryGreen,
                        focusedLabelColor = AppTheme.colors.primaryGreen,
                        cursorColor = AppTheme.colors.primaryGreen,
                        unfocusedBorderColor = AppTheme.colors.textSecondary.copy(alpha = 0.5f)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text("Confirm & Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppTheme.colors.textSecondary
                )
            ) {
                Text("Cancel", fontWeight = FontWeight.Medium)
            }
        }
    )
}