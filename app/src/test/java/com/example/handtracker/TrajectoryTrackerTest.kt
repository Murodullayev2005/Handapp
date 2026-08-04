package com.example.handtracker

import com.example.handtracker.domain.TrackingTarget
import com.example.handtracker.domain.TrajectoryTracker
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrajectoryTrackerTest {

    @Test
    fun `addLandmarks adds trajectory points for index fingertip`() {
        val tracker = TrajectoryTracker(trackingTarget = TrackingTarget.INDEX_FINGER_TIP)

        // Mock 21 landmarks where landmark 8 is index tip (0.5, 0.5)
        val landmarks = (0 until 21).map { index ->
            if (index == 8) NormalizedLandmark.create(0.5f, 0.5f, 0.0f)
            else NormalizedLandmark.create(0.1f, 0.1f, 0.0f)
        }

        tracker.addLandmarks(0, landmarks)

        val activeTrajectories = tracker.getActiveTrajectories()
        assertNotNull(activeTrajectories[0])
        assertEquals(1, activeTrajectories[0]?.size)
        assertEquals(0.5f, activeTrajectories[0]!![0].x, 0.01f)
        assertEquals(0.5f, activeTrajectories[0]!![0].y, 0.01f)
    }

    @Test
    fun `purgeOldPoints removes expired points`() {
        val tracker = TrajectoryTracker(fadeDurationMs = 1000L)

        val landmarks = (0 until 21).map { NormalizedLandmark.create(0.5f, 0.5f, 0.0f) }
        tracker.addLandmarks(0, landmarks)

        // Purge points older than 1000ms by passing future timestamp
        val futureTime = System.currentTimeMillis() + 5000L
        tracker.purgeOldPoints(futureTime)

        val activeTrajectories = tracker.getActiveTrajectories()
        assertTrue(activeTrajectories.isEmpty())
    }
}
