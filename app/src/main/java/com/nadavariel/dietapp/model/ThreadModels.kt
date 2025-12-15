package com.nadavariel.dietapp.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.Timestamp
import java.util.Date

data class Thread(
    val id: String = "",
    val header: String = "",
    val paragraph: String = "",
    val topic: String = "",
    val type: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ThreadWithStats(
    val thread: Thread,
    val score: Int
)

data class Comment(
    val id: String = "",
    val threadId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val createdAt: Timestamp = Timestamp(Date())
)

data class Like(
    val userId: String = "",
    val authorName: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class Topic(
    val key: String,
    val displayName: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradient: List<Color>
)

val communityTopics = listOf(
    Topic("Training", "Training & Fitness", "Workouts, tips, and goals", Icons.Outlined.FitnessCenter, listOf(Color(0xFFF9484A), Color(0xFFFBD72B))),
    Topic("Diet", "Diet & Nutrition", "Plans, science, and questions", Icons.Outlined.RestaurantMenu, listOf(Color(0xFF7586F5), Color(0xFFF6F6F8))),
    Topic("Recipes", "Healthy Recipes", "Share your tasty creations",
        Icons.AutoMirrored.Outlined.MenuBook, listOf(Color(0xFF16A085), Color(0xFFF4D03F))),
    Topic("MentalHealth", "Mental Wellness", "Mindfulness and motivation", Icons.Outlined.SelfImprovement, listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))),
    Topic("SuccessStories", "Success Stories", "Inspire and be inspired", Icons.Outlined.EmojiEvents, listOf(Color(0xFFF3904F), Color(0xFF3B4371))),
    Topic("GearAndTech", "Gear & Tech", "Watches, apps, and equipment", Icons.Outlined.Computer, listOf(Color(0xFF434343), Color(0xFF000000)))
)

data class NewsArticle(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val source: String,
    val publishedDate: Long,
    val imageUrl: String? = null
)

object NewsSourcesConfig {
    val sources = listOf(
        // ✅ NutritionFacts.org - Evidence-based nutrition by Dr. Michael Greger
        "https://nutritionfacts.org/feed/",

        // ✅ Nutrition Stripped - Registered dietitian, mindful nutrition & recipes
        "https://www.skinnytaste.com/feed/",

        // ✅ Precision Nutrition - Science-backed nutrition, diet & health coaching
        "https://www.precisionnutrition.com/blog/feed/",

        // ✅ Sharon Palmer (Plant-Based RDN) - Plant-based nutrition & sustainability
        "https://sharonpalmer.com/feed/",

        // ✅ The Real Food Dietitians - Dietitian-authored recipes & nutrition tips
        "https://www.eatingwell.com/feed/"
    )
}