@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.nadavariel.dietapp.AuthResult
import com.nadavariel.dietapp.AuthViewModel
import com.nadavariel.dietapp.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel = viewModel(),
    onBack: () -> Unit,
    onSignUpSuccess: () -> Unit,
    onNavigateToSignIn: () -> Unit // New callback for "Log In" link
) {
    val context = LocalContext.current
    val email by authViewModel.emailState
    val password by authViewModel.passwordState
    val confirmPassword by authViewModel.confirmPasswordState
    val authResult by authViewModel.authResult.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    authViewModel.firebaseAuthWithGoogle(idToken) { // Pass onSignUpSuccess for Google Sign-In
                        onSignUpSuccess()
                    }
                } else {
                    scope.launch {
                        snackbarHostState.showSnackbar("Google Sign-In failed: No ID Token found.")
                    }
                }
            } catch (e: ApiException) {
                scope.launch {
                    snackbarHostState.showSnackbar("Google Sign-In failed: ${e.localizedMessage}")
                }
            } catch (e: Exception) { // Catch other potential exceptions during result processing
                scope.launch {
                    snackbarHostState.showSnackbar("An unexpected error occurred during Google Sign-In.")
                }
            }
        } else {
            scope.launch {
                // User cancelled or another error occurred (result.resultCode might be Activity.RESULT_CANCELED)
                snackbarHostState.showSnackbar("Google Sign-In cancelled or failed.")
            }
        }
    }

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                // Handled by the onSuccess callbacks directly within signUp/firebaseAuthWithGoogle
                authViewModel.resetAuthResult() // Reset only after the callback completes
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
            else -> Unit
        }
    }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
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
            Text("Create Account", fontSize = 28.sp, modifier = Modifier.padding(bottom = 24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { authViewModel.emailState.value = it },
                label = { Text("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = password,
                onValueChange = { authViewModel.passwordState.value = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { authViewModel.confirmPasswordState.value = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            Button(
                onClick = { authViewModel.signUp(onSignUpSuccess) }, // Correctly passes the onSuccess lambda
                modifier = Modifier.fillMaxWidth(),
                enabled = authResult != AuthResult.Loading
            ) {
                if (authResult == AuthResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign Up", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val annotatedTextLogin = buildAnnotatedString {
                append("Already have an account? ")
                pushStringAnnotation(tag = "LOGIN", annotation = "Log in")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                    append("Log in")
                }
                pop()
            }
            ClickableText(
                text = annotatedTextLogin,
                onClick = { offset ->
                    annotatedTextLogin.getStringAnnotations(tag = "LOGIN", start = offset, end = offset)
                        .firstOrNull()?.let {
                            onNavigateToSignIn()
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
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
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