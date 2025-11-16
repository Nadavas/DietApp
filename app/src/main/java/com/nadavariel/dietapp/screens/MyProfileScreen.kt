package com.nadavariel.dietapp.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.util.AvatarConstants
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    authViewModel: AuthViewModel,
    goalsViewModel: GoalsViewModel, // <-- PARAMETER ADDED
    navController: NavController
) {
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val hasMissingProfileDetails by authViewModel.hasMissingPrimaryProfileDetails.collectAsStateWithLifecycle()
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // --- 1. COLLECT GOALS STATE ---
    val goals by goalsViewModel.goals.collectAsStateWithLifecycle()

    // --- 2. HELPER LOGIC TO FIND GOALS ---
    val calorieGoal = remember(goals) {
        (goals.find { it.text.contains("calorie", ignoreCase = true) }?.value ?: "Not Set")
            .let { if (it.isNotBlank() && it != "Not Set") "$it kcal" else "Not Set" }
    }
    val proteinGoal = remember(goals) {
        (goals.find { it.text.contains("protein", ignoreCase = true) }?.value ?: "Not Set")
            .let { if (it.isNotBlank() && it != "Not Set") "$it g" else "Not Set" }
    }
    val weightGoal = remember(goals) {
        (goals.find { it.text.contains("weight", ignoreCase = true) }?.value ?: "Not Set")
            .let { if (it.isNotBlank() && it != "Not Set") "$it kg" else "Not Set" }
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
            verticalArrangement = Arrangement.Top
        ) {
            Image(
                painter = painterResource(id = AvatarConstants.getAvatarResId(userProfile.avatarId)),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .padding(bottom = 16.dp)
            )
            Text(
                text = "Personal Details",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Warning for missing essential details
            AnimatedVisibility(
                visible = hasMissingProfileDetails,
                enter = expandVertically(expandFrom = Alignment.Top),
                exit = shrinkVertically(shrinkTowards = Alignment.Top)
            ) {
                val missingFields = mutableListOf<String>()
                if (userProfile.name.isBlank()) {
                    missingFields.add("name")
                }
                if (userProfile.startingWeight <= 0f) {
                    missingFields.add("starting weight")
                }
                if (userProfile.height <= 0f) {
                    missingFields.add("height")
                }

                val message = if (missingFields.isNotEmpty()) {
                    val formattedFields = when (missingFields.size) {
                        1 -> missingFields[0]
                        2 -> "${missingFields[0]} and ${missingFields[1]}"
                        else -> {
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

            // Essential profile details (always visible)
            ProfileDetailRow("Name:", userProfile.name.ifEmpty { "Not Set" })
            ProfileDetailRow("Starting Weight:", if (userProfile.startingWeight > 0f) "${userProfile.startingWeight} kg" else "Not Set")
            ProfileDetailRow("Height:", if (userProfile.height > 0f) "${userProfile.height} cm" else "Not Set")

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileDetailRow(
                    label = "Date of Birth:",
                    value = userProfile.dateOfBirth?.let { dateFormatter.format(it) } ?: "Not Set"
                )
                ProfileDetailRow(
                    label = "Gender:",
                    value = userProfile.gender.displayName
                )
            }

            // --- 3. ADDED GOALS SECTION ---
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your Targets",
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp) // Aligned with "Personal Details"
            )

            ProfileDetailRow("Calorie Target:", calorieGoal)
            ProfileDetailRow("Protein Target:", proteinGoal)
            ProfileDetailRow("Weight Target:", weightGoal)
            // --- END OF ADDED SECTION ---

            Spacer(modifier = Modifier.height(32.dp))

            // Edit profile button
            Button(
                onClick = { navController.navigate(NavRoutes.UPDATE_PROFILE) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Text("Edit Profile & Goals") // <-- Updated button text
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
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.4f) // Adjust weight as needed
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.6f) // Adjust weight as needed
        )
    }
}