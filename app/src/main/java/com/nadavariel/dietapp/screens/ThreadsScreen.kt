package com.nadavariel.dietapp.screens

import android.annotation.SuppressLint
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.viewmodel.ThreadViewModel
import com.nadavariel.dietapp.NavRoutes

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadsScreen(
    navController: NavController,
    threadViewModel: ThreadViewModel = viewModel()
) {
    val topics = listOf("Training", "Diet", "Recipes")

    // Observe threads from Firestore
    val threads by threadViewModel.threads.collectAsState()

    var selectedTopic by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Community Threads") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("create_thread") },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Thread")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            if (selectedTopic == null) {
                // Show list of topics
                Text(
                    "Choose a Topic",
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                topics.forEachIndexed { index, topic ->
                    // Fun gradient colors per topic
                    val gradient = when (index) {
                        0 -> listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        1 -> listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primaryContainer)
                        else -> listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.secondaryContainer)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(gradient))
                            .clickable { selectedTopic = topic }
                            .padding(20.dp)
                    ) {
                        Text(
                            text = topic,
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            } else {
                // Show threads inside selected topic
                Text(
                    "$selectedTopic Threads",
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                threads.filter { it.topic == selectedTopic }.forEach { thread ->
                    // Local state for counts
                    var likeCount by remember(thread.id) { mutableStateOf(0) }
                    var commentCount by remember(thread.id) { mutableStateOf(0) }

                    // Fetch counts once for this card
                    LaunchedEffect(thread.id) {
                        threadViewModel.getLikeCountForThread(thread.id) { likeCount = it }
                        threadViewModel.getCommentCountForThread(thread.id) { commentCount = it }
                    }

                    // Gradient background per thread card
                    val cardBrush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.surface
                        )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(cardBrush)
                            .clickable {
                                // Navigate to ThreadDetailScreen, passing the thread's ID
                                navController.navigate(NavRoutes.threadDetail(thread.id))
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .animateContentSize()
                        ) {
                            // Header + Type row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = thread.header,
                                    fontSize = 16.sp,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = thread.type,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Likes and Comments row with icons
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Likes",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "$likeCount",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Icon(
                                    imageVector = Icons.Default.Comment,
                                    contentDescription = "Comments",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "$commentCount",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Author
                            Text(
                                "by ${thread.authorName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Back to topics
                OutlinedButton(
                    onClick = { selectedTopic = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back to Topics")
                }
            }
        }
    }
}
