@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.ui.components

import android.app.Activity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.nadavariel.dietapp.model.FoodNutritionalInfo
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.data.AvatarConstants
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.GoogleSignInFlowResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun HoveringNotificationCard(
    message: String,
    showSpinner: Boolean,
    onClick: (() -> Unit)?
) {
    val cardColor = if (showSpinner) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }

    val textColor = if (showSpinner) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }

    val iconColor = if (showSpinner) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        modifier = Modifier.clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSpinner) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = iconColor,
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = message,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun GeminiConfirmationDialog(
    foodInfoList: List<FoodNutritionalInfo>,
    onAccept: (List<FoodNutritionalInfo>) -> Unit,
    onCancel: () -> Unit
) {
    // --- SET UP INTERNAL STATE FOR EDITING ---
    var isEditing by remember { mutableStateOf(false) }
    val editableFoodList = remember { mutableStateListOf(*foodInfoList.toTypedArray()) }

    // Calculate total calories based on the *editable* list
    val totalCalories = remember(editableFoodList.sumOf { it.calories?.toIntOrNull() ?: 0 }) {
        editableFoodList.sumOf { it.calories?.toIntOrNull() ?: 0 }
    }
    // --- END OF STATE SETUP ---

    AlertDialog(
        onDismissRequest = { /* Do nothing (blocking) */ },
        containerColor = Color.White, // Force white background
        title = {
            Text(
                "Meal Breakdown (${editableFoodList.size})",
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    "Gemini recognized the following items. Each will be logged separately:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // --- RENDER BASED ON isEditing STATE ---
                    itemsIndexed(editableFoodList, key = { index, item -> item.foodName ?: index }) { index, foodInfo ->
                        if (isEditing) {
                            // --- EDITING VIEW ---
                            EditableFoodItem(
                                item = foodInfo,
                                onCaloriesChange = {
                                    editableFoodList[index] = foodInfo.copy(calories = it)
                                },
                                onProteinChange = {
                                    editableFoodList[index] = foodInfo.copy(protein = it)
                                },
                                onCarbsChange = {
                                    editableFoodList[index] = foodInfo.copy(carbohydrates = it)
                                },
                                onFatChange = {
                                    editableFoodList[index] = foodInfo.copy(fat = it)
                                }
                            )
                        } else {
                            // --- READ-ONLY VIEW ---
                            ReadOnlyFoodItem(index = index, foodInfo = foodInfo)
                        }

                        if (index < editableFoodList.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Total: $totalCalories kcal",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = AppTheme.colors.primaryGreen
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAccept(editableFoodList.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primaryGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Log Meal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Discard")
                }
                Spacer(Modifier.width(4.dp))
                TextButton(
                    onClick = { isEditing = !isEditing },
                    colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.textSecondary)
                ) {
                    Text(if (isEditing) "Done" else "Edit")
                }
            }
        }
    )
}

// --- 4. NEW COMPOSABLE FOR READ-ONLY ITEM ---
@Composable
private fun ReadOnlyFoodItem(index: Int, foodInfo: FoodNutritionalInfo) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}. ${foodInfo.foodName.orEmpty()}",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Text(
                text = "${foodInfo.calories.orEmpty()} kcal",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
        val serving = if (foodInfo.servingUnit.isNullOrBlank()) "" else "${foodInfo.servingAmount.orEmpty()} ${foodInfo.servingUnit}"
        if (serving.isNotBlank()) {
            Text(
                text = serving,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Protein: ${foodInfo.protein.orEmpty()} g", style = MaterialTheme.typography.labelSmall)
            Text(text = "Carbs: ${foodInfo.carbohydrates.orEmpty()} g", style = MaterialTheme.typography.labelSmall)
            Text(text = "Fat: ${foodInfo.fat.orEmpty()} g", style = MaterialTheme.typography.labelSmall)
        }
    }
}

