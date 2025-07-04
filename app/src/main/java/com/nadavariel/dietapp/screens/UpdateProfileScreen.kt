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
import com.nadavariel.dietapp.AuthViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
import com.nadavariel.dietapp.NavRoutes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image // ⭐ NEW: Import Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape // ⭐ NEW: Import CircleShape
import androidx.compose.ui.draw.clip // ⭐ NEW: Import clip
import androidx.compose.ui.layout.ContentScale // ⭐ NEW: Import ContentScale
import androidx.compose.ui.res.painterResource // ⭐ NEW: Import painterResource
import androidx.compose.ui.window.Dialog // ⭐ NEW: Import Dialog
import androidx.compose.foundation.lazy.grid.GridCells // ⭐ NEW: For LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid // ⭐ NEW: For LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // ⭐ NEW: For LazyVerticalGrid items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface // ⭐ NEW: For Dialog content background
import androidx.compose.material3.TextButton // ⭐ NEW: For dialog buttons
import com.nadavariel.dietapp.util.AvatarConstants // ⭐ NEW: Import AvatarConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfileScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    onBack: () -> Unit,
    isNewUser: Boolean = false
) {
    val context = LocalContext.current
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()

    var nameInput by remember(userProfile.name) { mutableStateOf(userProfile.name) }
    var weightInput by remember(userProfile.weight) { mutableStateOf(if (userProfile.weight > 0f) userProfile.weight.toString() else "") }
    var dateOfBirthInput: Date? by remember(userProfile.dateOfBirth) { mutableStateOf(userProfile.dateOfBirth) }
    var targetWeightInput by remember(userProfile.targetWeight) { mutableStateOf(if (userProfile.targetWeight > 0f) userProfile.targetWeight.toString() else "") }
    // ⭐ NEW: State for selected avatar ID, initialized from userProfile
    var selectedAvatarId by remember(userProfile.avatarId) { mutableStateOf(userProfile.avatarId) }

    // ⭐ NEW: State to control avatar selection dialog visibility
    var showAvatarDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    LaunchedEffect(userProfile, isNewUser) {
        nameInput = if (isNewUser && userProfile.name.isBlank() && authViewModel.currentUser?.email != null) {
            authViewModel.currentUser?.email?.substringBefore("@") ?: ""
        } else {
            userProfile.name
        }
        // Ensure avatarId is also updated if userProfile changes from external source
        selectedAvatarId = userProfile.avatarId
    }

    val saveProfileAction: () -> Unit = {
        // ⭐ MODIFIED: Pass the selectedAvatarId to updateProfile
        authViewModel.updateProfile(nameInput, weightInput, dateOfBirthInput, targetWeightInput, selectedAvatarId) {
            if (isNewUser) {
                navController.navigate(NavRoutes.HOME) {
                    popUpTo(NavRoutes.UPDATE_PROFILE_BASE) { inclusive = true }
                    launchSingleTop = true
                }
            } else {
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewUser) "Create Profile" else "Update Profile") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isNewUser) "Tell us about yourself!" else "Update Your Profile",
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // ⭐ NEW: Avatar Display and Selection Button
            Image(
                painter = painterResource(id = AvatarConstants.getAvatarResId(selectedAvatarId)),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { showAvatarDialog = true } // Open dialog on click
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showAvatarDialog = true }) {
                Text("Change Avatar")
            }
            Spacer(modifier = Modifier.height(24.dp))
            // ⭐ END NEW: Avatar Display and Selection Button

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
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = dateOfBirthInput?.let { dateFormatter.format(it) } ?: "",
                onValueChange = { /* Read-only, no direct text input */ },
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

            OutlinedTextField(
                value = targetWeightInput,
                onValueChange = { newValue ->
                    if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        targetWeightInput = newValue
                    }
                },
                label = { Text("Target Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                singleLine = true
            )

            Button(
                onClick = { saveProfileAction() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isNewUser) "Create Profile" else "Save Changes")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isNewUser) {
                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back")
                }
            }
        }
    }

    // ⭐ NEW: Avatar Selection Dialog
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
                        columns = GridCells.Fixed(4), // 4 avatars per row
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp), // Limit height to make it scrollable if many avatars
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
                                    .size(64.dp) // Size of each avatar in the grid
                                    .clip(CircleShape)
                                    .clickable {
                                        selectedAvatarId = avatarId
                                        showAvatarDialog = false // Close dialog after selection
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
    // ⭐ END NEW: Avatar Selection Dialog
}
