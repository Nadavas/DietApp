package com.nadavariel.dietapp.models

enum class InputType { DOB, HEIGHT, WEIGHT, TARGET_WEIGHT, EXERCISE_TYPE }
data class Question(
    val text: String,
    val options: List<String>? = null,
    val inputType: InputType? = null
)

data class UserAnswer(
    val question: String = "",
    val answer: String = ""
)