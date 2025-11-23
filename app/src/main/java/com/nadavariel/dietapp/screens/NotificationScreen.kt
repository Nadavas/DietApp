package com.nadavariel.dietapp.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.model.NotificationPreference
import com.nadavariel.dietapp.ui.AppTheme
import com.nadavariel.dietapp.ui.account.StyledAlertDialog
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

    val sortedNotifications = remember(allNotifications) {
        val oneTime = allNotifications.filter { it.repetition == "ONCE" }
            .sortedWith(compareBy({ it.hour }, { it.minute }))
        val recurring = allNotifications.filter { it.repetition == "DAILY" }
            .sortedWith(compareBy({ it.hour }, { it.minute }))

        oneTime + recurring
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedPreference by remember { mutableStateOf<NotificationPreference?>(null) }
    var preferenceToDelete by remember { mutableStateOf<NotificationPreference?>(null) }

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
        onResult = { isGranted -> hasNotificationPermission = isGranted }
    )

    LaunchedEffect(key1 = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminders", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = AppTheme.colors.textPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        selectedPreference = null
                        showAddDialog = true
                    }
                },
                containerColor = AppTheme.colors.primaryGreen,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, "Add New Reminder")
            }
        },
        containerColor = AppTheme.colors.screenBackground
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
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Notifications are disabled. Please enable them in settings.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (allNotifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No reminders set yet.",
                        color = AppTheme.colors.textSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedNotifications, key = { it.id }) { pref ->
                        NotificationCard(
                            preference = pref,
                            onToggle = { enabled ->
                                notificationViewModel.toggleNotification(pref, enabled)
                            },
                            onEdit = {
                                selectedPreference = pref
                                showAddDialog = true
                            },
                            onDelete = {
                                preferenceToDelete = pref
                            }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
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

    if (preferenceToDelete != null) {
        StyledAlertDialog(
            onDismissRequest = { preferenceToDelete = null },
            title = "Delete Reminder",
            text = "Are you sure you want to delete this reminder?",
            confirmButtonText = "Delete",
            dismissButtonText = "Cancel",
            onConfirm = {
                preferenceToDelete?.let { notificationViewModel.deleteNotification(it) }
                preferenceToDelete = null
            }
        )
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun NotificationCard(
    preference: NotificationPreference,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var isSwitchOn by remember(preference.id) { mutableStateOf(preference.isEnabled) }

    LaunchedEffect(preference.isEnabled) {
        isSwitchOn = preference.isEnabled
    }

    val timeText = String.format("%02d:%02d", preference.hour, preference.minute)

    val isMeal = preference.type == "MEAL"
    val iconVector = if (isMeal) Icons.Rounded.Restaurant else Icons.Rounded.FitnessCenter
    val themeColor = if (isMeal) Color(0xFF9C27B0) else Color(0xFFD32F2F)

    val isRecurring = preference.repetition == "DAILY"

    // Helper to format selected days
    val repeatText = remember(preference.daysOfWeek) {
        when {
            !isRecurring -> ""
            preference.daysOfWeek.size == 7 -> "Every day"
            preference.daysOfWeek.containsAll(listOf(2,3,4,5,6)) && preference.daysOfWeek.size == 5 -> "Weekdays"
            preference.daysOfWeek.containsAll(listOf(1,7)) && preference.daysOfWeek.size == 2 -> "Weekends"
            else -> {
                val days = mapOf(1 to "Sun", 2 to "Mon", 3 to "Tue", 4 to "Wed", 5 to "Thu", 6 to "Fri", 7 to "Sat")
                preference.daysOfWeek.sorted().joinToString(", ") { days[it] ?: "" }
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(themeColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = themeColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )

                        if (isRecurring) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.Repeat,
                                contentDescription = "Recurring",
                                tint = AppTheme.colors.primaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (isRecurring) {
                        Text(
                            text = repeatText,
                            style = MaterialTheme.typography.labelMedium,
                            color = AppTheme.colors.primaryGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = preference.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = isSwitchOn,
                    onCheckedChange = { newValue ->
                        isSwitchOn = newValue
                        onToggle(newValue)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = AppTheme.colors.primaryGreen,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = AppTheme.colors.textSecondary.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditNotificationDialog(
    viewModel: NotificationViewModel,
    preferenceToEdit: NotificationPreference?,
    onDismiss: () -> Unit
) {
    val isEdit = preferenceToEdit != null
    val defaultMealMessage = "Time to log your meal!"
    val defaultWeightMessage = "Time to log your weight!"

    val initialHour = if (isEdit) preferenceToEdit.hour else Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val initialMinute = if (isEdit) preferenceToEdit.minute else Calendar.getInstance().get(Calendar.MINUTE)

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    // NEW: State to toggle between Clock Dial and Keyboard Input
    var showTimeInput by remember { mutableStateOf(false) }

    var repetition by remember { mutableStateOf(preferenceToEdit?.repetition ?: "ONCE") }
    var type by remember { mutableStateOf(preferenceToEdit?.type ?: "MEAL") }
    var message by remember { mutableStateOf(preferenceToEdit?.message ?: defaultMealMessage) }

    var selectedDays by remember {
        mutableStateOf(preferenceToEdit?.daysOfWeek ?: listOf(1,2,3,4,5,6,7))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Edit Reminder" else "Add New Reminder",
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = Color.White,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // NEW: Toggle Logic for TimePicker vs TimeInput
                if (showTimeInput) {
                    TimeInput(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            timeSelectorSelectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = AppTheme.colors.primaryGreen,
                            timeSelectorUnselectedContainerColor = AppTheme.colors.textSecondary.copy(alpha = 0.1f),
                            timeSelectorUnselectedContentColor = AppTheme.colors.textPrimary
                        )
                    )
                } else {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = AppTheme.colors.textPrimary,
                            selectorColor = AppTheme.colors.primaryGreen,
                            containerColor = Color.White,
                            periodSelectorBorderColor = AppTheme.colors.primaryGreen,
                            periodSelectorSelectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                            periodSelectorSelectedContentColor = AppTheme.colors.primaryGreen,
                            periodSelectorUnselectedContainerColor = Color.Transparent,
                            periodSelectorUnselectedContentColor = AppTheme.colors.textSecondary,
                            timeSelectorSelectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                            timeSelectorSelectedContentColor = AppTheme.colors.primaryGreen,
                            timeSelectorUnselectedContainerColor = AppTheme.colors.textSecondary.copy(alpha = 0.1f),
                            timeSelectorUnselectedContentColor = AppTheme.colors.textPrimary
                        )
                    )
                }

                // NEW: Toggle Button
                IconButton(onClick = { showTimeInput = !showTimeInput }) {
                    Icon(
                        imageVector = if (showTimeInput) Icons.Rounded.AccessTime else Icons.Filled.Keyboard,
                        contentDescription = "Toggle Input Mode",
                        tint = AppTheme.colors.primaryGreen
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = type == "MEAL",
                        onClick = { type = "MEAL"; message = defaultMealMessage },
                        label = { Text("Meal") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = AppTheme.colors.primaryGreen
                        )
                    )
                    FilterChip(
                        selected = type == "WEIGHT",
                        onClick = { type = "WEIGHT"; message = defaultWeightMessage },
                        label = { Text("Weight") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = AppTheme.colors.primaryGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = repetition == "ONCE",
                        onClick = { repetition = "ONCE" },
                        label = { Text("One Time") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = AppTheme.colors.primaryGreen
                        )
                    )
                    FilterChip(
                        selected = repetition == "DAILY",
                        onClick = { repetition = "DAILY" },
                        label = { Text("Recurring") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppTheme.colors.primaryGreen.copy(alpha = 0.2f),
                            selectedLabelColor = AppTheme.colors.primaryGreen
                        )
                    )
                }

                if (repetition == "DAILY") {
                    Spacer(modifier = Modifier.height(16.dp))
                    DaySelector(selectedDays = selectedDays) { newDays ->
                        selectedDays = newDays
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.primaryGreen,
                        focusedLabelColor = AppTheme.colors.primaryGreen
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalPref = (preferenceToEdit ?: NotificationPreference()).copy(
                        hour = timePickerState.hour,
                        minute = timePickerState.minute,
                        repetition = repetition,
                        message = message,
                        isEnabled = true,
                        type = type,
                        daysOfWeek = selectedDays
                    )
                    viewModel.saveNotification(finalPref)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primaryGreen)
            ) {
                Text(if (isEdit) "Update" else "Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = AppTheme.colors.textSecondary)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DaySelector(
    selectedDays: List<Int>,
    onSelectionChange: (List<Int>) -> Unit
) {
    val days = listOf("S", "M", "T", "W", "T", "F", "S")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, label ->
            val dayValue = index + 1
            val isSelected = selectedDays.contains(dayValue)

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) AppTheme.colors.primaryGreen else Color.LightGray.copy(alpha = 0.3f))
                    .clickable {
                        val newList = if (isSelected) {
                            selectedDays - dayValue
                        } else {
                            selectedDays + dayValue
                        }
                        if (newList.isNotEmpty()) {
                            onSelectionChange(newList)
                        }
                    }
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else AppTheme.colors.textSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}