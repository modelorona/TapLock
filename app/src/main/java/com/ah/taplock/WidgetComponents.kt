package com.ah.taplock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun WidgetStylePreview(
    style: TapLockWidgetStyle,
    showIcon: Boolean,
    iconBitmap: ImageBitmap,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val fillColor = when (style) {
        TapLockWidgetStyle.TRANSPARENT -> Color.Transparent
        TapLockWidgetStyle.GLASS -> Color(0x660F172A)
        TapLockWidgetStyle.SOLID -> Color(0xDD111827)
        TapLockWidgetStyle.OUTLINE -> Color.Transparent
    }
    val borderColor = when (style) {
        TapLockWidgetStyle.GLASS -> Color.White.copy(alpha = 0.18f)
        TapLockWidgetStyle.OUTLINE -> Color.White.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(172.dp)
                .height(84.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(26.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fillColor, shape)
                    .then(
                        if (borderColor != Color.Transparent) {
                            Modifier.border(1.dp, borderColor, shape)
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (showIcon) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = null,
                        modifier = Modifier.size(42.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingButtonPreview(
    iconBitmap: ImageBitmap,
    sizeDp: Float,
    opacityPercent: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .alpha(opacityPercent / 100f)
                .background(Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                modifier = Modifier.size((sizeDp * 0.68f).dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
