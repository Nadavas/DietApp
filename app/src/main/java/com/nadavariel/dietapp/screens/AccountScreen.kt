package com.nadavariel.dietapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes // Ensure NavRoutes is imported

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Account",
                        fontSize = 24.sp // Set the font size to 24.sp
                    )
                }, // Title for this screen
                navigationIcon = {
                    // Assuming you'll navigate back from here only if user came via deep link or not from bottom nav
                    // For bottom nav, usually no back button here.
                    // If this screen is reachable via a standard back stack (not bottom nav primary),
                    // you might want to enable this:
                    /*
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                    */
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile List Item
            ListItemWithArrow(
                text = "Profile",
                onClick = { navController.navigate(NavRoutes.MY_PROFILE) } // Navigate to MyProfileScreen
            )
            Divider() // Visual separator

            // Settings List Item
            ListItemWithArrow(
                text = "Settings",
                onClick = { navController.navigate(NavRoutes.SETTINGS) } // Navigate to SettingsScreen
            )
            Divider() // Visual separator

            // You can add more items here later
            // ListItemWithArrow(text = "About", onClick = { /* Navigate to About screen */ })
            // Divider()
        }
    }
}

@Composable
fun ListItemWithArrow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // Make the whole row clickable
            .padding(vertical = 12.dp, horizontal = 8.dp), // Add padding for touch target
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Push text left, arrow right
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Go to $text",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}