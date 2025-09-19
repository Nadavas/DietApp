package com.nadavariel.dietapp.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                topics.forEach { topic ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selectedTopic = topic },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = topic,
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                // Show threads inside selected topic
                Text(
                    "$selectedTopic Threads",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                threads.filter { it.topic == selectedTopic }.forEach { thread ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                // Navigate to ThreadDetailScreen, passing the thread's ID
                                navController.navigate(NavRoutes.threadDetail(thread.id))
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                thread.header,
                                fontSize = 16.sp,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                thread.paragraph,
                                fontSize = 14.sp,
                                maxLines = 5 // preview only
                            )
                            Spacer(modifier = Modifier.height(6.dp))
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Topics")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Create new thread button (always visible at bottom)
            Button(
                onClick = {
                    navController.navigate("create_thread")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create New Thread")
            }
        }
    }
}
