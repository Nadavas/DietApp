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
    var header by remember { mutableStateOf("") }
    var paragraph by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("Training") }

    val topics = listOf("Training", "Diet", "Recipes")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Thread") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = header,
                onValueChange = { header = it },
                label = { Text("Thread Header") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = paragraph,
                onValueChange = { paragraph = it },
                label = { Text("Thread Paragraph") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp), // ensures the box has some initial height
                maxLines = Int.MAX_VALUE,     // allows multiple lines
                singleLine = false,           // ensures it's not forced into a single line
            )

            // Dropdown for topic
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = topic,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Topic") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    topics.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                topic = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val user = FirebaseAuth.getInstance().currentUser
                    val name = user?.displayName ?: user?.email ?: "Anonymous"
                    threadViewModel.createThread(header, paragraph, topic, name)
                    navController.popBackStack()
                },
                enabled = header.isNotBlank() && paragraph.isNotBlank()
            ) {
                Text("Post Thread")
            }
        }
    }
}
