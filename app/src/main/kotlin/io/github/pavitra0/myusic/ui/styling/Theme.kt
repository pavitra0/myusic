package io.github.pavitra0.myusic.ui.styling

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

fun Bitmap.extractSeedColor(): Color {
    val scaled = Bitmap.createScaledBitmap(this, 32, 32, false)
    var maxSat = -1f
    var bestColor = Color(0xFF6750A4)
    val hsv = FloatArray(3)
    for (x in 0 until scaled.width) {
        for (y in 0 until scaled.height) {
            val pixel = scaled.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            android.graphics.Color.RGBToHSV(r, g, b, hsv)
            if (hsv[1] > maxSat && hsv[2] > 0.2f && hsv[2] < 0.9f) {
                maxSat = hsv[1]
                bestColor = Color(r, g, b)
            }
        }
    }
    return bestColor
}

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Color? = null,
    pureBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val effectiveDark = darkTheme || pureBlack

    val baseScheme = when {
        seedColor != null -> {
            rememberDynamicColorScheme(
                primary = seedColor,
                isDark = effectiveDark,
                isAmoled = false, // We handle AMOLED ourselves below for full control
                style = PaletteStyle.TonalSpot
            )
        }

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (effectiveDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        effectiveDark -> darkScheme

        else -> lightScheme
    }

    val colorScheme = if (effectiveDark && pureBlack) {
        baseScheme.copy(
            background = Color.Black,
            onBackground = Color.White,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceBright = Color(0xFF1C1C1C),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color.Black,
            surfaceContainer = Color.Black,
            surfaceContainerHigh = Color(0xFF0A0A0A),
            surfaceContainerHighest = Color(0xFF111111),
            surfaceVariant = Color(0xFF111111),
            inverseSurface = Color.White
        )
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}