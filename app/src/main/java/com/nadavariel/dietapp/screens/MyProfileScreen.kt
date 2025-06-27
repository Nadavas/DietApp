package com.nadavariel.dietapp.screens

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Correct import for StateFlow in Compose
import androidx.navigation.NavController
import com.nadavariel.dietapp.AuthResult
import com.nadavariel.dietapp.AuthViewModel
import com.nadavariel.dietapp.NavRoutes
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val context = LocalContext.current // Get context for DatePickerDialog

    val currentUser = authViewModel.currentUser
    val userProfile = authViewModel.userProfile

    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showReauthDialog by remember { mutableStateOf(false) }
    var reauthPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // State to control visibility of additional details
    var showAdditionalDetails by remember { mutableStateOf(false) }

    // Date of Birth state (will be populated from userProfile.dateOfBirth)
    var dateOfBirth: Date? by remember { mutableStateOf(null) }

    // Format for displaying date of birth
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }


    val authResult by authViewModel.authResult.collectAsStateWithLifecycle()

    LaunchedEffect(authResult) {
        when (authResult) {
            AuthResult.Success -> {
                errorMessage = null
                authViewModel.signOut()
                navController.navigate(NavRoutes.LANDING) {
                    popUpTo(NavRoutes.HOME) { inclusive = true }
                    launchSingleTop = true
                }
                authViewModel.resetAuthResult()
            }
            is AuthResult.Error -> {
                val error = (authResult as AuthResult.Error).message
                if (error == "re-authenticate-required") {
                    showReauthDialog = true
                    errorMessage = "Please re-enter your password to confirm deletion."
                } else {
                    errorMessage = error
                }
            }
            AuthResult.Loading -> {
                errorMessage = null
            }
            AuthResult.Idle -> {
                errorMessage = null
            }
        }
    }

    // ⭐ NEW: LaunchedEffect to initialize dateOfBirth from userProfile
    LaunchedEffect(userProfile) {
        // Assuming userProfile.dateOfBirth is a Date or Timestamp
        // You'll need to adjust this based on how dateOfBirth is stored in UserProfile
        dateOfBirth = userProfile.dateOfBirth // Make sure userProfile has a dateOfBirth property (e.g., Date type)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
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
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // Changed to Top for better layout with hideable section
        ) {
            Text(
                text = "Profile Details",
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Essential Details
            ProfileDetailRow("Name:", userProfile.name.ifEmpty { "Not set" })
            ProfileDetailRow("Current Weight:", if (userProfile.weight > 0f) "${userProfile.weight} kg" else "Not set")
            ProfileDetailRow("Target Weight:", if (userProfile.targetWeight > 0f) "${userProfile.targetWeight} kg" else "Not set")

            Spacer(modifier = Modifier.height(16.dp))

            // Show/Hide Button for Additional Info
            Button(
                onClick = { showAdditionalDetails = !showAdditionalDetails },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showAdditionalDetails) "Hide Details" else "Show More Details")
            }

            // Animated visibility for additional details
            AnimatedVisibility(
                visible = showAdditionalDetails,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ProfileDetailRow("Email:", currentUser?.email ?: "N/A")

                    // ⭐ NEW: Date of Birth field
                    ProfileDetailRow(
                        label = "Date of Birth:",
                        value = dateOfBirth?.let { dateFormatter.format(it) } ?: "Not set"
                    )
                    // If you want to allow editing DOB directly from this screen, you'd add a button and dialog here
                    // However, the request implies it's more for display, and editing happens via "Edit Profile".
                    // If you want to enable direct editing, it'd look something like this:
                    /*
                    Button(onClick = {
                        showDatePicker(context, dateOfBirth ?: Date()) { newDate ->
                            dateOfBirth = newDate
                            // You would then need to call a function in AuthViewModel
                            // to update this date in Firestore for the user profile.
                            // authViewModel.updateUserDateOfBirth(newDate)
                        }
                    }) {
                        Text("Edit DOB")
                    }
                    */
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate(NavRoutes.UPDATE_PROFILE) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Edit Profile")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showDeleteConfirmationDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Account")
            }

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmationDialog = false
                authViewModel.resetAuthResult()
            },
            title = { Text("Confirm Account Deletion") },
            text = { Text("Are you sure you want to permanently delete your account and all associated data? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirmationDialog = false
                    errorMessage = null
                    authViewModel.deleteCurrentUser(
                        onSuccess = { /* Handled by LaunchedEffect */ },
                        onError = { /* Handled by LaunchedEffect */ }
                    )
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Re-authentication Dialog
    if (showReauthDialog) {
        AlertDialog(
            onDismissRequest = {
                showReauthDialog = false
                reauthPassword = ""
                authViewModel.resetAuthResult()
            },
            title = { Text("Re-authentication Required") },
            text = {
                Column {
                    Text(errorMessage ?: "Please re-enter your password.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reauthPassword,
                        onValueChange = { reauthPassword = it },
                        label = { Text("Password") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentEmail = currentUser?.email
                        if (currentEmail != null && reauthPassword.isNotBlank()) {
                            errorMessage = null
                            authViewModel.signIn(currentEmail, reauthPassword) {
                                showReauthDialog = false
                                reauthPassword = ""
                                authViewModel.deleteCurrentUser(
                                    onSuccess = { /* Handled by LaunchedEffect */ },
                                    onError = { /* Handled by LaunchedEffect */ }
                                )
                            }
                        } else {
                            errorMessage = "Please enter your password."
                        }
                    },
                    enabled = reauthPassword.isNotBlank()
                ) {
                    Text("Confirm & Delete")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showReauthDialog = false
                    reauthPassword = ""
                    authViewModel.resetAuthResult()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.6f)
        )
    }
}

// ⭐ NEW: Helper function to show Android's DatePickerDialog
fun showDatePicker(
    context: Context,
    initialDate: Date,
    onDateSelected: (Date) -> Unit
) {
    val calendar = Calendar.getInstance().apply { time = initialDate }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDayOfMonth ->
            val newCalendar = Calendar.getInstance().apply {
                set(selectedYear, selectedMonth, selectedDayOfMonth)
            }
            onDateSelected(newCalendar.time)
        },
        year,
        month,
        day
    )
    datePickerDialog.show()
}