// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val TrackOn = Color(0xFF34C759)
private val TrackOff = Color(0xFFE5E5EA)

/**
 * Công tắc viên tròn trượt kiểu iOS (bật xanh), tap target lớn cho người già.
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
        animationSpec = tween(180),
        label = "iosSwitchTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 32.dp else 0.dp,
        animationSpec = tween(180),
        label = "iosSwitchThumb",
    )

    Box(
        modifier = modifier
            .semantics {
                this.role = Role.Switch
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            }
            .size(width = 72.dp, height = 44.dp)
            .bounceClick(scaleDown = 0.94f, onClick = {
                if (enabled) onCheckedChange(!checked)
            }),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(trackColor)
                .padding(3.dp),
        ) {
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = thumbOffset)
                    .shadow(3.dp, CircleShape),
                shape = CircleShape,
                color = Color.White,
            ) {}
        }
    }
}
