package com.nadavariel.dietapp.data

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
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.regex.Pattern

class NewsRepository {

    private val tag = "NewsRepository"

    // FETCH LOGIC - STRICT FAIRNESS: Max 2 articles per source
    suspend fun fetchLatestArticles(totalLimit: Int = 20): List<NewsArticle> =
        withContext(Dispatchers.IO) {
            val allArticles = mutableListOf<NewsArticle>()

            NewsSourcesConfig.sources.forEach { feedUrl ->
                try {
                    // 1. Fetch and parse articles from each source
                    val fetched = fetchAndParse(feedUrl)

                    // 2. STRICT FAIRNESS ENFORCEMENT:
                    // Take EXACTLY 2 articles max per source to ensure fair representation
                    // With 5 sources × 2 articles = 10 articles max, no source can dominate
                    val limited = fetched.take(2)

                    allArticles.addAll(limited)

                    Log.d(tag, "Added ${limited.size} articles from ${extractSourceName(feedUrl)}")
                } catch (e: Exception) {
                    Log.e(tag, "Error fetching $feedUrl", e)
                }
            }

            // 3. Sort by date to show the freshest articles first
            // Since each source contributes max 2 articles, we maintain fairness
            val result = allArticles
                .distinctBy { it.title } // Remove any duplicate titles
                .sortedByDescending { it.publishedDate } // Newest first
                .take(totalLimit) // Take requested number

            Log.d(tag, "Returning ${result.size} total articles")
            result
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
            } else {
                Log.w(tag, "HTTP ${connection.responseCode} for $urlString")
            }
        } catch (e: Exception) {
            Log.e(tag, "Network error downloading $urlString: ${e.message}")
        }
        return null
    }

    // PARSER (Includes Image Extraction Logic)
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
                                tagName.equals("title", true) -> {
                                    currentTitle = safeNextText(parser)
                                }
                                tagName.equals("description", true) ||
                                        tagName.equals("summary", true) ||
                                        tagName.equals("content", true) -> {
                                    val content = safeNextText(parser)
                                    currentDescription = content
                                    // Try extracting image from HTML if not found yet
                                    if (currentImage == null) {
                                        currentImage = extractImageFromHtml(content)
                                    }
                                }
                                tagName.equals("link", true) -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    if (!href.isNullOrEmpty()) {
                                        currentLink = href
                                    } else {
                                        val text = safeNextText(parser)
                                        if (text.isNotEmpty()) currentLink = text
                                    }
                                }
                                // DATE PARSING - Support multiple date tag names
                                tagName.contains("date", true) ||
                                        tagName.equals("published", true) ||
                                        tagName.equals("pubDate", true) ||
                                        tagName.equals("updated", true) -> {
                                    val dateText = safeNextText(parser)
                                    if (dateText.isNotEmpty() && currentDate.isEmpty()) {
                                        currentDate = dateText
                                    }
                                }
                                // IMAGE EXTRACTION
                                tagName.contains("thumbnail", true) ||
                                        tagName.equals("enclosure", true) ||
                                        tagName.equals("image", true) ||
                                        tagName.equals("media:content", true) -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    if (!url.isNullOrEmpty() && currentImage == null) {
                                        currentImage = url
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (tagName.equals("item", true) || tagName.equals("entry", true)) {
                            insideItem = false
                            if (currentTitle.isNotEmpty() && currentLink.isNotEmpty()) {
                                val parsedDate = parseDate(currentDate)
                                articles.add(
                                    NewsArticle(
                                        UUID.randomUUID().toString(),
                                        cleanText(currentTitle),
                                        cleanHtml(currentDescription),
                                        currentLink,
                                        sourceName,
                                        parsedDate,
                                        currentImage
                                    )
                                )
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing XML for $sourceName: ${e.message}", e)
        }
        return articles
    }

    // UTILS
    private fun safeNextText(parser: XmlPullParser) = try {
        parser.nextText() ?: ""
    } catch (_: Exception) {
        ""
    }

    private fun extractSourceName(url: String): String = when {
        url.contains("nutritionfacts", true) -> "NutritionFacts.org"
        url.contains("skinnytaste", true) -> "Skinnytaste" // ✅ Added this
        url.contains("precisionnutrition", true) -> "Precision Nutrition"
        url.contains("sharonpalmer", true) -> "Sharon Palmer"
        url.contains("eatingwell", true) -> "EatingWell"   // ✅ Added this
        else -> "Nutrition News"
    }

    private fun extractImageFromHtml(html: String): String? {
        val matcher = Pattern.compile("<img[^>]+src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>").matcher(html)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun cleanHtml(html: String) = html
        .replace(Regex("<[^>]*>"), "") // Remove HTML tags
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .trim()
        .take(200)

    private fun cleanText(text: String) = text
        .replace("\n", " ")
        .replace("  ", " ")
        .trim()

    /**
     * Parse date strings from RSS feeds with comprehensive format support.
     * CRITICAL: This must correctly parse dates or all articles will show as "Just now"
     */
    private fun parseDate(dateString: String): Long {
        if (dateString.isEmpty()) {
            Log.w(tag, "Empty date string, using current time")
            return System.currentTimeMillis()
        }

        // Remove any timezone abbreviations that SimpleDateFormat struggles with
        val cleanedDate = dateString
            .replace("GMT", "+0000")
            .replace("UTC", "+0000")
            .trim()

        // Try multiple common RSS date formats
        val formats = listOf(
            // RSS 2.0 format: "Fri, 05 Dec 2024 10:30:00 +0000"
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US),

            // ISO 8601 / Atom format: "2024-12-05T10:30:00Z"
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),

            // Alternative formats
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
            SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US) // Date only
        )

        for (format in formats) {
            try {
                format.isLenient = false
                format.timeZone = TimeZone.getTimeZone("UTC")

                val parsedDate = format.parse(cleanedDate)
                if (parsedDate != null) {
                    val now = System.currentTimeMillis()
                    val parsedTime = parsedDate.time

                    // Sanity check: date shouldn't be in the future or too old (>2 years)
                    if (parsedTime <= now && (now - parsedTime) < (730L * 24 * 60 * 60 * 1000)) {
                        Log.d(tag, "✓ Parsed date: '$dateString' -> ${Date(parsedTime)}")
                        return parsedTime
                    } else {
                        Log.w(tag, "Date outside valid range: $parsedTime")
                    }
                }
            } catch (_: Exception) {
                // Try next format
            }
        }

        // If all parsing fails, log warning and use current time
        Log.w(tag, "⚠ Failed to parse date: '$dateString' - defaulting to current time")
        return System.currentTimeMillis()
    }
}