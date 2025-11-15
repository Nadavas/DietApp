package com.nadavariel.dietapp

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.nadavariel.dietapp.model.FoodNutritionalInfo
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.screens.*
import com.nadavariel.dietapp.ui.DietAppTheme
import com.nadavariel.dietapp.ui.components.GeminiConfirmationDialog
import com.nadavariel.dietapp.ui.components.HoveringNotificationCard
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.ThreadViewModel
import com.nadavariel.dietapp.viewmodel.NotificationViewModel
import com.nadavariel.dietapp.screens.EnergyDetailScreen
import com.nadavariel.dietapp.screens.MacrosDetailScreen
import com.nadavariel.dietapp.screens.CarbsDetailScreen
import com.nadavariel.dietapp.screens.MineralsDetailScreen
import com.nadavariel.dietapp.screens.VitaminsDetailScreen
import com.nadavariel.dietapp.viewmodel.DietPlanResult
import com.nadavariel.dietapp.viewmodel.GeminiResult
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import com.nadavariel.dietapp.viewmodel.QuestionsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                            modelClass.isAssignableFrom(QuestionsViewModel::class.java) -> {
                                @Suppress("UNCHECKED_CAST")
                                QuestionsViewModel() as T
                            }
                            modelClass.isAssignableFrom(GoalsViewModel::class.java) -> {
                                @Suppress("UNCHECKED_CAST")
                                GoalsViewModel() as T
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
            val questionsViewModel: QuestionsViewModel = viewModel(factory = appViewModelFactory)
            val goalsViewModel: GoalsViewModel = viewModel(factory = appViewModelFactory)


            val isDarkModeEnabled by authViewModel.isDarkModeEnabled.collectAsStateWithLifecycle()
            val isLoadingProfile by authViewModel.isLoadingProfile.collectAsStateWithLifecycle()
            val useDarkTheme = isDarkModeEnabled || isSystemInDarkTheme()
            val currentUser = authViewModel.currentUser

            var showGeminiDialog by remember { mutableStateOf<List<FoodNutritionalInfo>?>(null) }

            DietAppTheme(darkTheme = useDarkTheme) {

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val dietPlanResult by questionsViewModel.dietPlanResult.collectAsStateWithLifecycle()
                val geminiResult by foodLogViewModel.geminiResult.collectAsStateWithLifecycle()

                val dietPlanLoadingMessage = remember(dietPlanResult) {
                    if (dietPlanResult is DietPlanResult.Loading) "Building your plan..." else null
                }
                val mealLoadingMessage = remember(geminiResult) {
                    if (geminiResult is GeminiResult.Loading) "Analyzing meal..." else null
                }

                var planReadyMessage by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(dietPlanResult) {
                    when(val result = dietPlanResult) {
                        is DietPlanResult.Success -> {
                            planReadyMessage = "Your plan is ready! Click to view."

                            delay(3000L)

                            if (planReadyMessage != null) {
                                planReadyMessage = null
                                questionsViewModel.resetDietPlanResult()
                            }
                        }
                        is DietPlanResult.Error -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Error creating plan: ${result.message}",
                                    duration = SnackbarDuration.Long
                                )
                                questionsViewModel.resetDietPlanResult()
                            }
                        }
                        else -> { /* Do nothing for Idle/Loading */ }
                    }
                }

                LaunchedEffect(geminiResult) {
                    when(val result = geminiResult) {
                        is GeminiResult.Success -> {
                            Log.d("MainActivity", "Gemini success detected. Showing dialog.")
                            showGeminiDialog = result.foodInfoList
                        }
                        is GeminiResult.Error -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Error analyzing food: ${result.message}",
                                    duration = SnackbarDuration.Long
                                )
                                foodLogViewModel.resetGeminiResult()
                            }
                        }
                        else -> { /* Do nothing for Idle/Loading */ }
                    }
                }

                LaunchedEffect(currentUser, isLoadingProfile) {
                    if (!isLoadingProfile) {
                        if (currentUser == null) {
                            navController.navigate(NavRoutes.LANDING) {
                                popUpTo(navController.graph.id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                }

                val startDestination = remember {
                    if (authViewModel.isUserSignedIn()) {
                        NavRoutes.HOME
                    } else {
                        NavRoutes.LANDING
                    }
                }

                val currentRouteEntry by navController.currentBackStackEntryAsState()
                val selectedRoute = currentRouteEntry?.destination?.route
                    ?.split("?")?.firstOrNull()
                    ?.split("/")?.firstOrNull()

                Scaffold(
                    modifier = Modifier,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
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
                                        Icon(painterResource(id = R.drawable.ic_person_filled), contentDescription = "Account")
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.padding(innerPadding).fillMaxSize()
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
                                    onSignInSuccess = { isNewUser ->
                                        val route = if (isNewUser) "${NavRoutes.QUESTIONS}?startQuiz=true" else NavRoutes.HOME
                                        navController.navigate(route) {
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
                                    onSignUpSuccess = { isNewUser ->
                                        if (isNewUser) {
                                            // New user (Email or Google) -> Go to Questions
                                            navController.navigate("${NavRoutes.QUESTIONS}?startQuiz=true")
                                            // We DO NOT pop the stack here, so back button works
                                        } else {
                                            // Existing Google user -> Go to Home
                                            navController.navigate(NavRoutes.HOME) {
                                                popUpTo(NavRoutes.LANDING) { inclusive = true }
                                                launchSingleTop = true
                                            }
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
                                    goalViewModel = goalsViewModel,
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

                            composable(NavRoutes.STATS_ENERGY) {
                                EnergyDetailScreen(
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController
                                )
                            }
                            composable(NavRoutes.STATS_MACROS) {
                                MacrosDetailScreen(
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController
                                )
                            }
                            composable(NavRoutes.STATS_CARBS) {
                                CarbsDetailScreen(
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController
                                )
                            }
                            composable(NavRoutes.STATS_MINERALS) {
                                MineralsDetailScreen(
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController
                                )
                            }
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

                            composable(
                                route = "${NavRoutes.QUESTIONS}?startQuiz={startQuiz}",
                                arguments = listOf(
                                    navArgument("startQuiz") {
                                        type = NavType.BoolType
                                        defaultValue = false
                                    }
                                )
                            ) { backStackEntry ->
                                val startQuiz = backStackEntry.arguments?.getBoolean("startQuiz") == true
                                QuestionsScreen(
                                    navController = navController,
                                    questionsViewModel = questionsViewModel,
                                    authViewModel = authViewModel,
                                    startQuiz = startQuiz
                                )
                            }

                            composable(NavRoutes.GOALS) {
                                GoalsScreen(
                                    navController = navController,
                                    goalsViewModel = goalsViewModel
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

                            composable(NavRoutes.ADD_MANUAL_MEAL) {
                                AddManualMealScreen(
                                    foodLogViewModel = foodLogViewModel,
                                    navController = navController
                                )
                            }

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

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(innerPadding)
                                .padding(bottom = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimatedVisibility(
                                visible = dietPlanLoadingMessage != null,
                                enter = slideInVertically { it } + fadeIn(),
                                exit = slideOutVertically { it } + fadeOut()
                            ) {
                                dietPlanLoadingMessage?.let { msg ->
                                    HoveringNotificationCard(
                                        message = msg,
                                        showSpinner = true,
                                        onClick = null
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = mealLoadingMessage != null,
                                enter = slideInVertically { it } + fadeIn(),
                                exit = slideOutVertically { it } + fadeOut()
                            ) {
                                mealLoadingMessage?.let { msg ->
                                    HoveringNotificationCard(
                                        message = msg,
                                        showSpinner = true,
                                        onClick = null
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = planReadyMessage != null,
                                enter = slideInVertically { it } + fadeIn(),
                                exit = slideOutVertically { it } + fadeOut()
                            ) {
                                planReadyMessage?.let { msg ->
                                    HoveringNotificationCard(
                                        message = msg,
                                        showSpinner = false,
                                        onClick = {
                                            navController.navigate(NavRoutes.GOALS)
                                            planReadyMessage = null
                                            questionsViewModel.resetDietPlanResult()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showGeminiDialog != null) {
                    GeminiConfirmationDialog(
                        foodInfoList = showGeminiDialog!!,
                        onAccept = { modifiedList ->
                            foodLogViewModel.logMealsFromFoodInfoList(modifiedList)
                            showGeminiDialog = null
                        },
                        onEdit = {
                            Log.d("MainActivity", "Edit button clicked")
                        },
                        onCancel = {
                            foodLogViewModel.resetGeminiResult()
                            showGeminiDialog = null
                        }
                    )
                }
            }
        }
    }
}