package com.nadavariel.dietapp.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.nadavariel.dietapp.data.FoodNutritionalInfo
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GeminiResult
import com.nadavariel.dietapp.ui.meals.DateTimePickerSection
import com.nadavariel.dietapp.ui.meals.ServingAndCaloriesSection
import com.nadavariel.dietapp.ui.meals.MacronutrientsSection
import com.nadavariel.dietapp.ui.meals.MicronutrientsSection
import com.nadavariel.dietapp.ui.meals.SubmitMealButton
import com.nadavariel.dietapp.ui.meals.ImageInputSection
import com.nadavariel.dietapp.ui.home.glassmorphism
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

private const val FILE_PROVIDER_AUTHORITY = "com.nadavariel.dietapp.provider"

// REMOVED: The aggregateFoodInfo function is no longer needed

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
    var imageFileName by remember { mutableStateOf<String?>(null) }
    var imageB64 by remember { mutableStateOf<String?>(null) }
    var isImageProcessing by remember { mutableStateOf(false) }

    val geminiResult by foodLogViewModel.geminiResult.collectAsState()

    // MODIFIED: State to hold the LIST of meals proposed by Gemini for the dialog
    var proposedMealList by remember { mutableStateOf<List<FoodNutritionalInfo>?>(null) }

    fun clearImageState() {
        imageUri = null
        imageB64 = null
        imageFileName = null
        imageFile?.delete()
        imageFile = null
        foodName = ""
    }

    fun uriToBase64(uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    imageFileName = cursor.getString(nameIndex)
                }
            }
        }

        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        return try {
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AddEditMealScreen", "Error converting URI to Base64: ${e.message}")
            null
        }
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
            imageFileName = null
        }
    }

    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            imageFile = this
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = imageFile?.let { FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, it) }
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
                selectedDateTimeState = Calendar.getInstance().apply { time = it.timestamp.toDate() }
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
        }
    }

    // --- MODIFIED: Set proposedMealList state with the full list ---
    LaunchedEffect(geminiResult) {
        if (geminiResult is GeminiResult.Success) {
            val successResult = geminiResult as GeminiResult.Success

            // Check if the list contains valid data (at least one item with a name and calories)
            val validFoodInfoList = successResult.foodInfoList.filter {
                !it.food_name.isNullOrBlank() && it.calories?.toIntOrNull() != null
            }

            if (validFoodInfoList.isNotEmpty()) {
                // Set the state to trigger the dialog with the full list
                proposedMealList = validFoodInfoList
            } else {
                // If parsing failed to get minimum data, reset result
                foodLogViewModel.resetGeminiResult()
                // You may want to show an error message here
            }
        }
    }
    // --- END MODIFIED ---

    val onTakePhoto: () -> Unit = {
        clearImageState()
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                val file = createImageFile(context)
                val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
                takePictureLauncher.launch(uri)
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    val onUploadPhoto: () -> Unit = {
        clearImageState()
        pickImageLauncher.launch("image/*")
    }

    val dietAndNutritionGradient = remember {
        Brush.verticalGradient(listOf(Color(0x6103506C), Color(0xFF1644A0)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Edit Meal" else "Add New Meal",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(dietAndNutritionGradient)
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    GlassmorphicCard {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isEditMode) "Meal Name" else "Meal Input",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            if (isEditMode || imageUri == null) {
                                OutlinedTextField(
                                    value = foodName,
                                    onValueChange = { foodName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            if (!isEditMode) "e.g., 'A bowl of oatmeal with blueberries and a glass of orange juice'" else "Meal Name",
                                            color = Color.White.copy(alpha = 0.5f)
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            if (isEditMode) Icons.Default.EditNote else Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.8f)
                                        )
                                    },
                                    minLines = if (!isEditMode) 3 else 1,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color.White.copy(alpha = 0.6f),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                        cursorColor = Color.White
                                    )
                                )
                            }

                            if (!isEditMode && imageUri != null) {
                                Spacer(Modifier.height(16.dp))
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
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.6f)),
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove Photo",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isEditMode && imageUri == null) {
                    item {
                        ImageInputSection(
                            onTakePhotoClick = onTakePhoto,
                            onUploadPhotoClick = onUploadPhoto
                        )
                    }
                }

                if (isEditMode) {
                    item {
                        ServingAndCaloriesSection(
                            servingAmountText = servingAmountText, onServingAmountChange = { servingAmountText = it },
                            servingUnitText = servingUnitText, onServingUnitChange = { servingUnitText = it },
                            caloriesText = caloriesText, onCaloriesChange = { caloriesText = it }
                        )
                    }

                    item {
                        MacronutrientsSection(
                            proteinText = proteinText, onProteinChange = { proteinText = it },
                            carbsText = carbsText, onCarbsChange = { carbsText = it },
                            fatText = fatText, onFatChange = { fatText = it }
                        )
                    }

                    item {
                        MicronutrientsSection(
                            fiberText = fiberText, onFiberChange = { fiberText = it },
                            sugarText = sugarText, onSugarChange = { sugarText = it },
                            sodiumText = sodiumText, onSodiumChange = { sodiumText = it },
                            potassiumText = potassiumText, onPotassiumChange = { potassiumText = it },
                            calciumText = calciumText, onCalciumChange = { calciumText = it },
                            ironText = ironText, onIronChange = { ironText = it },
                            vitaminCText = vitaminCText, onVitaminCChange = { vitaminCText = it }
                        )
                    }
                }

                item {
                    DateTimePickerSection(
                        selectedDateTimeState = selectedDateTimeState,
                        onDateTimeUpdate = { selectedDateTimeState = it }
                    )
                }

                item {
                    val isButtonEnabled = if (isEditMode) {
                        foodName.isNotBlank() && (caloriesText.toIntOrNull() ?: 0) > 0
                    } else {
                        (foodName.isNotBlank() || imageB64 != null) && geminiResult !is GeminiResult.Loading && !isImageProcessing
                    }

                    SubmitMealButton(
                        isEditMode = isEditMode,
                        geminiResult = geminiResult,
                        isButtonEnabled = isButtonEnabled
                    ) {
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
                            foodLogViewModel.analyzeImageWithGemini(
                                foodName = foodName,
                                imageB64 = imageB64
                            )
                        }
                    }
                }
            }
        }
    }

    // --- Confirmation Dialog Logic (MODIFIED) ---
    if (proposedMealList != null) {
        val confirmedMealList = proposedMealList!!

        GeminiConfirmationDialog(
            foodInfoList = confirmedMealList,
            onAccept = {
                // User accepts: log ALL meals separately and navigate back
                val mealTimestamp = Timestamp(selectedDateTimeState.time)
                foodLogViewModel.logMealsFromFoodInfoList(confirmedMealList, mealTimestamp)
                proposedMealList = null
                navController.popBackStack()
            },
            onDeny = {
                // User denies: reset state and keep them on the screen with their input
                foodLogViewModel.resetGeminiResult()
                proposedMealList = null
            },
            onDismissRequest = {
                // Dismissal is equivalent to denial for simplicity
                foodLogViewModel.resetGeminiResult()
                proposedMealList = null
            }
        )
    }
    // --- END Confirmation Dialog Logic ---
}

