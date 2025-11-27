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
        "https://www.nutrition.gov/rss/news.xml",
        "https://nutritionsource.hsph.harvard.edu/feed/",
        "https://www.eatright.org/rss/xml/news.xml",
        "https://www.medicalnewstoday.com/rss/nutrition.xml",
        "https://www.sciencedaily.com/rss/health_medicine/nutrition.xml"
    )
}