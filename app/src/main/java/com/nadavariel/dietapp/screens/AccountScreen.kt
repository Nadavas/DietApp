package com.nadavariel.dietapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.ui.account.ImageIcon
import com.nadavariel.dietapp.ui.account.ReauthDialog
import com.nadavariel.dietapp.ui.account.StyledAlertDialog
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.util.AvatarConstants

// --- DESIGN TOKENS (matching HomeScreen) ---
private val VibrantGreen = Color(0xFF4CAF50)
private val DarkGreyText = Color(0xFF333333)
private val LightGreyText = Color(0xFF757575)
private val ScreenBackgroundColor = Color(0xFFF7F9FC)

/**
 * Enhanced profile header with better visual hierarchy and spacing
 */
@Composable
fun AccountHeaderInfo(
    name: String,
    email: String,
    avatarId: String?,
    modifier: Modifier = Modifier,
    onAvatarClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with subtle pulse effect suggestion
            Box(contentAlignment = Alignment.Center) {
                // Background circle for depth
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(VibrantGreen.copy(alpha = 0.1f), CircleShape)
                )
                Image(
                    painter = painterResource(id = AvatarConstants.getAvatarResId(avatarId)),
                    contentDescription = "Profile Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(3.dp, VibrantGreen, CircleShape)
                        .clickable { onAvatarClick() }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = DarkGreyText
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyLarge,
                color = LightGreyText
            )
        }
    }
}

/**
 * Redesigned menu row with modern card styling
 */
@Composable
private fun MenuRow(
    title: String,
    subtitle: String? = null,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit,
    hasNotification: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon container with subtle background
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(VibrantGreen.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides VibrantGreen) {
                    leadingIcon()
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkGreyText
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = LightGreyText
                    )
                }
            }

            if (hasNotification) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Icon(
                imageVector = Icons.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = LightGreyText,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Section divider with label
 */
@Composable
private fun SectionDivider(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = LightGreyText,
            modifier = Modifier.padding(end = 12.dp)
        )
        Divider(
            modifier = Modifier.weight(1f),
            color = LightGreyText.copy(alpha = 0.3f)
        )
    }
}

/**
 * Enhanced action buttons with better styling
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
        // Sign Out Button - Outlined style
        OutlinedButton(
            onClick = onSignOutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = LightGreyText
            ),
            border = BorderStroke(1.5.dp, LightGreyText.copy(alpha = 0.5f))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Sign Out",
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Sign Out",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Delete Account Button - Filled error style
        Button(
            onClick = onDeleteClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Account",
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Delete Account",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
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
                title = {
                    Text(
                        "My Account",
                        fontWeight = FontWeight.Bold,
                        color = DarkGreyText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ScreenBackgroundColor
                ),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back",
                            tint = DarkGreyText
                        )
                    }
                }
            )
        },
        containerColor = ScreenBackgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header Card
            item {
                AccountHeaderInfo(
                    name = currentUser?.displayName ?: "User",
                    email = currentUser?.email ?: "No email",
                    avatarId = authViewModel.currentAvatarId,
                    onAvatarClick = { navController.navigate(NavRoutes.MY_PROFILE) }
                )
            }

            // Settings Section
            item {
                SectionDivider("SETTINGS")
            }

            item {
                MenuRow(
                    title = "Profile",
                    subtitle = if (hasMissingDetails) "Complete your profile" else "Manage your information",
                    leadingIcon = { Icon(Icons.Filled.Person, "My Profile", modifier = Modifier.size(24.dp)) },
                    hasNotification = hasMissingDetails,
                    onClick = { navController.navigate(NavRoutes.MY_PROFILE) }
                )
            }

            item {
                MenuRow(
                    title = "Questions",
                    subtitle = "View your dietary preferences",
                    leadingIcon = {
                        ImageIcon(
                            painterResource(id = R.drawable.ic_query_filled),
                            "Questions"
                        )
                    },
                    onClick = { navController.navigate(NavRoutes.QUESTIONS) }
                )
            }

            item {
                MenuRow(
                    title = "Goals",
                    subtitle = "Set your nutrition targets",
                    leadingIcon = {
                        ImageIcon(
                            painterResource(id = R.drawable.ic_goals),
                            "Goals"
                        )
                    },
                    onClick = { navController.navigate(NavRoutes.GOALS) }
                )
            }

            item {
                MenuRow(
                    title = "Settings",
                    subtitle = "App preferences",
                    leadingIcon = { Icon(Icons.Filled.Settings, "Settings", modifier = Modifier.size(24.dp)) },
                    onClick = { navController.navigate(NavRoutes.SETTINGS) }
                )
            }

            // Account Actions Section
            item {
                SectionDivider("ACCOUNT ACTIONS")
            }

            item {
                AccountActionButtons(
                    onSignOutClick = { showSignOutDialog = true },
                    onDeleteClick = { showDeleteConfirmationDialog = true }
                )
            }

            // Error message
            errorMessage?.let {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Bottom spacing
            item { Spacer(modifier = Modifier.height(24.dp)) }
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