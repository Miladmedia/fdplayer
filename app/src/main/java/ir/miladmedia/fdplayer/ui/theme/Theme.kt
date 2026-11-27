package ir.miladmedia.fdplayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import ir.miladmedia.fdplayer.domain.model.AppThemeMode
import ir.miladmedia.fdplayer.domain.model.CustomPalettes

@Composable
fun FDPlayerTheme(
    themeMode: AppThemeMode,
    customPaletteIndex: Int,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()

    val colorScheme = when (themeMode) {
        AppThemeMode.DARK -> {
            val bg = DarkBg
            val surface = DarkSurface
            darkColorScheme(
                primary = ensureContrast(LavaViolet, surface, 3.2f),
                onPrimary = ensureContrast(Color.White, LavaViolet, 4.5f),
                secondary = ensureContrast(LavaCyan, surface, 3.2f),
                onSecondary = ensureContrast(Color(0xFF071318), LavaCyan, 4.5f),
                tertiary = LavaCoral,
                background = bg,
                onBackground = ensureContrast(Color(0xFFF1F5F9), bg, 7.0f),
                surface = surface,
                onSurface = ensureContrast(Color(0xFFF1F5F9), surface, 7.0f),
                surfaceVariant = Color(0xFF1F2742),
                onSurfaceVariant = ensureContrast(Color(0xFFCBD5E1), Color(0xFF1F2742), 4.5f)
            )
        }

        AppThemeMode.LIGHT -> {
            val bg = LightBg
            val surface = LightSurface
            lightColorScheme(
                primary = ensureContrast(Color(0xFF5B3CC4), surface, 4.5f),
                onPrimary = Color.White,
                secondary = ensureContrast(Color(0xFF007A8C), surface, 4.5f),
                onSecondary = Color.White,
                tertiary = Color(0xFFD6336C),
                background = bg,
                onBackground = ensureContrast(Color(0xFF0F172A), bg, 7.0f),
                surface = surface,
                onSurface = ensureContrast(Color(0xFF0F172A), surface, 7.0f),
                surfaceVariant = Color(0xFFE2E8F0),
                onSurfaceVariant = ensureContrast(Color(0xFF334155), Color(0xFFE2E8F0), 4.5f)
            )
        }

        AppThemeMode.SYSTEM -> {
            if (systemDark) {
                darkColorScheme(
                    primary = LavaViolet,
                    onPrimary = Color.White,
                    secondary = LavaCyan,
                    onSecondary = Color(0xFF071318),
                    background = DarkBg,
                    onBackground = ensureContrast(Color.White, DarkBg, 7.0f),
                    surface = DarkSurface,
                    onSurface = ensureContrast(Color.White, DarkSurface, 7.0f),
                    surfaceVariant = Color(0xFF1F2742),
                    onSurfaceVariant = ensureContrast(Color(0xFFCBD5E1), Color(0xFF1F2742), 4.5f)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF5B3CC4),
                    onPrimary = Color.White,
                    secondary = Color(0xFF007A8C),
                    onSecondary = Color.White,
                    background = LightBg,
                    onBackground = ensureContrast(Color(0xFF0F172A), LightBg, 7.0f),
                    surface = LightSurface,
                    onSurface = ensureContrast(Color(0xFF0F172A), LightSurface, 7.0f),
                    surfaceVariant = Color(0xFFE2E8F0),
                    onSurfaceVariant = ensureContrast(Color(0xFF334155), Color(0xFFE2E8F0), 4.5f)
                )
            }
        }

        AppThemeMode.CUSTOM -> {
            val palette = CustomPalettes.getOrElse(customPaletteIndex) { CustomPalettes.first() }
            val primary = Color(palette.primaryHex)
            val secondary = Color(palette.secondaryHex)
            val bg = Color(palette.backgroundHex)
            val surface = Color(palette.surfaceHex)
            val isDarkBg = bg.luminance() < 0.5f
            val baseText = if (isDarkBg) Color(0xFFF8FAFC) else Color(0xFF0F172A)
            val variantSurface = if (isDarkBg) {
                surface.copy(alpha = 0.85f)
            } else {
                Color(0xFFE2E8F0)
            }

            if (isDarkBg) {
                darkColorScheme(
                    primary = ensureContrast(primary, surface, 3.2f),
                    onPrimary = ensureContrast(Color.White, primary, 4.5f),
                    secondary = ensureContrast(secondary, surface, 3.2f),
                    onSecondary = ensureContrast(Color(0xFF090D16), secondary, 4.5f),
                    background = bg,
                    onBackground = ensureContrast(baseText, bg, 7.0f),
                    surface = surface,
                    onSurface = ensureContrast(baseText, surface, 7.0f),
                    surfaceVariant = variantSurface,
                    onSurfaceVariant = ensureContrast(baseText.copy(alpha = 0.85f), surface, 4.5f)
                )
            } else {
                lightColorScheme(
                    primary = ensureContrast(primary, surface, 4.5f),
                    onPrimary = ensureContrast(Color.White, primary, 4.5f),
                    secondary = ensureContrast(secondary, surface, 4.5f),
                    onSecondary = ensureContrast(Color(0xFF090D16), secondary, 4.5f),
                    background = bg,
                    onBackground = ensureContrast(baseText, bg, 7.0f),
                    surface = surface,
                    onSurface = ensureContrast(baseText, surface, 7.0f),
                    surfaceVariant = variantSurface,
                    onSurfaceVariant = ensureContrast(baseText.copy(alpha = 0.85f), surface, 4.5f)
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FDPlayerTypography,
        content = content
    )
}
