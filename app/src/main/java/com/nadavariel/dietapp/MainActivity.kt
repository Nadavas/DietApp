package com.nadavariel.dietapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
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
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.screens.StatisticsScreen
import com.nadavariel.dietapp.screens.AccountScreen
import com.nadavariel.dietapp.screens.SettingsScreen
import com.nadavariel.dietapp.screens.ChangePasswordScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.offset
import com.nadavariel.dietapp.screens.QuestionsScreen


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
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

            val isDarkModeEnabled by authViewModel.isDarkModeEnabled.collectAsStateWithLifecycle()
            val hasMissingProfileDetails by authViewModel.hasMissingPrimaryProfileDetails.collectAsStateWithLifecycle()

            val useDarkTheme = isDarkModeEnabled || isSystemInDarkTheme()

            DietAppTheme(darkTheme = useDarkTheme) {
                val startDestination = if (authViewModel.isUserSignedIn()) {
                    NavRoutes.HOME
                } else {
                    NavRoutes.LANDING
                }

                val currentRouteEntry by navController.currentBackStackEntryAsState()
                val selectedRoute = currentRouteEntry?.destination?.route?.split("/")?.firstOrNull()


                Scaffold(
                    modifier = Modifier,
                    bottomBar = {
                        if (selectedRoute == NavRoutes.HOME ||
                            selectedRoute == NavRoutes.ADD_EDIT_MEAL ||
                            selectedRoute == NavRoutes.STATISTICS ||
                            selectedRoute == NavRoutes.ACCOUNT
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
                                    onClick = {
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
                                    selected = selectedRoute == NavRoutes.STATISTICS,
                                    onClick = { navController.navigate(NavRoutes.STATISTICS) {
                                        popUpTo(NavRoutes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }},
                                    icon = { Icon(painterResource(id = R.drawable.ic_bar_filled), contentDescription = "Stats") },
                                    label = { Text("Stats") }
                                )

                                NavigationBarItem(
                                    selected = selectedRoute == NavRoutes.ACCOUNT,
                                    onClick = { navController.navigate(NavRoutes.ACCOUNT) {
                                        popUpTo(NavRoutes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }},
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (hasMissingProfileDetails) {
                                                    Badge(
                                                        Modifier
                                                            .size(8.dp)
                                                            .offset(x = 8.dp, y = (-4).dp)
                                                    ) {
                                                        // Content of the badge. Material Design recommends
                                                        // a dot for notifications or small numbers.
                                                        // By default, Badge uses theme's error color, which is typically red.

                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(painterResource(id = R.drawable.ic_person_filled), contentDescription = "Account")
                                        }
                                    },
                                    label = { Text("Account") }
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
                                    navController.navigate("${NavRoutes.UPDATE_PROFILE_BASE}?${NavRoutes.IS_NEW_USER_ARG}=true") {
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
                            )
                        }
                        composable(NavRoutes.STATISTICS) {
                            StatisticsScreen(
                                navController = navController
                            )
                        }

                        composable(NavRoutes.ACCOUNT) {
                            AccountScreen(
                                navController = navController,
                                authViewModel = authViewModel
                            )
                        }

                        composable(NavRoutes.MY_PROFILE) {
                            MyProfileScreen(
                                authViewModel = authViewModel,
                                navController = navController
                            )
                        }

                        composable(NavRoutes.SETTINGS) {
                            SettingsScreen(
                                navController = navController,
                                authViewModel = authViewModel
                            )
                        }

                        composable(NavRoutes.QUESTIONS) {
                            QuestionsScreen(
                                navController = navController
                            )
                        }

                        composable(NavRoutes.CHANGE_PASSWORD) {
                            ChangePasswordScreen(
                                navController = navController,
                                authViewModel = authViewModel
                            )
                        }

                        composable(
                            route = NavRoutes.UPDATE_PROFILE,
                            arguments = listOf(navArgument(NavRoutes.IS_NEW_USER_ARG) {
                                type = NavType.StringType
                                defaultValue = "false"
                                nullable = true
                            })
                        ) { backStackEntry ->
                            val isNewUserString = backStackEntry.arguments?.getString(NavRoutes.IS_NEW_USER_ARG)
                            val isNewUser = isNewUserString?.toBooleanStrictOrNull() ?: false

                            UpdateProfileScreen(
                                authViewModel = authViewModel,
                                navController = navController,
                                onBack = { navController.popBackStack() },
                                isNewUser = isNewUser
                            )
                        }

                        composable(NavRoutes.ADD_EDIT_MEAL) {
                            AddEditMealScreen(
                                foodLogViewModel = foodLogViewModel,
                                navController = navController,
                                mealToEdit = null
                            )
                        }

                        composable(
                            route = NavRoutes.ADD_EDIT_MEAL_WITH_ID,
                            arguments = listOf(navArgument(NavRoutes.MEAL_ID_ARG) {
                                type = NavType.StringType
                                nullable = true
                            })
                        ) { backStackEntry ->
                            val mealId = backStackEntry.arguments?.getString(NavRoutes.MEAL_ID_ARG)

                            val mealToEdit: Meal? by produceState<Meal?>(initialValue = null, mealId) {
                                value = if (mealId != null) {
                                    foodLogViewModel.getMealById(mealId)
                                } else {
                                    null
                                }
                            }

                            AddEditMealScreen(
                                foodLogViewModel = foodLogViewModel,
                                navController = navController,
                                mealToEdit = mealToEdit
                            )
                        }
                    }
                }
            }
        }
    }
}