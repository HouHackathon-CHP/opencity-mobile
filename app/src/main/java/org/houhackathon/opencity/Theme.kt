package org.houhackathon.opencity

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// OpenCity CyberCivic / Aurora Azure Color System
object OpenCityColors {
    val BrandNavy = Color(0xFF0F172A)
    val BrandNavyDark = Color(0xFF0B132B)
    val BrandNavyLight = Color(0xFF1E293B)

    val BrandAzure = Color(0xFF2563EB)
    val BrandAzureDark = Color(0xFF1D4ED8)
    val BrandAzureLight = Color(0xFFEFF6FF)
    val BrandAzureBorder = Color(0xFFBFDBFE)

    val BrandCyan = Color(0xFF06B6D4)
    val BrandCyanLight = Color(0xFFECFEFF)

    val BrandMint = Color(0xFF10B981)
    val BrandMintDark = Color(0xFF059669)
    val BrandMintLight = Color(0xFFECFDF5)
    val BrandMintBorder = Color(0xFFA7F3D0)

    val BrandAmber = Color(0xFFF59E0B)
    val BrandAmberDark = Color(0xFFD97706)
    val BrandAmberLight = Color(0xFFFFFBEB)
    val BrandAmberBorder = Color(0xFFFDE68A)

    val BrandRed = Color(0xFFEF4444)
    val BrandRedDark = Color(0xFFDC2626)
    val BrandRedLight = Color(0xFFFEF2F2)
    val BrandRedBorder = Color(0xFFFECACA)

    val SurfaceBg = Color(0xFFF8FAFC)
    val CardBg = Color(0xFFFFFFFF)

    val TextMain = Color(0xFF0F172A)
    val TextSecondary = Color(0xFF475569)
    val TextMuted = Color(0xFF64748B)
    val TextLight = Color(0xFF94A3B8)

    val BorderColor = Color(0xFFE2E8F0)
    val BorderDark = Color(0xFFCBD5E1)
}

val OpenCityShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val LightColorScheme = lightColorScheme(
    primary = OpenCityColors.BrandAzure,
    onPrimary = Color.White,
    primaryContainer = OpenCityColors.BrandAzureLight,
    onPrimaryContainer = OpenCityColors.BrandAzureDark,

    secondary = OpenCityColors.BrandMint,
    onSecondary = Color.White,
    secondaryContainer = OpenCityColors.BrandMintLight,
    onSecondaryContainer = OpenCityColors.BrandMintDark,

    tertiary = OpenCityColors.BrandCyan,
    onTertiary = Color.White,

    background = OpenCityColors.SurfaceBg,
    onBackground = OpenCityColors.TextMain,

    surface = OpenCityColors.CardBg,
    onSurface = OpenCityColors.TextMain,
    surfaceVariant = OpenCityColors.SurfaceBg,
    onSurfaceVariant = OpenCityColors.TextSecondary,

    error = OpenCityColors.BrandRed,
    onError = Color.White,
    errorContainer = OpenCityColors.BrandRedLight,
    onErrorContainer = OpenCityColors.BrandRedDark,

    outline = OpenCityColors.BorderColor,
    outlineVariant = OpenCityColors.BorderDark
)

@Composable
fun OpenCityTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        shapes = OpenCityShapes,
        content = content
    )
}
