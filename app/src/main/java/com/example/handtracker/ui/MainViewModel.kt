package com.example.handtracker.ui

import android.app.Application
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.handtracker.domain.DistanceEvaluation
import com.example.handtracker.domain.HandDistanceCalculator
import com.example.handtracker.domain.HandLandmarkerHelper
import com.example.handtracker.domain.ProximityState
import com.example.handtracker.domain.TrackingTarget
import com.example.handtracker.domain.TrajectoryPoint
import com.example.handtracker.domain.TrajectoryTracker
import com.example.handtracker.ui.components.TrajectoryColorTheme
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val handsDetectedCount: Int = 0,
    val inferenceTimeMs: Long = 0L,
    val fps: Int = 0,
    val distanceEvaluation: DistanceEvaluation = DistanceEvaluation(ProximityState.SAFE, 0f, 60, 0f),
    val trajectories: Map<Int, List<TrajectoryPoint>> = emptyMap(),
    val handLandmarksList: List<List<NormalizedLandmark>> = emptyList(),

    // User Configurable Preferences
    val warningThresholdRatio: Float = 0.38f,
    val fadeDurationMs: Long = 1500L,
    val strokeWidthPx: Float = 12f,
    val colorTheme: TrajectoryColorTheme = TrajectoryColorTheme.NEON_CYAN,
    val trackingTarget: TrackingTarget = TrackingTarget.INDEX_FINGER_TIP,
    val showSkeleton: Boolean = true,
    val delegate: Int = HandLandmarkerHelper.DELEGATE_GPU,
    val lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application), HandLandmarkerHelper.LandmarkerListener {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val distanceCalculator = HandDistanceCalculator()
    private val trajectoryTracker = TrajectoryTracker()
    var handLandmarkerHelper: HandLandmarkerHelper? = null
        private set

    // FPS calculation tracking
    private var frameCount = 0
    private var lastFpsTimestamp = SystemClock.uptimeMillis()

    init {
        initHelper()
    }

    fun initHelper() {
        handLandmarkerHelper?.clearHandLandmarker()
        handLandmarkerHelper = HandLandmarkerHelper(
            context = getApplication(),
            currentDelegate = _uiState.value.delegate,
            handLandmarkerHelperListener = this
        )
    }

    override fun onResults(resultBundle: HandLandmarkerHelper.ResultBundle) {
        viewModelScope.launch(Dispatchers.Default) {
            val now = SystemClock.uptimeMillis()

            // Calculate FPS
            frameCount++
            var currentFps = _uiState.value.fps
            if (now - lastFpsTimestamp >= 1000L) {
                currentFps = frameCount
                frameCount = 0
                lastFpsTimestamp = now
            }

            val result = resultBundle.results.firstOrNull()
            val landmarksList = result?.landmarks() ?: emptyList()
            val handsCount = landmarksList.size

            // Update Trajectory Tracker
            landmarksList.forEachIndexed { index, landmarks ->
                trajectoryTracker.addLandmarks(index, landmarks)
            }
            trajectoryTracker.purgeOldPoints(now)

            // Calculate Proximity Distance using primary hand (first hand)
            val primaryHandLandmarks = landmarksList.firstOrNull() ?: emptyList()
            distanceCalculator.warningThresholdRatio = _uiState.value.warningThresholdRatio
            val evaluation = distanceCalculator.evaluateHandProximity(primaryHandLandmarks)

            _uiState.update { state ->
                state.copy(
                    handsDetectedCount = handsCount,
                    inferenceTimeMs = resultBundle.inferenceTime,
                    fps = currentFps,
                    distanceEvaluation = evaluation,
                    trajectories = trajectoryTracker.getActiveTrajectories(),
                    handLandmarksList = landmarksList,
                    errorMessage = null
                )
            }
        }
    }

    override fun onError(error: String) {
        _uiState.update { it.copy(errorMessage = error) }
    }

    fun setWarningThreshold(ratio: Float) {
        _uiState.update { it.copy(warningThresholdRatio = ratio) }
    }

    fun setFadeDuration(durationMs: Long) {
        trajectoryTracker.fadeDurationMs = durationMs
        _uiState.update { it.copy(fadeDurationMs = durationMs) }
    }

    fun setStrokeWidth(widthPx: Float) {
        _uiState.update { it.copy(strokeWidthPx = widthPx) }
    }

    fun setColorTheme(theme: TrajectoryColorTheme) {
        _uiState.update { it.copy(colorTheme = theme) }
    }

    fun setTrackingTarget(target: TrackingTarget) {
        trajectoryTracker.trackingTarget = target
        trajectoryTracker.clearAll()
        _uiState.update { it.copy(trackingTarget = target, trajectories = emptyMap()) }
    }

    fun setShowSkeleton(show: Boolean) {
        _uiState.update { it.copy(showSkeleton = show) }
    }

    fun setDelegate(delegate: Int) {
        _uiState.update { it.copy(delegate = delegate) }
        initHelper()
    }

    fun toggleCamera() {
        val nextFacing = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        _uiState.update { it.copy(lensFacing = nextFacing) }
    }

    fun clearTrajectories() {
        trajectoryTracker.clearAll()
        _uiState.update { it.copy(trajectories = emptyMap()) }
    }

    override fun onCleared() {
        super.onCleared()
        handLandmarkerHelper?.clearHandLandmarker()
    }
}
