package com.ah.taplock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

private fun lockZoneFraction(lockZonePercent: Int): Float = (lockZonePercent / 100f).coerceIn(0.2f, 1f)

@Composable
fun LockScreenZonePreview(
    lockZonePercent: Int
) {
    val previewWidth = 120.dp
    val previewHeight = 220.dp
    val previewVerticalPadding = 16.dp
    val previewInnerHeight = 188.dp
    val highlightedHeight = previewInnerHeight * lockZoneFraction(lockZonePercent)
    val frameColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val zoneColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .testTag("lock_zone_preview")
                .width(previewWidth)
                .height(previewHeight)
                .border(1.dp, frameColor, shape)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                    shape
                )
                .padding(vertical = previewVerticalPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(highlightedHeight)
                        .background(
                            zoneColor,
                            RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
                        )
                )
            }
        }
    }
}

@Composable
fun LockScreenZoneLiveOverlay(
    lockZonePercent: Int
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lock_zone_live_overlay")
    ) {
        val highlightedHeight = maxHeight * lockZoneFraction(lockZonePercent)
        val zoneColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        val zoneBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.16f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(highlightedHeight)
                .background(
                    zoneColor,
                    RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .border(
                    width = 1.dp,
                    color = zoneBorderColor,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
        )
    }
}
