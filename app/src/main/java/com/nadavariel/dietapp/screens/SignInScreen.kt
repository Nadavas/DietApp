@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// import androidx.lifecycle.viewmodel.compose.viewModel // <-- REMOVED
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.viewmodel.GoogleSignInFlowResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    authViewModel: AuthViewModel, // <-- 1. REMOVED DEFAULT INITIALIZER
    onBack: () -> Unit,
    onSignInSuccess: (isNewUser: Boolean) -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    val context = LocalContext.current
    val email by authViewModel.emailState
    val password by authViewModel.passwordState
    val authResult by authViewModel.authResult.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val googleSignInClient = remember(context) {
        authViewModel.getGoogleSignInClient(context)
    }

    val launcher = rememberLauncherForActivityResult(StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            authViewModel.handleGoogleSignIn(account) { flowResult ->
                when (flowResult) {
                    GoogleSignInFlowResult.GoToHome -> onSignInSuccess(false)
                    GoogleSignInFlowResult.GoToSignUp -> onNavigateToSignUp()
                    GoogleSignInFlowResult.Error -> { /* Error handled by LaunchedEffect */ }
                }
            }
        } catch (e: ApiException) {
            scope.launch {
                snackbarHostState.showSnackbar("Google sign-in failed: ${e.message}")
            }
            authViewModel.resetAuthResult()
        }
    }

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                authViewModel.resetAuthResult()
                onSignInSuccess(false)
            }
            is AuthResult.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = result.message,
                        duration = SnackbarDuration.Short
                    )
                }
                authViewModel.resetAuthResult()
            }
            AuthResult.Loading -> {}
            AuthResult.Idle -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Sign In", fontSize = 28.sp, modifier = Modifier.padding(bottom = 24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { authViewModel.emailState.value = it },
                label = { Text("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { authViewModel.passwordState.value = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = authViewModel.rememberMeState.value,
                    onCheckedChange = { isChecked ->
                        authViewModel.rememberMeState.value = isChecked
                    }
                )
                Text("Remember Me", style = MaterialTheme.typography.bodyLarge)
            }

            Button(
                onClick = {
                    if (authResult != AuthResult.Loading) {
                        authViewModel.signIn(email, password, onSignInSuccess)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authResult != AuthResult.Loading
            ) {
                if (authResult == AuthResult.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Sign In", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val annotatedTextSignUp = buildAnnotatedString {
                append("Don't have an account? ")
                pushStringAnnotation(tag = "SIGNUP", annotation = "Sign up")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("Sign up")
                }
                pop()
            }
            ClickableText(
                text = annotatedTextSignUp,
                onClick = { offset ->
                    annotatedTextSignUp.getStringAnnotations(tag = "SIGNUP", start = offset, end = offset)
                        .firstOrNull()?.let {
                            onNavigateToSignUp()
                        }
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                "OR",
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            googleSignInClient.signOut().await()
                        } catch (e: Exception) {
                            Log.w("SignInScreen", "Could not sign out of Google Client: ${e.message}")
                        }
                        launcher.launch(googleSignInClient.signInIntent)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authResult != AuthResult.Loading
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.google_logo),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continue with Google",
                    fontSize = 18.sp
                )
            }
        }
    }
}