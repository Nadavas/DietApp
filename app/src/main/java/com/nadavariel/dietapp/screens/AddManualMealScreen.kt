package com.nadavariel.dietapp.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.nadavariel.dietapp.NavRoutes // <-- IMPORT ADDED
import com.nadavariel.dietapp.ui.meals.*
import com.nadavariel.dietapp.viewmodel.FoodLogViewModel
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualMealScreen(
    foodLogViewModel: FoodLogViewModel = viewModel(),
    navController: NavController
) {
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

    val screenBackgroundColor = Color(0xFFF7F9FC)
    val isButtonEnabled = foodName.isNotBlank() && (caloriesText.toIntOrNull() ?: 0) > 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Manual Meal", fontWeight = FontWeight.Bold) },
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
                            label = "Meal Name*",
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
                    vitaminCText, { vitaminCText = it }
                )
            }

            item { DateTimePickerSection(selectedDateTimeState) { selectedDateTimeState = it } }

            item {
                Spacer(modifier = Modifier.height(16.dp))

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
                            vitaminC = vitaminCText.toDoubleOrNull()
                        )
                        // --- THIS IS THE FIX ---
                        // Navigate to Home and clear the "Add Meal" screens from the stack
                        navController.popBackStack(NavRoutes.HOME, inclusive = false)
                        // --- END OF FIX ---
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isButtonEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Log Meal", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}