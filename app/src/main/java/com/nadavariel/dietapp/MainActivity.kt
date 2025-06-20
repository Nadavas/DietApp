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
import androidx.compose.runtime.produceState // Needed for getMealById in FoodLogViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
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
import com.nadavariel.dietapp.model.Meal // Make sure Meal data class is imported

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

                // ⭐ REVERTED THIS PART BACK TO YOUR ORIGINAL LOGIC
                val startDestination = if (authViewModel.isUserSignedIn()) {
                    NavRoutes.HOME // Assuming isProfileSetup check is handled within HomeScreen or not strictly needed here for initial nav
                } else {
                    NavRoutes.LANDING
                }

                val currentRouteEntry by navController.currentBackStackEntryAsState()
                // Get the base route (without arguments) for bottom bar selection
                val selectedRoute = currentRouteEntry?.destination?.route?.split("/")?.firstOrNull()


                Scaffold(
                    modifier = Modifier,
                    bottomBar = {
                        // Display bottom bar only on specific routes
                        if (selectedRoute == NavRoutes.HOME ||
                            selectedRoute == NavRoutes.ADD_EDIT_MEAL ||
                            selectedRoute == NavRoutes.MY_PROFILE
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
                                    // Highlight if either "add_edit_meal_screen" or "add_edit_meal_screen/{mealId}" is active
                                    selected = selectedRoute == NavRoutes.ADD_EDIT_MEAL,
                                    onClick = {
                                        // When clicking 'Add Meal' from bottom bar, always navigate to add new meal route
                                        navController.navigate(NavRoutes.ADD_EDIT_MEAL) {
                                            popUpTo(NavRoutes.HOME) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
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

                        // Composable for adding a new meal (no ID in route)
                        composable(NavRoutes.ADD_EDIT_MEAL) {
                            AddEditMealScreen(
                                foodLogViewModel = foodLogViewModel,
                                navController = navController,
                                mealToEdit = null // Explicitly pass null for adding new
                            )
                        }

                        // COMPOSABLE BLOCK FOR EDITING MEALS WITH ID (Uses produceState for suspend function)
                        composable(
                            route = NavRoutes.ADD_EDIT_MEAL_WITH_ID, // e.g., "add_edit_meal_screen/{mealId}"
                            arguments = listOf(navArgument(NavRoutes.MEAL_ID_ARG) {
                                type = NavType.StringType
                                nullable = true // Still nullable, as it's the same composable, but here it expects an ID for editing
                            })
                        ) { backStackEntry ->
                            val mealId = backStackEntry.arguments?.getString(NavRoutes.MEAL_ID_ARG)

                            // Use produceState to safely call the suspend function
                            val mealToEdit: Meal? by produceState<Meal?>(initialValue = null, mealId) {
                                if (mealId != null) {
                                    value = foodLogViewModel.getMealById(mealId) // Call the suspend function
                                } else {
                                    value = null // Should ideally not be hit with null mealId for this route
                                }
                            }

                            AddEditMealScreen(
                                foodLogViewModel = foodLogViewModel,
                                navController = navController,
                                mealToEdit = mealToEdit // Pass the meal object (will be null while fetching, then updated)
                            )
                        }
                    }
                }
            }
        }
    }
}