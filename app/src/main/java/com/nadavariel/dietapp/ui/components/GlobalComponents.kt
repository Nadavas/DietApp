package com.nadavariel.dietapp.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions // <-- NEW IMPORT
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField // <-- NEW IMPORT
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue // <-- NEW IMPORT
import androidx.compose.runtime.mutableStateOf // <-- NEW IMPORT
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue // <-- NEW IMPORT
import androidx.compose.runtime.mutableStateListOf // <-- NEW IMPORT
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType // <-- NEW IMPORT
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nadavariel.dietapp.model.FoodNutritionalInfo

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
    // --- 1. SET UP INTERNAL STATE FOR EDITING ---
    var isEditing by remember { mutableStateOf(false) }
    val editableFoodList = remember { mutableStateListOf(*foodInfoList.toTypedArray()) }

    // Calculate total calories based on the *editable* list
    val totalCalories = remember(editableFoodList.sumOf { it.calories?.toIntOrNull() ?: 0 }) {
        editableFoodList.sumOf { it.calories?.toIntOrNull() ?: 0 }
    }
    // --- END OF STATE SETUP ---

    AlertDialog(
        onDismissRequest = { /* Do nothing (blocking) */ },
        title = {
            Text(
                "Confirm Meal Components (${editableFoodList.size})",
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

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // --- 2. RENDER BASED ON isEditing STATE ---
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
                    text = "Total Calories to Log: $totalCalories kcal",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        // --- 3. UPDATE BUTTON LOGIC ---
        confirmButton = {
            Button(onClick = { onAccept(editableFoodList.toList()) }) {
                Text("Accept")
            }
        },
        dismissButton = {
            Row {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { isEditing = !isEditing }) {
                    Text(if (isEditing) "Save" else "Edit")
                }
            }
        }
        // --- END OF FIX ---
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