package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Visual ParkEasy Brand Badge matching the app launcher icon reference design:
 * Dark obsidian glassmorphic squircle with clean white P emblem and emerald green leaf.
 */
@Composable
fun ParkEasyBrandBadge(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp
) {
    val cornerRadius = size * 0.28f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color(0x3310B981),
                spotColor = Color(0x4400E676)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF070B14),
                        Color(0xFF030712)
                    )
                )
            )
            .border(
                BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0x6610B981),
                            Color(0x330066FF),
                            Color(0x4400B4D8)
                        )
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "ParkEasy App Icon",
            tint = Color.Unspecified,
            modifier = Modifier.size(size * 1.05f)
        )
    }
}
