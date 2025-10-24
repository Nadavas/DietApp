package com.nadavariel.dietapp.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
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
import com.nadavariel.dietapp.data.Topic
import com.nadavariel.dietapp.data.communityTopics
import com.nadavariel.dietapp.model.Thread
import com.nadavariel.dietapp.viewmodel.ThreadViewModel
import kotlinx.coroutines.delay
import kotlin.math.sin

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

    Scaffold(
        containerColor = ScreenBackgroundColor,
        modifier = Modifier.nestedScroll(TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState()).nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedTopic?.displayName ?: "Community",
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = DarkGreyText
                    )
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
                    // Enhanced Add Thread Button
                    Button(
                        onClick = { navController.navigate("create_thread") },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VibrantGreen
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "New Thread",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
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
        // Hottest Thread Section
        if (hottestThreads.isNotEmpty()) {
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Whatshot,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            "Trending Now",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DarkGreyText
                        )
                    }

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
                            slideInVertically(animationSpec = tween(600)) { height -> height } + fadeIn(animationSpec = tween(600)) togetherWith
                                    slideOutVertically(animationSpec = tween(600)) { height -> -height } + fadeOut(animationSpec = tween(600))
                        },
                        label = "HottestThreadAnimation"
                    ) { index ->
                        val thread = hottestThreads[index]
                        val topic = allTopics.find { it.key == thread.topic }
                        if (topic != null) {
                            EnhancedHottestThreadCard(
                                thread = thread,
                                topic = topic,
                                navController = navController,
                                threadViewModel = threadViewModel
                            )
                        }
                    }

                    // Pagination dots
                    if (hottestThreads.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            hottestThreads.indices.forEach { index ->
                                val isSelected = index == currentIndex
                                val width by animateFloatAsState(
                                    targetValue = if (isSelected) 24f else 8f,
                                    animationSpec = spring(),
                                    label = "dotWidth"
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .width(width.dp)
                                        .height(8.dp)
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

        // Topics Section Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Category,
                    contentDescription = null,
                    tint = VibrantGreen,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    "Explore Topics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = DarkGreyText
                )
            }
        }

        // Topic Grid
        val topicRows = allTopics.chunked(2)
        items(topicRows) { rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { topic ->
                    Box(modifier = Modifier.weight(1f)) {
                        EnhancedTopicCard(topic, onTopicSelected)
                    }
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Bottom spacing
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun EnhancedHottestThreadCard(
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

    Surface(
        onClick = { navController.navigate(NavRoutes.threadDetail(thread.id)) },
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        Box {
            // Decorative background pattern
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.15f)
            ) {
                for (i in 0..15) {
                    val x = size.width * (i / 15f)
                    val y = size.height * 0.5f + sin(i * 0.8f) * 40f
                    drawCircle(
                        color = Color.White,
                        radius = 30f,
                        center = Offset(x, y)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(topic.gradient))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Topic badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = topic.icon,
                            contentDescription = topic.displayName,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = topic.displayName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }

                    // Thread title
                    Text(
                        text = thread.header,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 28.sp
                    )

                    // Stats row with enhanced styling
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        EnhancedStatChip(
                            icon = Icons.Outlined.FavoriteBorder,
                            text = "$likeCount"
                        )
                        EnhancedStatChip(
                            icon = Icons.Outlined.ChatBubbleOutline,
                            text = "$commentCount"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnhancedStatChip(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun EnhancedTopicCard(topic: Topic, onTopicSelected: (Topic) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        tonalElevation = 2.dp,
        onClick = { onTopicSelected(topic) }
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(topic.gradient))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon container with subtle glow
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = topic.icon,
                        contentDescription = topic.displayName,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                }

                Text(
                    text = topic.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = topic.subtitle,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
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
                        "Be the first to start a conversation!\nTap 'New Thread' to get started.",
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
                EnhancedThreadCard(thread, topic, navController, threadViewModel)
            }
            // Bottom spacing
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun EnhancedThreadCard(
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
        Row {
            // Color accent bar
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(topic.gradient),
                        RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                    )
            )

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .weight(1f)
            ) {
                // Author row with enhanced styling
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Brush.linearGradient(topic.gradient), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Author",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
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
                            text = "2h ago",
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
}

@Composable
fun ThreadStatItem(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}