// MODIFIED: Dialog now accepts a list and uses a LazyColumn to display separate items
@Composable
fun GeminiConfirmationDialog(
    foodInfoList: List<FoodNutritionalInfo>,
    onAccept: () -> Unit,
    onDeny: () -> Unit,
    onDismissRequest: () -> Unit
) {
    // Calculate the total calories once for the summary
    val totalCalories = remember(foodInfoList) {
        foodInfoList.sumOf { it.calories?.toIntOrNull() ?: 0 }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                "Confirm Meal Components (${foodInfoList.size})",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    "Gemini recognized the following items. Each will be logged separately:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                // List Container with Height Limit for Scrolling
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Ensure the list doesn't exceed 250dp, making the list itself scrollable
                        .heightIn(max = 250.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(foodInfoList) { index, foodInfo ->

                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Row 1: Item Name and Calories
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Item Name (with weight to take most space)
                                Text(
                                    text = "${index + 1}. ${foodInfo.food_name.orEmpty()}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleSmall,
                                    // Give it weight so the calorie count can be pushed to the right
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                // Item Calories
                                Text(
                                    text = "${foodInfo.calories.orEmpty()} kcal",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.titleSmall,
                                    // FIX: Prevent line wrapping on the calorie text
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip // Clip if it still can't fit
                                )
                            }

                            // Row 2: Serving Unit
                            val serving = if (foodInfo.serving_unit.isNullOrBlank()) "" else "${foodInfo.serving_amount.orEmpty()} ${foodInfo.serving_unit}"
                            Text(
                                text = serving,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Row 3: Macros (Protein | Carbs | Fat)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Protein: ${foodInfo.protein.orEmpty()} g",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "Carbs: ${foodInfo.carbohydrates.orEmpty()} g",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "Fat: ${foodInfo.fat.orEmpty()} g",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        // Add a Divider, but not after the last item
                        if (index < foodInfoList.lastIndex) {
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Total Calories Summary
                Text(
                    text = "Total Calories to Log: $totalCalories kcal",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(onClick = onAccept) {
                Text("Accept & Log All")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDeny) {
                Text("Deny & Edit")
            }
        }
    )
}

@Composable
fun GlassmorphicCard(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassmorphism(shape = RoundedCornerShape(16.dp))
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            content()
        }
    }
}