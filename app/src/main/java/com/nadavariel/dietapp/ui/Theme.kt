package com.nadavariel.dietapp.ui

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// --- CUSTOM COLORS ---
data class AppColors(
    val primaryGreen: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val cardBackground: Color,
    val screenBackground: Color,
    val divider: Color,
    val homeGradient: List<Color>,
    val darkGreyText: Color,
    val statsGreen: Color,
    val accentTeal: Color,
    val softBlue: Color,
    val warmOrange: Color,
    val sunsetPink: Color,
    val statsGradient: List<Color>,
    val deepPurple: Color,
    val lightGreyText: Color,
    val statsBackground: Color,
    val healthOverviewBackground: Color,
    val healthOverviewTint: Color,
    val goalStrategyBackground: Color,
    val orange: Color,
    val purple: Color,
    val mealGuidelinesBackground: Color,
    val foodsToLimit: Color,
    val exampleMealPlanBackground: Color,
    val exampleMealPlanTint: Color,
    val personalizedTrainingBackground: Color,
    val disclaimer: Color,
    val disclaimerIcon: Color,
    val lightActivity: Color,
    val activeLifestyle: Color,
    val softRed: Color,
    val accentGreen: Color,
    val axisText: Color,
)

private val LightAppColors = AppColors(
    primaryGreen = Color(0xFF4CAF50),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF6B7280),
    cardBackground = Color.White,
    screenBackground = Color(0xFFF7F9FC),
    divider = Color(0xFFE5E7EB),
    homeGradient = listOf(Color.White, Color(0xFFF7F9FC)),
    darkGreyText = Color(0xFF333333),
    statsGreen = Color(0xFF00C853),
    accentTeal = Color(0xFF00BFA5),
    softBlue = Color(0xFF40C4FF),
    warmOrange = Color(0xFFFF6E40),
    sunsetPink = Color(0xFFFF4081),
    statsGradient = listOf(Color(0xFFF8F9FA), Color(0xFFFFFFFF)),
    deepPurple = Color(0xFF7C4DFF),
    lightGreyText = Color(0xFF757575),
    statsBackground = Color(0xFFE0E0E0),
    healthOverviewBackground = Color(0xFFE3F2FD),
    healthOverviewTint = Color(0xFF2196F3),
    goalStrategyBackground = Color(0xFFE8F5E9),
    orange = Color(0xFFFF9800),
    purple = Color(0xFF9C27B0),
    mealGuidelinesBackground = Color(0xFFF3E5F5),
    foodsToLimit = Color(0xFFD32F2F),
    exampleMealPlanBackground = Color(0xFFEDE7F6),
    exampleMealPlanTint = Color(0xFF5E35B1),
    personalizedTrainingBackground = Color(0xFFFFF3E0),
    disclaimer = Color(0xFFFFF8E1),
    disclaimerIcon = Color(0xFFFFA726),
    lightActivity = Color(0xFFFBC02D),
    activeLifestyle = Color(0xFF42A5F5),
    softRed = Color(0xFFEF5350),
    accentGreen = Color(0xFF81C784),
    axisText = Color(0xFF616161),
)

// --- CREATE THE COMPOSITION LOCAL ---
// This "provides" the colors to the rest of the app.
private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/**
 * This is the single object you will import in your screens.
 *
 * HOW TO USE:
 *
 * 1. Import it:
 * import com.nadavariel.dietapp.ui.AppTheme
 *
 * 2. Use it in your @Composable:
 * @Composable
 * fun MyScreen() {
 * Column(background = AppTheme.colors.ScreenBackground) { // <-- Updated name
 * Text(
 * "Hello",
 * color = AppTheme.colors.TextPrimary // <-- Updated name
 * )
 * Button(
 * colors = ButtonDefaults.buttonColors(
 * containerColor = AppTheme.material.colorScheme.primary
 * )
 * )
 * }
 * }
 */
object AppTheme {
    /**
     * Access your custom semantic colors.
     * e.g., `AppTheme.colors.PrimaryGreen`
     */
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current

    /**
     * Access the standard Material Design 3 colors.
     * e.g., `AppTheme.material.colorScheme.primary`
     */
    val material: MaterialTheme
        @Composable
        get() = MaterialTheme
}

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

// --- THEME FUNCTION ---

@Composable
fun DietAppTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            dynamicLightColorScheme(context)
        }
        else -> LightColorScheme
    }

    // This is the key: provide new custom colors to the app
    CompositionLocalProvider(LocalAppColors provides LightAppColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}