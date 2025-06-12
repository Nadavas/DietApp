package com.nadavariel.dietapp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController // Import NavController
import com.nadavariel.dietapp.AuthViewModel // Import AuthViewModel
import com.nadavariel.dietapp.NavRoutes // Import NavRoutes

@Composable
fun MyProfileScreen(
    authViewModel: AuthViewModel, // Pass AuthViewModel
    navController: NavController // Pass NavController for navigation
) {
    // Get the name and weight from the AuthViewModel
    val userName = authViewModel.nameState.value.ifEmpty { "" } // Display "Not set" if empty
    val userWeight = authViewModel.weightState.value.ifEmpty { "" } // Display "Not set" if empty

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "My Profile",
            fontSize = 28.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
            text = "Name: $userName",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = if (userWeight.isNotEmpty()) "Weight: $userWeight kg" else "Weight: ",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = {
                navController.navigate(NavRoutes.UPDATE_PROFILE)
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Update Profile")
        }
    }
}