// --- 5. NEW COMPOSABLE FOR EDITABLE ITEM ---
@Composable
private fun EditableFoodItem(
    item: FoodNutritionalInfo,
    onCaloriesChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = item.foodName.orEmpty(),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth()
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = item.calories.orEmpty(),
                onValueChange = onCaloriesChange,
                label = { Text("Kcal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = item.protein.orEmpty(),
                onValueChange = onProteinChange,
                label = { Text("Protein (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = item.carbohydrates.orEmpty(),
                onValueChange = onCarbsChange,
                label = { Text("Carbs (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = item.fat.orEmpty(),
                onValueChange = onFatChange,
                label = { Text("Fat (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun AvatarSelectionDialog(
    currentAvatarId: String,
    onDismiss: () -> Unit,
    onAvatarSelected: (String) -> Unit
    // Removed: onCustomImageSelected callback
) {
    // Removed: Photo Picker Launcher

    Dialog(onDismissRequest = onDismiss) {
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
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Removed: The "Upload Photo" Button (First Item)

                    // Existing Avatars
                    items(AvatarConstants.AVATAR_DRAWABLES) { (avatarId, drawableResId) ->
                        val isSelected = currentAvatarId == avatarId

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) AppTheme.colors.primaryGreen else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onAvatarSelected(avatarId)
                                    onDismiss()
                                }
                        ) {
                            Image(
                                painter = painterResource(id = drawableResId),
                                contentDescription = "Avatar $avatarId",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(if (isSelected) 3.dp else 0.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = onDismiss,
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

@Composable
fun UserAvatar(
    avatarId: String?,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    // 1. Check if it matches one of your pre-defined local avatars
    val localDrawable = AvatarConstants.AVATAR_DRAWABLES.find { it.first == avatarId }?.second

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            // Optional: Background color for the default icon if you want it
            .background(if (localDrawable == null) AppTheme.colors.primaryGreen.copy(alpha = 0.1f) else Color.Transparent)
    ) {
        if (localDrawable != null) {
            // Case A: It's a built-in avatar
            Image(
                painter = painterResource(id = localDrawable),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Case B: Fallback / Default (Handles null, empty, or old custom URLs)
            Image(
                painter = painterResource(id = R.drawable.ic_person_filled),
                contentDescription = "Default Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun StyledAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: String,
    confirmButtonText: String,
    dismissButtonText: String,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = AppTheme.colors.textSecondary
                )
            ) {
                Text(dismissButtonText)
            }
        }
    )
}

// --- SIGN IN AND UP COMPOSABLES ---


// --- 1. SHARED GOOGLE SIGN-IN LOGIC ---
@Composable
fun rememberGoogleSignInLauncher(
    authViewModel: AuthViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onAuthSuccess: (isNewUser: Boolean) -> Unit
): Pair<ManagedActivityResultLauncher<android.content.Intent, ActivityResult>, GoogleSignInClient> {
    val context = LocalContext.current

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember(context) {
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                authViewModel.handleGoogleSignIn(account) { flowResult ->
                    when (flowResult) {
                        GoogleSignInFlowResult.GoToHome -> onAuthSuccess(false)
                        GoogleSignInFlowResult.GoToSignUp -> onAuthSuccess(true)
                        GoogleSignInFlowResult.Error -> {}
                    }
                }
            } catch (e: ApiException) {
                scope.launch { snackbarHostState.showSnackbar("Google Sign-In failed: ${e.message}") }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("An unexpected error occurred: ${e.message}") }
            }
        } else {
            scope.launch { snackbarHostState.showSnackbar("Google Sign-In cancelled or failed.") }
        }
    }

    return Pair(launcher, googleSignInClient)
}

// --- 2. SHARED LAYOUT WRAPPER ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreenWrapper(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    content: @Composable BoxScope.(PaddingValues) -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AppTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = AppTheme.colors.textPrimary
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(AppTheme.colors.homeGradient))
                .padding(paddingValues)
        ) {
            content(paddingValues)
        }
    }
}

// --- 3. STYLED TEXT FIELD ---
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppTheme.colors.primaryGreen,
            focusedLabelColor = AppTheme.colors.primaryGreen,
            cursorColor = AppTheme.colors.primaryGreen
        )
    )
}

// --- 4. PRIMARY BUTTON (SIGN IN / NEXT) ---
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.primaryGreen,
            contentColor = Color.White
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White
            )
        } else {
            Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- 5. GOOGLE SIGN IN BUTTON ---
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean,
    text: String = "Continue with Google"
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        border = BorderStroke(1.dp, AppTheme.colors.textSecondary.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppTheme.colors.textPrimary
        )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.google_logo),
            contentDescription = "Google Logo",
            modifier = Modifier.size(24.dp),
            tint = Color.Unspecified
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, fontSize = 16.sp)
    }
}

// --- END OF SIGN IN AND UP COMPOSABLES