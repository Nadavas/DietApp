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
    const val UPDATE_PROFILE_BASE = "update_profile"
    const val IS_NEW_USER_ARG = "isNewUser"
    const val UPDATE_PROFILE = "$UPDATE_PROFILE_BASE?${IS_NEW_USER_ARG}={${IS_NEW_USER_ARG}}"
    const val STATISTICS = "statistics"
    const val ADD_EDIT_MEAL = "add_edit_meal"
    const val MEAL_ID_ARG = "mealId"
    const val ADD_EDIT_MEAL_WITH_ID = "$ADD_EDIT_MEAL/{$MEAL_ID_ARG}"
}