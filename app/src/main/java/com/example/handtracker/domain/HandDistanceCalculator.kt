package com.example.handtracker.domain

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class ProximityState {
    SAFE,
    WARNING,
    TOO_CLOSE
}

data class DistanceEvaluation(
    val proximityState: ProximityState,
    val closenessRatio: Float, // 0.0f (far) to 1.0f (extremely close)
    val estimatedDistanceCm: Int, // Rough visual estimate in cm (e.g. 15cm - 80cm)
    val boundingBoxAreaRatio: Float
)

class HandDistanceCalculator(
    var warningThresholdRatio: Float = 0.38f,
    var tooCloseThresholdRatio: Float = 0.52f
) {

    fun evaluateHandProximity(landmarks: List<NormalizedLandmark>): DistanceEvaluation {
        if (landmarks.isEmpty()) {
            return DistanceEvaluation(
                proximityState = ProximityState.SAFE,
                closenessRatio = 0f,
                estimatedDistanceCm = 60,
                boundingBoxAreaRatio = 0f
            )
        }

        var minX = 1.0f
        var maxX = 0.0f
        var minY = 1.0f
        var maxY = 0.0f

        for (lm in landmarks) {
            minX = min(minX, lm.x())
            maxX = max(maxX, lm.x())
            minY = min(minY, lm.y())
            maxY = max(maxY, lm.y())
        }

        val boxWidth = maxX - minX
        val boxHeight = maxY - minY
        val boundingBoxAreaRatio = boxWidth * boxHeight

        // Hand span length (Wrist landmark 0 to Middle Finger MCP landmark 9)
        val wrist = landmarks[0]
        val middleMcp = landmarks[9]
        val dx = middleMcp.x() - wrist.x()
        val dy = middleMcp.y() - wrist.y()
        val dz = middleMcp.z() - wrist.z()
        val handSpan3d = sqrt(dx * dx + dy * dy + dz * dz)

        // Combine bounding box size and hand span for robust scale estimate
        val combinedScale = (boxWidth * 0.4f) + (boxHeight * 0.4f) + (handSpan3d * 0.6f)

        // Map combined scale to a normalized 0.0 .. 1.0 closeness ratio
        val minExpectedScale = 0.12f
        val maxExpectedScale = 0.65f
        val closenessRatio = ((combinedScale - minExpectedScale) / (maxExpectedScale - minExpectedScale))
            .coerceIn(0.0f, 1.0f)

        // Estimated distance in cm (rough camera perspective mapping)
        // Scale factor: close = ~15cm, far = ~70cm
        val estimatedDistanceCm = (70f - (closenessRatio * 55f)).toInt().coerceIn(12, 80)

        val state = when {
            closenessRatio >= tooCloseThresholdRatio -> ProximityState.TOO_CLOSE
            closenessRatio >= warningThresholdRatio -> ProximityState.WARNING
            else -> ProximityState.SAFE
        }

        return DistanceEvaluation(
            proximityState = state,
            closenessRatio = closenessRatio,
            estimatedDistanceCm = estimatedDistanceCm,
            boundingBoxAreaRatio = boundingBoxAreaRatio
        )
    }
}
