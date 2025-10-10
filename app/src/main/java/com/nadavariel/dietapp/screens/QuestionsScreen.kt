package com.nadavariel.dietapp.screens

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nadavariel.dietapp.data.DietPlan
import com.nadavariel.dietapp.viewmodel.DietPlanResult
import com.nadavariel.dietapp.viewmodel.QuestionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionsScreen(
    navController: NavController,
    questionsViewModel: QuestionsViewModel = viewModel()
) {
    // Questions
    val questions = listOf(
        Question("What is your date of birth?", inputType = InputType.DOB),
        Question("What is your gender?", options = listOf("Male", "Female", "Other / Prefer not to say")),
        Question("What is your height?", inputType = InputType.HEIGHT),
        Question("What is your current weight?", inputType = InputType.WEIGHT),
        Question(
            "What is your primary fitness goal?",
            options = listOf("Lose weight", "Gain muscle", "Maintain current weight", "Improve overall health")
        ),
        Question("Do you have a target weight or body composition goal in mind?"),
        Question(
            "How aggressive do you want to be with your fitness goal timeline?",
            options = listOf("Very aggressive (1–2 months)", "Moderate (3–6 months)", "Gradual (6+ months or no rush)")
        ),
        Question(
            "How would you describe your daily activity level outside of exercise?",
            options = listOf("Sedentary", "Lightly active", "Moderately active", "Very active")
        ),
        Question("How many days per week do you engage in structured exercise?", options = listOf("0-1", "2-3", "4-5", "6-7")),
        Question("What types of exercise do you typically perform?")
    )

    val savedAnswers by questionsViewModel.userAnswers.collectAsState()
    var currentIndex by remember { mutableIntStateOf(0) }
    var answers by remember { mutableStateOf(mutableListOf<String?>().apply { repeat(questions.size) { add(null) } }) }

    val dietPlanResult by questionsViewModel.dietPlanResult.collectAsState()

    // Track if we should show the dialog
    var showDietPlanDialog by remember { mutableStateOf(false) }
    var currentDietPlan by remember { mutableStateOf<DietPlan?>(null) }

    // Sync Firestore answers
    LaunchedEffect(savedAnswers) {
        if (savedAnswers.isNotEmpty()) {
            val newAnswers = questions.map { q ->
                savedAnswers.find { it.question == q.text }?.answer
            }.toMutableList()
            answers = newAnswers
        }
    }

    // Handle diet plan result changes
    LaunchedEffect(dietPlanResult) {
        when (val result = dietPlanResult) {
            is DietPlanResult.Success -> {
                currentDietPlan = result.plan
                showDietPlanDialog = true
            }
            else -> { /* handled separately */ }
        }
    }

    // Format helpers
    fun isoToDisplay(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdfIso.parse(iso)
            val sdfDisplay = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdfDisplay.format(date!!)
        } catch (e: Exception) {
            iso
        }
    }

    fun displayToIso(year: Int, month: Int, day: Int): String =
        String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)

    fun parseWeight(answer: String?): Pair<String, String> {
        if (answer.isNullOrBlank()) return "" to "kg"
        val trimmed = answer.trim()
        return when {
            trimmed.contains("kg", true) -> trimmed.replace("kg", "", true).trim() to "kg"
            trimmed.contains("lb", true) -> trimmed.replace("lbs", "", true).replace("lb", "", true).trim() to "lbs"
            else -> trimmed to "kg"
        }
    }

    fun parseHeight(answer: String?): Pair<String, String> {
        if (answer.isNullOrBlank()) return "" to "cm"
        val trimmed = answer.trim()
        return when {
            trimmed.contains("cm", true) -> trimmed.replace("cm", "", true).trim() to "cm"
            Regex("""^(\d+)'\s*(\d+)""").matches(trimmed) -> trimmed to "ft"
            else -> trimmed to "cm"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Questionnaire") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingVals ->
        val q = questions[currentIndex]
        val context = LocalContext.current

        // Handle loading state
        if (dietPlanResult is DietPlanResult.Loading) {
            Dialog(onDismissRequest = {}) {
                Card {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text("Generating your personalized diet plan...")
                        }
                    }
                }
            }
        }

        // Handle success dialog
        if (showDietPlanDialog && currentDietPlan != null) {
            DietPlanSuccessDialog(
                plan = currentDietPlan!!,
                onDismiss = {
                    showDietPlanDialog = false
                    // Don't reset the result - keep it as Success
                }
            )
        }

        // Handle error dialog
        if (dietPlanResult is DietPlanResult.Error) {
            DietPlanErrorDialog(
                message = (dietPlanResult as DietPlanResult.Error).message,
                onDismiss = { questionsViewModel.resetDietPlanResult() }
            )
        }


        Column(
            modifier = Modifier
                .padding(paddingVals)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(q.text, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

            // Scrollable area for questions
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (q.text) {

                    // Date of Birth
                    "What is your date of birth?" -> {
                        val savedIso = answers[currentIndex] ?: ""
                        var showPicker by remember { mutableStateOf(false) }

                        LaunchedEffect(showPicker) {
                            if (showPicker) {
                                val cal = Calendar.getInstance()
                                if (savedIso.isNotBlank()) {
                                    val parts = savedIso.split("-").map { it.toInt() }
                                    cal.set(parts[0], parts[1] - 1, parts[2])
                                }
                                val dpd = DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        answers = answers.toMutableList().also {
                                            it[currentIndex] = displayToIso(year, month, day)
                                        }
                                    },
                                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                )
                                dpd.setOnDismissListener { showPicker = false }
                                dpd.show()
                            }
                        }

                        OutlinedTextField(
                            value = isoToDisplay(answers[currentIndex]),
                            onValueChange = {},
                            readOnly = true,
                            placeholder = { Text("Tap to pick date of birth") },
                            trailingIcon = {
                                IconButton(onClick = { showPicker = true }) {
                                    Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Height Input
                    "What is your height?" -> {
                        val (initialValue, initialUnit) = parseHeight(answers[currentIndex])
                        var unit by remember(currentIndex) { mutableStateOf(initialUnit) }
                        var value by remember(currentIndex) { mutableStateOf(initialValue) }

                        Column {
                            OutlinedTextField(
                                value = value,
                                onValueChange = {
                                    val filtered = it.filter { ch -> ch.isDigit() }
                                    if (filtered.length <= 3) {
                                        value = filtered
                                        answers = answers.toMutableList().also { list ->
                                            list[currentIndex] = "$filtered $unit"
                                        }
                                    }
                                },
                                placeholder = { Text("Enter height") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("cm", "ft").forEach { u ->
                                    FilterChip(
                                        selected = unit == u,
                                        onClick = {
                                            unit = u
                                            if (value.isNotBlank()) {
                                                answers = answers.toMutableList().also {
                                                    it[currentIndex] = "$value $u"
                                                }
                                            }
                                        },
                                        label = { Text(u) }
                                    )
                                }
                            }
                        }
                    }

                    // Weight Input
                    "What is your current weight?" -> {
                        val (initialValue, initialUnit) = parseWeight(answers[currentIndex])
                        var unit by remember(currentIndex) { mutableStateOf(initialUnit) }
                        var value by remember(currentIndex) { mutableStateOf(initialValue) }

                        Column {
                            OutlinedTextField(
                                value = value,
                                onValueChange = {
                                    val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                                    if (filtered.length <= 6) {
                                        value = filtered
                                        answers = answers.toMutableList().also {
                                            it[currentIndex] = "$filtered $unit"
                                        }
                                    }
                                },
                                placeholder = { Text("Enter weight") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("kg", "lbs").forEach { u ->
                                    FilterChip(
                                        selected = unit == u,
                                        onClick = {
                                            unit = u
                                            if (value.isNotBlank()) {
                                                answers = answers.toMutableList().also {
                                                    it[currentIndex] = "$value $u"
                                                }
                                            }
                                        },
                                        label = { Text(u) }
                                    )
                                }
                            }
                        }
                    }

                    // Generic options or text input
                    else -> {
                        if (q.options != null) {
                            q.options.forEach { opt ->
                                val selected = answers[currentIndex] == opt
                                OutlinedButton(
                                    onClick = { answers = answers.toMutableList().also { it[currentIndex] = opt } },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surface
                                    )
                                ) { Text(opt) }
                            }
                        } else {
                            OutlinedTextField(
                                value = answers[currentIndex] ?: "",
                                onValueChange = { new ->
                                    answers = answers.toMutableList().also { it[currentIndex] = new }
                                },
                                placeholder = { Text("Type your answer...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }


            // AI Assistance Button (only shows if answers have been saved)
            if (savedAnswers.isNotEmpty()) {
                val hasDietPlan = dietPlanResult is DietPlanResult.Success

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (hasDietPlan) {
                                showDietPlanDialog = true
                            } else {
                                questionsViewModel.generateDietPlan()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (hasDietPlan) "View Diet Plan" else "Get AI Dietary Assistance")
                    }

                    // Show regenerate button if plan exists
                    if (hasDietPlan) {
                        OutlinedButton(
                            onClick = { questionsViewModel.regenerateDietPlan() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Regenerate")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }


            val isValid = !answers[currentIndex].isNullOrBlank()
            Button(
                onClick = {
                    if (currentIndex < questions.lastIndex) currentIndex++
                    else {
                        questionsViewModel.saveUserAnswers(questions, answers)
                        navController.popBackStack()
                    }
                },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (currentIndex < questions.lastIndex) "Next" else "Submit")
            }
        }
    }
}

@Composable
fun DietPlanSuccessDialog(plan: DietPlan, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Your Personalized Diet Plan",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Calorie Info Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Daily Calories Target",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${plan.dailyCalories} kcal",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Macros Card
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Macronutrient Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroItem("Protein", plan.proteinGrams, "g")
                            MacroItem("Carbs", plan.carbsGrams, "g")
                            MacroItem("Fat", plan.fatGrams, "g")
                        }
                    }
                }

                // Recommendations Card
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Recommendations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            plan.recommendations,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Disclaimer
                Text(
                    plan.disclaimer,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun MacroItem(label: String, value: Int, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "$value$unit",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DietPlanErrorDialog(message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}


data class Question(
    val text: String,
    val options: List<String>? = null,
    val inputType: InputType? = null
)

enum class InputType {
    DOB, HEIGHT, WEIGHT, NUMBER, TEXT
}