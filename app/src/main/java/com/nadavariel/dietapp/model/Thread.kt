package com.nadavariel.dietapp.model

data class Thread(
    val id: String = "",
    val header: String = "",
    val paragraph: String = "",
    val topic: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
