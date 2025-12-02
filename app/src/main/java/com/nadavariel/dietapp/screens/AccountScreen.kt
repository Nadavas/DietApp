package com.nadavariel.dietapp.screens

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
import androidx.compose.material.icons.filled.Lock // Imported Lock icon
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.ui.account.StyledAlertDialog
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.viewmodel.AuthResult
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.util.AvatarConstants

// --- NEW HEADER COMPOSABLE ---
@Composable
private fun ModernAccountHeader(onSignOutClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp), // Padding applied to the Row itself
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically // This centers items vertically
        ) {
            // Left Side: Title and Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Account",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
                Text(
                    text = "Manage profile and preferences",
                    fontSize = 14.sp,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Right Side: Sign Out Button
            // Remove any extra padding here to let Row alignment take over
            TextButton(
                onClick = onSignOutClick,
                colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.softRed)
            ) {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

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
    val context = LocalContext.current

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(authResult) {
        when (val result = authResult) {
            is AuthResult.Success -> {
                errorMessage = null
                authViewModel.resetAuthResult()
            }

            is AuthResult.Error -> {
                // Only show generic errors here. Reauth errors are handled in Security screen now.
                if (result.message != "re-authenticate-required") {
                    errorMessage = result.message
                }
            }

            else -> errorMessage = null
        }
    }

    // Using a standard Column instead of Scaffold so the ModernHeader flows with content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.screenBackground)
    ) {
        // Modern Header is placed directly at the top
        ModernAccountHeader(onSignOutClick = { showSignOutDialog = true })

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {

            val showLoading = isLoadingProfile || currentUser == null

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
                            email = currentUser.email ?: "No email",
                            avatarId = userProfile.avatarId,
                            onAvatarClick = { navController.navigate(NavRoutes.MY_PROFILE) }
                        )
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
                            title = "Personal Quiz",
                            subtitle = "Define your profile & diet plan",
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
                            title = "Reminders",
                            subtitle = "Set alerts for meals & weight",
                            leadingIcon = { Icon(Icons.Filled.Notifications, "Notifications", modifier = Modifier.size(24.dp)) },
                            onClick = { navController.navigate(NavRoutes.NOTIFICATIONS) }
                        )
                    }

                    item {
                        MenuRow(
                            title = "Security & Privacy",
                            subtitle = "App lock, password & legal",
                            leadingIcon = { Icon(Icons.Filled.Lock, "Security", modifier = Modifier.size(24.dp)) },
                            onClick = { navController.navigate(NavRoutes.SECURITY) }
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
                authViewModel.signOut(context)
            }
        )
    }
}