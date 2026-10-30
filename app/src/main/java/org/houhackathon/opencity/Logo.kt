package org.houhackathon.opencity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Concept 1: "Nexus Portal" Vector Mark drawn natively in Jetpack Compose.
 * Dynamic gradient ring, gateway portal, and civic pulse indicator.
 */
@Composable
fun NexusPortalMark(
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val ringBrush = Brush.linearGradient(
        colors = listOf(
            OpenCityColors.BrandAzure,
            OpenCityColors.BrandCyan,
            OpenCityColors.BrandMint
        )
    )
    val coreBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF38BDF8),
            OpenCityColors.BrandAzure
        )
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val scale = w / 64f

        // 1. Outer Open Civic Ring (Stylized 'O' and 'C')
        val strokeWidth = 5.5f * scale
        val radius = 24f * scale
        val center = Offset(32f * scale, 32f * scale)

        drawArc(
            brush = ringBrush,
            startAngle = -45f,
            sweepAngle = 290f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 2. Inner Civic Gateway Tower (Path)
        val towerPath = Path().apply {
            moveTo(32f * scale, 17f * scale)
            lineTo(41f * scale, 26f * scale)
            lineTo(41f * scale, 43f * scale)
            lineTo(23f * scale, 43f * scale)
            lineTo(23f * scale, 26f * scale)
            close()
        }
        drawPath(path = towerPath, brush = coreBrush)

        // 3. Gateway Portal Opening Arch
        val archPath = Path().apply {
            moveTo(28f * scale, 43f * scale)
            lineTo(28f * scale, 34f * scale)
            cubicTo(
                28f * scale, 30.5f * scale,
                36f * scale, 30.5f * scale,
                36f * scale, 34f * scale
            )
            lineTo(36f * scale, 43f * scale)
            close()
        }
        drawPath(path = archPath, color = OpenCityColors.BrandNavyDark)

        // 4. Real-time Civic Pulse Indicator
        drawCircle(
            color = Color(0xFF34D399),
            radius = 2.5f * scale,
            center = Offset(32f * scale, 23.5f * scale)
        )
    }
}

@Composable
fun OpenCityLogo(
    size: Dp = 38.dp,
    showSubtitle: Boolean = true,
    isDarkTheme: Boolean = false,
    modifier: Modifier = Modifier
) {
    val mainColor = if (isDarkTheme) Color.White else OpenCityColors.TextMain
    val subColor = if (isDarkTheme) Color(0xFF94A3B8) else OpenCityColors.TextMuted

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NexusPortalMark(size = size)
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Open",
                    color = mainColor,
                    fontSize = (size.value * 0.52f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "City",
                    color = OpenCityColors.BrandAzure,
                    fontSize = (size.value * 0.52f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            }
            if (showSubtitle) {
                Text(
                    text = "CỔNG ĐÔ THỊ SỐ",
                    color = subColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }
    }
}
