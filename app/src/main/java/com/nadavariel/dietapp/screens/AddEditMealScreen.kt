package com.nadavariel.dietapp.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.models.Meal
import com.nadavariel.dietapp.models.NutrientData
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.AppDatePickerDialog
import com.nadavariel.dietapp.ui.AppMainHeader
import com.nadavariel.dietapp.ui.AppTimePickerDialog
import com.nadavariel.dietapp.ui.AppTopBar
import com.nadavariel.dietapp.ui.DateTimePickerRow
import com.nadavariel.dietapp.viewmodels.FoodLogViewModel
import com.nadavariel.dietapp.viewmodels.GeminiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val FILE_PROVIDER_AUTHORITY = "com.nadavariel.dietapp.provider"

enum class AddMealMode {
    PHOTO, TEXT, MANUAL
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMealScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    navController: NavController,
    mealToEdit: Meal? = null,
) {
    val context = LocalContext.current
    val isEditMode = mealToEdit != null

    var selectedMode by remember { mutableStateOf<AddMealMode?>(if (isEditMode) AddMealMode.MANUAL else null) }
    var foodName by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var fiberText by remember { mutableStateOf("") }
    var sugarText by remember { mutableStateOf("") }
    var sodiumText by remember { mutableStateOf("") }
    var potassiumText by remember { mutableStateOf("") }
    var calciumText by remember { mutableStateOf("") }
    var ironText by remember { mutableStateOf("") }
    var vitaminCText by remember { mutableStateOf("") }
    var vitaminAText by remember { mutableStateOf("") }
    var vitaminB12Text by remember { mutableStateOf("") }
    var servingAmountText by remember { mutableStateOf("") }
    var servingUnitText by remember { mutableStateOf("") }
    var showNutrients by remember { mutableStateOf(false) }
    var selectedDateTimeState by remember { mutableStateOf(Calendar.getInstance()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var imageB64 by remember { mutableStateOf<String?>(null) }
    var isImageProcessing by remember { mutableStateOf(false) }

    val geminiResult by foodLogViewModel.geminiResult.collectAsState()
    val isFutureTimeSelected by foodLogViewModel.isFutureTimeSelected.collectAsState()

    fun clearImageState() {
        imageUri = null
        imageB64 = null
        imageFile?.delete()
        imageFile = null
    }

    fun clearImageAndText() {
        clearImageState()
        foodName = ""
    }

    fun uriToBase64(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            imageFile = this
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = imageFile?.let { FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, it) }
        } else {
            clearImageAndText()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = createImageFile(context)
            val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
            takePictureLauncher.launch(uri)
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    val onTakePhoto: () -> Unit = {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val file = createImageFile(context)
            val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
            takePictureLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val onUploadPhoto: () -> Unit = {
        pickImageLauncher.launch("image/*")
    }

    LaunchedEffect(selectedDateTimeState.time) {
        foodLogViewModel.updateDateTimeCheck(selectedDateTimeState.time)
    }

    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            isImageProcessing = true
            imageB64 = withContext(Dispatchers.IO) {
                uriToBase64(imageUri!!)
            }
            isImageProcessing = false
        } else {
            imageB64 = null
        }
    }

    LaunchedEffect(mealToEdit) {
        if (isEditMode) {
            mealToEdit.let {
                foodName = it.foodName
                caloriesText = it.calories.toString()
                servingAmountText = it.servingAmount.orEmpty()
                servingUnitText = it.servingUnit.orEmpty()
                proteinText = it.protein?.toString() ?: ""
                carbsText = it.carbohydrates?.toString() ?: ""
                fatText = it.fat?.toString() ?: ""
                fiberText = it.fiber?.toString() ?: ""
                sugarText = it.sugar?.toString() ?: ""
                sodiumText = it.sodium?.toString() ?: ""
                potassiumText = it.potassium?.toString() ?: ""
                calciumText = it.calcium?.toString() ?: ""
                ironText = it.iron?.toString() ?: ""
                vitaminCText = it.vitaminC?.toString() ?: ""
                vitaminAText = it.vitaminA?.toString() ?: ""
                vitaminB12Text = it.vitaminB12?.toString() ?: ""
                selectedDateTimeState = Calendar.getInstance().apply { time = it.timestamp.toDate() }
                foodLogViewModel.updateDateTimeCheck(selectedDateTimeState.time)
            }
        } else {
            foodName = ""
            caloriesText = ""
            selectedDateTimeState = Calendar.getInstance()
            clearImageAndText()
            foodLogViewModel.updateDateTimeCheck(selectedDateTimeState.time)
        }
    }

    // --- UI Content ---

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.screenBackground)
    ) {
        // --- EDIT MODE ---
        if (isEditMode) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppTopBar(
                    title = "Edit Meal",
                    onBack = { navController.popBackStack() }
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "* Indicates required fields",
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.textSecondary,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                            )

                            AddMealSectionHeader(title = "Meal Details")
                            FormCard {
                                ThemedOutlinedTextField(
                                    value = foodName,
                                    onValueChange = { foodName = it },
                                    label = "Meal Name*",
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = Icons.Default.EditNote,
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Done
                                    )
                                )
                            }
                        }
                    }
                    item {
                        ServingAndCaloriesSection(
                            servingAmountText, { servingAmountText = it },
                            servingUnitText, { servingUnitText = it },
                            caloriesText, { caloriesText = it }
                        )
                    }
                    item {
                        MacronutrientsSection(
                            proteinText, { proteinText = it },
                            carbsText, { carbsText = it },
                            fatText, { fatText = it }
                        )
                    }
                    item {
                        MicronutrientsSection(
                            fiberText, { fiberText = it },
                            sugarText, { sugarText = it },
                            sodiumText, { sodiumText = it },
                            potassiumText, { potassiumText = it },
                            calciumText, { calciumText = it },
                            ironText, { ironText = it },
                            vitaminCText, { vitaminCText = it },
                            vitaminAText, { vitaminAText = it },
                            vitaminB12Text, { vitaminB12Text = it }
                        )
                    }

                    item { DateTimePickerSection(selectedDateTimeState) { selectedDateTimeState = it } }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))

                        val isButtonEnabled = foodName.isNotBlank() &&
                                (caloriesText.toIntOrNull() ?: 0) > 0 &&
                                !isFutureTimeSelected

                        if (isFutureTimeSelected) {
                            Text(
                                text = "⚠️ Cannot update meal to a time in the future.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                            )
                        }

                        SubmitMealButton(
                            geminiResult = geminiResult,
                            isButtonEnabled = isButtonEnabled
                        ) {
                            val calValue = caloriesText.toIntOrNull() ?: 0
                            val mealTimestamp = Timestamp(selectedDateTimeState.time)

                            foodLogViewModel.updateMeal(
                                mealToEdit.id,
                                newFoodName = foodName,
                                newCalories = calValue,
                                newServingAmount = servingAmountText,
                                newServingUnit = servingUnitText,
                                newTimestamp = mealTimestamp,
                                newProtein = proteinText.toDoubleOrNull(),
                                newCarbohydrates = carbsText.toDoubleOrNull(),
                                newFat = fatText.toDoubleOrNull(),
                                newFiber = fiberText.toDoubleOrNull(),
                                newSugar = sugarText.toDoubleOrNull(),
                                newSodium = sodiumText.toDoubleOrNull(),
                                newPotassium = potassiumText.toDoubleOrNull(),
                                newCalcium = calciumText.toDoubleOrNull(),
                                newIron = ironText.toDoubleOrNull(),
                                newVitaminC = vitaminCText.toDoubleOrNull(),
                                newVitaminA = vitaminAText.toDoubleOrNull(),
                                newVitaminB12 = vitaminB12Text.toDoubleOrNull()
                            )
                            navController.popBackStack()
                        }
                    }
                }
            }

        // --- ADD MODE ---
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                AppMainHeader(
                    title = "Log a Meal",
                    subtitle = "Choose how to add your meal"
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date & Time Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = AppTheme.colors.primaryGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "When did you eat?",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.colors.textPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                DateTimePickerSection(selectedDateTimeState) { selectedDateTimeState = it }

                                if (isFutureTimeSelected) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "⚠️ Time cannot be in the future",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }

                    // Mode Selection Header
                    item {
                        Text(
                            text = "How would you like to add this meal?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.textPrimary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Mode Selection Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ModeSelectionCard(
                                title = "Photo",
                                icon = Icons.Default.CameraAlt,
                                color = AppTheme.colors.warmOrange,
                                isSelected = selectedMode == AddMealMode.PHOTO,
                                onClick = { selectedMode = AddMealMode.PHOTO },
                                modifier = Modifier.weight(1f)
                            )
                            ModeSelectionCard(
                                title = "Text",
                                icon = Icons.Default.AutoAwesome,
                                color = AppTheme.colors.accentTeal,
                                isSelected = selectedMode == AddMealMode.TEXT,
                                onClick = { selectedMode = AddMealMode.TEXT },
                                modifier = Modifier.weight(1f)
                            )
                            ModeSelectionCard(
                                title = "Manual",
                                icon = Icons.Default.EditNote,
                                color = AppTheme.colors.vividGreen,
                                isSelected = selectedMode == AddMealMode.MANUAL,
                                onClick = { selectedMode = AddMealMode.MANUAL },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Content based on selected mode
                    when (selectedMode) {
                        AddMealMode.PHOTO -> {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Add meal with photo",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Take or upload a photo and AI will analyze the nutrition",
                                            fontSize = 13.sp,
                                            color = AppTheme.colors.textSecondary
                                        )

                                        if (imageUri != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            ) {
                                                AsyncImage(
                                                    model = imageUri,
                                                    contentDescription = "Selected Meal Photo",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                IconButton(
                                                    onClick = { clearImageState() },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.Black.copy(alpha = 0.5f))
                                                ) {
                                                    Icon(Icons.Default.Close, "Remove", tint = Color.White)
                                                }
                                            }
                                        } else {
                                            // No Image - Show Buttons
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Button(
                                                    onClick = onTakePhoto,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(56.dp),
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.warmOrange)
                                                ) {
                                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Camera", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                }
                                                Button(
                                                    onClick = onUploadPhoto,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(56.dp),
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.warmOrange)
                                                ) {
                                                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(20.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Upload", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                                }
                                            }
                                        }

                                        val aiButtonEnabled = imageB64 != null &&
                                                geminiResult !is GeminiResult.Loading &&
                                                !isImageProcessing

                                        Button(
                                            onClick = {
                                                val mealTimestamp = Timestamp(selectedDateTimeState.time)
                                                foodLogViewModel.analyzeMeal(
                                                    foodName = "",
                                                    imageB64 = imageB64,
                                                    mealTime = mealTimestamp
                                                )
                                                navController.popBackStack()
                                            },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            enabled = aiButtonEnabled,
                                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.warmOrange),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Analyze with AI", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                        AddMealMode.TEXT -> {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Describe your meal",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppTheme.colors.textPrimary
                                        )
                                        Text(
                                            text = "Type what you ate and AI will estimate the nutrition",
                                            fontSize = 13.sp,
                                            color = AppTheme.colors.textSecondary
                                        )

                                        var isFocused by remember { mutableStateOf(false) }

                                        OutlinedTextField(
                                            value = foodName,
                                            onValueChange = { foodName = it },
                                            label = { Text("E.g., Grilled chicken with rice") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .onFocusChanged { isFocused = it.isFocused },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = if (isFocused) AppTheme.colors.accentTeal else AppTheme.colors.textSecondary.copy(alpha = 0.5f)
                                                )
                                            },
                                            minLines = 3,
                                            shape = RoundedCornerShape(12.dp),
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Sentences,
                                                imeAction = ImeAction.Done
                                            ),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = AppTheme.colors.accentTeal,
                                                focusedLabelColor = AppTheme.colors.accentTeal,
                                                cursorColor = AppTheme.colors.accentTeal,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        )

                                        val aiButtonEnabled = foodName.isNotBlank() &&
                                                geminiResult !is GeminiResult.Loading

                                        Button(
                                            onClick = {
                                                val mealTimestamp = Timestamp(selectedDateTimeState.time)
                                                foodLogViewModel.analyzeMeal(
                                                    foodName = foodName,
                                                    imageB64 = null,
                                                    mealTime = mealTimestamp
                                                )
                                                navController.popBackStack()
                                            },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            enabled = aiButtonEnabled,
                                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.accentTeal),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Analyze with AI", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                        AddMealMode.MANUAL -> {
                            item {
                                Column {
                                    Text(
                                        text = "* Indicates required fields",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AppTheme.colors.textSecondary,
                                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                                    )

                                    AddMealSectionHeader(title = "Meal Details")
                                    FormCard {
                                        ThemedOutlinedTextField(
                                            value = foodName,
                                            onValueChange = { foodName = it },
                                            label = "Meal Name*",
                                            modifier = Modifier.fillMaxWidth(),
                                            leadingIcon = Icons.Default.EditNote,
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Sentences,
                                                imeAction = ImeAction.Done
                                            )
                                        )
                                    }
                                }
                            }

                            item {
                                ServingAndCaloriesSection(
                                    servingAmountText, { servingAmountText = it },
                                    servingUnitText, { servingUnitText = it },
                                    caloriesText, { caloriesText = it }
                                )
                            }

                            // --- Show/Hide Button ---
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TextButton(
                                        onClick = { showNutrients = !showNutrients },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = AppTheme.colors.primaryGreen
                                        )
                                    ) {
                                        Text(
                                            text = if (showNutrients) "Hide Nutrition Details" else "Add Macro & Micro Nutrients",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = if (showNutrients) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }

                            // Macros Section (Collapsible)
                            if (showNutrients) {
                                item {
                                    MacronutrientsSection(
                                        proteinText, { proteinText = it },
                                        carbsText, { carbsText = it },
                                        fatText, { fatText = it }
                                    )
                                }

                                // Micros Section (Collapsible)
                                item {
                                    MicronutrientsSection(
                                        fiberText, { fiberText = it },
                                        sugarText, { sugarText = it },
                                        sodiumText, { sodiumText = it },
                                        potassiumText, { potassiumText = it },
                                        calciumText, { calciumText = it },
                                        ironText, { ironText = it },
                                        vitaminCText, { vitaminCText = it },
                                        vitaminAText, { vitaminAText = it },
                                        vitaminB12Text, { vitaminB12Text = it }
                                    )
                                }
                            }

                            // Direct Log Button
                            item {
                                val manualButtonEnabled = foodName.isNotBlank() &&
                                        (caloriesText.toIntOrNull() ?: 0) > 0 &&
                                        !isFutureTimeSelected

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val mealTimestamp = Timestamp(selectedDateTimeState.time)
                                        foodLogViewModel.logMeal(
                                            foodName = foodName,
                                            calories = caloriesText.toIntOrNull() ?: 0,
                                            servingAmount = servingAmountText,
                                            servingUnit = servingUnitText,
                                            mealTime = mealTimestamp,
                                            protein = proteinText.toDoubleOrNull(),
                                            carbohydrates = carbsText.toDoubleOrNull(),
                                            fat = fatText.toDoubleOrNull(),
                                            fiber = fiberText.toDoubleOrNull(),
                                            sugar = sugarText.toDoubleOrNull(),
                                            sodium = sodiumText.toDoubleOrNull(),
                                            potassium = potassiumText.toDoubleOrNull(),
                                            calcium = calciumText.toDoubleOrNull(),
                                            iron = ironText.toDoubleOrNull(),
                                            vitaminC = vitaminCText.toDoubleOrNull(),
                                            vitaminA = vitaminAText.toDoubleOrNull(),
                                            vitaminB12 = vitaminB12Text.toDoubleOrNull()
                                        )
                                        navController.popBackStack(NavRoutes.HOME, inclusive = false)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    enabled = manualButtonEnabled,
                                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primaryGreen),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Log Meal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                        null -> {
                            // Initial state: nothing selected
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(40.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TouchApp,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp),
                                            tint = AppTheme.colors.textSecondary.copy(alpha = 0.4f)
                                        )
                                        Text(
                                            text = "Select a method above to get started",
                                            fontSize = 15.sp,
                                            color = AppTheme.colors.textSecondary,
                                            textAlign = TextAlign.Center,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// -------------------------------
// --------- COMPOSABLES ---------
// -------------------------------

@Composable
private fun ModeSelectionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = color,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (isSelected) 0.25f else 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) color else AppTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SubmitMealButton(
    geminiResult: GeminiResult,
    isButtonEnabled: Boolean,
    onClick: () -> Unit
) {

    Button(
        onClick = onClick,
        enabled = isButtonEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colors.primaryGreen,
            contentColor = Color.White,
            disabledContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.3f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        if (geminiResult is GeminiResult.Loading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Analyzing...", fontWeight = FontWeight.Bold)
        } else {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Update Meal", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AddMealSectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(AppTheme.colors.primaryGreen, CircleShape)
        )
        Text(
            text = title.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = AppTheme.colors.lightGreyText,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun FormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun ThemedOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = false,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = AppTheme.colors.lightGreyText) } },
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppTheme.colors.primaryGreen,
            focusedLabelColor = AppTheme.colors.primaryGreen,
            cursorColor = AppTheme.colors.primaryGreen,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}

