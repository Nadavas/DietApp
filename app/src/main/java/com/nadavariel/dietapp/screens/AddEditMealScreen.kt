package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.nadavariel.dietapp.model.Meal
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import com.nadavariel.dietapp.viewmodel.GeminiResult
import com.nadavariel.dietapp.ui.meals.SectionCard
import com.nadavariel.dietapp.ui.meals.DateTimePickerSection
import com.nadavariel.dietapp.ui.meals.ServingAndCaloriesSection
import com.nadavariel.dietapp.ui.meals.MacronutrientsSection
import com.nadavariel.dietapp.ui.meals.MicronutrientsSection
import com.nadavariel.dietapp.ui.meals.SubmitMealButton
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMealScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    navController: NavController,
    mealToEdit: Meal? = null,
) {
    // --- STATE AND LOGIC ---
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

    val geminiResult by foodLogViewModel.geminiResult.collectAsState()

    // --- INITIALIZATION / EDIT MODE SETUP ---
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
            // Clear states for Add mode - FIXED to use direct assignment
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
        }
    }

    // --- GEMINI RESULT HANDLER ---
    LaunchedEffect(geminiResult) {
        if (geminiResult is GeminiResult.Success) {
            val successResult = geminiResult as GeminiResult.Success
            val mealTimestamp = Timestamp(selectedDateTimeState.time)

            // Log each food item parsed by Gemini
            successResult.foodInfoList.forEach { foodInfo ->
                val cal = foodInfo.calories?.toIntOrNull()
                if (foodInfo.food_name != null && cal != null) {
                    foodLogViewModel.logMeal(
                        foodName = foodInfo.food_name,
                        calories = cal,
                        servingAmount = foodInfo.serving_amount,
                        servingUnit = foodInfo.serving_unit,
                        mealTime = mealTimestamp,
                        protein = foodInfo.protein?.toDoubleOrNull(),
                        carbohydrates = foodInfo.carbohydrates?.toDoubleOrNull(),
                        fat = foodInfo.fat?.toDoubleOrNull(),
                        fiber = foodInfo.fiber?.toDoubleOrNull(),
                        sugar = foodInfo.sugar?.toDoubleOrNull(),
                        sodium = foodInfo.sodium?.toDoubleOrNull(),
                        potassium = foodInfo.potassium?.toDoubleOrNull(),
                        calcium = foodInfo.calcium?.toDoubleOrNull(),
                        iron = foodInfo.iron?.toDoubleOrNull(),
                        vitaminC = foodInfo.vitaminC?.toDoubleOrNull()
                    )
                }
            }
            navController.popBackStack()
            foodLogViewModel.resetGeminiResult()
        }
    }

    // --- UI ---
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEditMode) "Edit Meal" else "Add New Meal") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- MEAL DESCRIPTION ---
            item {
                SectionCard(title = if (isEditMode) "Meal Name" else "Describe Your Meal") {
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(if (!isEditMode) "e.g., 'A bowl of oatmeal with blueberries and a glass of orange juice'" else "Meal Name") },
                        leadingIcon = { Icon(if (isEditMode) Icons.Default.EditNote else Icons.Default.AutoAwesome, contentDescription = null) },
                        minLines = if (!isEditMode) 3 else 1,
                    )
                }
            }

            // --- MANUAL DETAILS (EDIT MODE ONLY) ---
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


            // --- DATE & TIME PICKER ---
            item {
                DateTimePickerSection(
                    selectedDateTimeState = selectedDateTimeState,
                    onDateTimeUpdate = { selectedDateTimeState = it }
                )
            }

            // --- SUBMIT BUTTON ---
            item {
                val isButtonEnabled = if (isEditMode) {
                    foodName.isNotBlank() && (caloriesText.toIntOrNull() ?: 0) > 0
                } else {
                    foodName.isNotBlank() && geminiResult !is GeminiResult.Loading
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
                                foodName,
                                calValue,
                                servingAmountText,
                                servingUnitText,
                                mealTimestamp,
                                proteinText.toDoubleOrNull(),
                                carbsText.toDoubleOrNull(),
                                fatText.toDoubleOrNull(),
                                fiberText.toDoubleOrNull(),
                                sugarText.toDoubleOrNull(),
                                sodiumText.toDoubleOrNull(),
                                potassiumText.toDoubleOrNull(),
                                calciumText.toDoubleOrNull(),
                                ironText.toDoubleOrNull(),
                                vitaminCText.toDoubleOrNull()
                            )
                        }
                        navController.popBackStack()
                    } else {
                        foodLogViewModel.analyzeImageWithGemini(foodName)
                    }
                }
            }
        }
    }
}