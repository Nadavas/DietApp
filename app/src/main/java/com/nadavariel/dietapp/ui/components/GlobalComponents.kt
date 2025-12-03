package com.nadavariel.dietapp.ui.components

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.nadavariel.dietapp.model.FoodNutritionalInfo
import com.nadavariel.dietapp.R
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.util.AvatarConstants
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity
import java.io.File
import java.io.FileOutputStream

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
    onAvatarSelected: (String) -> Unit,
    onCustomImageSelected: (Uri) -> Unit // NEW: Callback for custom photo
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val appColor = AppTheme.colors

    // 1. Launcher to crop after picking
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val dataIntent = activityResult.data
            val resultUri = dataIntent?.let { UCrop.getOutput(it) }
            if (resultUri != null) {
                onCustomImageSelected(resultUri)
                onDismiss()
            }
        }
    }

// 2. Launcher to pick the image, then open crop UI
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // --- Step 1: Safe resize / guard ---
                val safeUri = resizeImageIfNeeded(context, uri)
                if (safeUri != null) {
                    // Prepare temp destination file
                    val destination = Uri.fromFile(
                        File(
                            context.cacheDir,
                            "cropped_${System.currentTimeMillis()}.jpg"
                        )
                    )

                    // --- Step 2: UCrop Options with Material3 and circle crop ---
                    val options = UCrop.Options().apply {

                        // Material3 Colors
                        setToolbarColor(scheme.surface.toArgb())
                        setStatusBarColor(scheme.surfaceVariant.toArgb())
                        setToolbarWidgetColor(scheme.onSurface.toArgb())
                        setActiveControlsWidgetColor(appColor.primaryGreen.toArgb())
                        setDimmedLayerColor(scheme.surface.copy(alpha = 0.85f).toArgb())

                        // Circle crop
                        setCircleDimmedLayer(true)
                        setShowCropGrid(false)
                        setShowCropFrame(false)

                        // Material-like behavior
                        setHideBottomControls(false)
                        setFreeStyleCropEnabled(false)
                        setAllowedGestures(
                            UCropActivity.SCALE,
                            UCropActivity.ROTATE,
                            UCropActivity.ALL
                        )

                        // Grid/frame colors
                        setCropFrameColor(scheme.primary.toArgb())
                        setCropGridColor(scheme.outlineVariant.toArgb())
                        setCropGridColumnCount(0)
                        setCropGridRowCount(0)

                        setCompressionQuality(95)
                    }

                    // --- Step 3: Launch uCrop with safeUri ---
                    val cropIntent = UCrop.of(safeUri, destination)
                        .withAspectRatio(1f, 1f) // Square crop (same as avatar)
                        .withOptions(options)
                        .withMaxResultSize(1080, 1080)
                        .getIntent(context)

                    cropLauncher.launch(cropIntent)
                }
            }
        }
    )



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
                    fontSize = 22.sp, // standardized font size
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing slightly for better look
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. NEW: The "Upload Photo" Button (First Item)
                    item {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF0F0F0)) // Light grey background
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Upload Photo",
                                tint = AppTheme.colors.primaryGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // 2. Existing Avatars
                    items(AvatarConstants.AVATAR_DRAWABLES) { (avatarId, drawableResId) ->
                        val isSelected = currentAvatarId == avatarId

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                // Standardized on the Border look (cleaner)
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
                                    // Add small padding if selected so the border doesn't overlap the image content
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
    // 1. Check if it matches one of your pre-defined local avatars (avatar_1, etc.)
    val localDrawable = AvatarConstants.AVATAR_DRAWABLES.find { it.first == avatarId }?.second

    val backgroundColor = if (localDrawable != null || avatarId.isNullOrBlank()) {
        AppTheme.colors.primaryGreen.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor) // Background while loading
    ) {
        if (localDrawable != null) {
            // Case A: It's a built-in avatar (local resource)
            Image(
                painter = painterResource(id = localDrawable),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!avatarId.isNullOrBlank()) {
            // Case B: It's a Custom Photo (URL or URI)
            AsyncImage(
                model = avatarId,
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Case C: Fallback / Default (e.g. if null)
            Image(
                painter = painterResource(id = R.drawable.ic_person_filled), // Ensure you have a default
                contentDescription = "Default Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun resizeImageIfNeeded(context: Context, sourceUri: Uri): Uri? {
    return try {
        // First, check the image size without loading full bitmap
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        // If image is too large, reject
        if (options.outWidth > 5000 || options.outHeight > 5000) {
            Toast.makeText(context, "Selected image is too large", Toast.LENGTH_SHORT).show()
            return null
        }

        // Load bitmap with reasonable sampling if needed
        val maxDimension = 1080
        var scale = 1
        while (options.outWidth / scale > maxDimension || options.outHeight / scale > maxDimension) {
            scale *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
        val bitmap = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: return null

        // Create temp file
        val tempFile = File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        Uri.fromFile(tempFile)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
        null
    }
}
