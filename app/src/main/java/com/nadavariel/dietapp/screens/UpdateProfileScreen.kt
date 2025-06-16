package com.nadavariel.dietapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nadavariel.dietapp.AuthViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.LaunchedEffect
// import androidx.compose.runtime.collectAsState // ⭐ REMOVED: No longer needed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateProfileScreen(
    authViewModel: AuthViewModel,
    navController: NavController,
    onBack: () -> Unit
) {
    // ⭐ CHANGED: Directly access userProfile from AuthViewModel, no collectAsState()
    val userProfile = authViewModel.userProfile

    // Initialize input fields with current profile data
    var nameInput by remember { mutableStateOf(userProfile.name) }
    var weightInput by remember { mutableStateOf(userProfile.weight.toString()) }
    var ageInput by remember { mutableStateOf(userProfile.age.toString()) }
    var targetWeightInput by remember { mutableStateOf(userProfile.targetWeight.toString()) }

    // Use LaunchedEffect to update input fields if userProfile changes externally
    // This part is still good to keep, as the underlying AuthViewModel.userProfile can change
    // even though it's no longer a Flow.
    LaunchedEffect(userProfile) {
        nameInput = userProfile.name
        weightInput = if (userProfile.weight > 0f) userProfile.weight.toString() else ""
        ageInput = if (userProfile.age > 0) userProfile.age.toString() else ""
        targetWeightInput = if (userProfile.targetWeight > 0f) userProfile.targetWeight.toString() else ""
    }

    val saveProfileAction: () -> Unit = {
        // Pass all fields to AuthViewModel's updateProfile
        authViewModel.updateProfile(nameInput, weightInput, ageInput, targetWeightInput) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                text = "Update Your Profile",
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            OutlinedTextField(
                value = weightInput,
                onValueChange = { newValue ->
                    // Allow only digits and one decimal point
                    if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        weightInput = newValue
                    }
                },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = ageInput,
                onValueChange = { newValue ->
                    // Allow only digits
                    if (newValue.all { it.isDigit() }) {
                        ageInput = newValue
                    }
                },
                label = { Text("Age") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = targetWeightInput,
                onValueChange = { newValue ->
                    // Allow only digits and one decimal point
                    if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        targetWeightInput = newValue
                    }
                },
                label = { Text("Target Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                singleLine = true,
                keyboardActions = KeyboardActions(
                    onDone = { saveProfileAction() }
                )
            )

            Button(
                onClick = { saveProfileAction() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Profile")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back")
            }
        }
    }
}