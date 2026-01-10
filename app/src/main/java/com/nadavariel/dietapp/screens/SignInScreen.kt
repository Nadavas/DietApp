@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadavariel.dietapp.ui.AppPrimaryButton
import com.nadavariel.dietapp.ui.AppTextField
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.AuthScreenWrapper
import com.nadavariel.dietapp.ui.GoogleSignInButton
import com.nadavariel.dietapp.ui.LegalDisclaimer
import com.nadavariel.dietapp.ui.rememberGoogleSignInLauncher
import com.nadavariel.dietapp.viewmodels.AuthResult
import com.nadavariel.dietapp.viewmodels.AuthViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun SignInScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onSignInSuccess: (isNewUser: Boolean) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val email by authViewModel.emailState
    val password by authViewModel.passwordState
    val authResult by authViewModel.authResult.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val (launcher, googleSignInClient) = rememberGoogleSignInLauncher(
        authViewModel = authViewModel,
        scope = scope,
        snackbarHostState = snackbarHostState,
        onAuthSuccess = onSignInSuccess
    )

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                authViewModel.resetAuthResult()
            }
            is AuthResult.Error -> {
                scope.launch { snackbarHostState.showSnackbar(result.message) }
                authViewModel.resetAuthResult()
            }
            else -> {}
        }
    }

    AuthScreenWrapper(
        snackbarHostState = snackbarHostState,
        onBack = onBack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sign In",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            AppTextField(
                value = email,
                onValueChange = { authViewModel.emailState.value = it },
                label = "Email",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            AppTextField(
                value = password,
                onValueChange = { authViewModel.passwordState.value = it },
                label = "Password",
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Remember Me Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = authViewModel.rememberMeState.value,
                    onCheckedChange = { isChecked ->
                        authViewModel.rememberMeState.value = isChecked
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = AppTheme.colors.primaryGreen
                    )
                )
                Text(
                    text = "Remember Me",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.colors.textPrimary
                )
            }

            AppPrimaryButton(
                text = "Sign In",
                isLoading = authResult == AuthResult.Loading,
                onClick = {
                    if (authResult != AuthResult.Loading) {
                        authViewModel.signIn(email, password) { isNewUser ->
                            onSignInSuccess(isNewUser)
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            val annotatedTextSignUp = buildAnnotatedString {
                append("Don't have an account? ")
                pushStringAnnotation(tag = "SIGNUP", annotation = "Sign up")
                withStyle(
                    style = SpanStyle(
                        color = AppTheme.colors.primaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append("Sign up")
                }
                pop()
            }
            ClickableText(
                text = annotatedTextSignUp,
                onClick = { offset ->
                    annotatedTextSignUp.getStringAnnotations(
                        tag = "SIGNUP",
                        start = offset,
                        end = offset
                    )
                        .firstOrNull()?.let { onNavigateToSignUp() }
                },
                modifier = Modifier.padding(bottom = 16.dp)
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
                            Log.w("SignInScreen", "Sign out failed: ${e.message}")
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
}