package com.nadavariel.dietapp.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.models.NewsArticle
import com.nadavariel.dietapp.models.Thread
import com.nadavariel.dietapp.models.Topic
import com.nadavariel.dietapp.models.communityTopics
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.viewmodels.ThreadViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.core.net.toUri

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ThreadsScreen(
    navController: NavController,
    threadViewModel: ThreadViewModel = viewModel(),
    initialTopicId: String? = null
) {
    val allTopics = communityTopics
    val threads by threadViewModel.threads.collectAsState()
    val hottestThreads by threadViewModel.hottestThreads.collectAsState()

    val selectedTopic = remember(initialTopicId) {
        allTopics.find { it.key == initialTopicId }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ModernCommunityHeader(
                title = selectedTopic?.displayName ?: "Community Hub",
                subtitle = selectedTopic?.subtitle ?: "Connect, share, and learn",
                showBack = selectedTopic != null,
                onBack = { navController.popBackStack() },
                onNewThread = { navController.navigate("create_thread") },
                onMyThreads = { navController.navigate(NavRoutes.MY_THREADS) }
            )

            if (selectedTopic == null) {
                CommunityHomeContent(
                    hottestThreads = hottestThreads,
                    allTopics = allTopics,
                    navController = navController,
                    threadViewModel = threadViewModel,
                    onTopicSelected = { topic ->
                        navController.navigate("thread_topic/${topic.key}")
                    }
                )
            } else {
                TopicThreadList(
                    threads = threads.filter { it.topic == selectedTopic.key },
                    topic = selectedTopic,
                    navController = navController,
                    threadViewModel = threadViewModel
                )
            }
        }
    }
}

// -------------------------------
// --------- COMPOSABLES ---------
// -------------------------------

