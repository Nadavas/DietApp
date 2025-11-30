package com.nadavariel.dietapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.nadavariel.dietapp.model.Topic
import com.nadavariel.dietapp.model.communityTopics
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.viewmodel.ThreadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateThreadScreen(
    navController: NavController,
    threadViewModel: ThreadViewModel = viewModel(),
    threadIdToEdit: String? = null
) {
    val topics = communityTopics
    val types = listOf("Question", "Guide", "Help", "Discussion")

    // --- State Management ---
    var header by remember { mutableStateOf("") }
    var paragraph by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf(topics.first()) }
    var type by remember { mutableStateOf("Question") }

    // Logic to distinguish between Edit Mode and Create Mode
    val isEditMode = threadIdToEdit != null
    // Loading state: Only true initially if we are in Edit Mode
    var isLoading by remember { mutableStateOf(isEditMode) }

    val selectedThread by threadViewModel.selectedThread.collectAsState()

    // 1. Initial Setup: Fetch data or clear previous state
    LaunchedEffect(threadIdToEdit) {
        if (threadIdToEdit != null) {
            // Edit Mode: Fetch specific thread
            threadViewModel.fetchThreadById(threadIdToEdit)
        } else {
            // Create Mode: Clear previous selection so fields are empty
            threadViewModel.clearSelectedThreadAndListeners()
        }
    }

    // 2. Populate fields when data arrives (Edit Mode only)
    LaunchedEffect(selectedThread) {
        if (isEditMode && selectedThread != null && selectedThread!!.id == threadIdToEdit) {
            header = selectedThread!!.header
            paragraph = selectedThread!!.paragraph
            type = selectedThread!!.type

            // Find the matching topic object
            val matchingTopic = topics.find { it.key == selectedThread!!.topic }
            if (matchingTopic != null) {
                selectedTopic = matchingTopic
            }
            // Data loaded, stop loading spinner
            isLoading = false
        }
    }

    val isFormValid = header.isNotBlank() && paragraph.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.screenBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- Header Section ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.offset(x = (-12).dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTheme.colors.textPrimary
                        )
                    }

                    Text(
                        text = if (isEditMode) "Edit Thread" else "Create Thread",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary
                    )
                    Text(
                        text = if (isEditMode) "Update your discussion details" else "Start a new discussion with the community",
                        fontSize = 14.sp,
                        color = AppTheme.colors.textSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // --- Main Content ---
            if (isLoading) {
                // Show loading spinner while fetching edit data
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppTheme.colors.primaryGreen)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {

                                // 1. Topic Selection
                                TopicDropdown(
                                    items = topics,
                                    selectedItem = selectedTopic,
                                    onItemSelected = { selectedTopic = it }
                                )

                                // 2. Type Selection
                                TypeDropdown(
                                    items = types,
                                    selectedItem = type,
                                    onItemSelected = { type = it }
                                )

                                HorizontalDivider(color = AppTheme.colors.textSecondary.copy(alpha = 0.1f))

                                // 3. Header Input
                                OutlinedTextField(
                                    value = header,
                                    onValueChange = { header = it },
                                    label = { Text("Thread Title") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = { Icon(Icons.Default.Title, null, tint = AppTheme.colors.primaryGreen) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppTheme.colors.primaryGreen,
                                        unfocusedBorderColor = AppTheme.colors.textSecondary.copy(alpha = 0.3f),
                                        focusedLabelColor = AppTheme.colors.primaryGreen
                                    ),
                                    singleLine = true
                                )

                                // 4. Content Input
                                OutlinedTextField(
                                    value = paragraph,
                                    onValueChange = { paragraph = it },
                                    label = { Text("What's on your mind?") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    leadingIcon = {
                                        Column(Modifier.padding(top = 12.dp)) {
                                            Icon(Icons.Default.Subject, null, tint = AppTheme.colors.primaryGreen)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppTheme.colors.primaryGreen,
                                        unfocusedBorderColor = AppTheme.colors.textSecondary.copy(alpha = 0.3f),
                                        focusedLabelColor = AppTheme.colors.primaryGreen
                                    ),
                                    textStyle = LocalTextStyle.current.copy(lineHeight = 24.sp)
                                )
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                if (isFormValid) {
                                    if (isEditMode && threadIdToEdit != null) {
                                        // UPDATE EXISTING THREAD
                                        threadViewModel.updateThread(
                                            threadId = threadIdToEdit,
                                            newHeader = header,
                                            newContent = paragraph,
                                            onSuccess = { navController.popBackStack() }
                                        )
                                    } else {
                                        // CREATE NEW THREAD
                                        val user = FirebaseAuth.getInstance().currentUser
                                        val name = user?.displayName?.takeIf { it.isNotBlank() }
                                            ?: user?.email?.substringBefore("@")
                                            ?: "Anonymous"
                                        threadViewModel.createThread(
                                            header,
                                            paragraph,
                                            selectedTopic.key,
                                            type,
                                            name
                                        )
                                        navController.popBackStack()
                                    }
                                }
                            },
                            enabled = isFormValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppTheme.colors.primaryGreen,
                                contentColor = Color.White,
                                disabledContainerColor = AppTheme.colors.textSecondary.copy(alpha = 0.2f)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(
                                if (isEditMode) Icons.Default.Edit else Icons.Default.PostAdd,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                if (isEditMode) "Update Thread" else "Post Thread",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        // Spacer for bottom navigation visibility
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDropdown(
    items: List<Topic>,
    selectedItem: Topic,
    onItemSelected: (Topic) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedItem.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Topic") },
            leadingIcon = {
                Icon(
                    selectedItem.icon,
                    contentDescription = null,
                    tint = AppTheme.colors.accentTeal
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.accentTeal,
                unfocusedBorderColor = AppTheme.colors.textSecondary.copy(alpha = 0.3f),
                focusedLabelColor = AppTheme.colors.accentTeal
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            items.forEach { topic ->
                DropdownMenuItem(
                    text = {
                        Text(
                            topic.displayName,
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            topic.icon,
                            contentDescription = null,
                            tint = AppTheme.colors.accentTeal
                        )
                    },
                    onClick = {
                        onItemSelected(topic)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeDropdown(
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedItem,
            onValueChange = {},
            readOnly = true,
            label = { Text("Thread Type") },
            leadingIcon = {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = AppTheme.colors.warmOrange
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.warmOrange,
                unfocusedBorderColor = AppTheme.colors.textSecondary.copy(alpha = 0.3f),
                focusedLabelColor = AppTheme.colors.warmOrange
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            items.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = AppTheme.colors.textPrimary) },
                    onClick = {
                        onItemSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}