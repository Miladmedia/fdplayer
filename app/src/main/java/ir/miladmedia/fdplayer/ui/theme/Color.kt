package ir.miladmedia.fdplayer.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val LavaCyan = Color(0xFF29F2DF)
val LavaViolet = Color(0xFF7F5AF0)
val LavaCoral = Color(0xFFFF5E7E)
val LavaAmber = Color(0xFFFF9F1C)

val DarkBg = Color(0xFF0A0D18)
val DarkSurface = Color(0xFF14192B)
val LightBg = Color(0xFFF3F6FC)
val LightSurface = Color(0xFFFFFFFF)

/**
 * Ensures sufficient WCAG contrast ratio between [foreground] and [background].
 * Automatically adjusts lightness toward white or deep charcoal if contrast < [minRatio].
 */
fun ensureContrast(
    foreground: Color,
    background: Color,
    minRatio: Float = 4.5f
): Color {
    if (contrastRatio(foreground, background) >= minRatio) {
        return foreground
    }
    val bgLum = background.luminance()
    val target = if (bgLum < 0.5f) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    var step = 0.15f
    var candidate = foreground
    while (step <= 1.0f) {
        candidate = lerpColor(foreground, target, step)
        if (contrastRatio(candidate, background) >= minRatio) {
            return candidate
        }
        step += 0.15f
    }
    return target
}

private fun contrastRatio(c1: Color, c2: Color): Float {
    val l1 = c1.luminance()
    val l2 = c2.luminance()
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

private fun lerpColor(start: Color, stop: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (stop.red - start.red) * t,
        green = start.green + (stop.green - start.green) * t,
        blue = start.blue + (stop.blue - start.blue) * t,
        alpha = start.alpha + (stop.alpha - start.alpha) * t
    )
}
