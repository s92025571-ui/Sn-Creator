package com.sncreator.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sncreator.app.ui.theme.DeepRoyalViolet
import com.sncreator.app.ui.theme.ElectricViolet
import com.sncreator.app.ui.theme.NeonPurple
import com.sncreator.app.ui.theme.SoftLavenderGlow
import com.sncreator.app.ui.theme.VoidBlack

/**
 * Lightweight, hardware-accelerated 3D-inspired faceted "SN" gaming emblem.
 * Uses GPU compositor properties (rotationY, rotationZ, scale, alpha) to stay smooth on all Android phones.
 */
@Composable
fun SnLogo3D(
    size: Dp = 112.dp,
    animate3D: Boolean = false,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "sn_logo_3d")

    val rotationYDeg by transition.animateFloat(
        initialValue = if (animate3D) -22f else 0f,
        targetValue = if (animate3D) 22f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotationY"
    )

    val glowAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = if (animate3D) 0.90f else 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "SN Creator Logo" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    if (animate3D) {
                        rotationY = rotationYDeg
                        cameraDistance = 14f * density
                    }
                }
        ) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)

            // Ambient purple halo glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricViolet.copy(alpha = 0.42f * glowAlpha),
                        NeonPurple.copy(alpha = 0.16f * glowAlpha),
                        Color.Transparent
                    ),
                    center = center,
                    radius = w * 0.54f
                ),
                radius = w * 0.54f,
                center = center
            )

            // Outer 3D Hexagon Crest
            val hexPath = Path().apply {
                moveTo(w * 0.50f, h * 0.08f)
                lineTo(w * 0.86f, h * 0.28f)
                lineTo(w * 0.86f, h * 0.72f)
                lineTo(w * 0.50f, h * 0.92f)
                lineTo(w * 0.14f, h * 0.72f)
                lineTo(w * 0.14f, h * 0.28f)
                close()
            }

            drawPath(
                path = hexPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF21113F),
                        Color(0xFF0E0A1B),
                        VoidBlack
                    ),
                    start = Offset(w * 0.15f, h * 0.10f),
                    end = Offset(w * 0.85f, h * 0.90f)
                )
            )

            // Inner Bevel Rim
            val innerHex = Path().apply {
                moveTo(w * 0.50f, h * 0.14f)
                lineTo(w * 0.80f, h * 0.31f)
                lineTo(w * 0.80f, h * 0.69f)
                lineTo(w * 0.50f, h * 0.86f)
                lineTo(w * 0.20f, h * 0.69f)
                lineTo(w * 0.20f, h * 0.31f)
                close()
            }

            drawPath(
                path = innerHex,
                color = DeepRoyalViolet.copy(alpha = 0.45f),
                style = Stroke(width = w * 0.015f)
            )

            drawPath(
                path = hexPath,
                brush = Brush.linearGradient(
                    colors = listOf(SoftLavenderGlow, ElectricViolet, DeepRoyalViolet),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                style = Stroke(
                    width = w * 0.032f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Stylized Geometric 'S'
            val sPath = Path().apply {
                moveTo(w * 0.46f, h * 0.34f)
                lineTo(w * 0.30f, h * 0.34f)
                lineTo(w * 0.27f, h * 0.39f)
                lineTo(w * 0.27f, h * 0.47f)
                lineTo(w * 0.31f, h * 0.50f)
                lineTo(w * 0.43f, h * 0.52f)
                lineTo(w * 0.43f, h * 0.61f)
                lineTo(w * 0.28f, h * 0.61f)
                lineTo(w * 0.26f, h * 0.66f)
                lineTo(w * 0.45f, h * 0.66f)
                lineTo(w * 0.48f, h * 0.61f)
                lineTo(w * 0.48f, h * 0.49f)
                lineTo(w * 0.44f, h * 0.46f)
                lineTo(w * 0.32f, h * 0.44f)
                lineTo(w * 0.32f, h * 0.39f)
                lineTo(w * 0.47f, h * 0.39f)
                close()
            }

            // Stylized Geometric 'N'
            val nPath = Path().apply {
                moveTo(w * 0.53f, h * 0.66f)
                lineTo(w * 0.53f, h * 0.34f)
                lineTo(w * 0.58f, h * 0.34f)
                lineTo(w * 0.69f, h * 0.55f)
                lineTo(w * 0.69f, h * 0.34f)
                lineTo(w * 0.74f, h * 0.34f)
                lineTo(w * 0.74f, h * 0.66f)
                lineTo(w * 0.69f, h * 0.66f)
                lineTo(w * 0.58f, h * 0.45f)
                lineTo(w * 0.58f, h * 0.66f)
                close()
            }

            // 3D drop depth offset
            val depthOffset = w * 0.015f
            drawPath(
                path = Path().apply { addPath(sPath, Offset(depthOffset, depthOffset)) },
                color = DeepRoyalViolet.copy(alpha = 0.85f)
            )
            drawPath(
                path = Path().apply { addPath(nPath, Offset(depthOffset, depthOffset)) },
                color = DeepRoyalViolet.copy(alpha = 0.85f)
            )

            val letterBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFDF4FF), SoftLavenderGlow, ElectricViolet),
                startY = h * 0.32f,
                endY = h * 0.68f
            )

            drawPath(path = sPath, brush = letterBrush)
            drawPath(path = nPath, brush = letterBrush)
        }
    }
}
