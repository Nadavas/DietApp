package com.nadavariel.dietapp.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseUser
import com.nadavariel.dietapp.data.Comment
import com.nadavariel.dietapp.data.communityTopics // Import communityTopics
import com.nadavariel.dietapp.model.Thread // Import your Thread model
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.ThreadViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadDetailScreen(
    navController: NavController,
    threadId: String,
    threadViewModel: ThreadViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val selectedThread by threadViewModel.selectedThread.collectAsState()
    val comments by threadViewModel.comments.collectAsState()
    val likeCount by threadViewModel.likeCount.collectAsState()
    val hasUserLiked by threadViewModel.hasUserLiked.collectAsState()

    val currentUser: FirebaseUser? = authViewModel.currentUser
    var newCommentText by remember { mutableStateOf("") }
    var showLikesDialog by remember { mutableStateOf(false) }
    var likedUsers by remember { mutableStateOf<List<String>>(emptyList()) }

    // Find the topic based on the thread's topic key to get its gradient and icon
    val topic = remember(selectedThread) {
        communityTopics.find { it.key == selectedThread?.topic }
    }
    val gradient = topic?.gradient ?: listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)

    LaunchedEffect(threadId) {
        threadViewModel.fetchThreadById(threadId)
        threadViewModel.listenForLikes(threadId)
    }

    DisposableEffect(Unit) {
        onDispose {
            threadViewModel.clearSelectedThreadAndComments()
        }
    }

    Scaffold(
        containerColor = Color.Transparent, // Make container transparent to see the background
        topBar = {
            TopAppBar(
                title = {
                    if (topic != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(topic.icon, contentDescription = null, tint = gradient.first(), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = topic.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // Make TopAppBar transparent
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            CommentInputField(
                value = newCommentText,
                onValueChange = { newCommentText = it },
                onSendClick = {
                    if (newCommentText.isNotBlank() && selectedThread != null) {
                        val authorNameToUse = currentUser?.displayName?.takeIf { it.isNotBlank() }
                            ?: currentUser?.email?.substringBefore("@")
                            ?: "Anonymous"
                        threadViewModel.addComment(
                            threadId = selectedThread!!.id,
                            commentText = newCommentText,
                            authorName = authorNameToUse
                        )
                        newCommentText = ""
                    }
                },
                gradient = gradient
            )
        }
    ) { paddingValues ->
        // The main content area is now a Box to layer the background and content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding here to the container
        ) {
            // 1. Themed Background (Drawn first, so it's in the back)
            if (topic != null) {
                ThemedBackground(icon = topic.icon, color = gradient.first())
            } else {
                // Fallback for when topic is loading
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)))
            }

            // 2. Your Content (Drawn on top of the background)
            if (selectedThread == null) {
                // Loading state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(), // It fills the Box
                    contentPadding = PaddingValues(bottom = 80.dp),
                ) {
                    // Main thread content
                    item {
                        ThreadContentView(
                            thread = selectedThread!!,
                            likeCount = likeCount,
                            hasUserLiked = hasUserLiked,
                            gradient = gradient,
                            onLikeClicked = {
                                if (currentUser != null) {
                                    val authorName = currentUser.displayName?.takeIf { it.isNotBlank() }
                                        ?: currentUser.email?.substringBefore("@")
                                        ?: "Anonymous"
                                    threadViewModel.toggleLike(threadId, currentUser.uid, authorName)
                                }
                            },
                            onLikesCountClicked = {
                                threadViewModel.getLikesForThread(threadId) { users ->
                                    likedUsers = users
                                    showLikesDialog = true
                                }
                            }
                        )
                    }

                    // Comments section
                    item {
                        CommentsHeaderSection(commentsCount = comments.size, color = gradient.first())
                    }

                    if (comments.isEmpty()) {
                        item { EmptyCommentsState() }
                    } else {
                        items(comments, key = { it.id }) { comment ->
                            CommentItemView(comment = comment, gradient = gradient)
                        }
                    }
                }
            }
        }
    }

    // Dialog to show users who liked the thread
    if (showLikesDialog) {
        LikesDialog(
            likedUsers = likedUsers,
            onDismiss = { showLikesDialog = false },
            gradient = gradient
        )
    }
}

@Composable
fun ThemedBackground(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        // This creates a grid of icons to form a wallpaper-like pattern
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(80.dp)
        ) {
            repeat(10) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    repeat(5) { colIndex ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color.copy(alpha = 0.05f), // Very low alpha for subtlety
                            modifier = Modifier
                                .size(80.dp)
                                .rotate(if ((rowIndex + colIndex) % 2 == 0) -15f else 15f) // Rotate icons slightly
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadContentView(
    thread: Thread,
    likeCount: Int,
    hasUserLiked: Boolean,
    gradient: List<Color>,
    onLikeClicked: () -> Unit,
    onLikesCountClicked: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Author Info
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Brush.linearGradient(gradient), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = thread.authorName.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(
                    text = thread.authorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateFormatter.format(Date(thread.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Thread Header
        Text(
            text = thread.header,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Thread Content
        Text(
            text = thread.paragraph,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )

        // Likes section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Animated Like Button
            val likeColor by animateColorAsState(
                targetValue = if (hasUserLiked) gradient.first() else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(300),
                label = ""
            )
            IconButton(onClick = onLikeClicked) {
                AnimatedContent(
                    targetState = hasUserLiked,
                    transitionSpec = { scaleIn(spring(0.8f)) togetherWith scaleOut(animationSpec = tween(200)) },
                    label = "like_icon"
                ) { liked ->
                    Icon(
                        imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = likeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            // Like count text
            AnimatedContent(
                targetState = likeCount,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "like_count"
            ) { count ->
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(
                        enabled = count > 0,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onLikesCountClicked
                    )
                )
            }
        }
    }
}

@Composable
fun CommentInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    gradient: List<Color>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a thoughtful comment...") },
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                maxLines = 4
            )

            val isEnabled = value.isNotBlank()
            IconButton(
                onClick = onSendClick,
                enabled = isEnabled,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(if (isEnabled) gradient else listOf(Color.Gray, Color.Gray)),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun CommentsHeaderSection(commentsCount: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = color
        )
        Text(
            text = if (commentsCount == 0) "Comments" else "$commentsCount Comments",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CommentItemView(comment: Comment, gradient: List<Color>) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Author Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Brush.linearGradient(gradient), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = comment.authorName.firstOrNull()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Comment Content
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = dateFormatter.format(comment.createdAt.toDate()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyCommentsState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Text(
                "Be the first to comment",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun LikesDialog(
    likedUsers: List<String>,
    onDismiss: () -> Unit,
    gradient: List<Color>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = gradient.first()) }
        },
        title = { Text("Liked by", fontWeight = FontWeight.Bold) },
        text = {
            if (likedUsers.isEmpty()) {
                Text("No one has liked this yet.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(likedUsers) { name ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Brush.linearGradient(gradient), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.firstOrNull()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
