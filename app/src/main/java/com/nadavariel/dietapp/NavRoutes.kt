package com.nadavariel.dietapp

object NavRoutes {
    const val LANDING = "landing"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val HOME = "home"
    const val ACCOUNT = "account"
    const val MY_PROFILE = "my_profile"
    const val SETTINGS = "settings"
    const val CHANGE_PASSWORD = "change_password"
    const val QUESTIONS = "questions"
    const val THREADS = "threads"
    const val CREATE_THREAD = "create_thread"
    const val THREAD_DETAIL_WITH_ARG = "thread_detail/{threadId}"
    const val DIET_PLAN = "diet_plan"
    const val UPDATE_PROFILE_BASE = "update_profile"
    const val IS_NEW_USER_ARG = "isNewUser"
    const val UPDATE_PROFILE = "$UPDATE_PROFILE_BASE?${IS_NEW_USER_ARG}={${IS_NEW_USER_ARG}}"
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
    const val ADD_MANUAL_MEAL = "add_manual_meal"

    fun threadDetail(threadId: String) = "thread_detail/$threadId"

}