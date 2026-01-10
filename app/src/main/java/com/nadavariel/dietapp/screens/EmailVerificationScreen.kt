package com.nadavariel.dietapp.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nadavariel.dietapp.ui.AppPrimaryButton
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationScreen(
    authViewModel: AuthViewModel,
    onVerificationCompleted: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppTheme.colors.screenBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MarkEmailRead,
                contentDescription = null,
                tint = AppTheme.colors.primaryGreen,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Verify your Email",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "We've sent a verification link to:\n${authViewModel.currentUser?.email}",
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                color = AppTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            AppPrimaryButton(
                text = "I've Verified My Email",
                isLoading = isChecking,
                onClick = {
                    scope.launch {
                        isChecking = true
                        val isVerified = authViewModel.checkEmailVerificationStatus()
                        isChecking = false

                        if (isVerified) {
                            onVerificationCompleted()
                        } else {
                            Toast.makeText(context, "Email not verified yet. Please check your inbox.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = {
                    isLoading = true
                    authViewModel.sendVerificationEmail(
                        onSuccess = {
                            isLoading = false
                            Toast.makeText(context, "Email sent!", Toast.LENGTH_SHORT).show()
                        },
                        onError = {
                            isLoading = false
                            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                enabled = !isLoading
            ) {
                Text("Resend Email", color = AppTheme.colors.primaryGreen, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onSignOut) {
                Text("Wrong email? Sign Out", color = AppTheme.colors.softRed)
            }
        }
    }
}