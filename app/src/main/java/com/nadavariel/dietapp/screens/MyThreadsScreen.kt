package com.nadavariel.dietapp.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.model.Thread
import com.nadavariel.dietapp.model.communityTopics
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.AppTopBar
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.ThreadViewModel

@Composable
fun MyThreadsScreen(
    navController: NavController,
    threadViewModel: ThreadViewModel,
    authViewModel: AuthViewModel
) {
    val userThreads by threadViewModel.userThreads.collectAsState()
    val currentUser = authViewModel.currentUser
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf<Thread?>(null) }

    LaunchedEffect(currentUser) {
        currentUser?.let { threadViewModel.fetchUserThreads(it.uid) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AppTopBar(
                title = "My Threads",
                onBack = { navController.popBackStack() },
                containerColor = Color.White
            )

            if (userThreads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("You haven't posted any threads yet.", color = AppTheme.colors.textSecondary)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(userThreads, key = { it.id }) { thread ->
                        MyThreadCard(
                            thread = thread,
                            onEdit = {
                                val route = NavRoutes.createThread(threadId = thread.id)
                                navController.navigate(route)
                            },
                            onDelete = { showDeleteDialog = thread },
                            onClick = { navController.navigate(NavRoutes.threadDetail(thread.id)) }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Thread") },
            text = { Text("Are you sure you want to delete '${showDeleteDialog?.header}'? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog?.let { thread ->
                            threadViewModel.deleteThread(
                                threadId = thread.id,
                                onSuccess = {
                                    showDeleteDialog = null
                                    Toast.makeText(context, "Thread deleted", Toast.LENGTH_SHORT).show()
                                },
                                onError = { errorMsg ->
                                    showDeleteDialog = null
                                    Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
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
private fun MyThreadCard(
    thread: Thread,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val topic = communityTopics.find { it.key == thread.topic }
    val accentColor = topic?.gradient?.first() ?: AppTheme.colors.primaryGreen

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = topic?.displayName ?: "General",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                // Action Buttons
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, "Edit", tint = AppTheme.colors.primaryGreen, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = thread.header,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = thread.paragraph,
                style = MaterialTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}