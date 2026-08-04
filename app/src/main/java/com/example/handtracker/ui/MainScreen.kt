package com.example.handtracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.handtracker.ui.components.CameraPreviewView
import com.example.handtracker.ui.components.DistanceWarningBanner
import com.example.handtracker.ui.components.HandTrajectoryOverlay
import com.example.handtracker.ui.components.SettingsBottomSheet
import com.example.handtracker.ui.components.TopMetricsHeader

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. CameraX Preview Layer
        val helper = viewModel.handLandmarkerHelper
        if (helper != null) {
            CameraPreviewView(
                handLandmarkerHelper = helper,
                lensFacing = uiState.lensFacing
            )
        }

        // 2. Continuous Trajectory Canvas & Hand Skeleton Overlay
        HandTrajectoryOverlay(
            trajectories = uiState.trajectories,
            handLandmarksList = uiState.handLandmarksList,
            fadeDurationMs = uiState.fadeDurationMs,
            strokeWidthPx = uiState.strokeWidthPx,
            colorTheme = uiState.colorTheme,
            showSkeleton = uiState.showSkeleton
        )

        // 3. Top Header Bar & Distance Warning Notification
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            TopMetricsHeader(
                handsDetectedCount = uiState.handsDetectedCount,
                inferenceTimeMs = uiState.inferenceTimeMs,
                fps = uiState.fps,
                evaluation = uiState.distanceEvaluation,
                onOpenSettings = { showSettingsSheet = true }
            )

            DistanceWarningBanner(
                evaluation = uiState.distanceEvaluation
            )
        }

        // 4. Floating Action Button to Switch Front / Back Camera
        FloatingActionButton(
            onClick = { viewModel.toggleCamera() },
            containerColor = Color(0xBB1A1D24),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(54.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cameraswitch,
                contentDescription = "Switch Camera"
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 5. Settings Bottom Sheet Modal
        if (showSettingsSheet) {
            SettingsBottomSheet(
                warningThreshold = uiState.warningThresholdRatio,
                onWarningThresholdChange = viewModel::setWarningThreshold,
                fadeDurationMs = uiState.fadeDurationMs,
                onFadeDurationChange = viewModel::setFadeDuration,
                strokeWidthPx = uiState.strokeWidthPx,
                onStrokeWidthChange = viewModel::setStrokeWidth,
                colorTheme = uiState.colorTheme,
                onColorThemeChange = viewModel::setColorTheme,
                trackingTarget = uiState.trackingTarget,
                onTrackingTargetChange = viewModel::setTrackingTarget,
                showSkeleton = uiState.showSkeleton,
                onShowSkeletonChange = viewModel::setShowSkeleton,
                delegate = uiState.delegate,
                onDelegateChange = viewModel::setDelegate,
                onClearTrajectories = viewModel::clearTrajectories,
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}
