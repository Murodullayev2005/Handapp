package com.example.handtracker.domain

import android.os.SystemClock
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

data class TrajectoryPoint(
    val x: Float, // Normalized 0.0 .. 1.0
    val y: Float, // Normalized 0.0 .. 1.0
    val timestampMs: Long = SystemClock.uptimeMillis()
)

enum class TrackingTarget {
    INDEX_FINGER_TIP, // Landmark 8
    PALM_CENTER,      // Average of Wrist #0, Index MCP #5, Pinky MCP #17
    WRIST             // Landmark 0
}

class TrajectoryTracker(
    var trackingTarget: TrackingTarget = TrackingTarget.INDEX_FINGER_TIP,
    var fadeDurationMs: Long = 1500L,
    var smoothingAlpha: Float = 0.45f // Exponential smoothing factor (0.0 = max smooth, 1.0 = raw)
) {
    private val pointsMap = mutableMapOf<Int, MutableList<TrajectoryPoint>>()
    private val lastSmoothedPoints = mutableMapOf<Int, Pair<Float, Float>>()

    fun addLandmarks(handIndex: Int, landmarks: List<NormalizedLandmark>) {
        if (landmarks.isEmpty()) return

        val rawTarget = when (trackingTarget) {
            TrackingTarget.INDEX_FINGER_TIP -> {
                val tip = landmarks[8]
                Pair(tip.x(), tip.y())
            }
            TrackingTarget.WRIST -> {
                val wrist = landmarks[0]
                Pair(wrist.x(), wrist.y())
            }
            TrackingTarget.PALM_CENTER -> {
                val w = landmarks[0]
                val indexMcp = landmarks[5]
                val pinkyMcp = landmarks[17]
                val cx = (w.x() + indexMcp.x() + pinkyMcp.x()) / 3f
                val cy = (w.y() + indexMcp.y() + pinkyMcp.y()) / 3f
                Pair(cx, cy)
            }
        }

        // Apply exponential moving average filter
        val previousSmoothed = lastSmoothedPoints[handIndex]
        val (finalX, finalY) = if (previousSmoothed != null) {
            val smoothedX = previousSmoothed.first + smoothingAlpha * (rawTarget.first - previousSmoothed.first)
            val smoothedY = previousSmoothed.second + smoothingAlpha * (rawTarget.second - previousSmoothed.second)
            Pair(smoothedX, smoothedY)
        } else {
            rawTarget
        }

        lastSmoothedPoints[handIndex] = Pair(finalX, finalY)

        val list = pointsMap.getOrPut(handIndex) { mutableListOf() }
        list.add(TrajectoryPoint(finalX, finalY))
    }

    fun purgeOldPoints(currentTimeMs: Long = SystemClock.uptimeMillis()) {
        val iterator = pointsMap.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val list = entry.value
            list.removeAll { point -> (currentTimeMs - point.timestampMs) > fadeDurationMs }
            if (list.isEmpty()) {
                iterator.remove()
                lastSmoothedPoints.remove(entry.key)
            }
        }
    }

    fun clearAll() {
        pointsMap.clear()
        lastSmoothedPoints.clear()
    }

    fun getActiveTrajectories(): Map<Int, List<TrajectoryPoint>> {
        return pointsMap.toMap()
    }
}
