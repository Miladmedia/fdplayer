package ir.miladmedia.fdplayer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import ir.miladmedia.fdplayer.domain.model.ThemeMode
import kotlin.random.Random

private val Primary = Color(0xFF5B4BDB)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFFE4DFFF)
private val OnPrimaryContainer = Color(0xFF1A0F6B)
private val Secondary = Color(0xFF00897B)
private val Background = Color(0xFFFAF9FF)
private val Surface = Color(0xFFFFFFFF)
private val OnSurface = Color(0xFF1B1B21)

private val DarkPrimary = Color(0xFFC7BFFF)
private val DarkOnPrimary = Color(0xFF2A1D8F)
private val DarkPrimaryContainer = Color(0xFF412DC0)
private val DarkOnPrimaryContainer = Color(0xFFE4DFFF)
private val DarkSecondary = Color(0xFF4DB6AC)
private val DarkBackground = Color(0xFF121218)
private val DarkSurface = Color(0xFF1B1B21)
private val DarkOnSurface = Color(0xFFE5E1EA)

/** WCAG AA for normal text; every generated pair is nudged until it clears this. */
private const val MIN_CONTRAST = 4.5f

private val LightScheme: ColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    onSurface = OnSurface
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface
)

/** WCAG relative luminance ratio between two colours, always >= 1. */
private fun contrastRatio(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

/**
 * Walks [foreground] towards the end of the scale (white in dark mode, black in light
 * mode) until it clears [MIN_CONTRAST] against [background]. Convergence is guaranteed
 * because pure white on black is 21:1, so the loop always terminates at the last step.
 */
private fun ensureContrast(background: Color, foreground: Color, dark: Boolean): Color {
    if (contrastRatio(background, foreground) >= MIN_CONTRAST) return foreground
    val target = if (dark) Color.White else Color.Black
    var candidate = foreground
    for (step in 1..10) {
        candidate = lerp(foreground, target, step / 10f)
        if (contrastRatio(background, candidate) >= MIN_CONTRAST) return candidate
    }
    return target
}

/** Picks whichever of the light/dark option reads better on [background]. */
private fun readableOn(background: Color): Color =
    if (contrastRatio(background, Color.White) >= contrastRatio(background, Color.Black)) {
        Color.White
    } else {
        Color.Black
    }

/** Builds a random but always legible palette for the requested brightness. */
private fun randomScheme(dark: Boolean, seed: Int): ColorScheme {
    val random = Random(seed)

    // Hue pairs are drawn far apart so primary and secondary never look identical.
    val primaryHue = random.nextFloat() * 360f
    val secondaryHue = (primaryHue + 120f + random.nextFloat() * 120f) % 360f

    val background = if (dark) {
        Color.hsl(primaryHue, 0.14f, 0.07f)
    } else {
        Color.hsl(primaryHue, 0.28f, 0.97f)
    }
    val surface = if (dark) {
        Color.hsl(primaryHue, 0.16f, 0.11f)
    } else {
        Color.hsl(primaryHue, 0.18f, 0.99f)
    }

    val primary = ensureContrast(
        background = background,
        foreground = if (dark) Color.hsl(primaryHue, 0.62f, 0.72f)
        else Color.hsl(primaryHue, 0.68f, 0.42f),
        dark = dark
    )
    val secondary = ensureContrast(
        background = background,
        foreground = if (dark) Color.hsl(secondaryHue, 0.55f, 0.70f)
        else Color.hsl(secondaryHue, 0.62f, 0.36f),
        dark = dark
    )

    val onPrimary = readableOn(primary)
    val onSecondary = readableOn(secondary)

    val primaryContainer = if (dark) {
        Color.hsl(primaryHue, 0.55f, 0.30f)
    } else {
        Color.hsl(primaryHue, 0.85f, 0.90f)
    }
    val secondaryContainer = if (dark) {
        Color.hsl(secondaryHue, 0.48f, 0.28f)
    } else {
        Color.hsl(secondaryHue, 0.75f, 0.90f)
    }
    val onPrimaryContainer = ensureContrast(
        background = primaryContainer,
        foreground = if (dark) Color.hsl(primaryHue, 0.95f, 0.93f)
        else Color.hsl(primaryHue, 0.90f, 0.16f),
        dark = dark
    )
    val onSecondaryContainer = ensureContrast(
        background = secondaryContainer,
        foreground = if (dark) Color.hsl(secondaryHue, 0.90f, 0.93f)
        else Color.hsl(secondaryHue, 0.90f, 0.16f),
        dark = dark
    )

    val onBackground = ensureContrast(
        background = background,
        foreground = if (dark) Color.hsl(primaryHue, 0.20f, 0.90f)
        else Color.hsl(primaryHue, 0.30f, 0.14f),
        dark = dark
    )
    val onSurface = ensureContrast(
        background = surface,
        foreground = if (dark) Color.hsl(primaryHue, 0.18f, 0.88f)
        else Color.hsl(primaryHue, 0.28f, 0.15f),
        dark = dark
    )
    val outline = ensureContrast(
        background = background,
        foreground = if (dark) Color.hsl(primaryHue, 0.25f, 0.60f)
        else Color.hsl(primaryHue, 0.25f, 0.45f),
        dark = dark
    )

    val base = if (dark) darkColorScheme() else lightColorScheme()

    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        outline = outline
    )
}

@Composable
fun FdPlayerTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM, ThemeMode.RANDOM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    // Regenerated once per composition session, so the palette is random per launch
    // but stable while the process lives (no flicker on recomposition).
    val randomSeed = remember { Random.nextInt() }

    val scheme = when (themeMode) {
        ThemeMode.RANDOM -> randomScheme(dark = dark, seed = randomSeed)
        else -> if (dark) DarkScheme else LightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = FdTypography,
        content = content
    )
}