@Composable
private fun ServingAndCaloriesSection(
    servingAmountText: String, onServingAmountChange: (String) -> Unit,
    servingUnitText: String, onServingUnitChange: (String) -> Unit,
    caloriesText: String, onCaloriesChange: (String) -> Unit
) {
    Column {
        AddMealSectionHeader(title = "Serving & Calories")
        FormCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemedOutlinedTextField(
                    value = servingAmountText,
                    onValueChange = onServingAmountChange,
                    label = "Amount",
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                ThemedOutlinedTextField(
                    value = servingUnitText,
                    onValueChange = onServingUnitChange,
                    label = "Unit (e.g. g, ml)",
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(8.dp))
            ThemedOutlinedTextField(
                value = caloriesText,
                onValueChange = { if (it.all(Char::isDigit)) onCaloriesChange(it) },
                label = "Total Calories (kcal)*",
                leadingIcon = Icons.Outlined.LocalFireDepartment,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}

@Composable
private fun MacronutrientsSection(
    proteinText: String, onProteinChange: (String) -> Unit,
    carbsText: String, onCarbsChange: (String) -> Unit,
    fatText: String, onFatChange: (String) -> Unit
) {
    Column {
        AddMealSectionHeader(title = "Macronutrients")
        FormCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val commonModifier = Modifier.weight(1f)

                ThemedOutlinedTextField(
                    value = proteinText,
                    onValueChange = onProteinChange,
                    label = "Protein (g)",
                    leadingIcon = null,
                    modifier = commonModifier,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                ThemedOutlinedTextField(
                    value = carbsText,
                    onValueChange = onCarbsChange,
                    label = "Carbs (g)",
                    leadingIcon = null,
                    modifier = commonModifier,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                ThemedOutlinedTextField(
                    value = fatText,
                    onValueChange = onFatChange,
                    label = "Fat (g)",
                    leadingIcon = null,
                    modifier = commonModifier,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun MicronutrientsSection(
    fiberText: String, onFiberChange: (String) -> Unit, sugarText: String, onSugarChange: (String) -> Unit,
    sodiumText: String, onSodiumChange: (String) -> Unit, potassiumText: String, onPotassiumChange: (String) -> Unit,
    calciumText: String, onCalciumChange: (String) -> Unit, ironText: String, onIronChange: (String) -> Unit,
    vitaminCText: String, onVitaminCChange: (String) -> Unit,
    vitaminAText: String, onVitaminAChange: (String) -> Unit,
    vitaminB12Text: String, onVitaminB12Change: (String) -> Unit
) {

    val micros = listOf(
        NutrientData("Fiber (g)", fiberText, onFiberChange),
        NutrientData("Sugar (g)", sugarText, onSugarChange),
        NutrientData("Sodium (mg)", sodiumText, onSodiumChange),
        NutrientData("Potassium (mg)", potassiumText, onPotassiumChange),
        NutrientData("Calcium (mg)", calciumText, onCalciumChange),
        NutrientData("Iron (mg)", ironText, onIronChange),
        NutrientData("Vitamin C (mg)", vitaminCText, onVitaminCChange),
        NutrientData("Vitamin A (mcg)", vitaminAText, onVitaminAChange),
        NutrientData("Vitamin B12 (mcg)", vitaminB12Text, onVitaminB12Change)
    )

    Column {
        AddMealSectionHeader(title = "Micronutrients & Fiber")
        FormCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                micros.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { item ->
                            NutrientTextField(
                                label = item.label,
                                value = item.value,
                                onValueChange = item.onChange,
                                modifier = Modifier.weight(1f) // Equal width for all
                            )
                        }
                        // If the last row has only 1 item, add a spacer to keep the grid structure
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NutrientTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ThemedOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun DateTimePickerSection(
    selectedDateTimeState: Calendar,
    onDateTimeUpdate: (Calendar) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column {
        AddMealSectionHeader(title = "Date & Time")
        FormCard {
            DateTimePickerRow(
                icon = Icons.Default.CalendarMonth,
                label = "Date",
                value = dateFormat.format(selectedDateTimeState.time)
            ) {
                showDatePicker = true
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DateTimePickerRow(
                icon = Icons.Default.Schedule,
                label = "Time",
                value = timeFormat.format(selectedDateTimeState.time)
            ) {
                showTimePicker = true
            }
        }
    }

    if (showDatePicker) {
        AppDatePickerDialog(
            initialDate = selectedDateTimeState,
            onDismiss = { showDatePicker = false },
            onDateSelected = { newCal -> onDateTimeUpdate(newCal) }
        )
    }

    if (showTimePicker) {
        AppTimePickerDialog(
            initialTime = selectedDateTimeState,
            onDismiss = { showTimePicker = false },
            onTimeSelected = { newCal -> onDateTimeUpdate(newCal) }
        )
    }
}