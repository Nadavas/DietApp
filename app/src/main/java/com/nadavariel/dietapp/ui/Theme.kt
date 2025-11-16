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

// --- 1. DEFINE YOUR CUSTOM COLORS ---
// This class unifies all your colors from HomeColors, QuestionColors, etc.
data class AppColors(
    val PrimaryGreen: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val CardBackground: Color,
    val ScreenBackground: Color,
    val Divider: Color,
    val HomeGradient: List<Color>, // Added from our last conversation
    val DarkGreyText: Color,
    val StatsGreen: Color,
    val AccentTeal: Color,
    val SoftBlue: Color,
    val WarmOrange: Color,
    val SunsetPink: Color,
    val StatsGradient: List<Color>,
    val DeepPurple: Color,
    val VibrantGreen: Color,
    val LightGreyText: Color,
    val HealthyGreen: Color,
)

// --- 2. CREATE THE SINGLE (LIGHT) PALETTE ---
// Since you don't use dark mode, we only define one.
private val LightAppColors = AppColors(
    PrimaryGreen = Color(0xFF4CAF50),
    TextPrimary = Color(0xFF1A1A1A),
    TextSecondary = Color(0xFF6B7280),
    CardBackground = Color.White,
    ScreenBackground = Color(0xFFF7F9FC),
    Divider = Color(0xFFE5E7EB),
    HomeGradient = listOf(Color.White, Color(0xFFF7F9FC)),
    DarkGreyText = Color(0xFF333333),
    StatsGreen = Color(0xFF00C853),
    AccentTeal = Color(0xFF00BFA5),
    SoftBlue = Color(0xFF40C4FF),
    WarmOrange = Color(0xFFFF6E40),
    SunsetPink = Color(0xFFFF4081),
    StatsGradient = listOf(Color(0xFFF8F9FA), Color(0xFFFFFFFF)),
    DeepPurple = Color(0xFF7C4DFF),
    VibrantGreen = Color(0xFF4CAF50),
    LightGreyText = Color(0xFF757575),
    HealthyGreen = Color(0xFF4CAF50),
)

// --- 3. CREATE THE COMPOSITION LOCAL ---
// This "provides" the colors to the rest of the app.
private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

// --- 4. CREATE THE SINGLE THEME OBJECT (THE PART YOU IMPORT) ---
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

// --- 5. UPDATED THEME FUNCTION ---
@Composable
fun DietAppTheme(
    // darkTheme: Boolean = isSystemInDarkTheme(), // Removed as requested
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // Always use light dynamic color
            dynamicLightColorScheme(context)
        }
        else -> LightColorScheme
    }

    // This is the key: We provide your new custom colors to the app
    CompositionLocalProvider(LocalAppColors provides LightAppColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}