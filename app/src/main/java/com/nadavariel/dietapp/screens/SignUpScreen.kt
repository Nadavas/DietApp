package com.nadavariel.dietapp.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.nadavariel.dietapp.AuthViewModel
import com.nadavariel.dietapp.AuthResult // Assuming AuthResult is in the same package as AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.nadavariel.dietapp.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel = viewModel(),
    onBack: () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    val context = LocalContext.current
    val email by authViewModel.emailState
    val password by authViewModel.passwordState
    val confirmPassword by authViewModel.confirmPasswordState
    val authResult by authViewModel.authResult.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                authViewModel.firebaseAuthWithGoogle(idToken)
            }
        } catch (e: ApiException) {
            scope.launch {
                snackbarHostState.showSnackbar("Google Sign-In failed: ${e.localizedMessage}")
            }
        }
    }

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                authViewModel.resetAuthResult()
                onSignUpSuccess()
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

    // Configure Google Sign-In options
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { authViewModel.confirmPasswordState.value = it },
                label = { Text("Confirm Password") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            Button(
                onClick = { authViewModel.signUp(onSignUpSuccess) },
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

            // Google Sign-In Button
            OutlinedButton(
                onClick = {
                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Black
                )
            ) {
                // Google logo icon
                Icon(
                    painter = painterResource(id = R.drawable.google_logo),
                    contentDescription = "Google Logo",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified // <-- this keeps original colors of the image
                )

                Spacer(modifier = Modifier.width(8.dp)) // space between icon and text

                Text(
                    text = "Sign Up with Google",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onBack) {
                Text("Back to Landing")
            }
        }
    }
}