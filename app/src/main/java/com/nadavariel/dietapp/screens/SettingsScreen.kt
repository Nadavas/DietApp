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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel // ⭐ NEW: Import for viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.AuthViewModel // ⭐ NEW: Import AuthViewModel
import com.nadavariel.dietapp.NavRoutes // ⭐ NEW: Import NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel() // ⭐ NEW: Inject AuthViewModel
) {
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
                .padding(horizontal = 16.dp, vertical = 8.dp), // Adjusted padding for better list item display
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // Changed to Top for list items
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ⭐ NEW: Conditionally display Change Password option
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
                        .clickable { navController.navigate(NavRoutes.CHANGE_PASSWORD) } // Navigate to new screen
                )
                HorizontalDivider()
            }

            // Other settings items can go here later
            Spacer(modifier = Modifier.height(16.dp))
            Text("Other settings options will appear here soon!")
            // You can remove this placeholder text when you add more settings items.
        }
    }
}