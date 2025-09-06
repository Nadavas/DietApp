package com.nadavariel.dietapp.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import com.nadavariel.dietapp.ui.theme.EveningSectionColor
import com.nadavariel.dietapp.ui.theme.MorningSectionColor
import com.nadavariel.dietapp.ui.theme.NightSectionColor
import com.nadavariel.dietapp.ui.theme.NoonSectionColor
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

// Represents the different times of day for meals
enum class MealSection(val sectionName: String, val color: Color) {
    MORNING("Morning", MorningSectionColor),
    NOON("Noon", NoonSectionColor),
    EVENING("Evening", EveningSectionColor),
    NIGHT("Night", NightSectionColor);

    companion object {
        // Determines the meal section based on the timestamp
        @RequiresApi(Build.VERSION_CODES.O)
        fun getMealSection(mealTimestamp: Date): MealSection {
            // Converts Date to LocalTime for easier comparison
            val localTime = mealTimestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()

            return when {
                localTime.isBefore(LocalTime.of(12, 0)) -> MORNING
                localTime.isBefore(LocalTime.of(18, 0)) -> NOON
                localTime.isBefore(LocalTime.of(22, 0)) -> EVENING
                else -> NIGHT
            }
        }
    }
}