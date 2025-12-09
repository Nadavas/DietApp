package com.nadavariel.dietapp

object NavRoutes {
    const val LANDING = "landing"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val HOME = "home"
    const val ACCOUNT = "account"
    const val MY_PROFILE = "my_profile"
    const val SECURITY = "security"
    const val QUESTIONS = "questions"
    const val THREADS = "threads"
    const val MY_THREADS = "my_threads"
    const val CREATE_THREAD = "create_thread?threadId={threadId}"
    const val THREAD_DETAIL_WITH_ARG = "thread_detail/{threadId}"

    // --- HELPER FUNCTIONS ---
    // Use this when navigating TO the screen
    fun createThread(threadId: String? = null): String {
        return if (threadId != null) {
            "create_thread?threadId=$threadId"
        } else {
            "create_thread"
        }
    }

    fun threadDetail(threadId: String) = "thread_detail/$threadId"
    // ----------------------

    const val DIET_PLAN = "diet_plan"
    const val EDIT_PROFILE_BASE = "update_profile"
    const val IS_NEW_USER_ARG = "isNewUser"
    const val EDIT_PROFILE = "$EDIT_PROFILE_BASE?${IS_NEW_USER_ARG}={${IS_NEW_USER_ARG}}"
    const val STATISTICS = "statistics"
    const val ADD_EDIT_MEAL = "add_edit_meal"
    const val MEAL_ID_ARG = "mealId"
    const val ADD_EDIT_MEAL_WITH_ID = "$ADD_EDIT_MEAL/{$MEAL_ID_ARG}"
    const val NOTIFICATIONS = "notifications_screen"
    const val STATS_ENERGY = "stats/energy"
    const val STATS_MACROS = "stats/macros"
    const val STATS_CARBS = "stats/carbs"
    const val STATS_MINERALS = "stats/minerals"
    const val STATS_VITAMINS = "stats/vitamins"
    const val WEIGHT_TRACKER = "weight_tracker"
    const val ALL_ACHIEVEMENTS = "all_achievements"
}