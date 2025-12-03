package com.nadavariel.dietapp.data

import com.nadavariel.dietapp.model.InputType
import com.nadavariel.dietapp.model.Question

object QuestionnaireConstants {
    // Key Constants (used for database lookups)
    const val DOB_QUESTION = "What is your date of birth?"
    const val GENDER_QUESTION = "What is your gender?"
    const val HEIGHT_QUESTION = "What is your height?"
    const val WEIGHT_QUESTION = "What is your weight?"
    const val TARGET_WEIGHT_QUESTION = "What is your target weight?" // Updated phrasing

    // AI/Goal Specific Questions
    const val CALORIES_GOAL_QUESTION = "How many calories a day is your target?"
    const val PROTEIN_GOAL_QUESTION = "How many grams of protein a day is your target?"

    // The Master List
    val questions = listOf(
        Question(DOB_QUESTION, inputType = InputType.DOB),
        Question(GENDER_QUESTION, options = listOf("Male", "Female", "Other / Prefer not to say")),
        Question(HEIGHT_QUESTION, inputType = InputType.HEIGHT),
        Question(WEIGHT_QUESTION, inputType = InputType.WEIGHT),
        Question("What is your primary fitness goal?", options = listOf("Lose weight", "Gain muscle", "Maintain current weight", "Improve overall health")),
        Question(TARGET_WEIGHT_QUESTION, inputType = InputType.TARGET_WEIGHT),
        Question("How aggressive do you want to be with your fitness goal timeline?", options = listOf("Very aggressive (1–2 months)", "Moderate (3–6 months)", "Gradual (6+ months or no rush)")),
        Question("How would you describe your daily activity level outside of exercise?", options = listOf("Sedentary", "Lightly active", "Moderately active", "Very active")),
        Question("How many days per week do you engage in structured exercise?", options = listOf("0-1", "2-3", "4-5", "6-7")),
        Question(
            "What types of exercise do you typically perform?",
            inputType = InputType.EXERCISE_TYPE,
            options = listOf("Cardio", "Strength Training", "Yoga / Pilates", "Team Sports", "Swimming", "HIIT", "Other")
        )
    )
}