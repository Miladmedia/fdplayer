package ir.miladmedia.fdplayer.domain.model

enum class AppThemeMode(val key: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system"),
    CUSTOM("custom");

    companion object {
        fun fromKey(key: String?): AppThemeMode =
            entries.find { it.key == key } ?: DARK
    }
}

data class CustomPaletteOption(
    val index: Int,
    val nameFa: String,
    val nameEn: String,
    val primaryHex: Long,
    val secondaryHex: Long,
    val backgroundHex: Long,
    val surfaceHex: Long
)

val CustomPalettes: List<CustomPaletteOption> = listOf(
    CustomPaletteOption(0, "نئون لاوا", "Neon Lava", 0xFF7F5AF0, 0xFF2CB67D, 0xFF0F0E17, 0xFF1A1829),
    CustomPaletteOption(1, "اقیانوس عمیق", "Deep Ocean", 0xFF00B4D8, 0xFF90E0EF, 0xFF03045E, 0xFF0A1142),
    CustomPaletteOption(2, "غروب کویر", "Desert Sunset", 0xFFFF6B6B, 0xFFFFD166, 0xFF1F1218, 0xFF2E1B24),
    CustomPaletteOption(3, "جنگل شمال", "Hyrcanian Forest", 0xFF52B788, 0xFFB7E4C7, 0xFF081C15, 0xFF132A21),
    CustomPaletteOption(4, "یاقوت بنفش", "Royal Amethyst", 0xFFC77DFF, 0xFFE0AAFF, 0xFF10002B, 0xFF240046),
    CustomPaletteOption(5, "طلای شب", "Midnight Gold", 0xFFFFB703, 0xFFFB8500, 0xFF121212, 0xFF1E1E1E),
    CustomPaletteOption(6, "رز روشن", "Soft Rose", 0xFFD63384, 0xFFF783AC, 0xFFFFF0F6, 0xFFFFDEEB),
    CustomPaletteOption(7, "آسمان صبح", "Morning Sky", 0xFF0284C7, 0xFF38BDF8, 0xFFF0F9FF, 0xFFE0F2FE)
)

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val customPaletteIndex: Int = 0,
    val micRhythmConsentGranted: Boolean = false,
    val micRhythmEnabled: Boolean = false,
    val languageTag: String = "fa"
)
