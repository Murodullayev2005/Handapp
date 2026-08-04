package com.example.handtracker

import com.example.handtracker.domain.HandDistanceCalculator
import com.example.handtracker.domain.ProximityState
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandDistanceCalculatorTest {

    @Test
    fun `empty landmarks list returns safe state`() {
        val calculator = HandDistanceCalculator()
        val result = calculator.evaluateHandProximity(emptyList())
        assertEquals(ProximityState.SAFE, result.proximityState)
        assertEquals(0f, result.closenessRatio, 0.01f)
    }

    @Test
    fun `far hand landmarks produce safe state`() {
        val calculator = HandDistanceCalculator(warningThresholdRatio = 0.40f)

        // Create small hand landmarks in 0.0 .. 0.1 bounds
        val landmarks = (0 until 21).map {
            NormalizedLandmark.create(0.4f + (it % 3) * 0.02f, 0.4f + (it / 3) * 0.02f, 0.0f)
        }

        val result = calculator.evaluateHandProximity(landmarks)
        assertEquals(ProximityState.SAFE, result.proximityState)
        assertTrue(result.closenessRatio < 0.40f)
    }

    @Test
    fun `large hand landmarks produce too close state`() {
        val calculator = HandDistanceCalculator(tooCloseThresholdRatio = 0.50f)

        // Create large hand landmarks occupying large frame span 0.1 .. 0.9
        val landmarks = (0 until 21).map {
            NormalizedLandmark.create(0.1f + (it % 3) * 0.35f, 0.1f + (it / 3) * 0.25f, 0.0f)
        }

        val result = calculator.evaluateHandProximity(landmarks)
        assertEquals(ProximityState.TOO_CLOSE, result.proximityState)
        assertTrue(result.closenessRatio >= 0.50f)
    }
}
