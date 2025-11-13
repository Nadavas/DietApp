package com.nadavariel.dietapp.ui.questions

import com.nadavariel.dietapp.model.InputType
import com.nadavariel.dietapp.model.Question

// Define questions list
internal val questions = listOf(
    Question("What is your date of birth?", inputType = InputType.DOB),
    Question("What is your gender?", options = listOf("Male", "Female", "Other / Prefer not to say")),
    Question("What is your height?", inputType = InputType.HEIGHT),
    Question("What is your weight?", inputType = InputType.WEIGHT),
    Question("What is your primary fitness goal?", options = listOf("Lose weight", "Gain muscle", "Maintain current weight", "Improve overall health")),
    Question("Do you have a target weight or body composition goal in mind?", inputType = InputType.TARGET_WEIGHT),
    Question("How aggressive do you want to be with your fitness goal timeline?", options = listOf("Very aggressive (1–2 months)", "Moderate (3–6 months)", "Gradual (6+ months or no rush)")),
    Question("How would you describe your daily activity level outside of exercise?", options = listOf("Sedentary", "Lightly active", "Moderately active", "Very active")),
    Question("How many days per week do you engage in structured exercise?", options = listOf("0-1", "2-3", "4-5", "6-7")),
    Question(
        "What types of exercise do you typically perform?",
        inputType = InputType.EXERCISE_TYPE,
        options = listOf("Cardio", "Strength Training", "Yoga / Pilates", "Team Sports", "Swimming", "HIIT", "Other")
    )
)

// --- Screen State ---
internal enum class ScreenState { LANDING, EDITING, QUIZ_MODE }