package com.nadavariel.dietapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Keep this import
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState // Ensure this is the correct one
import androidx.compose.runtime.getValue // For the 'by' delegate
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.getOrNull
// import androidx.compose.ui.text.font.FontWeight // Only if explicitly used outside MaterialTheme styles
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseUser
import com.nadavariel.dietapp.data.Comment // Assuming Comment class is correct
import com.nadavariel.dietapp.model.Thread // Correct import for your Thread class
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.ThreadViewModel
import java.text.SimpleDateFormat
import java.util.Date // Import Date for converting Long to Date
import java.util.Locale
import kotlin.text.isNotBlank
import kotlin.text.split

// Removed: import kotlin.text.split // This was unused
// Removed: import androidx.slice.builders.header // This was unused

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadDetailScreen(
    navController: NavController,
    threadId: String, // Received from navigation
    threadViewModel: ThreadViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel() // To get current user's name
) {
    // Ensure authViewModel.currentUser is a StateFlow<YourUserType?>
    // and threadViewModel.selectedThread is StateFlow<Thread?>
    // and threadViewModel.comments is StateFlow<List<Comment>>
    val selectedThread by threadViewModel.selectedThread.collectAsState()
    val comments by threadViewModel.comments.collectAsState()
    val currentUser: FirebaseUser? = authViewModel.currentUser // <<<< CORRECT WAY TO ACCESS

    var newCommentText by remember { mutableStateOf("") }

    LaunchedEffect(threadId) {
        threadViewModel.fetchThreadById(threadId)
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
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
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
                                // Corrected authorNameToUse logic
                                val authorNameToUse = currentUser?.displayName?.takeIf { displayName ->
                                    displayName.isNotBlank() // 'it' refers to displayName here
                                } ?: currentUser?.email?.split("@")?.getOrNull(0) // Use getOrNull for safety
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
                    .padding(paddingValues) // Apply padding from Scaffold
            ) {
                item {
                    ThreadContentView(thread = selectedThread!!)
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
                items(comments, key = { it.id }) { comment -> // 'it' here is a Comment object
                    CommentItemView(comment = comment)
                }
                item {
                    Spacer(modifier = Modifier.height(72.dp)) // For bottom bar overlap
                }
            }
        }
    }
}

@Composable
fun ThreadContentView(thread: Thread) {
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
            Text(
                comment.authorName,
                style = MaterialTheme.typography.titleSmall
                // fontWeight = MaterialTheme.typography.titleSmall.fontWeight // Usually inherited from style
            )
            Text(
                // Assuming comment.createdAt is a com.google.firebase.Timestamp
                // If comment.createdAt is a Long, use: dateFormatter.format(Date(comment.createdAt))
                dateFormatter.format(comment.createdAt.toDate()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
