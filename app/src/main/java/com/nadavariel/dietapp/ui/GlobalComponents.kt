@file:Suppress("DEPRECATION")

package com.nadavariel.dietapp.ui

import android.app.Activity
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.rounded.AccessTime
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.nadavariel.dietapp.model.FoodNutritionalInfo
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.data.AvatarConstants
import com.nadavariel.dietapp.viewmodel.AuthViewModel
import com.nadavariel.dietapp.viewmodel.GoogleSignInFlowResult
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppMainHeader(
    title: String,
    subtitle: String,
    action: @Composable (() -> Unit)? = null // Optional slot for the "Sign Out" button
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Title and Subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    lineHeight = 40.sp // Ensures descenders don't get cut off
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Right Side: Optional Action (e.g., Sign Out)
            if (action != null) {
                // Add a small spacer just in case text gets long
                Spacer(modifier = Modifier.width(8.dp))
                action()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    onBack: () -> Unit,
    showBack: Boolean = true,
    // Optional: For the "Icon" version
    icon: ImageVector? = null,
    iconColor: Color? = null,
    // Optional: Overrides
    containerColor: Color = AppTheme.colors.screenBackground,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            if (icon != null && iconColor != null) {
                // --- ICON + TITLE LAYOUT ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.darkGreyText
                    )
                }
            } else {
                // --- STANDARD TITLE LAYOUT ---
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.darkGreyText
                )
            }
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppTheme.colors.darkGreyText
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor
        ),
        actions = actions
    )
}

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
        modifier = Modifier.clickable(enabled = onClick != null) {
            if (onClick != null) {
                onClick()
            }
        }
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
    var isEditing by remember { mutableStateOf(false) }
    val editableFoodList = remember { mutableStateListOf(*foodInfoList.toTypedArray()) }

    val totalCalories = remember(editableFoodList.sumOf { it.calories?.toIntOrNull() ?: 0 }) {
        editableFoodList.sumOf { it.calories?.toIntOrNull() ?: 0 }
    }

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
) {
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
    size: Dp,
    modifier: Modifier = Modifier
) {
    val localDrawable = AvatarConstants.AVATAR_DRAWABLES.find { it.first == avatarId }?.second

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
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

@Composable
fun rememberGoogleSignInLauncher(
    authViewModel: AuthViewModel,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onAuthSuccess: (isNewUser: Boolean) -> Unit
): Pair<ManagedActivityResultLauncher<Intent, ActivityResult>, GoogleSignInClient> {
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
                .background(Color.White)
                .padding(paddingValues)
        ) {
            content(paddingValues)
        }
    }
}

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

// --- DATE & TIME ---

@Composable
fun DateTimePickerRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.primaryGreen,
            textAlign = TextAlign.End
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initialDate: Calendar,
    onDismiss: () -> Unit,
    onDateSelected: (Calendar) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.timeInMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Calendar.getInstance().apply { timeInMillis = millis }
                        val updatedCal = (initialDate.clone() as Calendar).apply {
                            set(Calendar.YEAR, newDate.get(Calendar.YEAR))
                            set(Calendar.MONTH, newDate.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, newDate.get(Calendar.DAY_OF_MONTH))
                        }

                        val now = Calendar.getInstance()
                        if (updatedCal.after(now)) {
                            onDateSelected(now)
                        } else {
                            onDateSelected(updatedCal)
                        }
                    }
                    onDismiss()
                }
            ) {
                Text("OK", color = AppTheme.colors.primaryGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppTheme.colors.textSecondary)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = Color.White)
    ) {
        DatePicker(
            state = datePickerState,
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = AppTheme.colors.primaryGreen,
                selectedDayContentColor = Color.White,
                todayDateBorderColor = AppTheme.colors.primaryGreen,
                todayContentColor = AppTheme.colors.primaryGreen
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    initialTime: Calendar,
    onDismiss: () -> Unit,
    onTimeSelected: (Calendar) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialTime.get(Calendar.MINUTE),
        is24Hour = true
    )

    var showTimeInput by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val updatedCal = (initialTime.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }

                    val now = Calendar.getInstance()
                    if (updatedCal.after(now)) {
                        onTimeSelected(now)
                    } else {
                        onTimeSelected(updatedCal)
                    }
                    onDismiss()
                }
            ) {
                Text("OK", color = AppTheme.colors.primaryGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = AppTheme.colors.textSecondary)
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (showTimeInput) {
                    TimeInput(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            timeSelectorSelectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = AppTheme.colors.primaryGreen,
                            timeSelectorUnselectedContainerColor = AppTheme.colors.textSecondary.copy(alpha = 0.1f),
                            timeSelectorUnselectedContentColor = AppTheme.colors.textPrimary
                        )
                    )
                } else {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = AppTheme.colors.textPrimary,
                            selectorColor = AppTheme.colors.primaryGreen,
                            containerColor = Color.White,
                            periodSelectorBorderColor = AppTheme.colors.primaryGreen,
                            periodSelectorSelectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                            periodSelectorSelectedContentColor = AppTheme.colors.primaryGreen,
                            periodSelectorUnselectedContainerColor = Color.Transparent,
                            periodSelectorUnselectedContentColor = AppTheme.colors.textSecondary,
                            timeSelectorSelectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = AppTheme.colors.primaryGreen,
                            timeSelectorUnselectedContainerColor = AppTheme.colors.textSecondary.copy(alpha = 0.1f),
                            timeSelectorUnselectedContentColor = AppTheme.colors.textPrimary
                        )
                    )
                }

                IconButton(onClick = { showTimeInput = !showTimeInput }) {
                    Icon(
                        imageVector = if (showTimeInput) Icons.Rounded.AccessTime else Icons.Filled.Keyboard,
                        contentDescription = "Toggle Input Mode",
                        tint = AppTheme.colors.primaryGreen
                    )
                }
            }
        },
        containerColor = Color.White
    )
}

// --- END OF DATE & TIME ---

@Composable
fun LegalDisclaimer(
    modifier: Modifier = Modifier,
    textColor: Color = AppTheme.colors.textSecondary
) {
    val uriHandler = LocalUriHandler.current
    val projectId = "dietapp-c5e7e"

    val annotatedString = buildAnnotatedString {
        append("By continuing, you agree to our ")

        // TERMS OF SERVICE LINK
        pushStringAnnotation(tag = "TERMS", annotation = "https://$projectId.web.app/terms.html")
        withStyle(style = SpanStyle(color = AppTheme.colors.primaryGreen, textDecoration = TextDecoration.Underline)) {
            append("Terms of Service")
        }
        pop()

        append(" and ")

        // PRIVACY POLICY LINK
        pushStringAnnotation(tag = "PRIVACY", annotation = "https://$projectId.web.app/privacy.html")
        withStyle(style = SpanStyle(color = AppTheme.colors.primaryGreen, textDecoration = TextDecoration.Underline)) {
            append("Privacy Policy")
        }
        pop()

        append(".")
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodySmall.copy(
            color = textColor,
            textAlign = TextAlign.Center
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    uriHandler.openUri(annotation.item)
                }
        },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)
    )
}