package com.nadavariel.dietapp.model

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import com.nadavariel.dietapp.ui.theme.EveningSectionColor
import com.nadavariel.dietapp.ui.theme.MorningSectionColor
import com.nadavariel.dietapp.ui.theme.NightSectionColor
import com.nadavariel.dietapp.ui.theme.NoonSectionColor
import java.time.LocalTime
import java.time.ZoneId // Required for converting Date to LocalTime
import java.util.Date

/**
 * Represents the different meal sections of a day.
 * Each section has a display name and an associated color.
 */
enum class MealSection(val sectionName: String, val color: Color) {
    MORNING("Morning", MorningSectionColor),
    NOON("Noon", NoonSectionColor),
    EVENING("Evening", EveningSectionColor),
    NIGHT("Night", NightSectionColor);

    companion object {
        /**
         * Determines the MealSection for a given meal timestamp.
         * Assumes the following time boundaries:
         * - Morning: 00:00 - 11:59
         * - Noon: 12:00 - 17:59
         * - Evening: 18:00 - 21:59
         * - Night: 22:00 - 23:59
         *
         * @param mealTimestamp The timestamp of the meal.
         * @return The corresponding MealSection.
         */
        @RequiresApi(Build.VERSION_CODES.O) // LocalTime requires API 26
        fun getMealSection(mealTimestamp: Date): MealSection {
            // Convert java.util.Date to java.time.LocalTime
            val localTime = mealTimestamp.toInstant().atZone(ZoneId.systemDefault()).toLocalTime()

            return when {
                localTime.isBefore(LocalTime.of(12, 0)) -> MORNING // Before 12:00 PM
                localTime.isBefore(LocalTime.of(18, 0)) -> NOON    // 12:00 PM to 5:59 PM
                localTime.isBefore(LocalTime.of(22, 0)) -> EVENING // 6:00 PM to 9:59 PM
                else -> NIGHT                                      // 10:00 PM to 23:59 PM
            }
        }
    }
}