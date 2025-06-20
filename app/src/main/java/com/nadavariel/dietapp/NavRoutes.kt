package com.nadavariel.dietapp

object NavRoutes {
    const val LANDING = "landing"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val HOME = "home"
    const val MY_PROFILE = "my_profile"

    // ⭐ MODIFIED: UpdateProfile route now includes an optional argument
    // This allows passing 'isNewUser=true' or 'isNewUser=false'
    const val UPDATE_PROFILE_BASE = "update_profile" // Base route without arguments
    const val IS_NEW_USER_ARG = "isNewUser" // New argument key
    const val UPDATE_PROFILE = "$UPDATE_PROFILE_BASE?${IS_NEW_USER_ARG}={${IS_NEW_USER_ARG}}"


    const val ADD_EDIT_MEAL = "add_edit_meal"
    const val MEAL_ID_ARG = "mealId"
    const val ADD_EDIT_MEAL_WITH_ID = "$ADD_EDIT_MEAL/{$MEAL_ID_ARG}"
}