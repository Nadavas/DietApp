package com.nadavariel.dietapp.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.model.Topic
import com.nadavariel.dietapp.model.communityTopics
import com.nadavariel.dietapp.model.Thread
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.viewmodel.ThreadViewModel
import kotlinx.coroutines.delay
import kotlin.math.sin
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.nadavariel.dietapp.viewmodel.NewsViewModel
import com.nadavariel.dietapp.model.NewsArticle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ThreadsScreen(
    navController: NavController,
    threadViewModel: ThreadViewModel = viewModel()
) {
    val allTopics = communityTopics
    val threads by threadViewModel.threads.collectAsState()
    val hottestThreads by threadViewModel.hottestThreads.collectAsState()

    var selectedTopicKey by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedTopic: Topic? = remember(selectedTopicKey) {
        allTopics.find { it.key == selectedTopicKey }
    }

    if (selectedTopicKey != null) {
        BackHandler(enabled = true) {
            selectedTopicKey = null
        }
    }

    // Pulsing animation for New button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        containerColor = AppTheme.colors.screenBackground,
        modifier = Modifier.nestedScroll(TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()).nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Animated sparkle icon
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(3000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "sparkleRotation"
                        )
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AppTheme.colors.primaryGreen,
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(rotation)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = selectedTopic?.displayName ?: "Community",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = AppTheme.colors.darkGreyText
                        )
                    }
                },
                navigationIcon = {
                    if (selectedTopicKey != null) {
                        IconButton(onClick = { selectedTopicKey = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Community",
                                tint = AppTheme.colors.darkGreyText
                            )
                        }
                    }
                },
                actions = {
                    // Pulsing FAB-style Add Thread Button
                    FloatingActionButton(
                        onClick = { navController.navigate("create_thread") },
                        containerColor = AppTheme.colors.primaryGreen,
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(48.dp)
                            .scale(pulseScale)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Thread",
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                "New",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.colors.screenBackground
                )
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedTopicKey,
            transitionSpec = {
                fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it / 2 } togetherWith
                        fadeOut(tween(400)) + slideOutHorizontally(tween(400)) { -it / 2 }
            },
            label = "Topic/Thread Transition"
        ) { key ->
            if (key == null || selectedTopic == null) {
                CommunityHomeScreen(
                    paddingValues = paddingValues,
                    hottestThreads = hottestThreads,
                    allThreads = threads, // Pass all threads for the "Latest" feed
                    allTopics = allTopics,
                    navController = navController,
                    threadViewModel = threadViewModel,
                    onTopicSelected = { topic ->
                        selectedTopicKey = topic.key
                    }
                )
            } else {
                ThreadList(
                    paddingValues = paddingValues,
                    threads = threads.filter { it.topic == key },
                    topic = selectedTopic,
                    navController = navController,
                    threadViewModel = threadViewModel
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CommunityHomeScreen(
    paddingValues: PaddingValues,
    hottestThreads: List<Thread>,
    allThreads: List<Thread>,
    allTopics: List<Topic>,
    navController: NavController,
    threadViewModel: ThreadViewModel,
    newsViewModel: NewsViewModel = viewModel(),
    onTopicSelected: (Topic) -> Unit
) {
    // FIX 1: Get the current context for the Intent
    val context = LocalContext.current

    // FIX 2: Collect the state from the ViewModel
    // This resolves 'articles' and 'isLoadingNews' errors
    val articles by newsViewModel.articles.collectAsState()
    val isLoadingNews by newsViewModel.isLoading.collectAsState()

    LazyColumn(
        modifier = Modifier.padding(paddingValues),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Animated Floating Particles Background
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            ) {
                FloatingParticles()
            }
        }

        // Daily Challenge Card
        item {
            DailyChallengeCard()
        }

        // Hottest Thread Section (Carousel)
        if (hottestThreads.isNotEmpty()) {
            item {
                Column {
                    AnimatedSectionHeader(
                        icon = Icons.Outlined.Whatshot,
                        text = "Trending Now",
                        iconColor = Color(0xFFFF6B6B)
                    )

                    var currentIndex by remember { mutableIntStateOf(0) }

                    LaunchedEffect(key1 = hottestThreads.size) {
                        while (true) {
                            delay(5000)
                            if (hottestThreads.isNotEmpty()) {
                                currentIndex = (currentIndex + 1) % hottestThreads.size
                            }
                        }
                    }

                    AnimatedContent(
                        targetState = currentIndex,
                        transitionSpec = {
                            (slideInVertically(animationSpec = tween(600)) { height -> height } +
                                    fadeIn(animationSpec = tween(600)) +
                                    scaleIn(initialScale = 0.95f, animationSpec = tween(600))) togetherWith
                                    (slideOutVertically(animationSpec = tween(600)) { height -> -height } +
                                            fadeOut(animationSpec = tween(600)) +
                                            scaleOut(targetScale = 0.95f, animationSpec = tween(600)))
                        },
                        label = "HottestThreadAnimation"
                    ) { index ->
                        val thread = hottestThreads[index]
                        val topic = allTopics.find { it.key == thread.topic }
                        if (topic != null) {
                            DynamicHottestThreadCard(
                                thread = thread,
                                topic = topic,
                                navController = navController,
                                threadViewModel = threadViewModel
                            )
                        }
                    }

                    if (hottestThreads.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            hottestThreads.indices.forEach { index ->
                                val isSelected = index == currentIndex
                                val width by animateFloatAsState(
                                    targetValue = if (isSelected) 32f else 8f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "dotWidth"
                                )
                                val height by animateFloatAsState(
                                    targetValue = if (isSelected) 8f else 6f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "dotHeight"
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .width(width.dp)
                                        .height(height.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isSelected) AppTheme.colors.primaryGreen else AppTheme.colors.lightGreyText.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Topics Section with animated header
        item {
            AnimatedSectionHeader(
                icon = Icons.Outlined.Category,
                text = "Explore Topics",
                iconColor = AppTheme.colors.primaryGreen
            )
        }

        // Horizontal scrolling topics with staggered animation
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                itemsIndexed(allTopics) { index, topic ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index * 100L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(500)) + scaleIn(tween(500), initialScale = 0.8f)
                    ) {
                        EnhancedTopicCard(topic, onTopicSelected)
                    }
                }
            }
        }

        // Nutrition News Section Header
        item {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedSectionHeader(
                    icon = Icons.Outlined.Article,
                    text = "Nutrition News",
                    iconColor = Color(0xFFFF9800)
                )

                // Refresh button
                IconButton(
                    onClick = { newsViewModel.refresh() },
                    modifier = Modifier.size(32.dp)
                ) {
                    val rotation by rememberInfiniteTransition(label = "refresh").animateFloat(
                        initialValue = 0f,
                        targetValue = if (isLoadingNews) 360f else 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "refreshRotation"
                    )
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Refresh",
                        tint = AppTheme.colors.lightGreyText,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }
        }

        // News Content Logic
        if (isLoadingNews) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AppTheme.colors.primaryGreen,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        } else if (articles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Article,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = AppTheme.colors.lightGreyText.copy(alpha = 0.5f)
                        )
                        Text(
                            "No articles available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppTheme.colors.lightGreyText,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = { newsViewModel.refresh() }) {
                            Text("Tap to retry")
                        }
                    }
                }
            }
        } else {
            // FIX 3: articles is now a List<NewsArticle>, so items() works correctly
            items(articles) { article ->
                NewsArticleCard(
                    article = article,
                    onClick = {
                        // FIX 4: context and article.url are now resolved
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                        context.startActivity(intent)
                    }
                )
            }
        }

        // Bottom spacing
        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

