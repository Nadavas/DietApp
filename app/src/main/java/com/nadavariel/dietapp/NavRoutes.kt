package com.nadavariel.dietapp

object NavRoutes {
    const val LANDING = "landing"
    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val HOME = "home"
    const val MY_PROFILE = "my_profile"
    const val UPDATE_PROFILE = "update_profile"
    const val ADD_EDIT_MEAL = "add_edit_meal" // NEW Route for adding/editing meals

    const val MEAL_ID_ARG = "mealId" // Key for the argument

    // ⭐ FIX: Use string concatenation (+) instead of interpolation for const val
    const val ADD_EDIT_MEAL_WITH_ID = "$ADD_EDIT_MEAL/{$MEAL_ID_ARG}"
}