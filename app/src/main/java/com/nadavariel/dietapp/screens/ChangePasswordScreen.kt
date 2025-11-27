package com.nadavariel.dietapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nadavariel.dietapp.ui.AppTheme

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
        containerColor = AppTheme.colors.screenBackground, // Match app background
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Change Password",
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
                .padding(20.dp), // Standard padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Only show the fields if the user is an email/password user
            if (authViewModel.isEmailPasswordUser) {

                // Added a subtle card container or just spacing for the form
                Spacer(modifier = Modifier.height(16.dp))

                // Current Password
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primaryGreen,
                        focusedLabelColor = AppTheme.colors.primaryGreen,
                        cursorColor = AppTheme.colors.primaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.LockOpen, contentDescription = null, tint = AppTheme.colors.textSecondary)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // New Password
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primaryGreen,
                        focusedLabelColor = AppTheme.colors.primaryGreen,
                        cursorColor = AppTheme.colors.primaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AppTheme.colors.textSecondary)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm New Password
                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primaryGreen,
                        focusedLabelColor = AppTheme.colors.primaryGreen,
                        cursorColor = AppTheme.colors.primaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    leadingIcon = {
                        Icon(Icons.Default.LockReset, contentDescription = null, tint = AppTheme.colors.textSecondary)
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Change Password Button
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.primaryGreen
                    )
                ) {
                    Text(
                        "Update Password",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                errorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Show this message for Google Sign-In users
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Password management is not available for accounts created with Google Sign-In.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }
}