package com.nadavariel.dietapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.nadavariel.dietapp.repositories.UserPreferencesRepository
import com.nadavariel.dietapp.models.FoodNutritionalInfo
import com.nadavariel.dietapp.models.Meal
import com.nadavariel.dietapp.screens.*
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.DietAppTheme
import com.nadavariel.dietapp.ui.GeminiConfirmationDialog
import com.nadavariel.dietapp.ui.HoveringNotificationCard
import com.nadavariel.dietapp.viewmodels.*
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

            @Suppress("UNCHECKED_CAST")
            val appViewModelFactory = remember {
                object : ViewModelProvider.Factory {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return when {
                            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                                AuthViewModel(preferencesRepository) as T
                            }
                            modelClass.isAssignableFrom(FoodLogViewModel::class.java) -> {
                                FoodLogViewModel() as T
                            }
                            modelClass.isAssignableFrom(ThreadViewModel::class.java) -> {
                                ThreadViewModel() as T
                            }
                            modelClass.isAssignableFrom(ReminderViewModel::class.java) -> {
                                ReminderViewModel(application) as T
                            }
                            modelClass.isAssignableFrom(QuizViewModel::class.java) -> {
                                QuizViewModel() as T
                            }
                            modelClass.isAssignableFrom(DietPlanViewModel::class.java) -> {
                                DietPlanViewModel() as T
                            }
                            else ->
                                throw IllegalArgumentException(
                                    "Unknown ViewModel class: ${modelClass.name}"
                                )
                        }
                    }
                }
            }

            val authViewModel: AuthViewModel = viewModel(factory = appViewModelFactory)
            val foodLogViewModel: FoodLogViewModel = viewModel(factory = appViewModelFactory)
            val threadViewModel: ThreadViewModel = viewModel(factory = appViewModelFactory)
            val reminderViewModel: ReminderViewModel = viewModel(factory = appViewModelFactory)
            val quizViewModel: QuizViewModel = viewModel(factory = appViewModelFactory)
            val dietPlanViewModel: DietPlanViewModel = viewModel(factory = appViewModelFactory)

            val isLoadingProfile by authViewModel.isLoadingProfile.collectAsStateWithLifecycle()
            val currentUser = authViewModel.currentUser

            var showGeminiDialog by remember { mutableStateOf<List<FoodNutritionalInfo>?>(null) }

            DietAppTheme {

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val dietPlanResult by quizViewModel.dietPlanResult.collectAsStateWithLifecycle()
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

                            try {
                                delay(3000L)
                                // If we reach here, the timer finished naturally.
                                // We reset the result state so the logic is clean for next time.
                                quizViewModel.resetDietPlanResult()
                            } finally {
                                // This block runs whether the timer finished OR if the user
                                // reset data (cancelling this coroutine).
                                // This guarantees the message disappears.
                                planReadyMessage = null
                            }
                        }
                        is DietPlanResult.Error -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Error creating plan: ${result.message}",
                                    duration = SnackbarDuration.Long
                                )
                                quizViewModel.resetDietPlanResult()
                            }
                        }
                        else -> {
                            // Safety: If state changes to Idle/Loading (like during reset),
                            // ensure message is gone.
                            planReadyMessage = null
                        }
                    }
                }

                LaunchedEffect(geminiResult) {
                    when(val result = geminiResult) {
                        is GeminiResult.Success -> {
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
                            quizViewModel.resetDietPlanResult()
                            foodLogViewModel.resetGeminiResult()

                            navController.navigate(NavRoutes.LANDING) {
                                popUpTo(navController.graph.id) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                }

                // Define this reusable logic to ensure identical behavior
                val handleAuthNavigation: (Boolean) -> Unit = { isNewUser ->
                    if (!authViewModel.isEmailVerified()) {
                        // Case A: Not Verified -> Blocking Screen
                        navController.navigate(NavRoutes.EMAIL_VERIFICATION) {
                            popUpTo(NavRoutes.LANDING) { inclusive = true }
                        }
                    } else if (isNewUser) {
                        // Case B: Verified + New -> Quiz
                        navController.navigate(
                            "${NavRoutes.QUESTIONS}?startQuiz=true&source=onboarding") {
                            popUpTo(NavRoutes.LANDING) { inclusive = true }
                        }
                    } else {
                        // Case C: Verified + Existing -> Home
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.LANDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }

                val startDestination = remember {
                    if (authViewModel.isUserSignedIn()) {
                        if (!authViewModel.isEmailVerified()) {
                            NavRoutes.EMAIL_VERIFICATION
                        } else {
                            NavRoutes.HOME
                        }
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
                                containerColor = AppTheme.colors.primaryGreen,
                                contentColor = Color.White.copy(alpha = 0.9f)
                            ) {
                                NavigationBarItem(
                                    selected = selectedRoute == NavRoutes.HOME,
                                    onClick = {
                                        navController.navigate(NavRoutes.HOME) {
                                            popUpTo(navController.graph.id) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = { Icon(painterResource(id = R.drawable.ic_home_filled),
                                        contentDescription = "Home") },
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
                                    icon = { Icon(painterResource(id = R.drawable.ic_bar_filled),
                                        contentDescription = "Stats") },
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
                                    icon = { Icon(painterResource(id = R.drawable.ic_forum),
                                        contentDescription = "Threads") },
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
                                        Icon(painterResource(id = R.drawable.ic_person_filled),
                                            contentDescription = "Account")
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
                                GreetingScreen(
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
                                        navController.popBackStack()
                                    },
                                    onSignInSuccess = { isNewUser ->
                                        handleAuthNavigation(isNewUser)
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
                                        navController.popBackStack()
                                    },
                                    onSignUpSuccess = { isNewUser ->
                                        handleAuthNavigation(isNewUser)
                                    },
                                    onNavigateToSignIn = {
                                        navController.navigate(NavRoutes.SIGN_IN) {
                                            popUpTo(NavRoutes.SIGN_UP) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // 3. Add the Verification Screen
                            composable(NavRoutes.EMAIL_VERIFICATION) {
                                EmailVerificationScreen(
                                    authViewModel = authViewModel,
                                    onVerificationCompleted = {
                                        navController.navigate(
                                            "${NavRoutes.QUESTIONS}?startQuiz=true&source=onboarding") {
                                            popUpTo(NavRoutes.EMAIL_VERIFICATION) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onSignOut = {
                                        authViewModel.signOut(applicationContext)
                                        navController.navigate(NavRoutes.LANDING) {
                                            popUpTo(navController.graph.id) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(NavRoutes.HOME) {
                                val isGenerating by remember(dietPlanResult) {
                                    mutableStateOf(dietPlanResult is DietPlanResult.Loading)
                                }

                                HomeScreen(
                                    authViewModel = authViewModel,
                                    foodLogViewModel = foodLogViewModel,
                                    goalViewModel = dietPlanViewModel,
                                    navController = navController,
                                    isGeneratingPlan = isGenerating
                                )
                            }

                            composable(
                                route = "${NavRoutes.WEIGHT_TRACKER}?openWeightLog={openWeightLog}",
                                arguments = listOf(
                                    navArgument("openWeightLog") {
                                        type = NavType.BoolType
                                        defaultValue = false
                                    }
                                ),
                                deepLinks = listOf(
                                    navDeepLink {
                                        uriPattern = "dietapp://weight_tracker?openWeightLog={openWeightLog}"
                                    }
                                )
                            ) { backStackEntry ->
                                val openWeightLog = backStackEntry
                                    .arguments?.getBoolean("openWeightLog") == true

                                WeightScreen(
                                    navController = navController,
                                    foodLogViewModel = foodLogViewModel,
                                    authViewModel = authViewModel,
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
                                    goalsViewModel = dietPlanViewModel,
                                    navController = navController
                                )
                            }
                            composable(NavRoutes.NOTIFICATIONS) {
                                RemindersScreen(
                                    navController = navController,
                                    reminderViewModel = reminderViewModel
                                )
                            }

                            composable(
                                route = "${NavRoutes.QUESTIONS}?startQuiz={startQuiz}&source={source}",
                                arguments = listOf(
                                    navArgument("startQuiz") {
                                        type = NavType.BoolType
                                        defaultValue = false
                                    },
                                    navArgument("source") {
                                        type = NavType.StringType
                                        defaultValue = "home"
                                    }
                                )
                            ) { backStackEntry ->
                                val startQuiz = backStackEntry.arguments?.getBoolean("startQuiz") == true
                                val source = backStackEntry.arguments?.getString("source") ?: "home"

                                QuizScreen(
                                    navController = navController,
                                    quizViewModel = quizViewModel,
                                    authViewModel = authViewModel,
                                    foodLogViewModel = foodLogViewModel,
                                    startQuiz = startQuiz,
                                    source = source
                                )
                            }

                            composable(NavRoutes.DIET_PLAN) {
                                DietPlanScreen(
                                    navController = navController,
                                    dietPlanViewModel = dietPlanViewModel
                                )
                            }
                            composable(NavRoutes.SECURITY) {
                                SecurityScreen(
                                    navController = navController,
                                    authViewModel = authViewModel,
                                    quizViewModel = quizViewModel
                                )
                            }

                            composable(
                                route = NavRoutes.EDIT_PROFILE,
                                arguments = listOf(navArgument(NavRoutes.IS_NEW_USER_ARG) {
                                    type = NavType.StringType; defaultValue = "false"; nullable = true
                                })
                            ) { backStackEntry ->
                                val isNewUserString = backStackEntry.arguments?.getString(NavRoutes.IS_NEW_USER_ARG)
                                val isNewUser = isNewUserString?.toBooleanStrictOrNull() == true
                                EditProfileScreen(
                                    authViewModel = authViewModel,
                                    dietPlanViewModel = dietPlanViewModel,
                                    navController = navController,
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
                            composable(NavRoutes.THREADS) {
                                ThreadsScreen(
                                    navController = navController,
                                    threadViewModel = threadViewModel,
                                )
                            }

                            composable(
                                route = NavRoutes.CREATE_THREAD,
                                arguments = listOf(navArgument("threadId") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                })
                            ) { backStackEntry ->
                                val threadId = backStackEntry.arguments?.getString("threadId")
                                CreateThreadScreen(
                                    navController = navController,
                                    threadViewModel = threadViewModel,
                                    threadIdToEdit = threadId
                                )
                            }

                            composable(NavRoutes.MY_THREADS) {
                                MyThreadsScreen(
                                    navController = navController,
                                    threadViewModel = threadViewModel,
                                    authViewModel = authViewModel
                                )
                            }

                            composable(NavRoutes.ALL_ACHIEVEMENTS) {
                                val weeklyCalories by foodLogViewModel
                                    .weeklyCalories.collectAsStateWithLifecycle()
                                val weeklyProtein by foodLogViewModel
                                    .weeklyProtein.collectAsStateWithLifecycle()
                                val weeklyMacroPercentages by foodLogViewModel
                                    .weeklyMacroPercentages.collectAsStateWithLifecycle()

                                AchievementsScreen(
                                    navController = navController,
                                    weeklyCalories = weeklyCalories,
                                    weeklyProtein = weeklyProtein.mapValues { it.value.toFloat() },
                                    weeklyMacroPercentages = weeklyMacroPercentages
                                )
                            }

                            composable(
                                route = "thread_topic/{topicId}",
                                arguments = listOf(navArgument("topicId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val topicId = backStackEntry.arguments?.getString("topicId")
                                ThreadsScreen(
                                    navController = navController,
                                    threadViewModel = threadViewModel,
                                    initialTopicId = topicId
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
                                            navController.navigate(NavRoutes.DIET_PLAN)
                                            planReadyMessage = null
                                            quizViewModel.resetDietPlanResult()
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