// --- NEW COMPOSABLE: Daily Challenge ---
@Composable
fun DailyChallengeCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier.background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF2196F3), Color(0xFF21CBF3))
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocalDrink,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "DAILY CHALLENGE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.8f),
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hydration Hero",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Drink 8 glasses of water today. 2,431 users participating!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Button(
                    onClick = { /* Join challenge logic */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF2196F3)
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("JOIN", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FloatingParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    // Fix: Using list to store animated values
    val particleOffsets = List(8) { i ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 100f,
            animationSpec = infiniteRepeatable(
                animation = tween((2000 + i * 500), easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "particle$i"
        )
    }

    val particleColor = AppTheme.colors.primaryGreen.copy(alpha = 0.1f)

    Canvas(modifier = Modifier.fillMaxSize()) {
        particleOffsets.forEachIndexed { i, offset ->
            drawCircle(
                color = particleColor,
                radius = 4f + (i % 3) * 2f,
                center = Offset(
                    x = size.width * (i / 8f),
                    y = (offset.value % size.height)
                )
            )
        }
    }
}

@Composable
fun AnimatedSectionHeader(icon: ImageVector, text: String, iconColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "iconPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(28.dp)
                .scale(scale)
        )
        Text(
            text,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = AppTheme.colors.darkGreyText
        )
    }
}

