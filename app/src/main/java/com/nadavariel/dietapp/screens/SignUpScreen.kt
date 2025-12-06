@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.components.AvatarSelectionDialog
import com.nadavariel.dietapp.ui.components.UserAvatar
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.GoogleSignInFlowResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onSignUpSuccess: (isNewUser: Boolean) -> Unit,
    onNavigateToSignIn: () -> Unit
) {
    val context = LocalContext.current
    val name by authViewModel.nameState
    val email by authViewModel.emailState
    val password by authViewModel.passwordState
    val confirmPassword by authViewModel.confirmPasswordState
    val selectedAvatarId by authViewModel.selectedAvatarId
    val authResult by authViewModel.authResult.collectAsState()

    val isGoogleSignUp by authViewModel.isGoogleSignUp

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
                        GoogleSignInFlowResult.GoToHome -> onSignUpSuccess(false) // Existing -> Home
                        // New User -> Already created in VM -> Go to Questionnaire
                        GoogleSignInFlowResult.GoToSignUp -> onSignUpSuccess(true)
                        GoogleSignInFlowResult.Error -> {}
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
            is AuthResult.Success -> authViewModel.resetAuthResult()
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
                            contentDescription = "Back",
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = AppTheme.colors.textPrimary
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(AppTheme.colors.homeGradient))
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    // FIX: Changed horizontal padding to 24.dp to match SignInScreen width.
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

                // Avatar Selection with Edit Overlay
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clickable { showAvatarDialog = true }
                ) {
                    // FIX: Use UserAvatar
                    UserAvatar(
                        avatarId = selectedAvatarId,
                        size = 120.dp
                    )

                    // Edit Icon Overlay
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

                OutlinedTextField(
                    value = name,
                    onValueChange = { authViewModel.nameState.value = it },
                    label = { Text("Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primaryGreen,
                        focusedLabelColor = AppTheme.colors.primaryGreen,
                        cursorColor = AppTheme.colors.primaryGreen
                    )
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { authViewModel.emailState.value = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    singleLine = true,
                    enabled = !isGoogleSignUp,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primaryGreen,
                        focusedLabelColor = AppTheme.colors.primaryGreen,
                        cursorColor = AppTheme.colors.primaryGreen
                    )
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
                    enabled = !isGoogleSignUp,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primaryGreen,
                        focusedLabelColor = AppTheme.colors.primaryGreen,
                        cursorColor = AppTheme.colors.primaryGreen
                    )
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { authViewModel.confirmPasswordState.value = it },
                    label = { Text("Confirm Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    singleLine = true,
                    enabled = !isGoogleSignUp,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primaryGreen,
                        focusedLabelColor = AppTheme.colors.primaryGreen,
                        cursorColor = AppTheme.colors.primaryGreen
                    )
                )

                Button(
                    onClick = {
                        Log.d("SignUpScreen", "Next clicked. nameState='${authViewModel.nameState.value}', avatarId='${authViewModel.selectedAvatarId.value}'")
                        authViewModel.signUp { onSignUpSuccess(true) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = authResult != AuthResult.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.primaryGreen,
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (authResult == AuthResult.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Next", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val annotatedTextLogin = buildAnnotatedString {
                    append("Already have an account? ")
                    pushStringAnnotation(tag = "LOGIN", annotation = "Log in")
                    withStyle(style = SpanStyle(color = AppTheme.colors.primaryGreen, fontWeight = FontWeight.Bold)) {
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

                if (!isGoogleSignUp) {
                    Text(
                        "OR",
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = AppTheme.colors.textSecondary,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = authResult != AuthResult.Loading,
                        border = BorderStroke(1.dp, AppTheme.colors.textSecondary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppTheme.colors.textPrimary
                        )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.google_logo),
                            contentDescription = "Google Logo",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        // FIX: Removed maxLines and overflow to match SignIn button behavior
                        Text(
                            text = "Continue with Google",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (showAvatarDialog) {
        AvatarSelectionDialog(
            currentAvatarId = authViewModel.selectedAvatarId.value.toString(),
            onDismiss = { showAvatarDialog = false },
            onAvatarSelected = { newId ->
                authViewModel.selectedAvatarId.value = newId
            }
        )
    }
}