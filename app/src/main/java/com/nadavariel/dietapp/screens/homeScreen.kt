package com.nadavariel.dietapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.AuthViewModel

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel = viewModel(),
    onSignOut: () -> Unit
) {
    val currentUser = Firebase.auth.currentUser // Get current user

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to the App!", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(16.dp))
        currentUser?.email?.let {
            Text("You are signed in as: $it", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            authViewModel.signOut()
            onSignOut()
        }) {
            Text("Sign Out")
        }
    }
}