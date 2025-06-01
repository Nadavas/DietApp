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


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DietAppTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.LANDING,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavRoutes.LANDING) {
                            Greeting(
                                onSignInClick = { navController.navigate(NavRoutes.SIGN_IN) },
                                onSignUpClick = { navController.navigate(NavRoutes.SIGN_UP) }
                            )
                        }
                        composable(NavRoutes.SIGN_IN) {
                            SignInScreen(onBack = { navController.popBackStack() })
                        }
                        composable(NavRoutes.SIGN_UP) {
                            SignUpScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
