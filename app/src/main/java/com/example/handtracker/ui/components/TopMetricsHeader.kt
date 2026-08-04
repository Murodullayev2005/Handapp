package com.example.handtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.handtracker.domain.DistanceEvaluation
import com.example.handtracker.domain.ProximityState

@Composable
fun TopMetricsHeader(
    handsDetectedCount: Int,
    inferenceTimeMs: Long,
    fps: Int,
    evaluation: DistanceEvaluation,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tracking Status Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xBB1A1D24))
                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (handsDetectedCount > 0) Color(0xFF00FF66) else Color(
                                0xFFFF3366
                            )
                        )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (handsDetectedCount > 0) "$handsDetectedCount Hand${if (handsDetectedCount > 1) "s" else ""}" else "Searching...",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // FPS & Latency Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xBB1A1D24))
                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$fps FPS | ${inferenceTimeMs}ms",
                color = Color(0xFF00E5FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Proximity Status Indicator
        val (stateColor, stateLabel) = when (evaluation.proximityState) {
            ProximityState.SAFE -> Pair(Color(0xFF00FF66), "Safe")
            ProximityState.WARNING -> Pair(Color(0xFFFFBB00), "Caution")
            ProximityState.TOO_CLOSE -> Pair(Color(0xFFFF3333), "Too Close")
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xBB1A1D24))
                .border(1.dp, stateColor.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = stateLabel,
                color = stateColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Settings Button
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0xBB1A1D24))
                .border(1.dp, Color(0x40FFFFFF), CircleShape)
                .clickable { onOpenSettings() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
