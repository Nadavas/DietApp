package com.nadavariel.dietapp.data

import com.google.firebase.Timestamp

data class Like(
    val userId: String = "",
    val authorName: String = "",
    val timestamp: Timestamp = Timestamp.now()
)