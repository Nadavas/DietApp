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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.nadavariel.dietapp.NavRoutes
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.ui.HomeColors
import com.nadavariel.dietapp.ui.meals.*
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GeminiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val FILE_PROVIDER_AUTHORITY = "com.nadavariel.dietapp.provider"

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
    // START: Added for Vitamin A and B12
    var vitaminAText by remember { mutableStateOf("") }
    var vitaminB12Text by remember { mutableStateOf("") }
    // END: Added for Vitamin A and B12
    var servingAmountText by remember { mutableStateOf("") }
    var servingUnitText by remember { mutableStateOf("") }
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
        // Do not clear foodName here, as the user might want to switch
        // from photo to text
    }

    // --- MODIFIED clearImageAndText ---
    // This is called by the photo pickers
    fun clearImageAndText() {
        imageUri = null
        imageB64 = null
        imageFile?.delete()
        imageFile = null
        foodName = "" // Clear food name when picking a photo
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

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri =
                imageFile?.let { FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, it) }
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
        clearImageAndText()
        imageUri = uri
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
                // START: Added for Vitamin A and B12
                vitaminAText = it.vitaminA?.toString() ?: ""
                vitaminB12Text = it.vitaminB12?.toString() ?: ""
                // END: Added for Vitamin A and B12
                selectedDateTimeState =
                    Calendar.getInstance().apply { time = it.timestamp.toDate() }
                foodLogViewModel.updateDateTimeCheck(selectedDateTimeState.time)
            }
        } else {
            foodName = ""
            caloriesText = ""
            proteinText = ""
            carbsText = ""
            fatText = ""
            fiberText = ""
            sugarText = ""
            sodiumText = ""
            potassiumText = ""
            calciumText = ""
            ironText = ""
            vitaminCText = ""
            // START: Added for Vitamin A and B12
            vitaminAText = ""
            vitaminB12Text = ""
            // END: Added for Vitamin A and B12
            servingAmountText = ""
            servingUnitText = ""
            selectedDateTimeState = Calendar.getInstance()
            clearImageAndText() // Use the full clear
            foodLogViewModel.updateDateTimeCheck(selectedDateTimeState.time)
        }
    }

    val onTakePhoto: () -> Unit = {
        clearImageAndText()
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
        clearImageAndText()
        pickImageLauncher.launch("image/*")
    }

    val screenBackgroundColor = Color(0xFFF7F9FC)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Edit Meal" else "Add Meal",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBackgroundColor,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = screenBackgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (isEditMode) {
                // --- EDIT MODE (same as before) ---
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
                        servingAmountText,
                        { servingAmountText = it },
                        servingUnitText,
                        { servingUnitText = it },
                        caloriesText,
                        { caloriesText = it })
                }
                item {
                    MacronutrientsSection(
                        proteinText,
                        { proteinText = it },
                        carbsText,
                        { carbsText = it },
                        fatText,
                        { fatText = it })
                }
                item {
                    MicronutrientsSection(
                        fiberText,
                        { fiberText = it },
                        sugarText,
                        { sugarText = it },
                        sodiumText,
                        { sodiumText = it },
                        potassiumText,
                        { potassiumText = it },
                        calciumText,
                        { calciumText = it },
                        ironText,
                        { ironText = it },
                        vitaminCText,
                        { vitaminCText = it },
                        // START: Added for Vitamin A and B12
                        vitaminAText,
                        { vitaminAText = it },
                        vitaminB12Text,
                        { vitaminB12Text = it }
                        // END: Added for Vitamin A and B12
                    )
                }
            } else {
                // --- ADD MODE (new hub structure) ---

                item { SectionHeader(title = "Enter with AI") }

                item { SubSectionHeader(title = "Add with Photo") }
                item {
                    ImageInputSection(
                        onTakePhotoClick = onTakePhoto,
                        onUploadPhotoClick = onUploadPhoto
                    )
                }

                // --- 1. MOVED IMAGE PREVIEW HERE ---
                // Show the image preview *only if* an image is selected
                if (imageUri != null) {
                    item {
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
                                // Use clearImageState to hide image AND show text fields
                                onClick = { clearImageState() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Close, "Remove Photo", tint = Color.White)
                            }
                        }
                    }
                }

                // --- 2. WRAP TEXT ENTRY IN A VISIBILITY CHECK ---
                // Only show the "OR" and "Add with Text" if NO image is selected
                if (imageUri == null) {
                    item { SubSectionHeader(title = "OR") }

                    item { SubSectionHeader(title = "Add with Text") }
                    item {
                        FormCard {
                            ThemedOutlinedTextField(
                                value = foodName,
                                onValueChange = { foodName = it },
                                label = "Describe your meal...",
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = Icons.Default.AutoAwesome,
                                minLines = 3
                            )
                        }
                    }
                }
                // --- END OF FIX ---

                item {
                    val aiButtonEnabled = (foodName.isNotBlank() || imageB64 != null) &&
                            geminiResult !is GeminiResult.Loading &&
                            !isImageProcessing

                    SubmitMealButton(
                        isEditMode = false,
                        geminiResult = geminiResult,
                        isButtonEnabled = aiButtonEnabled
                    ) {
                        val mealTimestamp = Timestamp(selectedDateTimeState.time)
                        foodLogViewModel.analyzeImageWithGemini(
                            foodName = foodName,
                            imageB64 = imageB64,
                            mealTime = mealTimestamp
                        )
                        navController.popBackStack()
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            "OR",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                }
                item { SectionHeader(title = "Enter Manually") }
                item {
                    FormCard {
                        Text(
                            text = "Log a meal by entering all nutritional information yourself.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HomeColors.TextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { navController.navigate(NavRoutes.ADD_MANUAL_MEAL) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HomeColors.PrimaryGreen
                            )
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Log Meal Manually", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Date time picker is shared for all flows
            item { DateTimePickerSection(selectedDateTimeState) { selectedDateTimeState = it } }

            // This button is now only for EDIT mode
            if (isEditMode) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    val isButtonEnabled = foodName.isNotBlank() &&
                            (caloriesText.toIntOrNull() ?: 0) > 0 &&
                            !isFutureTimeSelected

                    if (isFutureTimeSelected) {
                        Text(
                            text = "⚠️ Cannot update meal to a time in the future. Please adjust the time or date.",
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

                        // We can safely use 'mealToEdit!!' because 'isEditMode' is true
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
                            // START: Added for Vitamin A and B12
                            newVitaminA = vitaminAText.toDoubleOrNull(),
                            newVitaminB12 = vitaminB12Text.toDoubleOrNull()
                            // END: Added for Vitamin A and B12
                        )

                        navController.popBackStack()
                    }
                }
            }
        }
    }
}