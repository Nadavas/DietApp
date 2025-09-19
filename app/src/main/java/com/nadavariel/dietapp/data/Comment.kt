// In your data package, e.g., data/Comment.kt
package com.nadavariel.dietapp.data

import com.google.firebase.Timestamp
import java.util.Date

data class Comment(
    val id: String = "",
    val threadId: String = "",
    val authorId: String = "",
    val authorName: String = "", // Denormalized for easy display
    val text: String = "",
    val createdAt: Timestamp = Timestamp(Date()) // Using server timestamp is better if possible
)