@Composable
private fun CommunityHomeContent(
    hottestThreads: List<Thread>,
    allTopics: List<Topic>,
    navController: NavController,
    threadViewModel: ThreadViewModel,
    onTopicSelected: (Topic) -> Unit
) {
    val context = LocalContext.current

    val newsError by threadViewModel.newsError.collectAsState()
    val articles by threadViewModel.newsArticles.collectAsState()
    val isLoadingNews by threadViewModel.isNewsLoading.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (hottestThreads.isNotEmpty()) {
            item { SectionHeaderModern(title = "Trending Now") }
            item {
                HottestThreadsCarouselModern(
                    hottestThreads = hottestThreads,
                    allTopics = allTopics,
                    navController = navController,
                    threadViewModel = threadViewModel
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeaderModern(title = "Explore Topics")
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
            ) {
                itemsIndexed(allTopics) { index, topic ->
                    TopicCardModern(topic, onTopicSelected)
                }
            }
        }

        // --- NEWS SECTION ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeaderModern(title = "Health news from the world")

                if (isLoadingNews) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = AppTheme.colors.warmOrange
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { threadViewModel.refreshNews() }
                            .padding(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = AppTheme.colors.textSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (newsError != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { threadViewModel.refreshNews() }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = newsError ?: "Failed to load news",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "RETRY",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        if (articles.isNotEmpty()) {
            item {
                NewsHighlightCard(
                    article = articles.first(),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, articles.first().url.toUri())
                        context.startActivity(intent)
                    }
                )
            }

            items(articles.drop(1)) { article ->
                NewsCardInternal(
                    article = article,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, article.url.toUri())
                        context.startActivity(intent)
                    }
                )
            }
        } else if (!isLoadingNews && newsError == null) {
            item {
                Text(
                    text = "No news available right now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun TopicThreadList(
    threads: List<Thread>,
    topic: Topic,
    navController: NavController,
    threadViewModel: ThreadViewModel
) {
    if (threads.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(topic.gradient.first().copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = topic.gradient.first()
                    )
                }
                Text(
                    "No threads here yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
                Text(
                    "Be the first to start a conversation!",
                    fontSize = 14.sp,
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(threads, key = { it.id }) { thread ->
                ThreadCardModern(thread, topic, navController, threadViewModel)
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ModernCommunityHeader(
    title: String,
    subtitle: String,
    showBack: Boolean,
    onBack: () -> Unit,
    onNewThread: () -> Unit,
    onMyThreads: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showBack) {
                    IconButton(onClick = onBack, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (!showBack) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // "My Threads" Button
                    OutlinedButton(
                        onClick = onMyThreads,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.softBlue),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AppTheme.colors.softBlue
                        )
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("My Threads", fontWeight = FontWeight.Bold)
                    }

                    // "New Thread" Button
                    Button(
                        onClick = onNewThread,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.primaryGreen,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(2.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("New Thread", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderModern(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.textPrimary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun HottestThreadsCarouselModern(
    hottestThreads: List<Thread>,
    allTopics: List<Topic>,
    navController: NavController,
    threadViewModel: ThreadViewModel
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(key1 = hottestThreads.size) {
        while (true) {
            delay(5000)
            if (hottestThreads.isNotEmpty()) currentIndex = (currentIndex + 1) % hottestThreads.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                fadeIn(tween(600)) togetherWith fadeOut(tween(600))
            },
            label = "Carousel"
        ) { index ->
            if (hottestThreads.isNotEmpty()) {
                val thread = hottestThreads[index]
                val topic = allTopics.find { it.key == thread.topic }
                if (topic != null) {
                    ThreadCardModern(
                        thread,
                        topic,
                        navController,
                        threadViewModel,
                        isFeatured = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun TopicCardModern(topic: Topic, onTopicSelected: (Topic) -> Unit) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable { onTopicSelected(topic) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(topic.gradient.first().copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = topic.icon,
                    contentDescription = null,
                    tint = topic.gradient.first(),
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = topic.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ThreadCardModern(
    thread: Thread,
    topic: Topic,
    navController: NavController,
    threadViewModel: ThreadViewModel,
    modifier: Modifier = Modifier,
    isFeatured: Boolean = false
) {
    val likeCount by produceState(initialValue = 0, thread.id) {
        threadViewModel.getLikeCountForThread(thread.id) { count -> value = count }
    }
    val commentCount by produceState(initialValue = 0, thread.id) {
        threadViewModel.getCommentCountForThread(thread.id) { count -> value = count }
    }

    val primaryColor = topic.gradient.first()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { navController.navigate(NavRoutes.threadDetail(thread.id)) },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if(isFeatured) 4.dp else 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(primaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        topic.icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFeatured) "TRENDING IN ${topic.displayName.uppercase()}" else topic.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                    Text(
                        text = getRelativeTime(thread.timestamp),
                        fontSize = 12.sp,
                        color = AppTheme.colors.textSecondary
                    )
                }

                if (isFeatured) {
                    Icon(
                        Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = AppTheme.colors.softRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = thread.header,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = thread.paragraph,
                fontSize = 14.sp,
                color = AppTheme.colors.textSecondary,
                maxLines = if (isFeatured) 2 else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                StatItemModern(Icons.Default.FavoriteBorder, "$likeCount", AppTheme.colors.textSecondary)
                StatItemModern(Icons.Default.ChatBubbleOutline, "$commentCount", AppTheme.colors.textSecondary)
                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Read More",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        }
    }
}

@Composable
private fun NewsHighlightCard(article: NewsArticle, onClick: () -> Unit) {
    val (sourceColor, sourceIcon) = getNewsSourceStyle(article.source)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // IMAGE AREA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(sourceColor.copy(alpha = 0.1f))
            ) {
                if (article.imageUrl != null) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = sourceIcon,
                        contentDescription = null,
                        tint = sourceColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(80.dp).align(Alignment.Center)
                    )
                }

                Surface(
                    modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
                    shape = RoundedCornerShape(8.dp),
                    color = AppTheme.colors.warmOrange
                ) {
                    Text(
                        text = "TOP STORY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // CONTENT AREA
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = article.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 3,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${article.source} • ${getRelativeTime(article.publishedDate)}",
                        fontSize = 12.sp,
                        color = AppTheme.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ChevronRight, null, tint = sourceColor)
                }
            }
        }
    }
}


@Composable
private fun NewsCardInternal(article: NewsArticle, onClick: () -> Unit) {
    val (sourceColor, sourceIcon) = getNewsSourceStyle(article.source)

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // IMAGE / ICON BOX
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(80.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                if (article.imageUrl != null) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(sourceColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(sourceIcon, null, tint = sourceColor, modifier = Modifier.size(32.dp))
                    }
                }
            }

            // TEXT
            Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = article.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Text(
                    text = "${article.source} • ${getRelativeTime(article.publishedDate)}",
                    fontSize = 12.sp,
                    color = AppTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun StatItemModern(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now" // Less than 1 minute
        diff < 3600_000 -> "${diff / 60_000}m" // Minutes
        diff < 86400_000 -> "${diff / 3600_000}h" // Hours
        diff < 604800_000 -> "${diff / 86400_000}d" // Days
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp)) // Date
    }
}

@Composable
private fun getNewsSourceStyle(source: String): Pair<Color, ImageVector> {
    return when {
        source.contains("Harvard", true) -> Pair(AppTheme.colors.sunsetPink, Icons.Default.School)
        source.contains("Nutrition", true) -> Pair(AppTheme.colors.primaryGreen, Icons.Default.Spa)
        source.contains("Medical", true) -> Pair(AppTheme.colors.softBlue, Icons.Default.LocalHospital)
        source.contains("Science", true) -> Pair(AppTheme.colors.accentTeal, Icons.Default.Science)
        source.contains("Eat", true) -> Pair(AppTheme.colors.warmOrange, Icons.Default.Restaurant)
        else -> Pair(AppTheme.colors.textSecondary, Icons.AutoMirrored.Filled.Article)
    }
}