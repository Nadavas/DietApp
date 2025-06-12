package com.nadavariel.dietapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold // Keep this import
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nadavariel.dietapp.screens.Greeting
import com.nadavariel.dietapp.screens.SignInScreen
import com.nadavariel.dietapp.screens.SignUpScreen
import com.nadavariel.dietapp.ui.theme.DietAppTheme
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.remember
import com.nadavariel.dietapp.data.UserPreferencesRepository
import com.nadavariel.dietapp.data.dataStore
import com.nadavariel.dietapp.screens.HomeScreen
import com.nadavariel.dietapp.screens.MyProfileScreen

// NEW IMPORTS FOR BOTTOM NAVIGATION BAR
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.res.painterResource
// END NEW IMPORTS

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DietAppTheme {
                val navController = rememberNavController()

                val preferencesRepository = remember { UserPreferencesRepository(applicationContext) }

                val authViewModelFactory = remember {
                    object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return AuthViewModel(preferencesRepository) as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class")
                        }
                    }
                }

                val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)

                val startDestination = if (authViewModel.isUserSignedIn()) {
                    NavRoutes.HOME
                } else {
                    NavRoutes.LANDING
                }

                // --- START MOVED SCAFFOLD AND BOTTOM NAV BAR ---
                val currentRoute by navController.currentBackStackEntryAsState()
                val selectedRoute = currentRoute?.destination?.route

                Scaffold(
                    modifier = Modifier,
                    bottomBar = {
                        // Only show the bottom navigation bar if the current route is HOME or MY_PROFILE
                        // This prevents it from showing on login/signup/landing screens
                        if (selectedRoute == NavRoutes.HOME || selectedRoute == NavRoutes.MY_PROFILE) {
                            NavigationBar {
                                // Home button
                                NavigationBarItem(
                                    selected = selectedRoute == NavRoutes.HOME,
                                    onClick = { navController.navigate(NavRoutes.HOME) {
                                        popUpTo(NavRoutes.HOME) { // Pop up to Home, ensuring it's the single top in this stack
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }},
                                    icon = { Icon(painterResource(id = R.drawable.ic_home_filled), contentDescription = "Home") },
                                    label = { Text("Home") }
                                )

                                // My Profile button
                                NavigationBarItem(
                                    selected = selectedRoute == NavRoutes.MY_PROFILE,
                                    onClick = { navController.navigate(NavRoutes.MY_PROFILE) {
                                        popUpTo(NavRoutes.HOME) { // Pop up to Home, ensuring it's the single top in this stack
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }},
                                    icon = { Icon(painterResource(id = R.drawable.ic_person_filled), contentDescription = "My Profile") },
                                    label = { Text("Profile") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        // Apply the padding from Scaffold to the NavHost content
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavRoutes.LANDING) {
                            authViewModel.resetAuthResult()
                            Greeting(
                                onSignInClick = {
                                    authViewModel.clearInputFields()
                                    navController.navigate(NavRoutes.SIGN_IN)
                                },
                                onSignUpClick = {
                                    authViewModel.clearInputFields()
                                    navController.navigate(NavRoutes.SIGN_UP)
                                }
                            )
                        }
                        composable(NavRoutes.SIGN_IN) {
                            SignInScreen(
                                authViewModel = authViewModel,
                                onBack = {
                                    authViewModel.clearInputFields()
                                    navController.popBackStack()
                                },
                                onSignInSuccess = {
                                    navController.navigate(NavRoutes.HOME) {
                                        popUpTo(NavRoutes.LANDING) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(NavRoutes.SIGN_UP) {
                            SignUpScreen(
                                authViewModel = authViewModel,
                                onBack = {
                                    authViewModel.clearInputFields()
                                    navController.popBackStack()
                                },
                                onSignUpSuccess = {
                                    navController.navigate(NavRoutes.HOME) {
                                        popUpTo(NavRoutes.LANDING) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(NavRoutes.HOME) {
                            // HomeScreen no longer needs its own Scaffold or NavController passed directly
                            HomeScreen(
                                authViewModel = authViewModel,
                                // navController = navController, // REMOVE this parameter from HomeScreen
                                onSignOut = {
                                    navController.navigate(NavRoutes.LANDING) {
                                        popUpTo(NavRoutes.HOME) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(NavRoutes.MY_PROFILE) {
                            MyProfileScreen(
                                // authViewModel = authViewModel // Optional, pass if needed
                            )
                        }
                    }
                }
                // --- END MOVED SCAFFOLD AND BOTTOM NAV BAR ---
            }
        }
    }
}