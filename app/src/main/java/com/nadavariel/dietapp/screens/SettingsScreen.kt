package com.nadavariel.dietapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications // NEW: Import for notifications icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    // Get dark mode state from viewmodel
    val isDarkModeEnabled by authViewModel.isDarkModeEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
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

            // Dark mode
            ListItem(
                headlineContent = { Text("Dark Mode") },
                leadingContent = {
                    Icon(Icons.Filled.DarkMode, contentDescription = "Dark Mode")
                },
                trailingContent = {
                    Switch(
                        checked = isDarkModeEnabled,
                        onCheckedChange = { enabled ->
                            authViewModel.toggleDarkMode(enabled)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider()

            // NEW: Notifications
            ListItem(
                headlineContent = { Text("Notifications") },
                leadingContent = {
                    Icon(Icons.Default.Notifications, contentDescription = "Meal Reminder Notifications")
                },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go to Notifications")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(NavRoutes.NOTIFICATIONS) } // NEW: Navigation action
            )
            HorizontalDivider()

            // Change Password (only for manual registered users)
            if (authViewModel.isEmailPasswordUser) {
                ListItem(
                    headlineContent = { Text("Change Password") },
                    leadingContent = {
                        Icon(Icons.Filled.Lock, contentDescription = "Change Password")
                    },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Go to Change Password")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(NavRoutes.CHANGE_PASSWORD) }
                )
                HorizontalDivider()
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}