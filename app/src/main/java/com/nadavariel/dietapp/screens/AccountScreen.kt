package com.nadavariel.dietapp.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // <-- 1. IMPORT ADDED
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.ui.account.ReauthDialog
import com.nadavariel.dietapp.ui.account.StyledAlertDialog
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.util.AvatarConstants

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f), CircleShape)
                )
                Image(
                    painter = painterResource(id = AvatarConstants.getAvatarResId(avatarId)),
                    contentDescription = "Profile Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .clickable { onAvatarClick() }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.darkGreyText
            )
            Text(
                text = email,
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.lightGreyText
            )
        }
    }
}

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
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides AppTheme.colors.primaryGreen) {
                    leadingIcon()
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.darkGreyText
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.lightGreyText
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
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = AppTheme.colors.lightGreyText,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

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
            color = AppTheme.colors.lightGreyText,
            modifier = Modifier.padding(end = 12.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = AppTheme.colors.lightGreyText.copy(alpha = 0.3f)
        )
    }
}

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
        OutlinedButton(
            onClick = onSignOutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = AppTheme.colors.lightGreyText
            ),
            border = BorderStroke(1.5.dp, AppTheme.colors.lightGreyText.copy(alpha = 0.5f))
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
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val isLoadingProfile by authViewModel.isLoadingProfile.collectAsStateWithLifecycle()
    val context = LocalContext.current // <-- 2. GET CONTEXT

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                errorMessage = null
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "My Account",
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.darkGreyText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.screenBackground
                )
            )
        },
        containerColor = AppTheme.colors.screenBackground
    ) { paddingValues ->
        // --- START OF FIX: Add loading wrapper ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // This combined check prevents the flash of default content
            val showLoading = isLoadingProfile || (authViewModel.currentUser != null && userProfile.name.isBlank())

            if (showLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AppTheme.colors.primaryGreen)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        AccountHeaderInfo(
                            name = userProfile.name.ifBlank { "User" },
                            email = currentUser?.email ?: "No email",
                            avatarId = userProfile.avatarId,
                            onAvatarClick = { navController.navigate(NavRoutes.MY_PROFILE) }
                        )
                    }

                    item {
                        SectionDivider("SETTINGS")
                    }

                    item {
                        MenuRow(
                            title = "Profile",
                            subtitle = "Manage your information",
                            leadingIcon = { Icon(Icons.Filled.Person, "My Profile", modifier = Modifier.size(24.dp)) },
                            hasNotification = false,
                            onClick = { navController.navigate(NavRoutes.MY_PROFILE) }
                        )
                    }

                    item {
                        MenuRow(
                            title = "Questions",
                            subtitle = "View your dietary preferences",
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_query_filled),
                                    contentDescription = "Questions",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = { navController.navigate(NavRoutes.QUESTIONS) }
                        )
                    }

                    item {
                        MenuRow(
                            title = "Diet Plan",
                            subtitle = "View your personalized plan",
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_diet_plan),
                                    contentDescription = "Goals",
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = { navController.navigate(NavRoutes.DIET_PLAN) }
                        )
                    }

                    item {
                        MenuRow(
                            title = "Notifications",
                            subtitle = "Meal reminder notifications",
                            leadingIcon = { Icon(Icons.Filled.Notifications, "Notifications", modifier = Modifier.size(24.dp)) },
                            onClick = { navController.navigate(NavRoutes.NOTIFICATIONS) }
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

                    item {
                        SectionDivider("ACCOUNT ACTIONS")
                    }

                    item {
                        AccountActionButtons(
                            onSignOutClick = { showSignOutDialog = true },
                            onDeleteClick = { showDeleteConfirmationDialog = true }
                        )
                    }

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

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
        // --- END OF FIX ---
    }

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
                // --- 3. PASS CONTEXT TO SIGN OUT ---
                authViewModel.signOut(context)
                // --- END OF FIX ---
            }
        )
    }
}