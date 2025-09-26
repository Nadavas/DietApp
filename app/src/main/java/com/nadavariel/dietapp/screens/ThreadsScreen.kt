package com.nadavariel.dietapp.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.model.Thread // Ensure you import your Thread model
import com.nadavariel.dietapp.viewmodel.ThreadViewModel

// ## MODIFICATION 1: The Topic data class ##
// It now holds a 'key' (for Firebase) and a 'displayName' (for the UI).
data class Topic(
    val key: String,
    val displayName: String,
    val icon: ImageVector,
    val gradient: List<Color>
)

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ThreadsScreen(
    navController: NavController,
    threadViewModel: ThreadViewModel = viewModel()
) {
    // ## MODIFICATION 2: The Topics List ##
    // This list now uses your original Firebase keys ("Training", "Diet", "Recipes")
    // while showing the new, enhanced UI.
    val topics = listOf(
        Topic("Training", "Training & Fitness", Icons.Outlined.FitnessCenter, listOf(Color(0xFFF9484A), Color(0xFFFBD72B))),
        Topic("Diet", "Diet & Nutrition", Icons.Outlined.RestaurantMenu, listOf(Color(0xFF6A11CB), Color(0xFF2575FC))),
        Topic("Recipes", "Healthy Recipes", Icons.Outlined.MenuBook, listOf(Color(0xFF16A085), Color(0xFFF4D03F)))
    )

    val threads by threadViewModel.threads.collectAsState()

    // ## MODIFICATION 3: State Management ##
    // We now store the selected topic's 'key' to ensure correct filtering.
    var selectedTopicKey by remember { mutableStateOf<String?>(null) }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        topBar = {
            // Find the display name from the selected key for the title
            val currentTopic = topics.find { it.key == selectedTopicKey }
            TopAppBar(
                title = { Text(if (currentTopic == null) "Community Topics" else "${currentTopic.displayName} Threads") },
                navigationIcon = {
                    if (selectedTopicKey != null) {
                        IconButton(onClick = { selectedTopicKey = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to topics")
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("create_thread") },
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Thread", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = selectedTopicKey,
            transitionSpec = {
                fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) togetherWith
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(300))
            },
            label = "Topic/Thread"
        ) { topicKey ->
            if (topicKey == null) {
                TopicSelectionGrid(paddingValues, topics) { selectedKey ->
                    selectedTopicKey = selectedKey
                }
            } else {
                // ## MODIFICATION 4: The Filter ##
                // This now correctly filters threads using the topic 'key' from Firebase.
                ThreadList(paddingValues, threads.filter { it.topic == topicKey }, navController, threadViewModel)
            }
        }
    }
}


@Composable
fun TopicSelectionGrid(
    paddingValues: PaddingValues,
    topics: List<Topic>,
    onTopicSelected: (String) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier
            .padding(paddingValues)
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp
    ) {
        items(topics) { topic ->
            TopicCard(topic, onTopicSelected)
        }
    }
}

@Composable
fun TopicCard(topic: Topic, onTopicSelected: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onTopicSelected(topic.key) }, // Pass the key on click
        shadowElevation = 4.dp,
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.verticalGradient(topic.gradient))
                .padding(vertical = 32.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = topic.icon,
                    contentDescription = topic.displayName,
                    modifier = Modifier.size(36.dp),
                    tint = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = topic.displayName, // Show the pretty display name
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}


@Composable
fun ThreadList(
    paddingValues: PaddingValues,
    threads: List<Thread>,
    navController: NavController,
    threadViewModel: ThreadViewModel
) {
    if (threads.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            Text("No threads in this topic yet.\nBe the first to post!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(threads, key = { it.id }) { thread ->
                ThreadCard(thread, navController, threadViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadCard(
    thread: Thread,
    navController: NavController,
    threadViewModel: ThreadViewModel
) {
    var likeCount by remember(thread.id) { mutableStateOf(0) }
    var commentCount by remember(thread.id) { mutableStateOf(0) }

    LaunchedEffect(thread.id) {
        threadViewModel.getLikeCountForThread(thread.id) { count -> likeCount = count }
        threadViewModel.getCommentCountForThread(thread.id) { count -> commentCount = count }
    }

    Card(
        onClick = { navController.navigate(NavRoutes.threadDetail(thread.id)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = thread.header,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false)
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(thread.type) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "by ${thread.authorName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Likes",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "$likeCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = "Comments",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "$commentCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}