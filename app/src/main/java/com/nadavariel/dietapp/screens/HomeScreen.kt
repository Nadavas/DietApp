package com.nadavariel.dietapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.AuthViewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.collectAsState // Import for StateFlow
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.model.UserProfile // Import the UserProfile data class

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    foodLogViewModel: FoodLogViewModel,
    navController: NavController,
    onSignOut: () -> Unit
) {
    val currentUser = Firebase.auth.currentUser
    // Observe the userProfile StateFlow to get the user's name
    val userProfile by authViewModel.userProfile.collectAsState()
    val userName = userProfile.name // Access the name property from the UserProfile object

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (userName.isNotBlank()) "Hey $userName!" else "Welcome to the App!",
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        currentUser?.email?.let {
            Text("You are signed in as: $it", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(32.dp))

        // TODO: This is where we'll add the statistics display and daily meal list later

        Button(onClick = {
            authViewModel.signOut() // Call signOut on AuthViewModel
            onSignOut() // Trigger the navigation callback in MainActivity
        }) {
            Text("Sign Out")
        }
    }
}