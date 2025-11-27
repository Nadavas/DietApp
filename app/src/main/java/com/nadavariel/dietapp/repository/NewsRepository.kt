package com.nadavariel.dietapp.repository

import com.nadavariel.dietapp.model.NewsArticle
import com.nadavariel.dietapp.model.NewsSourcesConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class NewsRepository {

    suspend fun fetchLatestArticles(limit: Int = 5): List<NewsArticle> = withContext(Dispatchers.IO) {
        val articles = mutableListOf<NewsArticle>()

        NewsSourcesConfig.sources.forEach { feedUrl ->
            try {
                val fetchedArticles = parseFeed(feedUrl)
                articles.addAll(fetchedArticles)
            } catch (e: Exception) {
                // Log error but continue with other sources
                e.printStackTrace()
            }
        }

        // Sort by date and take top N articles
        articles
            .sortedByDescending { it.publishedDate }
            .take(limit)
    }

    private fun parseFeed(feedUrl: String): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()

        try {
            val xmlContent = URL(feedUrl).readText()
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var currentArticle: MutableMap<String, String>? = null
            var currentTag: String? = null
            val sourceName = extractSourceName(feedUrl)

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag == "item" || currentTag == "entry") {
                            currentArticle = mutableMapOf()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        currentArticle?.let { article ->
                            val text = parser.text?.trim() ?: ""
                            if (text.isNotEmpty()) {
                                when (currentTag) {
                                    "title" -> article["title"] = text
                                    "description", "summary" -> article["description"] = text
                                    "link" -> article["url"] = text
                                    "pubDate", "published" -> article["date"] = text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "item" || parser.name == "entry") {
                            currentArticle?.let { article ->
                                if (article.containsKey("title") && article.containsKey("url")) {
                                    articles.add(
                                        NewsArticle(
                                            id = UUID.randomUUID().toString(),
                                            title = article["title"] ?: "",
                                            description = cleanHtml(article["description"] ?: ""),
                                            url = article["url"] ?: "",
                                            source = sourceName,
                                            publishedDate = parseDate(article["date"] ?: "")
                                        )
                                    )
                                }
                            }
                            currentArticle = null
                        }
                        currentTag = null
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return articles
    }

    private fun extractSourceName(url: String): String {
        return when {
            url.contains("nutrition.gov") -> "Nutrition.gov"
            url.contains("harvard") -> "Harvard Nutrition"
            url.contains("eatright") -> "EatRight.org"
            url.contains("medicalnewstoday") -> "Medical News Today"
            url.contains("sciencedaily") -> "Science Daily"
            else -> "Nutrition News"
        }
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }

    private fun parseDate(dateString: String): Long {
        if (dateString.isEmpty()) return System.currentTimeMillis()

        val formats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        )

        for (format in formats) {
            try {
                return format.parse(dateString)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                continue
            }
        }

        return System.currentTimeMillis()
    }
}