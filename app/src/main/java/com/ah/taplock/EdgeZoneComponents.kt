package com.ah.taplock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun EdgeZonePreview(
    leftEnabled: Boolean,
    rightEnabled: Boolean,
    edgeWidthDp: Int,
    topOffsetPercent: Int,
    bottomOffsetPercent: Int,
    topLeftCornerEnabled: Boolean,
    topRightCornerEnabled: Boolean,
    bottomLeftCornerEnabled: Boolean,
    bottomRightCornerEnabled: Boolean,
    cornerSizeDp: Int
) {
    val previewZoneWidth = (
        6f + (edgeWidthDp.toFloat() / TapLockEdgeZones.MAX_WIDTH_DP.toFloat()) * 18f
        ).dp
    val previewCornerSize = (
        8f + (cornerSizeDp.toFloat() / TapLockEdgeZones.MAX_CORNER_SIZE_DP.toFloat()) * 24f
        ).dp
    val topOffsetFraction = (topOffsetPercent / 100f)
        .coerceIn(
            TapLockEdgeZones.MIN_OFFSET_PERCENT / 100f,
            TapLockEdgeZones.MAX_OFFSET_PERCENT / 100f
        )
    val bottomOffsetFraction = (bottomOffsetPercent / 100f)
        .coerceIn(
            TapLockEdgeZones.MIN_OFFSET_PERCENT / 100f,
            TapLockEdgeZones.MAX_OFFSET_PERCENT / 100f
        )
    val zoneColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val frameColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val shape = RoundedCornerShape(24.dp)
    val previewWidth = 120.dp
    val previewHeight = 220.dp
    val previewVerticalPadding = 16.dp
    val previewInnerHeight = 188.dp
    val topInset = previewInnerHeight * topOffsetFraction
    val bottomInset = previewInnerHeight * bottomOffsetFraction

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
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
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = topInset, bottom = bottomInset)
                ) {
                    if (leftEnabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(previewZoneWidth)
                                .background(
                                    zoneColor,
                                    RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                                )
                        )
                    }

                    if (rightEnabled) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(previewZoneWidth)
                                .background(
                                    zoneColor,
                                    RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                                )
                        )
                    }
                }

                if (topLeftCornerEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(previewCornerSize)
                            .background(
                                zoneColor,
                                RoundedCornerShape(bottomEnd = 12.dp)
                            )
                    )
                }

                if (topRightCornerEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(previewCornerSize)
                            .background(
                                zoneColor,
                                RoundedCornerShape(bottomStart = 12.dp)
                            )
                    )
                }

                if (bottomLeftCornerEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(previewCornerSize)
                            .background(
                                zoneColor,
                                RoundedCornerShape(topEnd = 12.dp)
                            )
                    )
                }

                if (bottomRightCornerEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(previewCornerSize)
                            .background(
                                zoneColor,
                                RoundedCornerShape(topStart = 12.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun EdgeZoneLiveOverlay(
    leftEnabled: Boolean,
    rightEnabled: Boolean,
    edgeWidthDp: Int,
    topOffsetPercent: Int,
    bottomOffsetPercent: Int,
    topLeftCornerEnabled: Boolean,
    topRightCornerEnabled: Boolean,
    bottomLeftCornerEnabled: Boolean,
    bottomRightCornerEnabled: Boolean,
    cornerSizeDp: Int
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("edge_zone_live_overlay")
    ) {
        val topInset = maxHeight * (topOffsetPercent / 100f)
        val bottomInset = maxHeight * (bottomOffsetPercent / 100f)
        val zoneColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        val zoneBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.06f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topInset, bottom = bottomInset)
            ) {
                if (leftEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .width(edgeWidthDp.dp)
                            .background(
                                zoneColor,
                                RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = zoneBorderColor,
                                shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            )
                    )
                }

                if (rightEnabled) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(edgeWidthDp.dp)
                            .background(
                                zoneColor,
                                RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = zoneBorderColor,
                                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            )
                    )
                }
            }

            if (topLeftCornerEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(cornerSizeDp.dp)
                        .background(
                            zoneColor,
                            RoundedCornerShape(bottomEnd = 20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = zoneBorderColor,
                            shape = RoundedCornerShape(bottomEnd = 20.dp)
                        )
                )
            }

            if (topRightCornerEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(cornerSizeDp.dp)
                        .background(
                            zoneColor,
                            RoundedCornerShape(bottomStart = 20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = zoneBorderColor,
                            shape = RoundedCornerShape(bottomStart = 20.dp)
                        )
                )
            }

            if (bottomLeftCornerEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(cornerSizeDp.dp)
                        .background(
                            zoneColor,
                            RoundedCornerShape(topEnd = 20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = zoneBorderColor,
                            shape = RoundedCornerShape(topEnd = 20.dp)
                        )
                )
            }

            if (bottomRightCornerEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(cornerSizeDp.dp)
                        .background(
                            zoneColor,
                            RoundedCornerShape(topStart = 20.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = zoneBorderColor,
                            shape = RoundedCornerShape(topStart = 20.dp)
                        )
                )
            }
        }
    }
}
