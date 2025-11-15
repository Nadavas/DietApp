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
import com.nadavariel.dietapp.model.Meal
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
    var servingAmountText by remember { mutableStateOf("") }
    var servingUnitText by remember { mutableStateOf("") }
    var selectedDateTimeState by remember { mutableStateOf(Calendar.getInstance()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var imageB64 by remember { mutableStateOf<String?>(null) }
    var isImageProcessing by remember { mutableStateOf(false) }
    val geminiResult by foodLogViewModel.geminiResult.collectAsState()

    // --- THIS STATE IS NO LONGER USED ---
    // var proposedMealList by remember { mutableStateOf<List<FoodNutritionalInfo>?>(null) }

    val isFutureTimeSelected by foodLogViewModel.isFutureTimeSelected.collectAsState()


    fun clearImageState() {
        imageUri = null
        imageB64 = null
        imageFile?.delete()
        imageFile = null
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

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri =
                imageFile?.let { FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, it) }
        } else {
            clearImageState()
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
        clearImageState()
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
            servingAmountText = ""
            servingUnitText = ""
            selectedDateTimeState = Calendar.getInstance()
            clearImageState()
            foodLogViewModel.updateDateTimeCheck(selectedDateTimeState.time)
        }
    }

    // --- THIS LAUNCHEDEFFECT IS NO LONGER NEEDED AND HAS BEEN REMOVED ---
    /*
    LaunchedEffect(geminiResult) {
        if (geminiResult is GeminiResult.Success) {
            ...
        }
    }
    */

    val onTakePhoto: () -> Unit = {
        clearImageState()
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
        clearImageState()
        pickImageLauncher.launch("image/*")
    }

    val screenBackgroundColor = Color(0xFFF7F9FC)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Edit Meal" else "Add New Meal",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    // Back button is now always present
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
            if (!isEditMode) {
                item { SectionHeader(title = "Add with Photo") }
                item {
                    ImageInputSection(
                        onTakePhotoClick = onTakePhoto,
                        onUploadPhotoClick = onUploadPhoto
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
            }

            item {
                Column {
                    SectionHeader(title = if (isEditMode) "Meal Details" else "Enter Manually")
                    FormCard {
                        ThemedOutlinedTextField(
                            value = foodName,
                            onValueChange = { foodName = it },
                            label = if (!isEditMode) "Describe your meal..." else "Meal Name",
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = if (isEditMode) Icons.Default.EditNote else Icons.Default.AutoAwesome,
                            minLines = if (!isEditMode) 3 else 1
                        )
                        if (!isEditMode && imageUri != null) {
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp)
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
                                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Close, "Remove Photo", tint = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            if (isEditMode) {
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
                        { vitaminCText = it })
                }
            }

            item { DateTimePickerSection(selectedDateTimeState) { selectedDateTimeState = it } }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                val baseButtonEnabled = if (isEditMode) {
                    foodName.isNotBlank() && (caloriesText.toIntOrNull() ?: 0) > 0
                } else {
                    (foodName.isNotBlank() || imageB64 != null) && geminiResult !is GeminiResult.Loading && !isImageProcessing
                }

                val isButtonEnabled = if (isEditMode) {
                    baseButtonEnabled && !isFutureTimeSelected
                } else {
                    baseButtonEnabled
                }

                if (isEditMode && isFutureTimeSelected) {
                    Text(
                        text = "⚠️ Cannot update meal to a time in the future. Please adjust the time or date.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }

                SubmitMealButton(isEditMode, geminiResult, isButtonEnabled) {
                    if (isEditMode) {
                        val calValue = caloriesText.toIntOrNull() ?: 0
                        val mealTimestamp = Timestamp(selectedDateTimeState.time)

                        if (mealToEdit != null) {
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
                                newVitaminC = vitaminCText.toDoubleOrNull()
                            )
                        }
                        navController.popBackStack()
                    } else {
                        // --- THIS IS THE FIX ---
                        // 1. Get the timestamp
                        val mealTimestamp = Timestamp(selectedDateTimeState.time)
                        // 2. Call analyze and pass the timestamp
                        foodLogViewModel.analyzeImageWithGemini(
                            foodName = foodName,
                            imageB64 = imageB64,
                            mealTime = mealTimestamp
                        )
                        // 3. Navigate away immediately
                        navController.popBackStack()
                        // --- END OF FIX ---
                    }
                }
            }
        }
    }
}
