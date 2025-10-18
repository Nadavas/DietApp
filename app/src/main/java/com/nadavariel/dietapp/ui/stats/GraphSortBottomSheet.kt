package com.nadavariel.dietapp.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nadavariel.dietapp.data.GraphPreference // Ensure this import is correct
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun GraphSortBottomSheet(
    preferences: List<GraphPreference>,
    onPreferencesUpdated: (List<GraphPreference>) -> Unit,
    onDismiss: () -> Unit
) {
    // Use mutableStateOf to hold the list state for reordering
    val prefsState = remember { mutableStateOf(preferences.sortedBy { it.order }) }

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            prefsState.value = prefsState.value.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        // Optional: Add onDragEnd if you want to save immediately after drag
        // onDragEnd = { startIndex, endIndex ->
        //     val finalPreferences = prefsState.value.mapIndexed { index, pref ->
        //         pref.copy(order = index)
        //     }
        //     onPreferencesUpdated(finalPreferences)
        // }
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .navigationBarsPadding() // Add padding for gesture navigation
    ) {
        // Use the consistent SectionHeader
        SectionHeader(title = "Organize Charts")

        LazyColumn(
            state = reorderableState.listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Allow list to scroll if many items
                .reorderable(reorderableState)
        ) {
            items(prefsState.value, key = { it.id }) { item ->
                ReorderableItem(reorderableState, key = item.id) { isDragging ->
                    // Use FormCard style for the list items
                    FormCard { // Wrap item content in FormCard
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                // Reduced padding inside the card row
                                .padding(vertical = 4.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DragHandle,
                                contentDescription = "Drag to reorder",
                                modifier = Modifier
                                    .detectReorderAfterLongPress(reorderableState)
                                    .padding(horizontal = 8.dp), // Padding around handle
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                                fontWeight = if (item.isMacro) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Switch(
                                checked = item.isVisible,
                                onCheckedChange = { isChecked ->
                                    prefsState.value = prefsState.value.map {
                                        if (it.id == item.id) it.copy(isVisible = isChecked) else it
                                    }
                                },
                                // Apply consistent coloring
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = HealthyGreen,
                                    checkedTrackColor = HealthyGreen.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp)) // Ensure padding after switch
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp)) // Spacing between cards
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Use the consistent AppSubmitButton
        AppSubmitButton(
            text = "Save Preferences",
            onClick = {
                val finalPreferences = prefsState.value.mapIndexed { index, pref ->
                    pref.copy(order = index)
                }
                onPreferencesUpdated(finalPreferences)
                onDismiss()
            }
        )
        Spacer(modifier = Modifier.height(8.dp)) // Add padding below button if needed
    }
}