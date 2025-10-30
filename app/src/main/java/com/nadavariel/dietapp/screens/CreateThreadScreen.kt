package com.nadavariel.dietapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.nadavariel.dietapp.model.Topic
import com.nadavariel.dietapp.model.communityTopics
import com.nadavariel.dietapp.viewmodel.ThreadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateThreadScreen(
    navController: NavController,
    threadViewModel: ThreadViewModel = viewModel()
) {
    var header by remember { mutableStateOf("") }
    var paragraph by remember { mutableStateOf("") }
    val topics = communityTopics
    var selectedTopic by remember { mutableStateOf(topics.first()) }
    var type by remember { mutableStateOf("Question") }
    val types = listOf("Question", "Guide", "Help", "Discussion")
    val isFormValid = header.isNotBlank() && paragraph.isNotBlank()

    // Create the fixed gradient using the "Diet & Nutrition" colors
    val dietAndNutritionGradient = Brush.verticalGradient(
        listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
    )
    val startColor = Color(0xFF6A11CB)
    val endColor = Color(0xFF2575FC)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Thread", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        // The main container with the fixed gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dietAndNutritionGradient)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                val glassTextFieldColors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.15f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedLabelColor = Color.White.copy(alpha = 0.9f),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )

                TextField(
                    value = header,
                    onValueChange = { header = it },
                    label = { Text("Thread Header") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = glassTextFieldColors,
                    singleLine = true
                )

                TextField(
                    value = paragraph,
                    onValueChange = { paragraph = it },
                    label = { Text("What's on your mind?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = glassTextFieldColors
                )

                TopicDropdown(
                    items = topics,
                    selectedItem = selectedTopic,
                    onItemSelected = { selectedTopic = it },
                    colors = glassTextFieldColors,
                    menuBackgroundColor = startColor
                )

                TypeDropdown(
                    items = types,
                    selectedItem = type,
                    onItemSelected = { type = it },
                    colors = glassTextFieldColors
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (isFormValid) {
                            val user = FirebaseAuth.getInstance().currentUser
                            val name = user?.displayName?.takeIf { it.isNotBlank() }
                                ?: user?.email?.substringBefore("@")
                                ?: "Anonymous"
                            threadViewModel.createThread(header, paragraph, selectedTopic.key, type, name)
                            navController.popBackStack()
                        }
                    },
                    enabled = isFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.9f),
                        contentColor = endColor, // Use the blue from the gradient
                        disabledContainerColor = Color.White.copy(alpha = 0.3f),
                        disabledContentColor = Color.White.copy(alpha = 0.6f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = "Post", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Post Thread", fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.titleMedium.fontSize)
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
    onItemSelected: (Topic) -> Unit,
    colors: TextFieldColors,
    menuBackgroundColor: Color
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedItem.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Topic") },
            leadingIcon = {
                Icon(
                    selectedItem.icon,
                    contentDescription = null,
                    tint = Color.White
                )
            },
            trailingIcon = {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = colors
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(menuBackgroundColor.copy(alpha = 0.95f))
        ) {
            items.forEach { topic ->
                DropdownMenuItem(
                    text = { Text(topic.displayName, color = Color.White, fontWeight = FontWeight.Medium) },
                    leadingIcon = { Icon(topic.icon, contentDescription = null, tint = Color.White) },
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
    onItemSelected: (String) -> Unit,
    colors: TextFieldColors
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedItem,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Type") },
            trailingIcon = {
                CompositionLocalProvider(LocalContentColor provides Color.White) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = colors
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onItemSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}