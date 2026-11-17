package com.fleetpulse.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// FleetPulse identity colours (tailwind blue-600 / violet-600 etc.)
val FleetBlue = Color(0xFF2563EB)
val FleetBlueLight = Color(0xFF60A5FA)
val FleetGreen = Color(0xFF16A34A)
val FleetOrange = Color(0xFFEA580C)
val FleetViolet = Color(0xFF7C3AED)
val FleetAmber = Color(0xFFF59E0B)
val FleetRed = Color(0xFFDC2626)

val ElectricColor = Color(0xFF0EA5E9)
val PetrolColor = Color(0xFFF97316)

private val LightColors = lightColorScheme(
    primary = FleetBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = FleetViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color.White,
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFEEF2F7),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = FleetRed,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColors = darkColorScheme(
    primary = FleetBlueLight,
    onPrimary = Color(0xFF0B1220),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFF2DD4BF),
    onTertiary = Color(0xFF042F2E),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

private val FleetTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold),
        bodySmall = bodySmall.copy(fontSize = 12.sp),
        labelSmall = labelSmall.copy(letterSpacing = 0.4.sp, fontWeight = FontWeight.SemiBold),
    )
}

val MonoNumbers = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

@Composable
fun FleetPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = FleetTypography,
        content = content,
    )
}
