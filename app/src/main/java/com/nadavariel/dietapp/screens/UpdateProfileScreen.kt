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
import androidx.lifecycle.compose.collectAsStateWithLifecycle // ⭐ NEW: Import collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfileScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    onBack: () -> Unit,
    isNewUser: Boolean = false
) {
    val context = LocalContext.current
    // ⭐ MODIFIED: Collect userProfile as a state
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()

    // Initialize states from the collected userProfile value
    var nameInput by remember(userProfile.name) { mutableStateOf(userProfile.name) }
    var weightInput by remember(userProfile.weight) { mutableStateOf(if (userProfile.weight > 0f) userProfile.weight.toString() else "") }
    var dateOfBirthInput: Date? by remember(userProfile.dateOfBirth) { mutableStateOf(userProfile.dateOfBirth) }
    var targetWeightInput by remember(userProfile.targetWeight) { mutableStateOf(if (userProfile.targetWeight > 0f) userProfile.targetWeight.toString() else "") }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    LaunchedEffect(userProfile, isNewUser) {
        // Only set name for new user if it's currently blank and email is available
        nameInput = if (isNewUser && userProfile.name.isBlank() && authViewModel.currentUser?.email != null) {
            authViewModel.currentUser?.email?.substringBefore("@") ?: ""
        } else {
            userProfile.name
        }
        // These are already handled by remember(userProfile.property) above, but kept
        // for explicit clarity if additional logic was needed.
        // weightInput = if (userProfile.weight > 0f) userProfile.weight.toString() else ""
        // dateOfBirthInput = userProfile.dateOfBirth
        // targetWeightInput = if (userProfile.targetWeight > 0f) userProfile.targetWeight.toString() else ""
    }

    val saveProfileAction: () -> Unit = {
        authViewModel.updateProfile(nameInput, weightInput, dateOfBirthInput, targetWeightInput) {
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
                    .clickable { // Make the whole field clickable to open date picker
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
                trailingIcon = { // Add a calendar icon
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
}