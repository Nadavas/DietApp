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
    val screenBackground: Color,
    val divider: Color,
    val darkGreyText: Color,
    val vividGreen: Color,
    val accentTeal: Color,
    val softBlue: Color,
    val warmOrange: Color,
    val sunsetPink: Color,
    val lightGreyText: Color,
    val orange: Color,
    val purple: Color,
    val deepRed: Color,
    val darkPurple: Color,
    val tangerine: Color,
    val goldenYellow: Color,
    val skyBlue: Color,
    val softRed: Color,
    val axisText: Color,
)

private val LightAppColors = AppColors(
    primaryGreen = Color(0xFF4CAF50),
    textPrimary = Color(0xFF1A1A1A),
    textSecondary = Color(0xFF6B7280),
    screenBackground = Color(0xFFF7F9FC),
    divider = Color(0xFFE5E7EB),
    darkGreyText = Color(0xFF333333),
    vividGreen = Color(0xFF00C853),
    accentTeal = Color(0xFF00BFA5),
    softBlue = Color(0xFF40C4FF),
    warmOrange = Color(0xFFFF6E40),
    sunsetPink = Color(0xFFFF4081),
    lightGreyText = Color(0xFF757575),
    orange = Color(0xFFFF9800),
    purple = Color(0xFF9C27B0),
    deepRed = Color(0xFFD32F2F),
    darkPurple = Color(0xFF5E35B1),
    tangerine = Color(0xFFFFA726),
    goldenYellow = Color(0xFFFBC02D),
    skyBlue = Color(0xFF42A5F5),
    softRed = Color(0xFFEF5350),
    axisText = Color(0xFF616161),
)

// --- CREATE THE COMPOSITION LOCAL ---
// This "provides" the colors to the rest of the app.
private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/**
 * This is the single object to import in the screens.
 *
 * HOW TO USE:
 *
 * 1. Import:
 * import com.nadavariel.dietapp.ui.AppTheme
 *
 * 2. Use in @Composable:
 * @Composable
 * fun MyScreen() {
 * Column(background = AppTheme.colors.ScreenBackground) { // <-- Updated name
 * Text(
 * "Hello",
 * color = AppTheme.colors.TextPrimary // <-- Updated name
 * )
 * Button(
 * colors = ButtonDefaults.buttonColors(
 * containerColor = AppTheme.material.colorScheme.primary   // <-- Updated name
 * )
 * )
 * }
 * }
 */
object AppTheme {
    /**
     * Access the custom semantic colors.
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