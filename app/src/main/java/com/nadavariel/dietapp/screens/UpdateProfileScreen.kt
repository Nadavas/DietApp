package com.nadavariel.dietapp.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.model.Gender
import com.nadavariel.dietapp.model.Goal
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.util.AvatarConstants
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfileScreen(
    authViewModel: AuthViewModel,
    goalsViewModel: GoalsViewModel,
    navController: NavController,
    onBack: () -> Unit,
    isNewUser: Boolean = false
) {
    val context = LocalContext.current
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val goals by goalsViewModel.goals.collectAsStateWithLifecycle()

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

    val saveProfileAction: () -> Unit = {
        authViewModel.updateProfile(
            name = nameInput,
            weight = weightInput,
            height = heightInput,
            dateOfBirth = dateOfBirthInput,
            avatarId = selectedAvatarId,
            gender = selectedGender
        )
        goalsViewModel.saveUserAnswers()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(AppTheme.colors.statsGradient))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Modern Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isNewUser) "Create Your Profile" else "Edit Profile",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = if (isNewUser) "Let's get to know you!" else "Update your details",
                            fontSize = 13.sp,
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar Selection Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Your Avatar",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )

                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(AppTheme.colors.primaryGreen.copy(alpha = 0.1f))
                                .clickable { showAvatarDialog = true }
                        ) {
                            Image(
                                painter = painterResource(
                                    id = AvatarConstants.getAvatarResId(selectedAvatarId)
                                ),
                                contentDescription = "User Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.primaryGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showAvatarDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = AppTheme.colors.primaryGreen
                            )
                        ) {
                            Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Change Avatar", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Personal Information Card
                ProfileEditCard(
                    title = "Personal Information",
                    icon = Icons.Default.Person,
                    iconColor = AppTheme.colors.softBlue
                ) {
                    ModernOutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = "Name",
                        icon = Icons.Default.Badge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    ModernOutlinedTextField(
                        value = dateOfBirthInput?.let { dateFormatter.format(it) } ?: "",
                        onValueChange = { },
                        label = "Date of Birth",
                        icon = Icons.Default.Cake,
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                val initialCalendar = Calendar.getInstance().apply {
                                    time = dateOfBirthInput ?: Date()
                                }
                                DatePickerDialog(
                                    context,
                                    { _: DatePicker, year: Int, month: Int, day: Int ->
                                        dateOfBirthInput = Calendar.getInstance().apply {
                                            set(year, month, day)
                                        }.time
                                    },
                                    initialCalendar.get(Calendar.YEAR),
                                    initialCalendar.get(Calendar.MONTH),
                                    initialCalendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Default.DateRange, "Select Date")
                            }
                        }
                    )

                    ExposedDropdownMenuBox(
                        expanded = isGenderDropdownExpanded,
                        onExpandedChange = { isGenderDropdownExpanded = !isGenderDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ModernOutlinedTextField(
                            value = selectedGender.displayName,
                            onValueChange = {},
                            label = "Gender",
                            icon = Icons.Default.Wc,
                            readOnly = true,
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenderDropdownExpanded)
                            }
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
                                    }
                                )
                            }
                        }
                    }
                }

                // Physical Stats Card
                ProfileEditCard(
                    title = "Physical Stats",
                    icon = Icons.Default.FitnessCenter,
                    iconColor = AppTheme.colors.statsGreen
                ) {
                    ModernOutlinedTextField(
                        value = weightInput,
                        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) weightInput = it },
                        label = "Starting Weight (kg)",
                        icon = Icons.Default.MonitorWeight,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )

                    ModernOutlinedTextField(
                        value = heightInput,
                        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d*$"))) heightInput = it },
                        label = "Height (cm)",
                        icon = Icons.Default.Height,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        )
                    )
                }

                // Goals Card
                if (goals.isNotEmpty()) {
                    ProfileEditCard(
                        title = "Your Targets",
                        icon = Icons.Default.TrackChanges,
                        iconColor = AppTheme.colors.accentTeal
                    ) {
                        Text(
                            text = "Fine-tune your nutrition targets",
                            fontSize = 13.sp,
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        goals.forEach { goal ->
                            EditableGoalField(
                                goal = goal,
                                onValueChange = { newValue ->
                                    goalsViewModel.updateAnswer(goal.id, newValue)
                                }
                            )
                        }
                    }
                }

                // Save Button
                Button(
                    onClick = saveProfileAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.primaryGreen
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isNewUser) "Create Profile" else "Save All Changes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAvatarDialog) {
        Dialog(onDismissRequest = { showAvatarDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Choose Your Avatar",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(AvatarConstants.AVATAR_DRAWABLES) { (avatarId, drawableResId) ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (avatarId == selectedAvatarId)
                                            AppTheme.colors.primaryGreen.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        selectedAvatarId = avatarId
                                        showAvatarDialog = false
                                    }
                            ) {
                                Image(
                                    painter = painterResource(id = drawableResId),
                                    contentDescription = "Avatar $avatarId",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(if (avatarId == selectedAvatarId) 4.dp else 0.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    TextButton(
                        onClick = { showAvatarDialog = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = AppTheme.colors.textSecondary
                        )
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileEditCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
            }

            content()
        }
    }
}

@Composable
private fun ModernOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTheme.colors.textSecondary
            )
        },
        trailingIcon = trailingIcon,
        readOnly = readOnly,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppTheme.colors.primaryGreen,
            focusedLabelColor = AppTheme.colors.primaryGreen,
            focusedLeadingIconColor = AppTheme.colors.primaryGreen
        ),
        keyboardOptions = keyboardOptions,
        singleLine = true
    )
}

@Composable
private fun EditableGoalField(
    goal: Goal,
    onValueChange: (String) -> Unit
) {
    var textValue by remember(goal.value) { mutableStateOf(goal.value ?: "") }

    val isCalorieGoal = goal.text.contains("calorie", ignoreCase = true)
    val isProteinGoal = goal.text.contains("protein", ignoreCase = true)
    val isWeightGoal = goal.text.contains("weight", ignoreCase = true)

    val goalIcon = when {
        isCalorieGoal -> Icons.Default.LocalFireDepartment
        isProteinGoal -> Icons.Default.Restaurant
        isWeightGoal -> Icons.Default.FlagCircle
        else -> Icons.Default.Flag
    }

    val goalColor = when {
        isCalorieGoal -> AppTheme.colors.warmOrange
        isProteinGoal -> AppTheme.colors.statsGreen
        isWeightGoal -> AppTheme.colors.accentTeal
        else -> AppTheme.colors.primaryGreen
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = goalIcon,
                contentDescription = null,
                tint = goalColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = goal.text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary
            )
        }

        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                textValue = newValue
                onValueChange(newValue)
            },
            label = {
                Text(
                    when {
                        isCalorieGoal -> "Target (kcal)"
                        isProteinGoal -> "Target (g)"
                        isWeightGoal -> "Target (kg)"
                        else -> "Enter your goal"
                    }
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = goalColor,
                focusedLabelColor = goalColor
            ),
            trailingIcon = {
                if (textValue.isNotBlank()) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = goalColor
                    )
                }
            }
        )
    }
}