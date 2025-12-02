package com.nadavariel.dietapp.repository

import android.util.Log
import com.nadavariel.dietapp.model.NewsArticle
import com.nadavariel.dietapp.model.NewsSourcesConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class NewsRepository {

    private val TAG = "NewsRepository"

    // FETCH LOGIC
    suspend fun fetchLatestArticles(totalLimit: Int = 20): List<NewsArticle> = withContext(Dispatchers.IO) {
        val allArticles = mutableListOf<NewsArticle>()

        NewsSourcesConfig.sources.forEach { feedUrl ->
            try {
                // 1. Fetch
                val fetched = fetchAndParse(feedUrl)

                // 2. "STRICT FAIRNESS" LOGIC:
                // Limit each source to exactly 2 articles max.
                // This ensures "The Conversation" (or any other) can never take more than 2 slots.
                val limited = fetched.take(2)

                allArticles.addAll(limited)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching $feedUrl", e)
            }
        }

        // 3. Sort by date to keep it fresh
        // Since we already limited the quantity, this sort won't result in flooding.
        allArticles
            .distinctBy { it.title }
            .sortedByDescending { it.publishedDate }
            .take(totalLimit)
    }

    private fun fetchAndParse(feedUrl: String): List<NewsArticle> {
        val xmlContent = downloadXml(feedUrl) ?: return emptyList()
        val sourceName = extractSourceName(feedUrl)
        return parseXml(xmlContent, sourceName)
    }

    // NETWORK CLIENT
    private fun downloadXml(urlString: String): String? {
        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.instanceFollowRedirects = true

            if (connection.responseCode == 200) {
                return connection.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error: ${e.message}")
        }
        return null
    }

    // PARSER (Includes your Image Extraction Logic)
    private fun parseXml(xmlContent: String, sourceName: String): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var currentTitle = ""
            var currentDescription = ""
            var currentLink = ""
            var currentDate = ""
            var currentImage: String? = null
            var insideItem = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagName = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (tagName.equals("item", true) || tagName.equals("entry", true)) {
                            insideItem = true
                            currentTitle = ""
                            currentDescription = ""
                            currentLink = ""
                            currentDate = ""
                            currentImage = null
                        } else if (insideItem) {
                            when {
                                tagName.equals("title", true) -> currentTitle = safeNextText(parser)
                                tagName.equals("description", true) || tagName.equals("summary", true) || tagName.equals("content", true) -> {
                                    val content = safeNextText(parser)
                                    currentDescription = content
                                    // Try extracting image from HTML if not found yet
                                    if (currentImage == null) currentImage = extractImageFromHtml(content)
                                }
                                tagName.equals("link", true) -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (!href.isNullOrEmpty()) currentLink = href else {
                                        val text = safeNextText(parser)
                                        if (text.isNotEmpty()) currentLink = text
                                    }
                                }
                                tagName.contains("date", true) || tagName.equals("published", true) -> {
                                    currentDate = safeNextText(parser)
                                }
                                // IMAGE EXTRACTION
                                tagName.contains("thumbnail", true) ||
                                        tagName.equals("enclosure", true) ||
                                        tagName.equals("image", true) ||
                                        tagName.equals("media:content", true) -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (!url.isNullOrEmpty()) currentImage = url
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", true) || tagName.equals("entry", true)) {
                            insideItem = false
                            if (currentTitle.isNotEmpty() && currentLink.isNotEmpty()) {
                                articles.add(NewsArticle(UUID.randomUUID().toString(), cleanText(currentTitle), cleanHtml(currentDescription), currentLink, sourceName, parseDate(currentDate), currentImage))
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) { e.printStackTrace() }
        return articles
    }

    // UTILS
    private fun safeNextText(parser: XmlPullParser) = try { parser.nextText() ?: "" } catch (e: Exception) { "" }

    private fun extractSourceName(url: String): String = when {
        url.contains("bbc") -> "BBC Health"
        url.contains("nutritionfacts") -> "NutritionFacts"
        url.contains("theconversation") -> "The Conversation"
        url.contains("nytimes") -> "NY Times"
        else -> "Health News"
    }

    private fun extractImageFromHtml(html: String): String? {
        val matcher = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>").matcher(html)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun cleanHtml(html: String) = html.replace(Regex("<[^>]*>"), "").replace("&nbsp;", " ").trim().take(200)
    private fun cleanText(text: String) = text.replace("\n", " ").trim()

    private fun parseDate(dateString: String): Long {
        if (dateString.isEmpty()) return System.currentTimeMillis()
        val formats = listOf(SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US), SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US))
        for (f in formats) { try { return f.parse(dateString)?.time ?: continue } catch (e: Exception) {} }
        return System.currentTimeMillis()
    }
}