package com.nadavariel.dietapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.ui.account.AccountActionButton
import com.nadavariel.dietapp.ui.account.AccountCard
import com.nadavariel.dietapp.ui.account.ProfileHeader
import com.nadavariel.dietapp.ui.account.StyledAlertDialog
import com.nadavariel.dietapp.ui.account.ReauthDialog
import com.nadavariel.dietapp.ui.account.ImageIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val currentUser = authViewModel.currentUser
    val authResult by authViewModel.authResult.collectAsStateWithLifecycle()
    val hasMissingDetails by authViewModel.hasMissingPrimaryProfileDetails.collectAsStateWithLifecycle()

    // --- State Variables ---
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // --- Constants ---
    val dietAndNutritionGradient = remember {
        Brush.verticalGradient(listOf(Color(0xFF6A11CB), Color(0xFF2575FC)))
    }
    val endColor = remember { Color(0xFF2575FC) }

    // --- Side Effects (Auth Result Handling) ---
    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                errorMessage = null
                if (showDeleteConfirmationDialog) {
                    authViewModel.signOut()
                    navController.navigate(NavRoutes.LANDING) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                }
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
            else -> errorMessage = null
        }
    }

    // --- UI Layout ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Account", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dietAndNutritionGradient)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Header
                ProfileHeader(
                    name = currentUser?.displayName ?: "User",
                    email = currentUser?.email ?: "No email"
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Menu Items
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccountCard(
                        title = "Profile",
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = "My Profile") },
                        hasNotification = hasMissingDetails,
                        onClick = { navController.navigate(NavRoutes.MY_PROFILE) }
                    )
                    AccountCard(
                        title = "Questions",
                        leadingIcon = { ImageIcon(painterResource(id = R.drawable.ic_query_filled), "Questions") },
                        onClick = { navController.navigate(NavRoutes.QUESTIONS) }
                    )
                    AccountCard(
                        title = "Goals",
                        leadingIcon = { ImageIcon(painterResource(id = R.drawable.ic_goals), "Goals") },
                        onClick = { navController.navigate(NavRoutes.GOALS) }
                    )
                    AccountCard(
                        title = "Settings",
                        leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = "Settings") },
                        onClick = { navController.navigate(NavRoutes.SETTINGS) }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action Buttons
                AccountActionButton(
                    text = "Sign Out",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    onClick = { showSignOutDialog = true },
                    contentColor = endColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                AccountActionButton(
                    text = "Delete Account",
                    icon = Icons.Default.Delete,
                    onClick = { showDeleteConfirmationDialog = true },
                    contentColor = MaterialTheme.colorScheme.error
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- Dialogs (Use external composables) ---
    if (showDeleteConfirmationDialog) {
        StyledAlertDialog(
            onDismissRequest = {
                showDeleteConfirmationDialog = false
                authViewModel.resetAuthResult()
            },
            title = "Confirm Deletion",
            text = "Are you sure you want to permanently delete your account? This action cannot be undone.",
            confirmButtonText = "Delete",
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

    if (showSignOutDialog) {
        StyledAlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = "Confirm Sign Out",
            text = "Are you sure you want to sign out?",
            confirmButtonText = "Sign Out",
            dismissButtonText = "Cancel",
            onConfirm = {
                showSignOutDialog = false
                authViewModel.signOut()
                navController.navigate(NavRoutes.LANDING) {
                    popUpTo(NavRoutes.HOME) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }
}