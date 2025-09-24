package com.nadavariel.dietapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseUser
import com.nadavariel.dietapp.data.Comment
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.ThreadViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    // ✅ State for showing likes dialog
    var showLikesDialog by remember { mutableStateOf(false) }
    var likedUsers by remember { mutableStateOf<List<String>>(emptyList()) }

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
        topBar = {
            TopAppBar(
                title = { Text(selectedThread?.header ?: "Thread Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        label = { Text("Write a comment...") },
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
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
                        enabled = newCommentText.isNotBlank() && selectedThread != null
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment")
                    }
                }
            }
        }
    ) { paddingValues ->
        if (selectedThread == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                item {
                    ThreadContentView(
                        thread = selectedThread!!,
                        likeCount = likeCount,
                        hasUserLiked = hasUserLiked,
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
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    if (comments.isNotEmpty()) {
                        Text(
                            "Comments",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                        )
                    } else {
                        Text(
                            "No comments yet. Be the first to comment!",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }
                }

                items(comments, key = { it.id }) { comment ->
                    CommentItemView(comment = comment)
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    // ✅ Dialog to show liked users
    if (showLikesDialog) {
        AlertDialog(
            onDismissRequest = { showLikesDialog = false },
            confirmButton = {
                TextButton(onClick = { showLikesDialog = false }) {
                    Text("Close")
                }
            },
            title = { Text("Liked by") },
            text = {
                if (likedUsers.isEmpty()) {
                    Text("No likes yet.")
                } else {
                    Column {
                        likedUsers.forEach { name ->
                            Text(text = name)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ThreadContentView(
    thread: com.nadavariel.dietapp.model.Thread,
    likeCount: Int,
    hasUserLiked: Boolean,
    onLikeClicked: () -> Unit,
    onLikesCountClicked: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy 'at' hh:mma", Locale.getDefault()) }
    Column(modifier = Modifier.padding(16.dp)) {
        Text(thread.header, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "by ${thread.authorName} on ${dateFormatter.format(Date(thread.timestamp))}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(thread.paragraph, style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLikeClicked) {
                if (hasUserLiked) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Unlike")
                } else {
                    Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Like")
                }
            }
            Text(
                "$likeCount likes",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onLikesCountClicked() }
            )
        }
    }
}

@Composable
fun CommentItemView(comment: Comment) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd, hh:mma", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(comment.authorName, style = MaterialTheme.typography.titleSmall)
            Text(
                dateFormatter.format(comment.createdAt.toDate()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
