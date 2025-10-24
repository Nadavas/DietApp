package com.nadavariel.dietapp.screens

import android.os.Build
import android.widget.TimePicker
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.model.NotificationPreference
import com.nadavariel.dietapp.viewmodel.NotificationViewModel
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O) // Assuming your app supports modern Android versions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val notifications by notificationViewModel.notifications.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPreference by remember { mutableStateOf<NotificationPreference?>(null) } // for editing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Reminders") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, "Add New Reminder")
            }
        }
    ) { paddingValues ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No reminders set yet. Tap '+' to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications, key = { it.id }) { pref ->
                    NotificationItem(
                        preference = pref,
                        onToggle = { enabled -> notificationViewModel.toggleNotification(pref, enabled) },
                        onEdit = { selectedPreference = pref; showAddDialog = true },
                        onDelete = { notificationViewModel.deleteNotification(pref) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditNotificationDialog(
            viewModel = notificationViewModel,
            preferenceToEdit = selectedPreference,
            onDismiss = { showAddDialog = false; selectedPreference = null }
        )
    }
}

@Composable
fun NotificationItem(
    preference: NotificationPreference,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val repetitionText = if (preference.repetition == "DAILY") "Every Day" else "One Time"
    val timeText = String.format("%02d:%02d", preference.hour, preference.minute)

    ListItem(
        headlineContent = { Text(timeText, style = MaterialTheme.typography.headlineSmall) },
        supportingContent = { Text(preference.message) },
        leadingContent = {
            Icon(Icons.Default.NotificationsActive, contentDescription = "Reminder Icon")
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(repetitionText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(end = 8.dp))
                Switch(checked = preference.isEnabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Reminder")
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    )
    HorizontalDivider()
}

@Composable
fun AddEditNotificationDialog(
    viewModel: NotificationViewModel,
    preferenceToEdit: NotificationPreference?,
    onDismiss: () -> Unit
) {
    val isEdit = preferenceToEdit != null
    val currentCalendar = preferenceToEdit?.getNextScheduledCalendar() ?: Calendar.getInstance()

    var hour by remember { mutableStateOf(currentCalendar.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(currentCalendar.get(Calendar.MINUTE)) }
    var repetition by remember { mutableStateOf(preferenceToEdit?.repetition ?: "DAILY") }
    var message by remember { mutableStateOf(preferenceToEdit?.message ?: "Time to log your next meal!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Reminder" else "Add New Reminder") },
        text = {
            Column(horizontalAlignment = Alignment.Start) {
                // Time Picker (using AndroidView for simplicity with TimePicker)
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        TimePicker(context).apply {
                            setIs24HourView(true)
                            setOnTimeChangedListener { _, h, m ->
                                hour = h
                                minute = m
                            }
                            // Set initial values
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                hour = currentCalendar.get(Calendar.HOUR_OF_DAY)
                                minute = currentCalendar.get(Calendar.MINUTE)
                            }
                        }
                    },
                    update = { view ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            view.hour = hour
                            view.minute = minute
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Repetition Options
                Text("Repetition", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = repetition == "DAILY",
                        onClick = { repetition = "DAILY" },
                        label = { Text("Every Day") }
                    )
                    FilterChip(
                        selected = repetition == "ONCE",
                        onClick = { repetition = "ONCE" },
                        label = { Text("One Time") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Message Field
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Notification Message") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPref = (preferenceToEdit ?: NotificationPreference()).copy(
                        hour = hour,
                        minute = minute,
                        repetition = repetition,
                        message = message,
                        isEnabled = true // Always enabled when saved/updated
                    )
                    viewModel.saveNotification(finalPref)
                    onDismiss()
                }
            ) {
                Text(if (isEdit) "Update" else "Add")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}