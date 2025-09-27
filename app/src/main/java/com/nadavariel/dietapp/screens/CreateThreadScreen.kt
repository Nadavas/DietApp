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
    // The onThreadCreated callback is not used when calling the ViewModel directly, can be removed if desired
    onThreadCreated: (title: String, topic: String, author: String) -> Unit,
    threadViewModel: ThreadViewModel = viewModel()
) {
    var header by remember { mutableStateOf("") }
    var paragraph by remember { mutableStateOf("") }
    // MODIFIED: This state now stores the KEY for Firebase (e.g., "Training")
    var topicKey by remember { mutableStateOf("Training") }
    var type by remember { mutableStateOf("Question") }

    // MODIFIED: Use a Map to link the Firebase key to a user-friendly display name.
    val topics = mapOf(
        "Training" to "Training & Fitness",
        "Diet" to "Diet & Nutrition",
        "Recipes" to "Healthy Recipes",
        "MentalHealth" to "Mental Wellness",
        "SuccessStories" to "Success Stories",
        "GearAndTech" to "Gear & Tech"
    )
    val types = listOf("Question", "Guide", "Help", "Discussion")

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
                    .heightIn(min = 100.dp),
                maxLines = Int.MAX_VALUE,
                singleLine = false,
            )

            // Dropdown for topic
            var topicExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = topicExpanded,
                onExpandedChange = { topicExpanded = !topicExpanded }
            ) {
                OutlinedTextField(
                    // MODIFIED: Display the user-friendly name from the map
                    value = topics[topicKey] ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Topic") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = topicExpanded,
                    onDismissRequest = { topicExpanded = false }
                ) {
                    // MODIFIED: Iterate over the map to show options
                    topics.forEach { (key, displayName) ->
                        DropdownMenuItem(
                            text = { Text(displayName) }, // Show the display name
                            onClick = {
                                topicKey = key // Save the key
                                topicExpanded = false
                            }
                        )
                    }
                }
            }

            // Dropdown for type
            var typeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    types.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                type = option
                                typeExpanded = false
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
                    // MODIFIED: Pass the stored key to the view model
                    threadViewModel.createThread(header, paragraph, topicKey, type, name)
                    navController.popBackStack()
                },
                enabled = header.isNotBlank() && paragraph.isNotBlank()
            ) {
                Text("Post Thread")
            }
        }
    }
}