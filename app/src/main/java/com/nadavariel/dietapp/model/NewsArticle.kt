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

        // ✅ NutritionFacts (Good quality)
        "https://nutritionfacts.org/feed/",

        // ✅ NY Times Health (Includes nutrition)
        "https://rss.nytimes.com/services/xml/rss/nyt/Health.xml",

        // ✅ BBC Health: Excellent global coverage, extremely reliable images (media:thumbnail).
        "http://feeds.bbci.co.uk/news/health/rss.xml",

        // ✅ The Conversation (Health): Academic but accessible news.
        // Uses ATOM format, but our new parser handles it + extracts images from the HTML.
        "https://theconversation.com/us/health/articles.atom"
    )
}