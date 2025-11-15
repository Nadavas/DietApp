@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
// import androidx.lifecycle.viewmodel.compose.viewModel // <-- REMOVED
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.util.AvatarConstants
import com.nadavariel.dietapp.viewmodel.GoogleSignInFlowResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel, // <-- 1. REMOVED DEFAULT INITIALIZER
    onBack: () -> Unit,
    onSignUpSuccess: (isNewUser: Boolean) -> Unit, // <-- This is the correct signature
    onNavigateToSignIn: () -> Unit
) {
    val context = LocalContext.current
    val name by authViewModel.nameState
    val email by authViewModel.emailState
    val password by authViewModel.passwordState
    val confirmPassword by authViewModel.confirmPasswordState
    val selectedAvatarId by authViewModel.selectedAvatarId
    val authResult by authViewModel.authResult.collectAsState()

    val isGoogleSignUp by authViewModel.isGoogleSignUp // This now works

    var showAvatarDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember(context) {
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                authViewModel.handleGoogleSignIn(account) { flowResult ->
                    when (flowResult) {
                        // --- 2. THIS IS THE FIX ---
                        GoogleSignInFlowResult.GoToHome -> {
                            // User exists, call success with isNewUser = false
                            onSignUpSuccess(false)
                        }
                        // --- END OF FIX ---
                        GoogleSignInFlowResult.GoToSignUp -> {
                            // New user, stay on this screen.
                        }
                        GoogleSignInFlowResult.Error -> { /* Error handled in LaunchedEffect */ }
                    }
                }
            } catch (e: ApiException) {
                scope.launch {
                    snackbarHostState.showSnackbar("Google Sign-In failed: ${e.message}")
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("An unexpected error occurred: ${e.message}")
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Google Sign-In cancelled or failed.")
            }
        }
    }

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                authViewModel.resetAuthResult()
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

            Image(
                painter = painterResource(id = AvatarConstants.getAvatarResId(selectedAvatarId)),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showAvatarDialog = true }) {
                Text("Choose Avatar")
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { authViewModel.nameState.value = it },
                label = { Text("Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { authViewModel.emailState.value = it },
                label = { Text("Email") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                singleLine = true,
                enabled = !isGoogleSignUp // This now works
            )

            OutlinedTextField(
                value = password,
                onValueChange = { authViewModel.passwordState.value = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                singleLine = true,
                enabled = !isGoogleSignUp // This now works
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { authViewModel.confirmPasswordState.value = it },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                enabled = !isGoogleSignUp // This now works
            )

            Button(
                onClick = {
                    // New log
                    Log.d("SignUpScreen", "Next clicked. nameState='${authViewModel.nameState.value}', avatarId='${authViewModel.selectedAvatarId.value}'")
                    authViewModel.signUp {
                        onSignUpSuccess(true) // This is a new user (Email or new Google)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = authResult != AuthResult.Loading
            ) {
                if (authResult == AuthResult.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Next", fontSize = 18.sp)
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

            if (!isGoogleSignUp) { // This now works
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
                                Log.w("SignUpScreen", "Could not sign out of Google Client: ${e.message}")
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

    if (showAvatarDialog) {
        Dialog(onDismissRequest = { showAvatarDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select an Avatar",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(AvatarConstants.AVATAR_DRAWABLES) { (avatarId, drawableResId) ->
                            Image(
                                painter = painterResource(id = drawableResId),
                                contentDescription = "Avatar $avatarId",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        authViewModel.selectedAvatarId.value = avatarId
                                        showAvatarDialog = false
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { showAvatarDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}