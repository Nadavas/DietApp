package com.nadavariel.dietapp.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.account.ReauthDialog
import com.nadavariel.dietapp.ui.account.StyledAlertDialog
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val authResult by authViewModel.authResult.collectAsStateWithLifecycle()
    val currentUser = authViewModel.currentUser

    // Password Change State
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var isPasswordSectionExpanded by remember { mutableStateOf(false) }

    // App Lock State (Mock for now)
    var isAppLockEnabled by remember { mutableStateOf(false) }

    // Dialog State
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    errorMessage = null // Clear error on success
                }
                // If success came from deleting account, the VM usually signs out,
                // creating a side effect in MainActivity that navigates away.
                authViewModel.resetAuthResult()
            }
            is AuthResult.Error -> {
                errorMessage = if (result.message == "re-authenticate-required") {
                    showReauthDialog = true
                    "Please re-enter your password to confirm deletion."
                } else {
                    result.message
                }
            }
            else -> { /* Idle or Loading */ }
        }
    }

    Scaffold(
        containerColor = AppTheme.colors.screenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Security & Privacy",
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.darkGreyText
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTheme.colors.darkGreyText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
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
            // --- SECTION 1: APP ACCESS ---
            Text(
                "APP ACCESS",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.lightGreyText
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint, // Or Lock
                                contentDescription = null,
                                tint = AppTheme.colors.primaryGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "App Lock",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.darkGreyText
                            )
                            Text(
                                text = "Biometric unlock",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppTheme.colors.lightGreyText
                            )
                        }
                    }
                    Switch(
                        checked = isAppLockEnabled,
                        onCheckedChange = { isAppLockEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppTheme.colors.primaryGreen
                        )
                    )
                }
            }

            // --- SECTION 2: ACCOUNT SECURITY (Manual Users Only) ---
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
                                    .background(AppTheme.colors.accentTeal.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = AppTheme.colors.accentTeal,
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
                                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primaryGreen)
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

            // --- SECTION 3: LEGAL ---
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
                onClick = { /* Navigate or open URL */ }
            )

            SecurityOptionRow(
                title = "Terms of Service",
                icon = Icons.Default.Description,
                iconColor = AppTheme.colors.softBlue,
                onClick = { /* Navigate or open URL */ }
            )

            // --- SECTION 4: DANGER ZONE ---
            Text(
                "DANGER ZONE",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )

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

    // --- Dialogs copied from AccountScreen ---

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
                        authViewModel.deleteCurrentUser(onSuccess = {}, onError = {})
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = AppTheme.colors.lightGreyText,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}