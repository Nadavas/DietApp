package com.nadavariel.dietapp.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.nadavariel.dietapp.viewmodel.ThreadViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateThreadScreen(
    navController: NavController,
    onThreadCreated: (title: String, topic: String, author: String) -> Unit,
    threadViewModel: ThreadViewModel = viewModel()
) {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val authorName = currentUser?.displayName ?: currentUser?.email ?: "Anonymous"

    var title by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf("") }
    val topics = listOf("Training", "Diet", "Recipes")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Thread") },
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Thread Title") },
                modifier = Modifier.fillMaxWidth()
            )

            // Topic Dropdown
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedTopic,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Topic") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    topics.forEach { topic ->
                        DropdownMenuItem(
                            text = { Text(topic) },
                            onClick = {
                                selectedTopic = topic
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (title.isNotBlank() && selectedTopic.isNotBlank()) {
                        threadViewModel.createThread(title, selectedTopic, authorName)
                        navController.popBackStack()
                    }
                },
                enabled = title.isNotBlank() && selectedTopic.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Post Thread")
            }
        }
    }
}
