package com.nadavariel.dietapp.screens

import android.app.DatePickerDialog
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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

    // Sync Firestore answers
    LaunchedEffect(savedAnswers) {
        if (savedAnswers.isNotEmpty()) {
            val newAnswers = questions.map { q ->
                savedAnswers.find { it.question == q.text }?.answer
            }.toMutableList()
            answers = newAnswers
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

        Column(
            modifier = Modifier
                .padding(paddingVals)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(q.text, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))

            // --- Question Rendering Logic ---
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
                    val parsed = parseHeight(answers[currentIndex])
                    var unit by remember { mutableStateOf(parsed.second) }
                    var value by remember { mutableStateOf(parsed.first) }

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
                    val parsed = parseWeight(answers[currentIndex])
                    var unit by remember { mutableStateOf(parsed.second) }
                    var value by remember { mutableStateOf(parsed.first) }

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

                // --- Target Goal ---
                "Do you have a target weight or body composition goal in mind?" -> {
                    val options = listOf(
                        "Reach a target weight",
                        "Reduce body fat",
                        "Increase muscle size",
                        "Improve strength",
                        "Improve endurance"
                    )
                    var selectedOption by remember { mutableStateOf<String?>(null) }
                    var numericInput by remember { mutableStateOf("") }
                    var unit by remember { mutableStateOf("kg") }

                    Column {
                        options.forEach { opt ->
                            FilterChip(
                                selected = selectedOption == opt,
                                onClick = {
                                    selectedOption = opt
                                    numericInput = ""
                                    answers = answers.toMutableList().also { it[currentIndex] = opt }
                                },
                                label = { Text(opt) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        if (selectedOption in listOf("Reach a target weight", "Reduce body fat", "Increase muscle size")) {
                            Spacer(Modifier.height(16.dp))
                            OutlinedTextField(
                                value = numericInput,
                                onValueChange = {
                                    val filtered = it.filter { ch -> ch.isDigit() || ch == '.' }
                                    numericInput = filtered
                                    val suffix = when (selectedOption) {
                                        "Reach a target weight" -> " $unit"
                                        "Reduce body fat" -> "%"
                                        "Increase muscle size" -> " cm"
                                        else -> ""
                                    }
                                    answers = answers.toMutableList().also {
                                        it[currentIndex] = "$selectedOption: $filtered$suffix"
                                    }
                                },
                                placeholder = {
                                    Text(
                                        when (selectedOption) {
                                            "Reach a target weight" -> "Enter target weight"
                                            "Reduce body fat" -> "Enter target body fat %"
                                            "Increase muscle size" -> "Enter increase in cm"
                                            else -> ""
                                        }
                                    )
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (selectedOption == "Reach a target weight") {
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("kg", "lbs").forEach { u ->
                                        FilterChip(
                                            selected = unit == u,
                                            onClick = {
                                                unit = u
                                                if (numericInput.isNotBlank())
                                                    answers = answers.toMutableList().also {
                                                        it[currentIndex] =
                                                            "$selectedOption: $numericInput $u"
                                                    }
                                            },
                                            label = { Text(u) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- Exercise Types ---
                "What types of exercise do you typically perform?" -> {
                    val options = listOf(
                        "Weight training", "Running / Cardio", "Cycling", "Swimming",
                        "HIIT / CrossFit", "Yoga / Pilates", "Sports"
                    )
                    var selectedOptions by remember { mutableStateOf(mutableSetOf<String>()) }
                    var otherText by remember { mutableStateOf("") }

                    Column {
                        options.forEach { opt ->
                            FilterChip(
                                selected = selectedOptions.contains(opt),
                                onClick = {
                                    selectedOptions = selectedOptions.toMutableSet().apply {
                                        if (contains(opt)) remove(opt) else add(opt)
                                    }
                                    val text = (selectedOptions + if (otherText.isNotBlank()) setOf("Other: $otherText") else emptySet())
                                        .joinToString(", ")
                                    answers = answers.toMutableList().also { it[currentIndex] = text }
                                },
                                label = { Text(opt) },
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = otherText,
                            onValueChange = {
                                otherText = it
                                val text = (selectedOptions + if (it.isNotBlank()) setOf("Other: $it") else emptySet())
                                    .joinToString(", ")
                                answers = answers.toMutableList().also { list -> list[currentIndex] = text }
                            },
                            placeholder = { Text("Other (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Generic options
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

            Spacer(Modifier.weight(1f))
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

data class Question(
    val text: String,
    val options: List<String>? = null,
    val inputType: InputType? = null
)

enum class InputType {
    DOB, HEIGHT, WEIGHT, NUMBER, TEXT
}
