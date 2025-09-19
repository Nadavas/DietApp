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
import androidx.navigation.NavController

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadsScreen(
    navController: NavController
) {
    // Sample threads (later will come from Firebase or ViewModel)
    val threads = listOf(
        ThreadItem(title = "Best high-protein breakfast ideas?", author = "Alice"),
        ThreadItem(title = "How do you stay consistent with workouts?", author = "Bob"),
        ThreadItem(title = "Healthy snacks for late-night cravings?", author = "Charlie")
    )

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
            Text("Latest Threads", fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

            threads.forEach { thread ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            // Later: navController.navigate("${NavRoutes.THREAD_DETAIL}/${thread.id}")
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(thread.title, fontSize = 16.sp, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("by ${thread.author}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Create new thread button
            Button(
                onClick = {
                    // Later: navController.navigate(NavRoutes.CREATE_THREAD)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create New Thread")
            }
        }
    }
}

data class ThreadItem(
    val id: String = "",
    val title: String,
    val author: String
)
