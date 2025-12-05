package com.nadavariel.dietapp.model

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