package com.nadavariel.dietapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.UserAvatar
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.GoalsViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    authViewModel: AuthViewModel,
    goalsViewModel: GoalsViewModel,
    navController: NavController
) {
    val goals by goalsViewModel.goals.collectAsStateWithLifecycle()
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    AppTheme.colors.primaryGreen.copy(alpha = 0.9f),
                                    AppTheme.colors.primaryGreen.copy(alpha = 0.7f)
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 0.dp, bottom = 16.dp)
                    ) {
                        IconButton(
                            onClick = { navController.popBackStack() },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Back",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(4.dp)
                            ) {
                                UserAvatar(
                                    avatarId = userProfile.avatarId,
                                    size = 120.dp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = userProfile.name.ifEmpty { "Welcome!" },
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (userProfile.name.isNotEmpty()) {
                                Text(
                                    text = "Your health journey",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // About You Section
                item {
                    ProfileSectionCard(
                        title = "About You",
                        icon = Icons.Default.Person,
                        iconColor = AppTheme.colors.softBlue
                    ) {
                        ProfileInfoItem(
                            icon = Icons.Default.Badge,
                            label = "Name",
                            value = userProfile.name.ifEmpty { "Not set" },
                            iconColor = AppTheme.colors.softBlue
                        )
                        ProfileInfoItem(
                            icon = Icons.Default.Cake,
                            label = "Date of Birth",
                            value = userProfile.dateOfBirth?.let { dateFormatter.format(it) }
                                ?: "Not set",
                            iconColor = AppTheme.colors.sunsetPink
                        )
                        ProfileInfoItem(
                            icon = Icons.Default.Wc,
                            label = "Gender",
                            value = userProfile.gender.displayName,
                            iconColor = AppTheme.colors.accentTeal
                        )
                    }
                }

                // Physical Stats Section
                item {
                    ProfileSectionCard(
                        title = "Physical Stats",
                        icon = Icons.Default.FitnessCenter,
                        iconColor = AppTheme.colors.vividGreen
                    ) {
                        ProfileInfoItem(
                            icon = Icons.Default.MonitorWeight,
                            label = "Starting Weight",
                            value = if (userProfile.startingWeight > 0f)
                                "${userProfile.startingWeight} kg"
                            else "Not set",
                            iconColor = AppTheme.colors.vividGreen
                        )
                        ProfileInfoItem(
                            icon = Icons.Default.Height,
                            label = "Height",
                            value = if (userProfile.height > 0f)
                                "${userProfile.height} cm"
                            else "Not set",
                            iconColor = AppTheme.colors.warmOrange
                        )
                    }
                }

                // Your Targets Section
                item {
                    ProfileSectionCard(
                        title = "Your Targets",
                        icon = Icons.Default.TrackChanges,
                        iconColor = AppTheme.colors.accentTeal
                    ) {
                        ProfileInfoItem(
                            icon = Icons.Default.LocalFireDepartment,
                            label = "Daily Calories",
                            value = calorieGoal,
                            iconColor = AppTheme.colors.warmOrange
                        )
                        ProfileInfoItem(
                            icon = Icons.Default.Restaurant,
                            label = "Daily Protein",
                            value = proteinGoal,
                            iconColor = AppTheme.colors.vividGreen
                        )
                        ProfileInfoItem(
                            icon = Icons.Default.FlagCircle,
                            label = "Target Weight",
                            value = weightGoal,
                            iconColor = AppTheme.colors.accentTeal
                        )
                    }
                }

                // Edit Profile Button
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { navController.navigate(NavRoutes.EDIT_PROFILE) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.primaryGreen
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Edit Profile & Goals",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ProfileSectionCard(
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
private fun ProfileInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = AppTheme.colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 16.sp,
                color = AppTheme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}