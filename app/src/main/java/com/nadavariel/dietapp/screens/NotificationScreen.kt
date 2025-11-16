package com.nadavariel.dietapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.TimePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.model.NotificationPreference
import com.nadavariel.dietapp.viewmodel.NotificationViewModel
import java.util.Calendar

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    navController: NavController,
    notificationViewModel: NotificationViewModel = viewModel()
) {
    val allNotifications by notificationViewModel.allNotifications.collectAsStateWithLifecycle(emptyList())

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPreference by remember { mutableStateOf<NotificationPreference?>(null) }

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
        }
    )

    LaunchedEffect(key1 = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("All Reminders") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    selectedPreference = null
                    showAddDialog = true
                }
            }) {
                Icon(Icons.Filled.Add, "Add New Reminder")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasNotificationPermission) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text(
                        text = "Notifications are disabled. Please grant permission in your device settings to receive reminders.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (allNotifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No reminders set yet. Tap '+' to add one.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allNotifications, key = { it.id }) { pref ->
                        NotificationItem(
                            preference = pref,
                            onToggle = { enabled ->
                                notificationViewModel.toggleNotification(pref, enabled)
                            },
                            onEdit = {
                                selectedPreference = pref
                                showAddDialog = true
                            },
                            onDelete = {
                                notificationViewModel.deleteNotification(pref)
                            }
                        )
                    }
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

    // Updated: Use a more descriptive type text
    val typeText = if (preference.type == "WEIGHT") "Weight Reminder" else "Meal Reminder"

    ListItem(
        headlineContent = { Text(timeText, style = MaterialTheme.typography.headlineSmall) },
        // UPDATED: Supporting content is now the type
        supportingContent = { Text(typeText) },
        leadingContent = {
            Icon(Icons.Default.NotificationsActive, contentDescription = "Reminder Icon")
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // UPDATED: Only show repetition text
                Text(
                    text = repetitionText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(checked = preference.isEnabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete Reminder", tint = MaterialTheme.colorScheme.error)
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

    val defaultMealMessage = "Time to log your next meal!"
    val defaultWeightMessage = "Time to log your weight!"

    val initialHour = if (isEdit) preferenceToEdit.hour else Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val initialMinute = if (isEdit) preferenceToEdit.minute else Calendar.getInstance().get(Calendar.MINUTE)

    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    var repetition by remember { mutableStateOf(preferenceToEdit?.repetition ?: "ONCE") }

    var type by remember { mutableStateOf(preferenceToEdit?.type ?: "MEAL") }

    var message by remember {
        mutableStateOf(
            preferenceToEdit?.message ?: defaultMealMessage
        )
    }

    // Removed the LaunchedEffect that was here

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Reminder" else "Add New Reminder") },
        text = {
            Column(horizontalAlignment = Alignment.Start) {

                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        TimePicker(context).apply {
                            setIs24HourView(true)
                            this.hour = initialHour
                            this.minute = initialMinute
                            setOnTimeChangedListener { _, h, m ->
                                hour = h
                                minute = m
                            }
                        }
                    },
                    update = { view ->
                        if (view.hour != hour) {
                            view.hour = hour
                        }
                        if (view.minute != minute) {
                            view.minute = minute
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Repetition", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = repetition == "ONCE",
                        onClick = { repetition = "ONCE" },
                        label = { Text("One Time") }
                    )
                    FilterChip(
                        selected = repetition == "DAILY",
                        onClick = { repetition = "DAILY" },
                        label = { Text("Every Day") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Type", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "MEAL",
                        onClick = {
                            type = "MEAL"
                            message = defaultMealMessage // Reverted: Always set message
                        },
                        label = { Text("Meal") }
                    )
                    FilterChip(
                        selected = type == "WEIGHT",
                        onClick = {
                            type = "WEIGHT"
                            message = defaultWeightMessage // Reverted: Always set message
                        },
                        label = { Text("Weight") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                        isEnabled = true,
                        type = type
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