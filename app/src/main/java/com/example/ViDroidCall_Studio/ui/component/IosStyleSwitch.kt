// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val TrackOn = Color(0xFF34C759)
private val TrackOff = Color(0xFFE9E9EB)

private val TrackWidth = 51.dp
private val TrackHeight = 31.dp
private val ThumbSize = 27.dp
private val ThumbInset = 2.dp

/**
 * Công tắc kiểu iOS: viên trắng trượt trong thanh pill, bật xanh lá, không scale khi chạm.
 */
@Composable
fun IosStyleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) TrackOn else TrackOff,
        animationSpec = tween(200),
        label = "iosSwitchTrack",
    )
    val thumbTravel = TrackWidth - ThumbInset * 2 - ThumbSize
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) thumbTravel else 0.dp,
        animationSpec = tween(200),
        label = "iosSwitchThumb",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(width = 51.dp, height = 44.dp)
            .semantics {
                this.role = Role.Switch
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                indication = null,
                interactionSource = interactionSource,
            ) {
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(TrackWidth, TrackHeight)
                .background(trackColor, RoundedCornerShape(percent = 50)),
        ) {
            Box(
                modifier = Modifier
                    .padding(start = ThumbInset, top = ThumbInset)
                    .offset(x = thumbOffset)
                    .size(ThumbSize)
                    .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
                    .background(Color.White, CircleShape),
            )
        }
    }
}
