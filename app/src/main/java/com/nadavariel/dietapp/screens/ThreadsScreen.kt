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
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.nadavariel.dietapp.viewmodel.ThreadViewModel
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

// --- DESIGN TOKENS (matching app theme) ---
private val VibrantGreen = Color(0xFF4CAF50)
private val DarkGreyText = Color(0xFF333333)
private val LightGreyText = Color(0xFF757575)
private val ScreenBackgroundColor = Color(0xFFF7F9FC)

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
        containerColor = ScreenBackgroundColor,
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
                            tint = VibrantGreen,
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(rotation)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = selectedTopic?.displayName ?: "Community",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = DarkGreyText
                        )
                    }
                },
                navigationIcon = {
                    if (selectedTopicKey != null) {
                        IconButton(onClick = { selectedTopicKey = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Community",
                                tint = DarkGreyText
                            )
                        }
                    }
                },
                actions = {
                    // Pulsing FAB-style Add Thread Button
                    FloatingActionButton(
                        onClick = { navController.navigate("create_thread") },
                        containerColor = VibrantGreen,
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
                    containerColor = ScreenBackgroundColor
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
    allTopics: List<Topic>,
    navController: NavController,
    threadViewModel: ThreadViewModel,
    onTopicSelected: (Topic) -> Unit
) {
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

        // Hottest Thread Section with flame animation
        if (hottestThreads.isNotEmpty()) {
            item {
                Column {
                    AnimatedSectionHeader(
                        icon = Icons.Outlined.Whatshot,
                        text = "Trending Now",
                        iconColor = Color(0xFFFF6B6B)
                    )

                    // Cycling hottest thread with animation
                    var currentIndex by remember { mutableStateOf(0) }

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

                    // Enhanced animated pagination dots
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
                                            if (isSelected) VibrantGreen else LightGreyText.copy(alpha = 0.3f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Stats Card
        item {
            QuickStatsCard(
                totalThreads = hottestThreads.size + allTopics.size * 3,
                activeUsers = 247
            )
        }

        // Topics Section with animated header
        item {
            AnimatedSectionHeader(
                icon = Icons.Outlined.Category,
                text = "Explore Topics",
                iconColor = VibrantGreen
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

        // Recent Activity Section
        item {
            AnimatedSectionHeader(
                icon = Icons.Outlined.Schedule,
                text = "Recent Activity",
                iconColor = Color(0xFF9C27B0)
            )
        }

        // Activity items with staggered entrance
        items(3) { index ->
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(index * 150L)
                visible = true
            }
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it }
            ) {
                RecentActivityItem(index)
            }
        }

        // Bottom spacing
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun FloatingParticles() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")

    // FIX: Animate all values outside the Canvas block
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

    Canvas(modifier = Modifier.fillMaxSize()) {
        particleOffsets.forEachIndexed { i, offset ->
            drawCircle(
                color = VibrantGreen.copy(alpha = 0.1f),
                radius = 4f + (i % 3) * 2f,
                center = Offset(
                    x = size.width * (i / 8f),
                    y = (offset.value % size.height) // Use .value here
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
            color = DarkGreyText
        )
    }
}

@Composable
fun QuickStatsCard(totalThreads: Int, activeUsers: Int) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            AnimatedStatItem(
                icon = Icons.Outlined.Forum,
                value = totalThreads,
                label = "Threads",
                color = Color(0xFF2196F3)
            )
            Divider(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp),
                color = LightGreyText.copy(alpha = 0.2f)
            )
            AnimatedStatItem(
                icon = Icons.Outlined.People,
                value = activeUsers,
                label = "Active Users",
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun AnimatedStatItem(icon: ImageVector, value: Int, label: String, color: Color) {
    val animatedValue by animateIntAsState(
        targetValue = value,
        animationSpec = tween(1500, easing = FastOutSlowInEasing),
        label = "statValue"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = "$animatedValue",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = LightGreyText
        )
    }
}

@Composable
fun RecentActivityItem(index: Int) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VibrantGreen.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = VibrantGreen,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "User ${index + 1} commented",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkGreyText
                )
                Text(
                    "${index + 2}m ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightGreyText
                )
            }
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = LightGreyText,
                modifier = Modifier.size(20.dp)
            )
        }
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
                val spacing = 80f
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
                    color = DarkGreyText,
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
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        .size(64.dp)
                        .offset(y = offsetY.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(topic.gradient.map { it.copy(alpha = 0.2f) })),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = topic.icon,
                        contentDescription = topic.displayName,
                        modifier = Modifier.size(32.dp),
                        tint = topic.gradient.first()
                    )
                }

                Text(
                    text = topic.displayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreyText,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = topic.subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = LightGreyText,
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
                            .background(VibrantGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "No threads",
                            modifier = Modifier.size(40.dp),
                            tint = VibrantGreen.copy(alpha = 0.6f)
                        )
                    }
                    Text(
                        "No Threads Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreyText
                    )
                    Text(
                        "Be the first to start a conversation!\nTap 'New' to get started.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = LightGreyText,
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
            // ** This part was missing from your composable,
            // but I'm adding it for consistency **
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .fillMaxWidth(0.3f) // Make it a small indicator
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
                        color = DarkGreyText
                    )
                    Text(
                        text = "2h ago", // This is hardcoded, consider passing real data
                        style = MaterialTheme.typography.bodySmall,
                        color = LightGreyText
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Thread title
            Text(
                text = thread.header,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DarkGreyText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Thread preview
            Text(
                text = thread.paragraph.take(120) + if (thread.paragraph.length > 120) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                color = LightGreyText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = LightGreyText.copy(alpha = 0.15f))

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

@Composable
fun ThreadStatItem(icon: ImageVector, text: String, color: Color) {
    // This is the aligned version, using the same style as AnimatedStatChip
    // but without the infinite animation.
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
            modifier = Modifier.size(18.dp) // Aligned with AnimatedStatChip
        )
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}