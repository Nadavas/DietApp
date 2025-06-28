package com.nadavariel.dietapp.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card // Import Card
import androidx.compose.material3.CardDefaults // Import CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nadavariel.dietapp.AuthViewModel
import com.nadavariel.dietapp.NavRoutes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    authViewModel: AuthViewModel,
    navController: NavController
) {
    val currentUser = authViewModel.currentUser
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val hasMissingProfileDetails by authViewModel.hasMissingPrimaryProfileDetails.collectAsStateWithLifecycle()

    var showAdditionalDetails by remember { mutableStateOf(false) }

    val dateOfBirth: Date? by remember(userProfile.dateOfBirth) { mutableStateOf(userProfile.dateOfBirth) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

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
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Profile Details",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            AnimatedVisibility(
                visible = hasMissingProfileDetails,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                val missingFields = mutableListOf<String>()
                if (userProfile.name.isBlank()) {
                    missingFields.add("name")
                }
                if (userProfile.weight <= 0f) {
                    missingFields.add("current weight")
                }
                if (userProfile.targetWeight <= 0f) {
                    missingFields.add("target weight")
                }

                val message = if (missingFields.isNotEmpty()) {
                    val formattedFields = when (missingFields.size) {
                        1 -> missingFields[0]
                        2 -> "${missingFields[0]} and ${missingFields[1]}"
                        else -> { // Handles 3 or more fields without duplicating the last one
                            val allButLast = missingFields.dropLast(1).joinToString(", ")
                            "$allButLast, and ${missingFields.last()}"
                        }
                    }
                    "Your essential profile details for $formattedFields appear to be incomplete. Please update your profile to ensure accuracy."
                } else {
                    "Some essential profile details appear to be incomplete. Please update your profile to ensure accuracy."
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ProfileDetailRow("Name:", userProfile.name.ifEmpty { "" })
            ProfileDetailRow("Current Weight:", if (userProfile.weight > 0f) "${userProfile.weight} kg" else "")
            ProfileDetailRow("Target Weight:", if (userProfile.targetWeight > 0f) "${userProfile.targetWeight} kg" else "")

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { showAdditionalDetails = !showAdditionalDetails },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showAdditionalDetails) "Hide Details" else "Show More Details")
            }

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

                    ProfileDetailRow(
                        label = "Date of Birth:",
                        value = dateOfBirth?.let { dateFormatter.format(it) } ?: "Not set"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navController.navigate(NavRoutes.UPDATE_PROFILE) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary, // Set the background color
                    contentColor = MaterialTheme.colorScheme.onSecondary // Set the text/icon color on secondary
                )
            ) {
                Text("Edit Profile")
            }
        }
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