@Composable
fun DynamicHottestThreadCard(
    thread: Thread,
    topic: Topic,
    navController: NavController,
    threadViewModel: ThreadViewModel
) {
    val likeCount by produceState(initialValue = 0, thread.id) {
        threadViewModel.getLikeCountForThread(thread.id) { count -> value = count }
    }
    val commentCount by produceState(initialValue = 0, thread.id) {
        threadViewModel.getCommentCountForThread(thread.id) { count -> value = count }
    }

    // Shimmer effect
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Card(
        onClick = { navController.navigate(NavRoutes.threadDetail(thread.id)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box {
            // Animated background pattern
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(shimmerAlpha)
            ) {

                repeat(10) { i ->
                    drawCircle(
                        color = topic.gradient.first().copy(alpha = 0.1f),
                        radius = 30f,
                        center = Offset(
                            x = size.width * (i / 10f),
                            y = size.height * 0.5f + sin(i * 0.8f) * 40f
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Topic badge with pulse
                val badgeScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "badgeScale"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .scale(badgeScale)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.horizontalGradient(topic.gradient.map { it.copy(alpha = 0.2f) }))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = topic.icon,
                        contentDescription = topic.displayName,
                        modifier = Modifier.size(20.dp),
                        tint = topic.gradient.first()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = topic.displayName.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = topic.gradient.first(),
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                // Thread title
                Text(
                    text = thread.header,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = AppTheme.colors.darkGreyText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 28.sp
                )

                // Stats row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AnimatedStatChip(
                        icon = Icons.Outlined.FavoriteBorder,
                        text = "$likeCount",
                        color = topic.gradient.first()
                    )
                    AnimatedStatChip(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        text = "$commentCount",
                        color = topic.gradient.last()
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedStatChip(icon: ImageVector, text: String, color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "chipPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chipScale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun EnhancedTopicCard(topic: Topic, onTopicSelected: (Topic) -> Unit) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(170.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { onTopicSelected(topic) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Reduced padding slightly to fit longer text
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Animated icon container
                val infiniteTransition = rememberInfiniteTransition(label = "iconBounce")
                val offsetY by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = -8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "iconOffset"
                )

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .offset(y = offsetY.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(topic.gradient.map { it.copy(alpha = 0.2f) })),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = topic.icon,
                        contentDescription = topic.displayName,
                        modifier = Modifier.size(30.dp),
                        tint = topic.gradient.first()
                    )
                }

                Text(
                    text = topic.displayName,
                    fontSize = 14.sp, // Slightly smaller to accommodate "Gears & Tech"
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.darkGreyText,
                    textAlign = TextAlign.Center,
                    maxLines = 2, // Allow wrapping for long names
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Text(
                    text = topic.subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppTheme.colors.lightGreyText,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ThreadList(
    paddingValues: PaddingValues,
    threads: List<Thread>,
    topic: Topic,
    navController: NavController,
    threadViewModel: ThreadViewModel
) {
    if (threads.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "No threads",
                            modifier = Modifier.size(40.dp),
                            tint = AppTheme.colors.primaryGreen.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        "No Threads Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.darkGreyText
                    )
                    Text(
                        "Be the first to start a conversation!\nTap 'New' to get started.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppTheme.colors.lightGreyText,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(threads, key = { it.id }) { thread ->
                CleanThreadCard(thread, topic, navController, threadViewModel)
            }
            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun CleanThreadCard(
    thread: Thread,
    topic: Topic,
    navController: NavController,
    threadViewModel: ThreadViewModel
) {
    val likeCount by produceState(initialValue = 0, thread.id) {
        threadViewModel.getLikeCountForThread(thread.id) { count -> value = count }
    }
    val commentCount by produceState(initialValue = 0, thread.id) {
        threadViewModel.getCommentCountForThread(thread.id) { count -> value = count }
    }

    Card(
        onClick = { navController.navigate(NavRoutes.threadDetail(thread.id)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Topic indicator strip
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(0.3f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(topic.gradient))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Author row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Brush.linearGradient(topic.gradient.map { it.copy(alpha = 0.2f) }), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Author",
                        modifier = Modifier.size(24.dp),
                        tint = topic.gradient.first()
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = thread.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.darkGreyText
                    )
                    Text(
                        text = getRelativeTime(thread.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.lightGreyText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Thread title
            Text(
                text = thread.header,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = AppTheme.colors.darkGreyText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Thread preview
            Text(
                text = thread.paragraph.take(120) + if (thread.paragraph.length > 120) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.lightGreyText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                color = AppTheme.colors.lightGreyText.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                ThreadStatItem(
                    Icons.Outlined.FavoriteBorder,
                    "$likeCount",
                    topic.gradient.first()
                )
                ThreadStatItem(
                    Icons.Outlined.ChatBubbleOutline,
                    "$commentCount",
                    topic.gradient.last()
                )
            }
        }
    }
}

// --- NEW COMPOSABLE: News Article Card ---
@Composable
fun NewsArticleCard(
    article: NewsArticle,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFFF9800),
                                Color(0xFFFFB74D)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Source and date
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFF9800).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = article.source,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = "•",
                        color = AppTheme.colors.lightGreyText.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                    Text(
                        text = getRelativeTime(article.publishedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.lightGreyText,
                        fontSize = 10.sp
                    )
                }

                // Title
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.darkGreyText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                // Description preview
                if (article.description.isNotEmpty()) {
                    Text(
                        text = article.description.take(100) + if (article.description.length > 100) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.lightGreyText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp,
                        fontSize = 12.sp
                    )
                }
            }

            // External link indicator
            Icon(
                Icons.Outlined.OpenInNew,
                contentDescription = "Open article",
                tint = Color(0xFFFF9800).copy(alpha = 0.6f),
                modifier = Modifier
                    .size(20.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun ThreadStatItem(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now" // Less than 1 minute
        diff < 3600_000 -> "${diff / 60_000}m ago" // Minutes
        diff < 86400_000 -> "${diff / 3600_000}h ago" // Hours
        diff < 604800_000 -> "${diff / 86400_000}d ago" // Days
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp)) // Date
    }
}