package com.nadavariel.dietapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val currentUser = authViewModel.currentUser
    val authResult by authViewModel.authResult.collectAsStateWithLifecycle()
    val hasMissingDetails by authViewModel.hasMissingPrimaryProfileDetails.collectAsStateWithLifecycle()

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    // Define the gradient and colors to match CreateThreadScreen
    val dietAndNutritionGradient = Brush.verticalGradient(
        listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
    )
    val endColor = Color(0xFF2575FC)

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
                        leadingIcon = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_query_filled),
                                contentDescription = "Questions",
                                colorFilter = ColorFilter.tint(Color.White)
                            )
                        },
                        onClick = { navController.navigate(NavRoutes.QUESTIONS) }
                    )
                    AccountCard(
                        title = "Goals",
                        leadingIcon = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_goals),
                                contentDescription = "Goals",
                                colorFilter = ColorFilter.tint(Color.White)
                            )
                        },
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

@Composable
fun ProfileHeader(name: String, email: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "U",
                    fontSize = 28.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = email,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun AccountCard(
    title: String,
    leadingIcon: @Composable () -> Unit,
    hasNotification: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.SemiBold, color = Color.White) },
            leadingContent = {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    leadingIcon()
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasNotification) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Navigate",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
fun AccountActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    contentColor: Color
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.White,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(
                text,
                fontWeight = FontWeight.Bold,
                fontSize = MaterialTheme.typography.titleMedium.fontSize
            )
        }
    }
}

@Composable
fun StyledAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String,
    dismissButtonText: String,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(dismissButtonText) }
        }
    )
}

@Composable
fun ReauthDialog(
    errorMessage: String?,
    password: String,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Re-authentication Required", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    errorMessage ?: "Please re-enter your password to proceed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (errorMessage != null) MaterialTheme.colorScheme.error else LocalContentColor.current
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Confirm & Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// Kept this for simplicity, can be merged into StyledAlertDialog if needed
@Composable
fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    StyledAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        text = text,
        confirmButtonText = "Sign Out",
        dismissButtonText = "Cancel",
        onConfirm = onConfirm
    )
}
