package ir.miladmedia.fdplayer.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import ir.miladmedia.fdplayer.ui.theme.LavaAmber
import ir.miladmedia.fdplayer.ui.theme.LavaCoral
import ir.miladmedia.fdplayer.ui.theme.LavaCyan
import ir.miladmedia.fdplayer.ui.theme.LavaViolet
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LavaLampBackground(
    dominantColors: List<Int>,
    isPlaying: Boolean,
    micRhythmActive: Boolean,
    micAmplitude: Float,
    modifier: Modifier = Modifier
) {
    val bgBase = MaterialTheme.colorScheme.background
    val paletteColors = if (dominantColors.size >= 3) {
        dominantColors.take(4).map { Color(it) }
    } else {
        listOf(LavaViolet, LavaCoral, LavaCyan, LavaAmber)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "lava_lamp")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 19000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )
    val simulatedPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "simulated_pulse"
    )

    val smoothedMicScale by animateFloatAsState(
        targetValue = if (micRhythmActive) (0.85f + micAmplitude * 0.65f) else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "mic_scale"
    )

    val dynamicScale = when {
        micRhythmActive -> smoothedMicScale
        isPlaying -> simulatedPulse
        else -> 0.92f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBase)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val baseRadius = minOf(w, h) * 0.44f * dynamicScale

            // Blob 1 (Top-Left / Floating)
            val c1 = Offset(
                x = w * 0.28f + cos(phase1) * (w * 0.14f),
                y = h * 0.26f + sin(phase1) * (h * 0.12f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        paletteColors[0].copy(alpha = 0.62f),
                        paletteColors[0].copy(alpha = 0.0f)
                    ),
                    center = c1,
                    radius = baseRadius * 1.15f
                ),
                radius = baseRadius * 1.15f,
                center = c1
            )

            // Blob 2 (Center-Right / Warm Core)
            val color2 = paletteColors.getOrElse(1) { LavaCoral }
            val c2 = Offset(
                x = w * 0.72f + sin(phase2) * (w * 0.16f),
                y = h * 0.44f + cos(phase1) * (h * 0.15f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color2.copy(alpha = 0.58f),
                        color2.copy(alpha = 0.0f)
                    ),
                    center = c2,
                    radius = baseRadius * 1.05f
                ),
                radius = baseRadius * 1.05f,
                center = c2
            )

            // Blob 3 (Lower-Center / Glow Accent)
            val color3 = paletteColors.getOrElse(2) { LavaCyan }
            val c3 = Offset(
                x = w * 0.48f + cos(phase2) * (w * 0.18f),
                y = h * 0.72f + sin(phase2) * (h * 0.10f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        color3.copy(alpha = 0.52f),
                        color3.copy(alpha = 0.0f)
                    ),
                    center = c3,
                    radius = baseRadius * 1.2f
                ),
                radius = baseRadius * 1.2f,
                center = c3
            )
        }
    }
}
