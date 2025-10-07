package com.nadavariel.dietapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.ui.account.ImageIcon
import com.nadavariel.dietapp.ui.account.ProfileHeader
import com.nadavariel.dietapp.ui.account.ReauthDialog
import com.nadavariel.dietapp.ui.account.StyledAlertDialog
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel

/**
 * A custom modifier to apply a "glassmorphism" effect.
 * This creates a transparent container with a subtle border.
 */
fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(16.dp),
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    borderWidth: Dp = 1.dp,
    color: Color
): Modifier = this
    .clip(shape)
    .border(borderWidth, borderColor, shape)

/**
 * A composable for a single row item in the account menu.
 */
@Composable
private fun MenuRow(
    title: String,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit,
    hasNotification: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Use LocalContentColor to tint the icon white by default
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            leadingIcon()
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        if (hasNotification) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Icon(
            imageVector = Icons.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Refactored action buttons with a glassmorphic design.
 */
@Composable
fun AccountActionButtons(
    onSignOutClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sign Out Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .glassmorphism(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)) // Applies the default semi-transparent white fill
                .clip(RoundedCornerShape(16.dp)) // for ripple effect
                .clickable { onSignOutClick() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }

// Delete Account Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .glassmorphism(
                    // Add this line to give the button a reddish fill
                    color = MaterialTheme.colorScheme.error.copy(alpha = 1f),
                    borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable { onDeleteClick() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Account",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Delete Account",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}


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
        Brush.verticalGradient(listOf(Color(0x6103506C), Color(0xFF1644A0)))
    }

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Header
                item {
                    ProfileHeader(
                        name = currentUser?.displayName ?: "User",
                        email = currentUser?.email ?: "No email"
                    )
                }

                // Menu Items
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassmorphism(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            )
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        MenuRow(
                            title = "Profile",
                            leadingIcon = { Icon(Icons.Filled.Person, "My Profile") },
                            hasNotification = hasMissingDetails,
                            onClick = { navController.navigate(NavRoutes.MY_PROFILE) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = Color.White.copy(alpha = 0.2f)
                        )
                        MenuRow(
                            title = "Questions",
                            leadingIcon = { ImageIcon(painterResource(id = R.drawable.ic_query_filled), "Questions") },
                            onClick = { navController.navigate(NavRoutes.QUESTIONS) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = Color.White.copy(alpha = 0.2f)
                        )
                        MenuRow(
                            title = "Goals",
                            leadingIcon = { ImageIcon(painterResource(id = R.drawable.ic_goals), "Goals") },
                            onClick = { navController.navigate(NavRoutes.GOALS) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = Color.White.copy(alpha = 0.2f)
                        )
                        MenuRow(
                            title = "Settings",
                            leadingIcon = { Icon(Icons.Filled.Settings, "Settings") },
                            onClick = { navController.navigate(NavRoutes.SETTINGS) }
                        )
                    }
                }

                // Action Buttons
                item {
                    AccountActionButtons(
                        onSignOutClick = { showSignOutDialog = true },
                        onDeleteClick = { showDeleteConfirmationDialog = true }
                    )
                }

                errorMessage?.let {
                    item {
                        Text(
                            text = it,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs ---
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