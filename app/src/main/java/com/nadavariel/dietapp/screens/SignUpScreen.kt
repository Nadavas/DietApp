@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadavariel.dietapp.ui.AppPrimaryButton
import com.nadavariel.dietapp.ui.AppTextField
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.AuthScreenWrapper
import com.nadavariel.dietapp.ui.AvatarSelectionDialog
import com.nadavariel.dietapp.ui.GoogleSignInButton
import com.nadavariel.dietapp.ui.LegalDisclaimer
import com.nadavariel.dietapp.ui.UserAvatar
import com.nadavariel.dietapp.ui.rememberGoogleSignInLauncher
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onSignUpSuccess: (isNewUser: Boolean) -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val name by authViewModel.nameState
    val email by authViewModel.emailState
    val password by authViewModel.passwordState
    val confirmPassword by authViewModel.confirmPasswordState
    val selectedAvatarId by authViewModel.selectedAvatarId
    val authResult by authViewModel.authResult.collectAsState()

    var showAvatarDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val (launcher, googleSignInClient) = rememberGoogleSignInLauncher(
        authViewModel = authViewModel,
        scope = scope,
        snackbarHostState = snackbarHostState,
        onAuthSuccess = onSignUpSuccess
    )

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> authViewModel.resetAuthResult()
            is AuthResult.Error -> {
                scope.launch { snackbarHostState.showSnackbar(result.message) }
                authViewModel.resetAuthResult()
            }
            else -> Unit
        }
    }

    AuthScreenWrapper(
        snackbarHostState = snackbarHostState,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Create Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { showAvatarDialog = true }
            ) {
                UserAvatar(avatarId = selectedAvatarId, size = 120.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.primaryGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AppTextField(
                value = name,
                onValueChange = { authViewModel.nameState.value = it },
                label = "Name"
            )

            AppTextField(
                value = email,
                onValueChange = { authViewModel.emailState.value = it },
                label = "Email"
            )

            AppTextField(
                value = password,
                onValueChange = { authViewModel.passwordState.value = it },
                label = "Password",
                visualTransformation = PasswordVisualTransformation()
            )

            AppTextField(
                value = confirmPassword,
                onValueChange = { authViewModel.confirmPasswordState.value = it },
                label = "Confirm Password",
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Create Account Button
            AppPrimaryButton(
                text = "Create Account",
                isLoading = authResult == AuthResult.Loading,
                onClick = {
                    // Basic Validation
                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                        scope.launch { snackbarHostState.showSnackbar("Please fill in all fields") }
                        return@AppPrimaryButton
                    }
                    if (password != confirmPassword) {
                        scope.launch { snackbarHostState.showSnackbar("Passwords do not match") }
                        return@AppPrimaryButton
                    }

                    // Perform Creation
                    scope.launch {
                        try {
                            authViewModel.createEmailUserAndProfile()

                            authViewModel.sendVerificationEmail(
                                onSuccess = { },
                                onError = { msg -> Log.e("SignUp", "Verification failed: $msg") }
                            )

                            onSignUpSuccess(true)

                        } catch (e: Exception) {
                            authViewModel.resetAuthResult()
                            snackbarHostState.showSnackbar(e.message ?: "Sign up failed")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            val annotatedTextLogin = buildAnnotatedString {
                append("Already have an account? ")
                pushStringAnnotation(tag = "LOGIN", annotation = "Log in")
                withStyle(
                    style = SpanStyle(
                        color = AppTheme.colors.primaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append("Log in")
                }
                pop()
            }
            ClickableText(
                text = annotatedTextLogin,
                onClick = { offset ->
                    annotatedTextLogin.getStringAnnotations(tag = "LOGIN", start = offset, end = offset)
                        .firstOrNull()?.let { onNavigateToSignIn() }
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                "OR",
                modifier = Modifier.padding(vertical = 16.dp),
                color = AppTheme.colors.textSecondary,
                fontSize = 14.sp
            )

            // Google Button
            GoogleSignInButton(
                enabled = authResult != AuthResult.Loading,
                onClick = {
                    scope.launch {
                        try {
                            googleSignInClient.signOut().await()
                        } catch (e: Exception) {
                            Log.w("SignUpScreen", "Sign out failed: ${e.message}")
                        }
                        launcher.launch(googleSignInClient.signInIntent)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            LegalDisclaimer()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAvatarDialog) {
        AvatarSelectionDialog(
            currentAvatarId = authViewModel.selectedAvatarId.value.toString(),
            onDismiss = { showAvatarDialog = false },
            onAvatarSelected = { newId -> authViewModel.selectedAvatarId.value = newId }
        )
    }
}