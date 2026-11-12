package ir.miladmedia.fdplayer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ir.miladmedia.fdplayer.R

val VazirFamily = FontFamily(
    Font(R.font.vazirmatn_light, FontWeight.Light),
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

private val Base = Typography()

val FdTypography = Typography(
    displaySmall = Base.displaySmall.copy(fontFamily = VazirFamily),
    headlineLarge = Base.headlineLarge.copy(fontFamily = VazirFamily),
    headlineMedium = Base.headlineMedium.copy(fontFamily = VazirFamily),
    headlineSmall = Base.headlineSmall.copy(fontFamily = VazirFamily, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontFamily = VazirFamily, fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontFamily = VazirFamily, fontWeight = FontWeight.Medium),
    titleSmall = Base.titleSmall.copy(fontFamily = VazirFamily, fontWeight = FontWeight.Medium),
    bodyLarge = Base.bodyLarge.copy(fontFamily = VazirFamily),
    bodyMedium = Base.bodyMedium.copy(fontFamily = VazirFamily, fontSize = 14.sp),
    bodySmall = Base.bodySmall.copy(fontFamily = VazirFamily),
    labelLarge = Base.labelLarge.copy(fontFamily = VazirFamily, fontWeight = FontWeight.Medium),
    labelMedium = Base.labelMedium.copy(fontFamily = VazirFamily),
    labelSmall = Base.labelSmall.copy(fontFamily = VazirFamily)
)
