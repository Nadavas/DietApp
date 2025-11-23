package com.nadavariel.dietapp.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Base64
import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.meals.*
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GeminiResult
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

    // --- State Variables ---
    var selectedMode by remember { mutableStateOf<AddMealMode?>(if (isEditMode) AddMealMode.MANUAL else null) }
    var foodName by remember { mutableStateOf("") }

    // Macros & Micros
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

    // Image & Time
    var selectedDateTimeState by remember { mutableStateOf(Calendar.getInstance()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var imageB64 by remember { mutableStateOf<String?>(null) }
    var isImageProcessing by remember { mutableStateOf(false) }

    val geminiResult by foodLogViewModel.geminiResult.collectAsState()
    val isFutureTimeSelected by foodLogViewModel.isFutureTimeSelected.collectAsState()

    // --- Helper Functions ---

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
        } catch (e: Exception) {
            Log.e("AddEditMealScreen", "Error converting URI to Base64: ${e.message}")
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

    // --- Launchers ---

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
        } else {
            Log.e("AddEditMealScreen", "Camera permission denied.")
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        // clearImageAndText() // Optional: Clear text if they pick a new image
        imageUri = uri
    }

    // --- Actions ---

    val onTakePhoto: () -> Unit = {
        // clearImageAndText()
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
        // clearImageAndText()
        pickImageLauncher.launch("image/*")
    }

    // --- Side Effects ---

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
            mealToEdit?.let {
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
            // Reset for Add Mode
            foodName = ""
            caloriesText = ""
            // ... reset other macros if needed
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
        // We split UI based on whether we are Editing (Full Form) or Adding (New Hub Flow)
        if (isEditMode) {
            // ================= EDIT MODE (Original Form Layout) =================
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Edit Meal", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = AppTheme.colors.screenBackground,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                containerColor = AppTheme.colors.screenBackground
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Column {
                            SectionHeader(title = "Meal Details")
                            FormCard {
                                ThemedOutlinedTextField(
                                    value = foodName,
                                    onValueChange = { foodName = it },
                                    label = "Meal Name",
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = Icons.Default.EditNote,
                                    minLines = 1
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
                            isEditMode = true,
                            geminiResult = geminiResult,
                            isButtonEnabled = isButtonEnabled
                        ) {
                            val calValue = caloriesText.toIntOrNull() ?: 0
                            val mealTimestamp = Timestamp(selectedDateTimeState.time)

                            foodLogViewModel.updateMeal(
                                mealToEdit!!.id,
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

        } else {
            // ================= ADD MODE (New Hub Design) =================
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Log a Meal",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = "Choose how to add your meal",
                            fontSize = 14.sp,
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Date & Time Section
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

                    // 2. Mode Selection Header
                    item {
                        Text(
                            text = "How would you like to add this meal?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.textPrimary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // 3. Mode Selection Cards
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
                                color = AppTheme.colors.statsGreen,
                                isSelected = selectedMode == AddMealMode.MANUAL,
                                onClick = { selectedMode = AddMealMode.MANUAL },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 4. Content based on selected mode
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
                                            // Image Selected View
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
                                                OutlinedButton(
                                                    onClick = onTakePhoto,
                                                    modifier = Modifier.weight(1f).height(48.dp),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Camera", fontSize = 14.sp)
                                                }
                                                OutlinedButton(
                                                    onClick = onUploadPhoto,
                                                    modifier = Modifier.weight(1f).height(48.dp),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(20.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Upload", fontSize = 14.sp)
                                                }
                                            }
                                        }

                                        val aiButtonEnabled = imageB64 != null &&
                                                geminiResult !is GeminiResult.Loading &&
                                                !isImageProcessing

                                        Button(
                                            onClick = {
                                                val mealTimestamp = Timestamp(selectedDateTimeState.time)
                                                foodLogViewModel.analyzeImageWithGemini(
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

                                        ThemedOutlinedTextField(
                                            value = foodName,
                                            onValueChange = { foodName = it },
                                            label = "E.g., Grilled chicken with rice",
                                            modifier = Modifier.fillMaxWidth(),
                                            leadingIcon = Icons.Default.AutoAwesome,
                                            minLines = 3
                                        )

                                        val aiButtonEnabled = foodName.isNotBlank() &&
                                                geminiResult !is GeminiResult.Loading

                                        Button(
                                            onClick = {
                                                val mealTimestamp = Timestamp(selectedDateTimeState.time)
                                                foodLogViewModel.analyzeImageWithGemini(
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
                                            text = "Enter nutrition manually",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AppTheme.colors.textPrimary
                                        )
                                        Text(
                                            text = "Log all nutritional information yourself for precise tracking",
                                            fontSize = 13.sp,
                                            color = AppTheme.colors.textSecondary
                                        )

                                        Button(
                                            onClick = { navController.navigate(NavRoutes.ADD_MANUAL_MEAL) },
                                            modifier = Modifier.fillMaxWidth().height(56.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.statsGreen),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Default.EditNote, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Continue to Manual Entry", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
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
                                        containerColor = AppTheme.colors.statsBackground.copy(alpha = 0.3f)
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

                    // Bottom Spacer
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// --- Helper Composable to fix the cut-off code ---
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
            containerColor = if (isSelected) color.copy(alpha = 0.12f) else Color.White
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