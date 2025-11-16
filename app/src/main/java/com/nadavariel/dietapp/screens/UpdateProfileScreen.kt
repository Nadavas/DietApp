package com.nadavariel.dietapp.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import com.nadavariel.dietapp.NavRoutes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import com.nadavariel.dietapp.util.AvatarConstants
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import com.nadavariel.dietapp.model.Gender
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MenuAnchorType
// Imports needed for the new section
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.nadavariel.dietapp.model.Goal
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.viewmodel.GoalsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfileScreen(
    authViewModel: AuthViewModel,
    goalsViewModel: GoalsViewModel, // <-- 1. ADDED GOALS VIEW MODEL
    navController: NavController,
    onBack: () -> Unit,
    isNewUser: Boolean = false
) {
    val context = LocalContext.current

    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val goals by goalsViewModel.goals.collectAsStateWithLifecycle() // <-- 2. COLLECT GOALS STATE

    // State variables
    var nameInput by remember(userProfile.name) { mutableStateOf(userProfile.name) }
    var weightInput by remember(userProfile.startingWeight) {
        mutableStateOf(if (userProfile.startingWeight > 0f) userProfile.startingWeight.toString() else "")
    }
    var heightInput by remember(userProfile.height) {
        mutableStateOf(if (userProfile.height > 0f) userProfile.height.toString() else "")
    }
    var dateOfBirthInput: Date? by remember(userProfile.dateOfBirth) { mutableStateOf(userProfile.dateOfBirth) }
    var selectedAvatarId by remember(userProfile.avatarId) { mutableStateOf(userProfile.avatarId) }
    var selectedGender by remember(userProfile.gender) { mutableStateOf(userProfile.gender) }
    var showAvatarDialog by remember { mutableStateOf(false) }
    var isGenderDropdownExpanded by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // --- 3. UPDATED SAVE ACTION ---
    val saveProfileAction: () -> Unit = {
        // Save profile details
        authViewModel.updateProfile(
            name = nameInput,
            weight = weightInput,
            height = heightInput,
            dateOfBirth = dateOfBirthInput,
            avatarId = selectedAvatarId,
            gender = selectedGender
        )

        // Save goal adjustments
        goalsViewModel.saveUserAnswers()

        // Navigate
        if (isNewUser) {
            navController.navigate(NavRoutes.HOME) {
                popUpTo(NavRoutes.UPDATE_PROFILE_BASE) { inclusive = true }
                launchSingleTop = true
            }
        } else {
            navController.popBackStack()
        }
    }

    LaunchedEffect(userProfile, isNewUser) {
        nameInput = if (isNewUser && userProfile.name.isBlank() && authViewModel.currentUser?.email != null) {
            authViewModel.currentUser?.email?.substringBefore("@") ?: ""
        } else {
            userProfile.name
        }
        weightInput = if (userProfile.startingWeight > 0f) userProfile.startingWeight.toString() else ""
        heightInput = if (userProfile.height > 0f) userProfile.height.toString() else ""
        dateOfBirthInput = userProfile.dateOfBirth
        selectedAvatarId = userProfile.avatarId
        selectedGender = userProfile.gender
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewUser) "Create Profile" else "Edit Profile") }, // <-- Title changed
                navigationIcon = {
                    IconButton(onClick = {
                        if (isNewUser) {
                            navController.navigate(NavRoutes.HOME) {
                                popUpTo(NavRoutes.UPDATE_PROFILE_BASE) { inclusive = true }
                                launchSingleTop = true
                            }
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = if (isNewUser) "Tell us about yourself!" else "", // <-- Title changed
                fontSize = 28.sp,
                modifier = if (isNewUser) Modifier.padding(vertical = 24.dp) else Modifier.padding(vertical = 0.dp)
            )

            Image(
                painter = painterResource(id = AvatarConstants.getAvatarResId(selectedAvatarId)),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { showAvatarDialog = true }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showAvatarDialog = true }) {
                Text("Change Avatar")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Profile Fields ---
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = weightInput,
                onValueChange = { newValue ->
                    if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        weightInput = newValue
                    }
                },
                label = { Text("Starting Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = heightInput,
                onValueChange = { newValue ->
                    if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        heightInput = newValue
                    }
                },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = dateOfBirthInput?.let { dateFormatter.format(it) } ?: "",
                onValueChange = { /* Read-only */ },
                label = { Text("Date of Birth") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable {
                        val initialCalendar = Calendar.getInstance().apply {
                            time = dateOfBirthInput ?: Date()
                        }
                        val year = initialCalendar.get(Calendar.YEAR)
                        val month = initialCalendar.get(Calendar.MONTH)
                        val day = initialCalendar.get(Calendar.DAY_OF_MONTH)

                        DatePickerDialog(
                            context,
                            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
                                val newDate = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth, selectedDayOfMonth)
                                }.time
                                dateOfBirthInput = newDate
                            },
                            year,
                            month,
                            day
                        ).show()
                    },
                trailingIcon = {
                    IconButton(onClick = {
                        val initialCalendar = Calendar.getInstance().apply {
                            time = dateOfBirthInput ?: Date()
                        }
                        val year = initialCalendar.get(Calendar.YEAR)
                        val month = initialCalendar.get(Calendar.MONTH)
                        val day = initialCalendar.get(Calendar.DAY_OF_MONTH)

                        DatePickerDialog(
                            context,
                            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
                                val newDate = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth, selectedDayOfMonth)
                                }.time
                                dateOfBirthInput = newDate
                            },
                            year,
                            month,
                            day
                        ).show()
                    }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date of Birth")
                    }
                },
                singleLine = true
            )

            ExposedDropdownMenuBox(
                expanded = isGenderDropdownExpanded,
                onExpandedChange = { isGenderDropdownExpanded = !isGenderDropdownExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = selectedGender.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenderDropdownExpanded) },
                    modifier = Modifier
                        .exposedDropdownSize()
                        .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isGenderDropdownExpanded,
                    onDismissRequest = { isGenderDropdownExpanded = false }
                ) {
                    Gender.entries.forEach { genderOption ->
                        DropdownMenuItem(
                            text = { Text(genderOption.displayName) },
                            onClick = {
                                selectedGender = genderOption
                                isGenderDropdownExpanded = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // --- 4. ADDED CUSTOM ADJUSTMENTS SECTION ---
            if (goals.isNotEmpty()) {
                Divider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = AppTheme.colors.TextSecondary.copy(alpha = 0.2f)
                )

                Text(
                    "Custom Adjustments",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.TextPrimary
                )

                Text(
                    "Fine-tune your targets manually",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppTheme.colors.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    goals.forEach { goal ->
                        EditableGoalCard(
                            goal = goal,
                            onValueChange = { newValue ->
                                goalsViewModel.updateAnswer(goal.id, newValue)
                            }
                        )
                    }
                }
            }
            // --- END OF ADDED SECTION ---

            Spacer(modifier = Modifier.height(32.dp)) // Increased spacing

            Button(
                onClick = { saveProfileAction() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isNewUser) "Create Profile" else "Save All Changes") // <-- Text updated
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showAvatarDialog) {
        Dialog(onDismissRequest = { showAvatarDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select an Avatar",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(AvatarConstants.AVATAR_DRAWABLES) { (avatarId, drawableResId) ->
                            Image(
                                painter = painterResource(id = drawableResId),
                                contentDescription = "Avatar $avatarId",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedAvatarId = avatarId
                                        showAvatarDialog = false
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { showAvatarDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

// --- 5. PASTED EDITABLEGOALCARD COMPOSABLE HERE ---
@Composable
fun EditableGoalCard(
    goal: Goal,
    onValueChange: (String) -> Unit
) {
    var textValue by remember(goal.value) { mutableStateOf(goal.value ?: "") }

    val isCalorieGoal = goal.text.contains("calorie", ignoreCase = true)
    val isProteinGoal = goal.text.contains("protein", ignoreCase = true)
    val isWeightGoal = goal.text.contains("weight", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = goal.text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.TextPrimary
            )

            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    textValue = newValue
                    onValueChange(newValue)
                },
                label = {
                    Text(when {
                        isCalorieGoal -> "Target (kcal)"
                        isProteinGoal -> "Target (g)"
                        isWeightGoal -> "Target (kg)"
                        else -> "Enter your goal"
                    })
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.PrimaryGreen,
                    focusedLabelColor = AppTheme.colors.PrimaryGreen
                ),
                trailingIcon = {
                    if (textValue.isNotBlank()) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = AppTheme.colors.PrimaryGreen
                        )
                    }
                }
            )
        }
    }
}