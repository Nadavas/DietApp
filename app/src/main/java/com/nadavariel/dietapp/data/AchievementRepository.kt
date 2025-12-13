package com.nadavariel.dietapp.data

import androidx.compose.ui.graphics.Color
import com.nadavariel.dietapp.model.Achievement

object AchievementRepository {

    // Helper: Normalize Macro Percentages (Handles 0-100 vs 0-1 scale)
    private fun getMacro(map: Map<String, Float>, keys: List<String>): Float {
        val value = keys.firstNotNullOfOrNull { map[it] } ?: return 0f
        return if (value > 1.0f) value / 100f else value
    }

    // UPDATE: More robust Micro getter (Checks multiple key variations)
    private fun getMicro(map: Map<String, Float>, keys: List<String>): Float {
        return keys.firstNotNullOfOrNull { map[it] } ?: 0f
    }

    val allAchievements = listOf(
        // --- Consistency Badges ---
        Achievement("c1", "The Starter", "Logged food at least 1 day this week.", "🌱", Color(0xFF8BC34A)) { d, _, _, _, _ -> d >= 1 },
        Achievement("c2", "Momentum", "Logged 3 days this week. Building a habit!", "🚀", Color(0xFF03A9F4)) { d, _, _, _, _ -> d >= 3 },
        Achievement("c3", "Week Warrior", "Perfect week! You logged all 7 days.", "🔥", Color(0xFFFF5722)) { d, _, _, _, _ -> d >= 7 },
        Achievement("c4", "Consistent", "Logged at least 5 days this week.", "🗓️", Color(0xFF9C27B0)) { d, _, _, _, _ -> d >= 5 },

        // --- Calorie Badges ---
        Achievement("k1", "On Target", "Avg calories 1800-2500 (Maintenance).", "🎯", Color(0xFFFFC107)) { _, c, _, _, _ -> c in 1800..2500 },
        Achievement("k2", "Light & Lean", "Avg calories under 1800 (Deficit).", "🪶", Color(0xFF00BCD4)) { _, c, _, _, _ -> c in 1000..1799 },
        Achievement("k3", "The Builder", "Avg calories over 2500 (Surplus).", "🏗️", Color(0xFF795548)) { _, c, _, _, _ -> c > 2500 },

        // --- Protein Badges ---
        Achievement("p1", "Protein Starter", "Avg > 60g protein/day.", "🥚", Color(0xFFCDDC39)) { _, _, p, _, _ -> p > 60 },
        Achievement("p2", "Muscle Maker", "Avg > 100g protein/day.", "🥩", Color(0xFFF44336)) { _, _, p, _, _ -> p > 100 },
        Achievement("p3", "Arnold Mode", "Avg > 150g protein/day.", "💪", Color(0xFF673AB7)) { _, _, p, _, _ -> p > 150 },

        // --- Macro Balance Badges ---
        Achievement("m1", "Balanced Plate", "Protein > 20%, Carbs > 30%, Fat < 35%.", "⚖️", Color(0xFF4CAF50)) { d, _, _, m, _ ->
            // Added d>=1 check to ensure we actually have data
            val p = getMacro(m, listOf("Protein", "Pro"))
            val c = getMacro(m, listOf("Carbohydrates", "Carbs"))
            val f = getMacro(m, listOf("Fat", "Fats"))
            d >= 1 && p > 0.2f && c > 0.3f && f < 0.35f
        },
        Achievement("m2", "Low Carb", "Carbs kept under 25%.", "🥑", Color(0xFF009688)) { d, _, _, m, _ ->
            val c = getMacro(m, listOf("Carbohydrates", "Carbs"))
            d >= 1 && c <= 0.25f // Removed > 0.01f restriction to allow 0 carbs
        },
        Achievement("m3", "Carb Loader", "Carbs over 50%.", "🍝", Color(0xFFFF9800)) { _, _, _, m, _ ->
            val c = getMacro(m, listOf("Carbohydrates", "Carbs"))
            c > 0.5f
        },
        Achievement("m4", "Keto Zone", "High Fat (>60%) and very low carbs (<10%).", "🥓", Color(0xFFD84315)) { d, _, _, m, _ ->
            val f = getMacro(m, listOf("Fat", "Fats"))
            val cRaw = m["Carbohydrates"] ?: m["Carbs"]
            // Default to 100% carbs if missing so we don't accidentally award Keto
            val c = if (cRaw == null) 1.0f else if (cRaw > 1f) cRaw / 100f else cRaw
            d >= 1 && f > 0.6f && c < 0.1f
        },

        // --- Lifestyle ---
        Achievement("v1", "Iron Will", "Logged 6+ days with high protein.", "⚔️", Color(0xFF607D8B)) { d, _, p, _, _ -> d >= 6 && p > 100 },
        Achievement("v2", "Hydration Hero", "Consistent logging (4+ days).", "💧", Color(0xFF2196F3)) { d, _, _, _, _ -> d >= 4 },
        Achievement("v3", "Sweet Tooth", "Carbs are high (>55%).", "🍭", Color(0xFFE91E63)) { _, _, _, m, _ ->
            val c = getMacro(m, listOf("Carbohydrates", "Carbs"))
            c > 0.55f
        },
        Achievement("v4", "Clean Sheet", "Logged 1500+ kcal for 5+ days.", "📝", Color(0xFF3F51B5)) { d, c, _, _, _ -> d >= 5 && c >= 1500 },

        // --- FIXED: COMPLEX NUTRIENTS (Removed 'value > 0' check) ---

        // 1. FIBER
        Achievement("f1", "Fiber Optic", "Avg Fiber > 30g.", "🌾", Color(0xFF8D6E63)) { _, _, _, _, mic ->
            getMicro(mic, listOf("Fiber", "Fiber, total dietary")) > 30f
        },

        // 2. LOW SUGAR (FIXED: Checks daysLogged instead of sugar > 0)
        Achievement("f2", "Sugar Smart", "Avg Sugar < 40g.", "🦷", Color(0xFF00BCD4)) { d, _, _, _, mic ->
            val s = getMicro(mic, listOf("Sugar", "Sugars", "Sugars, total"))
            d >= 1 && s < 40f
        },

        // 3. CALCIUM
        Achievement("f3", "Bone Guardian", "Avg Calcium > 1000mg.", "🦴", Color(0xFFE0E0E0)) { _, _, _, _, mic ->
            getMicro(mic, listOf("Calcium", "Ca")) > 1000f
        },

        // 4. IRON
        Achievement("f4", "Iron Heart", "Avg Iron > 15mg.", "🩸", Color(0xFFD32F2F)) { _, _, _, _, mic ->
            getMicro(mic, listOf("Iron", "Fe")) > 15f
        },

        // 5. VITAMIN C
        Achievement("f5", "Immunity Shield", "Avg Vit C > 90mg.", "🍊", Color(0xFFFF9800)) { _, _, _, _, mic ->
            getMicro(mic, listOf("Vitamin C", "Vit C", "Ascorbic acid")) > 90f
        },

        // 6. SODIUM (FIXED: Checks daysLogged)
        Achievement("f6", "Pressure Drop", "Sodium < 2300mg.", "❤️", Color(0xFFE91E63)) { d, _, _, _, mic ->
            val s = getMicro(mic, listOf("Sodium", "Na"))
            d >= 1 && s < 2300f
        },

        // 7. POTASSIUM
        Achievement("f7", "Electrolyte", "Avg Potassium > 3000mg.", "🍌", Color(0xFFFFEB3B)) { _, _, _, _, mic ->
            getMicro(mic, listOf("Potassium", "K")) > 3000f
        },

        // 8. VITAMIN A
        Achievement("f8", "Eagle Eye", "Avg Vit A > 800mcg.", "👁️", Color(0xFFFF5722)) { _, _, _, _, mic ->
            getMicro(mic, listOf("Vitamin A", "Vit A")) > 800f
        },

        // 9. MAGNESIUM
        Achievement("f9", "Zen Master", "Avg Magnesium > 350mg.", "🧘", Color(0xFF673AB7)) { _, _, _, _, mic ->
            getMicro(mic, listOf("Magnesium", "Mg")) > 350f
        },

        // 10. CHOLESTEROL (FIXED: Checks daysLogged)
        Achievement("f10", "Clean Arteries", "Cholesterol < 300mg.", "🩺", Color(0xFFCDDC39)) { d, _, _, _, mic ->
            val c = getMicro(mic, listOf("Cholesterol"))
            d >= 1 && c < 300f
        },

        // --- Special ---
        Achievement("s1", "Minimalist", "Logged 1-2 days, high protein.", "🎯", Color(0xFFFFD54F)) { d, _, p, _, _ -> d in 1..2 && p > 100 },
        Achievement("s2", "Feast Mode", "Avg calories > 3000.", "🍗", Color(0xFF8D6E63)) { _, c, _, _, _ -> c > 3000 }
    )
}