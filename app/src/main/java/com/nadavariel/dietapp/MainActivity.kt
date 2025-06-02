package com.nadavariel.dietapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
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
import com.nadavariel.dietapp.screens.HomeScreen


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DietAppTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = viewModel() // Get the ViewModel instance

                // Determine start destination based on sign-in state
                val startDestination = if (authViewModel.isUserSignedIn()) {
                    NavRoutes.HOME
                } else {
                    NavRoutes.LANDING
                }

                Scaffold(modifier = Modifier) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination, // Dynamic start destination
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavRoutes.LANDING) {
                            authViewModel.resetAuthResult() // Clear any previous auth state
                            Greeting(
                                onSignInClick = {
                                    authViewModel.clearInputFields() // Clear fields before navigating
                                    navController.navigate(NavRoutes.SIGN_IN)
                                },
                                onSignUpClick = {
                                    authViewModel.clearInputFields() // Clear fields before navigating
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
                                        popUpTo(NavRoutes.LANDING) { inclusive = true } // Clear back stack up to landing
                                        launchSingleTop = true // Avoid multiple copies of Home
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
                                        popUpTo(NavRoutes.LANDING) { inclusive = true } // Clear back stack
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(NavRoutes.HOME) {
                            HomeScreen(
                                authViewModel = authViewModel,
                                onSignOut = {
                                    navController.navigate(NavRoutes.LANDING) {
                                        popUpTo(NavRoutes.HOME) { inclusive = true } // Clear home from back stack
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
