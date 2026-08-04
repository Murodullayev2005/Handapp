package com.example.handtracker.ui.components

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.handtracker.domain.TrajectoryPoint
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

enum class TrajectoryColorTheme(val displayName: String, val primaryColor: Color, val glowColor: Color) {
    NEON_CYAN("Neon Cyan", Color(0xFF00F0FF), Color(0x8000F0FF)),
    GLOWING_PURPLE("Glowing Purple", Color(0xFFD000FF), Color(0x80D000FF)),
    ELECTRIC_GREEN("Electric Green", Color(0xFF00FF66), Color(0x8000FF66)),
    FIRE_RED("Fire Red", Color(0xFFFF3333), Color(0x80FF3333)),
    SOLAR_GOLD("Solar Gold", Color(0xFFFFB800), Color(0x80FFB800))
}

// Hand connections for drawing full hand skeleton mesh
private val HAND_CONNECTIONS = listOf(
    // Wrist to fingers
    Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),        // Thumb
    Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),        // Index
    Pair(5, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),   // Middle
    Pair(9, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16), // Ring
    Pair(13, 17), Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20) // Pinky
)

@Composable
fun HandTrajectoryOverlay(
    trajectories: Map<Int, List<TrajectoryPoint>>,
    handLandmarksList: List<List<NormalizedLandmark>>,
    fadeDurationMs: Long,
    strokeWidthPx: Float,
    colorTheme: TrajectoryColorTheme,
    showSkeleton: Boolean,
    modifier: Modifier = Modifier
) {
    val currentTime = SystemClock.uptimeMillis()

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Optional Hand Skeleton Mesh Overlay
        if (showSkeleton) {
            for (landmarks in handLandmarksList) {
                if (landmarks.size < 21) continue

                // Draw bones
                for ((startIndex, endIndex) in HAND_CONNECTIONS) {
                    val startLm = landmarks[startIndex]
                    val endLm = landmarks[endIndex]
                    val startPt = Offset(startLm.x() * width, startLm.y() * height)
                    val endPt = Offset(endLm.x() * width, endLm.y() * height)

                    drawLine(
                        color = Color.White.copy(alpha = 0.45f),
                        start = startPt,
                        end = endPt,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw joint nodes
                for (lm in landmarks) {
                    val nodePt = Offset(lm.x() * width, lm.y() * height)
                    drawCircle(
                        color = colorTheme.primaryColor.copy(alpha = 0.85f),
                        radius = 6f,
                        center = nodePt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = nodePt
                    )
                }
            }
        }

        // 2. Trajectory Vector Path Overlay
        for ((_, points) in trajectories) {
            if (points.size < 2) continue

            // Render path in segments with fading opacity
            for (i in 0 until points.size - 1) {
                val pt1 = points[i]
                val pt2 = points[i + 1]

                val age1 = currentTime - pt1.timestampMs
                val age2 = currentTime - pt2.timestampMs
                val avgAge = (age1 + age2) / 2f
                val alpha = (1.0f - (avgAge / fadeDurationMs.toFloat())).coerceIn(0.0f, 1.0f)

                if (alpha <= 0.01f) continue

                val startOffset = Offset(pt1.x * width, pt1.y * height)
                val endOffset = Offset(pt2.x * width, pt2.y * height)

                // Outer ambient glow stroke
                drawLine(
                    color = colorTheme.glowColor.copy(alpha = alpha * 0.5f),
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = strokeWidthPx * 2.2f,
                    cap = StrokeCap.Round
                )

                // Inner primary neon stroke
                drawLine(
                    color = colorTheme.primaryColor.copy(alpha = alpha),
                    start = startOffset,
                    end = endOffset,
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // Draw bright glowing tip head on newest point
            val latestPoint = points.lastOrNull()
            if (latestPoint != null) {
                val age = currentTime - latestPoint.timestampMs
                val headAlpha = (1.0f - (age / fadeDurationMs.toFloat())).coerceIn(0.0f, 1.0f)
                if (headAlpha > 0.05f) {
                    val headOffset = Offset(latestPoint.x * width, latestPoint.y * height)
                    drawCircle(
                        color = colorTheme.primaryColor.copy(alpha = headAlpha),
                        radius = strokeWidthPx * 1.5f,
                        center = headOffset
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = headAlpha),
                        radius = strokeWidthPx * 0.7f,
                        center = headOffset
                    )
                }
            }
        }
    }
}
