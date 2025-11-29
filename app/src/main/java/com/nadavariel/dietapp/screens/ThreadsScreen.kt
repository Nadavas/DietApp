package com.nadavariel.dietapp.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.nadavariel.dietapp.model.NewsArticle
import com.nadavariel.dietapp.model.Thread
import com.nadavariel.dietapp.model.Topic
import com.nadavariel.dietapp.model.communityTopics
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.viewmodel.NewsViewModel
import com.nadavariel.dietapp.viewmodel.ThreadViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ThreadsScreen(
    navController: NavController,
    threadViewModel: ThreadViewModel = viewModel(),
    initialTopicId: String? = null // New parameter from MainActivity
) {
    val allTopics = communityTopics
    val threads by threadViewModel.threads.collectAsState()
    val hottestThreads by threadViewModel.hottestThreads.collectAsState()

    // We determine the view mode based on the passed ID directly
    val selectedTopic = remember(initialTopicId) {
        allTopics.find { it.key == initialTopicId }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(AppTheme.colors.statsGradient))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header adapts based on whether we are in a Topic or Home
            ModernCommunityHeader(
                title = selectedTopic?.displayName ?: "Community Hub",
                subtitle = selectedTopic?.subtitle ?: "Connect, share, and learn",
                showBack = selectedTopic != null, // Show back arrow if inside a topic
                onBack = { navController.popBackStack() }, // Use system navigation to go back
                onNewThread = { navController.navigate("create_thread") },
                onMyThreads = { navController.navigate(NavRoutes.MY_THREADS) }
            )

            if (selectedTopic == null) {
                // Show Main List
                CommunityHomeContent(
                    hottestThreads = hottestThreads,
                    allTopics = allTopics,
                    navController = navController,
                    threadViewModel = threadViewModel,
                    // FIX: Navigate to the new Route ID instead of setting local state
                    onTopicSelected = { topic ->
                        navController.navigate("thread_topic/${topic.key}")
                    }
                )
            } else {
                // Show Topic Specific List
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

@Composable
fun CommunityHomeContent(
    hottestThreads: List<Thread>,
    allTopics: List<Topic>,
    navController: NavController,
    threadViewModel: ThreadViewModel,
    newsViewModel: NewsViewModel = viewModel(),
    onTopicSelected: (Topic) -> Unit
) {
    val context = LocalContext.current
    val articles by newsViewModel.articles.collectAsState()
    val isLoadingNews by newsViewModel.isLoading.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // FIX 1: Daily Challenge REMOVED

        // 2. Trending Section
        if (hottestThreads.isNotEmpty()) {
            item {
                SectionHeaderModern(title = "Trending Now")
            }

            item {
                // FIX 2: Fixed height carousel
                HottestThreadsCarouselModern(
                    hottestThreads = hottestThreads,
                    allTopics = allTopics,
                    navController = navController,
                    threadViewModel = threadViewModel
                )
            }
        }

        // 3. Topics Section
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

        // 4. News Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeaderModern(title = "Nutrition News")
                IconButton(onClick = { newsViewModel.refresh() }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = AppTheme.colors.textSecondary
                    )
                }
            }
        }

        if (isLoadingNews) {
            item {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppTheme.colors.primaryGreen)
                }
            }
        } else if (articles.isNotEmpty()) {
            items(articles) { article ->
                // FIX 4: Improved News Card
                EnhancedNewsCard(
                    article = article,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                        context.startActivity(intent)
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
fun TopicThreadList(
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

// -----------------------------------------------------------------------------
// MODERN COMPONENTS
// -----------------------------------------------------------------------------

// ... imports remain the same

@Composable
fun ModernCommunityHeader(
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
            // Top Row: Title and Back Button
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

            // ONLY SHOW BUTTONS IF WE ARE ON THE HOME SCREEN (showBack is false)
            if (!showBack) {
                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Row: Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. "My Threads" Button
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

                    // 2. "New Thread" Button
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
fun SectionHeaderModern(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = AppTheme.colors.textPrimary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun HottestThreadsCarouselModern(
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

    // FIX 2: Box with fixed height prevents jumping size
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
                    // Pass fillMaxSize so the card fills the 190.dp Box
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
fun TopicCardModern(topic: Topic, onTopicSelected: (Topic) -> Unit) {
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
fun ThreadCardModern(
    thread: Thread,
    topic: Topic,
    navController: NavController,
    threadViewModel: ThreadViewModel,
    isFeatured: Boolean = false,
    modifier: Modifier = Modifier // Added modifier param
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
            // Header: Icon + Topic Name + Date
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
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = thread.header,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Content Preview
            // If featured, we might hide this if title is long, or clamp lines further
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = thread.paragraph,
                fontSize = 14.sp,
                color = AppTheme.colors.textSecondary,
                maxLines = if (isFeatured) 2 else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            // Push stats to bottom if card has fixed height
            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
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

// FIX 4: Enhanced News Card
@Composable
fun EnhancedNewsCard(article: NewsArticle, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min) // Allows accent bar to fill height
        ) {
            // Left Accent Bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(AppTheme.colors.warmOrange)
            )

            // Content
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Source Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppTheme.colors.warmOrange.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = article.source.uppercase(),
                                fontSize = 10.sp,
                                color = AppTheme.colors.warmOrange,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getRelativeTime(article.publishedDate),
                            fontSize = 11.sp,
                            color = AppTheme.colors.textSecondary
                        )
                    }

                    // Title
                    Text(
                        text = article.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                }

                // Action Icon
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    tint = AppTheme.colors.textSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StatItemModern(icon: ImageVector, text: String, color: Color) {
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

fun getRelativeTime(timestamp: Long): String {
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