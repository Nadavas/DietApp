package com.nadavariel.dietapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nadavariel.dietapp.data.UserPreferencesRepository
import com.nadavariel.dietapp.screens.Greeting
import com.nadavariel.dietapp.screens.HomeScreen
import com.nadavariel.dietapp.screens.MyProfileScreen
import com.nadavariel.dietapp.screens.SignInScreen
import com.nadavariel.dietapp.screens.SignUpScreen
import com.nadavariel.dietapp.screens.UpdateProfileScreen
import com.nadavariel.dietapp.ui.theme.DietAppTheme
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.screens.AddEditMealScreen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add

// NO LONGER DEFINED HERE. It should be in NavRoutes.kt file.

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DietAppTheme {
                val navController = rememberNavController()

                val preferencesRepository = remember { UserPreferencesRepository(applicationContext) }

                val appViewModelFactory = remember {
                    object : ViewModelProvider.Factory {
                        @RequiresApi(Build.VERSION_CODES.O)
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return AuthViewModel(preferencesRepository) as T
                            }
                            if (modelClass.isAssignableFrom(FoodLogViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return FoodLogViewModel() as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class")
                        }
                    }
                }

                val authViewModel: AuthViewModel = viewModel(factory = appViewModelFactory)
                val foodLogViewModel: FoodLogViewModel = viewModel(factory = appViewModelFactory)

                val startDestination = if (authViewModel.isUserSignedIn()) {
                    NavRoutes.HOME
                } else {
                    NavRoutes.LANDING
                }

                val currentRoute by navController.currentBackStackEntryAsState()
                val selectedRoute = currentRoute?.destination?.route

                Scaffold(
                    modifier = Modifier,
                    bottomBar = {
                        if (selectedRoute == NavRoutes.HOME ||
                            selectedRoute == NavRoutes.MY_PROFILE ||
                            selectedRoute == NavRoutes.ADD_EDIT_MEAL
                        ) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedRoute == NavRoutes.HOME,
                                    onClick = { navController.navigate(NavRoutes.HOME) {
                                        popUpTo(NavRoutes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }},
                                    icon = { Icon(painterResource(id = R.drawable.ic_home_filled), contentDescription = "Home") },
                                    label = { Text("Home") }
                                )

                                NavigationBarItem(
                                    selected = selectedRoute == NavRoutes.ADD_EDIT_MEAL,
                                    onClick = { navController.navigate(NavRoutes.ADD_EDIT_MEAL) {
                                        popUpTo(NavRoutes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }},
                                    icon = { Icon(Icons.Filled.Add, contentDescription = "Add Meal") },
                                    label = { Text("Add Meal") }
                                )

                                NavigationBarItem(
                                    selected = selectedRoute == NavRoutes.MY_PROFILE,
                                    onClick = { navController.navigate(NavRoutes.MY_PROFILE) {
                                        popUpTo(NavRoutes.HOME) { saveState = true }
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
                                },
                                onNavigateToSignUp = {
                                    navController.navigate(NavRoutes.SIGN_UP) {
                                        popUpTo(NavRoutes.SIGN_IN) { inclusive = true }
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
                                    navController.navigate(NavRoutes.UPDATE_PROFILE) {
                                        popUpTo(NavRoutes.LANDING) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onNavigateToSignIn = {
                                    navController.navigate(NavRoutes.SIGN_IN) {
                                        popUpTo(NavRoutes.SIGN_UP) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable(NavRoutes.HOME) {
                            HomeScreen(
                                authViewModel = authViewModel,
                                foodLogViewModel = foodLogViewModel,
                                navController = navController,
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
                                authViewModel = authViewModel,
                                navController = navController
                            )
                        }
                        composable(NavRoutes.UPDATE_PROFILE) {
                            UpdateProfileScreen(
                                authViewModel = authViewModel,
                                navController = navController,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(NavRoutes.ADD_EDIT_MEAL) {
                            AddEditMealScreen(
                                foodLogViewModel = foodLogViewModel,
                                navController = navController,
                                mealToEdit = null
                            )
                        }
                    }
                }
            }
        }
    }
}