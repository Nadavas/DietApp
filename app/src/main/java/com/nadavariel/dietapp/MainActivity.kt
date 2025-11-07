package com.nadavariel.dietapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect // <-- IMPORT ADDED
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.nadavariel.dietapp.data.UserPreferencesRepository
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.screens.*
import com.nadavariel.dietapp.ui.DietAppTheme
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.ThreadViewModel
import com.nadavariel.dietapp.viewmodel.NotificationViewModel
import com.nadavariel.dietapp.screens.EnergyDetailScreen
import com.nadavariel.dietapp.screens.MacrosDetailScreen
import com.nadavariel.dietapp.screens.CarbsDetailScreen
import com.nadavariel.dietapp.screens.MineralsDetailScreen
import com.nadavariel.dietapp.screens.VitaminsDetailScreen

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            // You will need to rename this to UserPreferences(applicationContext) after your file merge
            val preferencesRepository = remember { UserPreferencesRepository(applicationContext) }

            val appViewModelFactory = remember {
                object : ViewModelProvider.Factory {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return when {
                            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                                @Suppress("UNCHECKED_CAST")
                                AuthViewModel(preferencesRepository) as T
                            }
                            modelClass.isAssignableFrom(FoodLogViewModel::class.java) -> {
                                @Suppress("UNCHECKED_CAST")
                                FoodLogViewModel() as T
                            }
                            modelClass.isAssignableFrom(ThreadViewModel::class.java) -> {
                                @Suppress("UNCHECKED_CAST")
                                ThreadViewModel() as T
                            }
                            modelClass.isAssignableFrom(NotificationViewModel::class.java) -> {
                                @Suppress("UNCHECKED_CAST")
                                NotificationViewModel(application) as T
                            }
                            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                        }
                    }
                }
            }

            val authViewModel: AuthViewModel = viewModel(factory = appViewModelFactory)
            val foodLogViewModel: FoodLogViewModel = viewModel(factory = appViewModelFactory)
            val threadViewModel: ThreadViewModel = viewModel(factory = appViewModelFactory)
            val notificationViewModel: NotificationViewModel = viewModel(factory = appViewModelFactory)

            val isDarkModeEnabled by authViewModel.isDarkModeEnabled.collectAsStateWithLifecycle()
            val hasMissingProfileDetails by authViewModel.hasMissingPrimaryProfileDetails.collectAsStateWithLifecycle()
            val isLoadingProfile by authViewModel.isLoadingProfile.collectAsStateWithLifecycle()

            val useDarkTheme = isDarkModeEnabled || isSystemInDarkTheme()

            // --- FIX 1: Read the currentUser state directly ---
            val currentUser = authViewModel.currentUser

            DietAppTheme(darkTheme = useDarkTheme) {
                // --- FIX 2: Add a LaunchedEffect to react to auth changes ---
                LaunchedEffect(currentUser, isLoadingProfile) {
                    if (!isLoadingProfile) { // Only navigate if we're not in the initial loading phase
                        if (currentUser == null) {
                            // User signed out or was deleted, force navigate to LANDING
                            navController.navigate(NavRoutes.LANDING) {
                                popUpTo(navController.graph.id) { // Pop the entire back stack
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                }

                val startDestination = if (authViewModel.isUserSignedIn()) {
                    NavRoutes.HOME
                } else {
                    NavRoutes.LANDING
                }

                val currentRouteEntry by navController.currentBackStackEntryAsState()

                // --- FIX: This now splits by '?' to remove parameters before checking the route ---
                val selectedRoute = currentRouteEntry?.destination?.route
                    ?.split("?")?.firstOrNull()
                    ?.split("/")?.firstOrNull()

                val showLoadingScreen = authViewModel.isUserSignedIn() && isLoadingProfile

                if (showLoadingScreen) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Scaffold(
                        modifier = Modifier,
                        bottomBar = {
                            val isStatsDetailScreen = selectedRoute?.startsWith("stats/") == true
                            if (!isStatsDetailScreen && (
                                        selectedRoute == NavRoutes.HOME ||
                                                selectedRoute == NavRoutes.ADD_EDIT_MEAL ||
                                                selectedRoute == NavRoutes.STATISTICS ||
                                                selectedRoute == NavRoutes.THREADS ||
                                                selectedRoute == NavRoutes.ACCOUNT
                                        )) {
                                NavigationBar(
                                    containerColor = Color(0xFF4CAF50),
                                    contentColor = Color.White.copy(alpha = 0.9f)
                                ) {
                                    NavigationBarItem(
                                        selected = selectedRoute == NavRoutes.HOME,
                                        onClick = {
                                            navController.navigate(NavRoutes.HOME) {
                                                popUpTo(NavRoutes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(painterResource(id = R.drawable.ic_home_filled), contentDescription = "Home") },
                                        label = { Text("Home") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            selectedTextColor = Color.White,
                                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                            unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                            indicatorColor = Color.White.copy(alpha = 0.15f)
                                        )
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
                                        label = { Text("Add Meal") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            selectedTextColor = Color.White,
                                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                            unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                            indicatorColor = Color.White.copy(alpha = 0.15f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = selectedRoute == NavRoutes.STATISTICS,
                                        onClick = {
                                            navController.navigate(NavRoutes.STATISTICS) {
                                                popUpTo(NavRoutes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(painterResource(id = R.drawable.ic_bar_filled), contentDescription = "Stats") },
                                        label = { Text("Stats") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            selectedTextColor = Color.White,
                                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                            unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                            indicatorColor = Color.White.copy(alpha = 0.15f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = selectedRoute == NavRoutes.THREADS,
                                        onClick = {
                                            navController.navigate(NavRoutes.THREADS) {
                                                popUpTo(NavRoutes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(painterResource(id = R.drawable.ic_forum), contentDescription = "Threads") },
                                        label = { Text("Threads") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            selectedTextColor = Color.White,
                                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                            unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                            indicatorColor = Color.White.copy(alpha = 0.15f)
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = selectedRoute == NavRoutes.ACCOUNT,
                                        onClick = {
                                            navController.navigate(NavRoutes.ACCOUNT) {
                                                popUpTo(NavRoutes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            BadgedBox(
                                                badge = {
                                                    if (hasMissingProfileDetails) {
                                                        Badge(
                                                            Modifier
                                                                .size(8.dp)
                                                                .offset(x = 8.dp, y = (-4).dp)
                                                        ) {}
                                                    }
                                                }
                                            ) {
                                                Icon(painterResource(id = R.drawable.ic_person_filled), contentDescription = "Account")
                                            }
                                        },
                                        label = { Text("Account") },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Color.White,
                                            selectedTextColor = Color.White,
                                            unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                            unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                            indicatorColor = Color.White.copy(alpha = 0.15f)
                                        )
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
                            composable(
                                route = "${NavRoutes.HOME}?openWeightLog={openWeightLog}",
                                arguments = listOf(
                                    navArgument("openWeightLog") {
                                        type = NavType.BoolType
                                        defaultValue = false
                                    }
                                ),
                                deepLinks = listOf(
                                    navDeepLink { uriPattern = "dietapp://home?openWeightLog={openWeightLog}" }
                                )
                            ) { backStackEntry ->
                                val openWeightLog = backStackEntry.arguments?.getBoolean("openWeightLog") == true

                                HomeScreen(
                                    authViewModel = authViewModel,
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController,
                                    openWeightLog = openWeightLog
                                )
                            }
                            composable(NavRoutes.STATISTICS) {
                                StatisticsScreen(
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController
                                )
                            }
                            // NEW: Energy & Protein Detail Screen
                            composable(NavRoutes.STATS_ENERGY) {
                                EnergyDetailScreen(
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController
                                )
                            }

// NEW: Macronutrients Detail Screen
                            composable(NavRoutes.STATS_MACROS) {
                                MacrosDetailScreen(
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController
                                )
                            }

// NEW: Fiber & Sugar Detail Screen
                            composable(NavRoutes.STATS_CARBS) {
                                CarbsDetailScreen(
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController
                                )
                            }

// NEW: Minerals Detail Screen
                            composable(NavRoutes.STATS_MINERALS) {
                                MineralsDetailScreen(
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController
                                )
                            }

// NEW: Vitamins Detail Screen
                            composable(NavRoutes.STATS_VITAMINS) {
                                VitaminsDetailScreen(
                                    foodLogViewModel = foodLogViewModel,
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
                            composable(NavRoutes.NOTIFICATIONS) {
                                NotificationScreen(
                                    navController = navController,
                                    notificationViewModel = notificationViewModel
                                )
                            }
                            composable(NavRoutes.QUESTIONS) {
                                QuestionsScreen(
                                    navController = navController
                                )
                            }
                            composable(NavRoutes.GOALS) {
                                GoalsScreen(
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
                                    type = NavType.StringType; defaultValue = "false"; nullable = true
                                })
                            ) { backStackEntry ->
                                val isNewUserString = backStackEntry.arguments?.getString(NavRoutes.IS_NEW_USER_ARG)
                                val isNewUser = isNewUserString?.toBooleanStrictOrNull() == true
                                UpdateProfileScreen(
                                    authViewModel = authViewModel,
                                    navController = navController,
                                    onBack = { navController.popBackStack() },
                                    isNewUser = isNewUser
                                )
                            }
                            composable(
                                route = NavRoutes.ADD_EDIT_MEAL,
                                deepLinks = listOf(
                                    navDeepLink { uriPattern = "dietapp://add_meal" }
                                )
                            ) {
                                AddEditMealScreen(
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController,
                                    mealToEdit = null
                                )
                            }
                            composable(
                                route = NavRoutes.ADD_EDIT_MEAL_WITH_ID,
                                arguments = listOf(navArgument(NavRoutes.MEAL_ID_ARG) {
                                    type = NavType.StringType; nullable = true
                                })
                            ) { backStackEntry ->
                                val mealId = backStackEntry.arguments?.getString(NavRoutes.MEAL_ID_ARG)
                                val mealToEdit: Meal? by produceState<Meal?>(initialValue = null, mealId) {
                                    value = if (mealId != null) foodLogViewModel.getMealById(mealId) else null
                                }
                                AddEditMealScreen(
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController,
                                    mealToEdit = mealToEdit
                                )
                            }

                            // --- Thread routes ---
                            composable(NavRoutes.THREADS) {
                                ThreadsScreen(
                                    navController = navController,
                                    threadViewModel = threadViewModel,
                                )
                            }
                            composable(NavRoutes.CREATE_THREAD) {
                                CreateThreadScreen(
                                    navController = navController,
                                    threadViewModel = threadViewModel
                                )
                            }
                            composable(
                                route = NavRoutes.THREAD_DETAIL_WITH_ARG,
                                arguments = listOf(navArgument("threadId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val threadId = backStackEntry.arguments?.getString("threadId")
                                if (threadId != null) {
                                    ThreadDetailScreen(
                                        navController = navController,
                                        threadId = threadId,
                                        threadViewModel = threadViewModel,
                                        authViewModel = authViewModel
                                    )
                                } else {
                                    Text("Error: Thread ID is missing.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}