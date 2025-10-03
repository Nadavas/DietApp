package com.nadavariel.dietapp.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// Put the Topic data class here so it can be used anywhere
data class Topic(
    val key: String,
    val displayName: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradient: List<Color>
)

// This is now the single source of truth for all topics
val communityTopics = listOf(
    Topic("Training", "Training & Fitness", "Workouts, tips, and goals", Icons.Outlined.FitnessCenter, listOf(Color(0xFFF9484A), Color(0xFFFBD72B))),
    Topic("Diet", "Diet & Nutrition", "Plans, science, and questions", Icons.Outlined.RestaurantMenu, listOf(Color(0xFFC475F5), Color(0xFF6C9DEF))),
    Topic("Recipes", "Healthy Recipes", "Share your tasty creations", Icons.Outlined.MenuBook, listOf(Color(0xFF16A085), Color(0xFFF4D03F))),
    Topic("MentalHealth", "Mental Wellness", "Mindfulness and motivation", Icons.Outlined.SelfImprovement, listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))),
    Topic("SuccessStories", "Success Stories", "Inspire and be inspired", Icons.Outlined.EmojiEvents, listOf(Color(0xFFF3904F), Color(0xFF3B4371))),
    Topic("GearAndTech", "Gear & Tech", "Watches, apps, and equipment", Icons.Outlined.Computer, listOf(Color(0xFF434343), Color(0xFF000000)))
)