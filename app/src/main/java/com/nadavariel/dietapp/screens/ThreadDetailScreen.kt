package com.nadavariel.dietapp.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseUser
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.model.Comment
import com.nadavariel.dietapp.model.Thread
import com.nadavariel.dietapp.model.communityTopics
import com.nadavariel.dietapp.ui.AppTheme
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
    val context = LocalContext.current

    var newCommentText by remember { mutableStateOf("") }
    var showLikesDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var likedUsers by remember { mutableStateOf<List<String>>(emptyList()) }

    val topic = remember(selectedThread) {
        communityTopics.find { it.key == selectedThread?.topic }
    }
    val accentColor = topic?.gradient?.first() ?: AppTheme.colors.primaryGreen
    val isOwner = remember(currentUser, selectedThread) {
        currentUser != null && selectedThread != null && currentUser.uid == selectedThread!!.authorId
    }

    LaunchedEffect(threadId) {
        threadViewModel.fetchThreadById(threadId)
        threadViewModel.listenForLikes(threadId)
    }

    DisposableEffect(Unit) {
        onDispose { threadViewModel.clearSelectedThreadAndListeners() }
    }

    Scaffold(
        containerColor = AppTheme.colors.screenBackground,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                    if (topic != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            topic.icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = topic.displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // --- Owner Menu ---
                    if (isOwner) {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = AppTheme.colors.textPrimary
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit Thread") },
                                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = AppTheme.colors.primaryGreen) },
                                    onClick = {
                                        showMenu = false
                                        // Navigate to Create Thread Screen with ID (Edit Mode)
                                        navController.navigate(NavRoutes.createThread(threadId))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Thread", color = Color.Red) },
                                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
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
                accentColor = accentColor
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (topic != null) {
                ThemedBackground(icon = topic.icon, color = accentColor)
            }

            if (selectedThread == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppTheme.colors.primaryGreen)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        ThreadContentView(
                            thread = selectedThread!!,
                            likeCount = likeCount,
                            hasUserLiked = hasUserLiked,
                            accentColor = accentColor,
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

                    item {
                        CommentsHeaderSection(commentsCount = comments.size)
                    }

                    if (comments.isEmpty()) {
                        item { EmptyCommentsState() }
                    } else {
                        items(comments, key = { it.id }) { comment ->
                            CommentItemView(comment = comment, accentColor = accentColor)
                        }
                    }
                }
            }
        }
    }

    if (showLikesDialog) {
        LikesDialog(
            likedUsers = likedUsers,
            onDismiss = { showLikesDialog = false },
            accentColor = accentColor
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Thread") },
            text = { Text("Are you sure you want to delete this thread? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        threadViewModel.deleteThread(
                            threadId = threadId,
                            onSuccess = {
                                showDeleteConfirmDialog = false
                                Toast.makeText(context, "Thread deleted", Toast.LENGTH_SHORT).show()
                                navController.popBackStack() // Go back to list
                            },
                            onError = { error ->
                                showDeleteConfirmDialog = false
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// -------------------------------
// --------- COMPOSABLES ---------
// -------------------------------

@Composable
private fun ThemedBackground(icon: ImageVector, color: Color) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(60.dp)
        ) {
            repeat(15) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(6) { colIndex ->
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color.copy(alpha = 0.04f),
                            modifier = Modifier
                                .size(40.dp)
                                .rotate(if ((rowIndex + colIndex) % 2 == 0) -15f else 15f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadContentView(
    thread: Thread,
    likeCount: Int,
    hasUserLiked: Boolean,
    accentColor: Color,
    onLikeClicked: () -> Unit,
    onLikesCountClicked: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = thread.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = dateFormatter.format(Date(thread.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                }
            }

            HorizontalDivider(color = AppTheme.colors.textSecondary.copy(alpha = 0.1f))

            Text(
                text = thread.header,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
            )

            Text(
                text = thread.paragraph,
                style = MaterialTheme.typography.bodyLarge,
                color = AppTheme.colors.textSecondary,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onLikeClicked,
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasUserLiked) accentColor.copy(alpha = 0.1f) else Color.Transparent,
                    border = if (hasUserLiked) null else androidx.compose.foundation.BorderStroke(1.dp, AppTheme.colors.textSecondary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnimatedContent(
                            targetState = hasUserLiked,
                            transitionSpec = { scaleIn(spring(0.8f)) togetherWith scaleOut(animationSpec = tween(200)) },
                            label = "like_icon"
                        ) { liked ->
                            Icon(
                                imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (liked) accentColor else AppTheme.colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        AnimatedContent(
                            targetState = likeCount,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "like_count"
                        ) { count ->
                            Text(
                                text = if (count == 0) "Like" else "$count",
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasUserLiked) accentColor else AppTheme.colors.textSecondary,
                            )
                        }
                    }
                }

                if (likeCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View likes",
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier
                            .clickable(onClick = onLikesCountClicked)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    accentColor: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Add a comment...", color = AppTheme.colors.textSecondary.copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = AppTheme.colors.textSecondary.copy(alpha = 0.3f),
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
                        color = if (isEnabled) accentColor else AppTheme.colors.textSecondary.copy(alpha = 0.3f),
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
private fun CommentsHeaderSection(commentsCount: Int) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = AppTheme.colors.textPrimary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = if (commentsCount == 0) "Comments" else "$commentsCount Comments",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.textPrimary
        )
    }
}

@Composable
private fun CommentItemView(comment: Comment, accentColor: Color) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.authorName.firstOrNull()?.uppercase() ?: "?",
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 14.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = comment.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = dateFormatter.format(comment.createdAt.toDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppTheme.colors.textSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.darkGreyText
                )
            }
        }
    }
}

@Composable
private fun EmptyCommentsState() {
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
                tint = AppTheme.colors.textSecondary.copy(alpha = 0.3f)
            )
            Text(
                "No comments yet",
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textSecondary.copy(alpha = 0.6f)
            )
            Text(
                "Be the first to join the conversation!",
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun LikesDialog(
    likedUsers: List<String>,
    onDismiss: () -> Unit,
    accentColor: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = accentColor) }
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
                                    .background(accentColor.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.firstOrNull()?.uppercase() ?: "?",
                                    color = accentColor,
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
        containerColor = Color.White
    )
}