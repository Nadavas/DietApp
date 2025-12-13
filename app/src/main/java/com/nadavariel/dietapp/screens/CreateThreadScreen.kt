package com.nadavariel.dietapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PostAdd
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
import com.nadavariel.dietapp.model.communityTopics
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.AppTopBar
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
    val isEditMode = threadIdToEdit != null


    var header by remember { mutableStateOf("") }
    var paragraph by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf(topics.first()) }
    var type by remember { mutableStateOf("Question") }
    var isLoading by remember { mutableStateOf(isEditMode) }

    val selectedThread by threadViewModel.selectedThread.collectAsState()

    // Initial Setup: Fetch data or clear previous state
    LaunchedEffect(threadIdToEdit) {
        if (threadIdToEdit != null) {
            // Edit Mode: Fetch specific thread
            threadViewModel.fetchThreadById(threadIdToEdit)
        } else {
            // Create Mode: Clear previous selection so fields are empty
            threadViewModel.clearSelectedThreadAndListeners()
        }
    }

    // Populate fields when data arrives (Edit Mode only)
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
            AppTopBar(
                title = if (isEditMode) "Edit Thread" else "Create Thread",
                onBack = { navController.popBackStack() },
                containerColor = Color.White
            )

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

                                // Topic Selection
                                AppDropdown(
                                    items = topics,
                                    selectedItem = selectedTopic,
                                    onItemSelected = { selectedTopic = it },
                                    label = "Select Topic",
                                    accentColor = AppTheme.colors.accentTeal,
                                    itemLabelMapper = { it.displayName },
                                    leadingIcon = { topic ->
                                        Icon(topic.icon, contentDescription = null, tint = AppTheme.colors.accentTeal)
                                    },
                                    itemLeadingIcon = { topic ->
                                        Icon(topic.icon, contentDescription = null, tint = AppTheme.colors.accentTeal)
                                    }
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Type Selection
                                AppDropdown(
                                    items = types,
                                    selectedItem = type,
                                    onItemSelected = { type = it },
                                    label = "Thread Type",
                                    accentColor = AppTheme.colors.warmOrange,
                                    itemLabelMapper = { it },
                                    leadingIcon = {
                                        Icon(Icons.Default.Category, contentDescription = null, tint = AppTheme.colors.warmOrange)
                                    },
                                    itemLeadingIcon = null
                                )

                                HorizontalDivider(color = AppTheme.colors.textSecondary.copy(alpha = 0.1f))

                                // Header Input
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

                                // Content Input
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
                                            Icon(Icons.AutoMirrored.Filled.Subject, null, tint = AppTheme.colors.primaryGreen)
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
                                    if (isEditMode) {
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
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AppDropdown(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    label: String,
    accentColor: Color,
    itemLabelMapper: (T) -> String,
    leadingIcon: @Composable (T) -> Unit,
    itemLeadingIcon: (@Composable (T) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = itemLabelMapper(selectedItem),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { leadingIcon(selectedItem) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = AppTheme.colors.textSecondary.copy(alpha = 0.3f),
                focusedLabelColor = accentColor
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = itemLabelMapper(item),
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingIcon = if (itemLeadingIcon != null) {
                        { itemLeadingIcon(item) }
                    } else null,
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    }
                )
            }
        }
    }
}