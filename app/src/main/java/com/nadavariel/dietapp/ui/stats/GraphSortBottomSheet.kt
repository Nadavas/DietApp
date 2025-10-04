package com.nadavariel.dietapp.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nadavariel.dietapp.data.GraphPreference

@Composable
fun GraphSortBottomSheet(
    preferences: List<GraphPreference>,
    onPreferencesUpdated: (List<GraphPreference>) -> Unit,
    onDismiss: () -> Unit
) {
    // Note: The move logic in the IconButtons doesn't work for reordering graphs,
    // as you mentioned in our stored conversation. Only show/hide is functional.
    var mutablePreferences by remember { mutableStateOf(preferences.sortedBy { it.order }.toMutableList()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Edit Graph Order & Visibility",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            items(mutablePreferences, key = { it.id }) { pref ->
                val currentIndex = mutablePreferences.indexOf(pref)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Switch(
                            checked = pref.isVisible,
                            onCheckedChange = { isChecked ->
                                // Update isVisible state locally
                                mutablePreferences = mutablePreferences.map {
                                    if (it.id == pref.id) it.copy(isVisible = isChecked) else it
                                }.toMutableList()
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = pref.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (pref.isMacro) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }

                    // Order Control Arrows (Functionality noted as non-working by user)
                    Row {
                        IconButton(
                            onClick = {
                                if (currentIndex > 0) {
                                    val newIndex = currentIndex - 1
                                    mutablePreferences.add(newIndex, mutablePreferences.removeAt(currentIndex))
                                }
                            },
                            enabled = currentIndex > 0
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                        }

                        IconButton(
                            onClick = {
                                if (currentIndex < mutablePreferences.size - 1) {
                                    // Move element down by adding it AFTER the element it's swapping with
                                    val newIndex = currentIndex + 1
                                    mutablePreferences.add(newIndex + 1, mutablePreferences.removeAt(currentIndex))
                                }
                            },
                            enabled = currentIndex < mutablePreferences.size - 1
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Save Button
        Button(
            onClick = {
                // Set the 'order' based on the current list index before saving
                val finalPreferences = mutablePreferences.mapIndexed { index, pref ->
                    pref.copy(order = index)
                }

                onPreferencesUpdated(finalPreferences)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save Preferences")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}