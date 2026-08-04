package com.example.handtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.handtracker.domain.HandLandmarkerHelper
import com.example.handtracker.domain.TrackingTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    warningThreshold: Float,
    onWarningThresholdChange: (Float) -> Unit,
    fadeDurationMs: Long,
    onFadeDurationChange: (Long) -> Unit,
    strokeWidthPx: Float,
    onStrokeWidthChange: (Float) -> Unit,
    colorTheme: TrajectoryColorTheme,
    onColorThemeChange: (TrajectoryColorTheme) -> Unit,
    trackingTarget: TrackingTarget,
    onTrackingTargetChange: (TrackingTarget) -> Unit,
    showSkeleton: Boolean,
    onShowSkeletonChange: (Boolean) -> Unit,
    delegate: Int,
    onDelegateChange: (Int) -> Unit,
    onClearTrajectories: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14171F),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Tracking & Visual Settings",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Distance Warning Sensitivity Threshold
            Text(
                text = "Warning Distance Threshold: ${(warningThreshold * 100).toInt()}%",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = warningThreshold,
                onValueChange = onWarningThresholdChange,
                valueRange = 0.20f..0.65f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFFF9900),
                    activeTrackColor = Color(0xFFFF9900)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Trajectory Fade Duration
            Text(
                text = "Trajectory Fade Out: ${(fadeDurationMs / 1000f)}s",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = fadeDurationMs.toFloat(),
                onValueChange = { onFadeDurationChange(it.toLong()) },
                valueRange = 400f..3500f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00F0FF),
                    activeTrackColor = Color(0xFF00F0FF)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Line Width Slider
            Text(
                text = "Trajectory Line Thickness: ${strokeWidthPx.toInt()} px",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Slider(
                value = strokeWidthPx,
                onValueChange = onStrokeWidthChange,
                valueRange = 4f..28f,
                colors = SliderDefaults.colors(
                    thumbColor = colorTheme.primaryColor,
                    activeTrackColor = colorTheme.primaryColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Color Theme Selector
            Text(
                text = "Trajectory Color Palette",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TrajectoryColorTheme.entries.forEach { theme ->
                    val isSelected = theme == colorTheme
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(theme.primaryColor)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onColorThemeChange(theme) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. Tracking Target Selector
            Text(
                text = "Tracking Target Point",
                color = Color(0xFFCCCCCC),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TrackingTarget.entries.forEach { target ->
                    val isSelected = target == trackingTarget
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF222632))
                            .clickable { onTrackingTargetChange(target) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (target) {
                                TrackingTarget.INDEX_FINGER_TIP -> "Fingertip"
                                TrackingTarget.PALM_CENTER -> "Palm"
                                TrackingTarget.WRIST -> "Wrist"
                            },
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Show Skeleton Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hand Skeleton Overlay",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Draw translucent 21-joint mesh",
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = showSkeleton,
                    onCheckedChange = onShowSkeletonChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00FF66)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7. GPU Acceleration Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GPU Acceleration",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Use OpenCL/GLES for low latency",
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = delegate == HandLandmarkerHelper.DELEGATE_GPU,
                    onCheckedChange = { isChecked ->
                        onDelegateChange(
                            if (isChecked) HandLandmarkerHelper.DELEGATE_GPU
                            else HandLandmarkerHelper.DELEGATE_CPU
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF00E5FF)
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Clear Trajectories Button
            Button(
                onClick = {
                    onClearTrajectories()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A2028)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Clear Current Trajectory Line",
                    color = Color(0xFFFF5555),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
