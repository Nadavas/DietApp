// In HomeScreen.kt
package com.nadavariel.dietapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding // Keep padding here
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// REMOVE these imports from HomeScreen.kt (they are now in MainActivity.kt or not needed)
// import androidx.compose.material3.Icon
// import androidx.compose.material3.NavigationBar
// import androidx.compose.material3.NavigationBarItem
// import androidx.compose.material3.Scaffold
// import androidx.compose.material3.MaterialTheme
// import androidx.navigation.NavController // REMOVE this
// import androidx.navigation.compose.currentBackStackEntryAsState // REMOVE this
// import androidx.compose.runtime.getValue // REMOVE this
// import androidx.compose.ui.res.painterResource // REMOVE this
// import com.nadavariel.dietapp.NavRoutes // REMOVE this
// import com.nadavariel.dietapp.R // REMOVE this

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nadavariel.dietapp.AuthViewModel
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement // Ensure this is present if using it for verticalArrangement

@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    // REMOVE navController: NavController parameter from here
    onSignOut: () -> Unit
) {
    val currentUser = Firebase.auth.currentUser

    // The Scaffold and NavigationBar are now in MainActivity.kt
    // The padding is passed to this Column via the Modifier from NavHost
    Column(
        modifier = Modifier
            .fillMaxSize()
            // .padding(paddingValues) <--- This padding is now passed directly via the modifier
            .padding(24.dp), // Keep your internal padding
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