// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ViDroidCall_Studio.R

/**
 * Component giao diện hiển thị bảng trợ lý nổi (TroLyNoiSheet).
 * Thiết kế tối giản, trực quan, loại bỏ toàn bộ thông số kỹ thuật nội bộ (model/GGUF/Fast-Path).
 */
@Composable
fun TroLyNoiSheet(
    data: AssistantOverlayData,
    modifier: Modifier = Modifier,
    onSheetClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSheetClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Khung thẻ nổi Google Assistant-style (28dp corners, translucent, shadow)
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color(0x33000000),
                    spotColor = Color(0x40000000)
                ),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFFFFFFFF).copy(alpha = 0.96f),
            border = BorderStroke(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Header thương hiệu: Logo lục giác xanh + Tên ViDroidCall (Không huy hiệu kỹ thuật)
                SheetHeader(isIdle = data.state == AssistantOverlayState.LISTENING)

                Spacer(modifier = Modifier.height(14.dp))

                // Nội dung động tương ứng từng trạng thái với hiệu ứng trượt dọc mượt mà
                AnimatedContent(
                    targetState = data.state,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(260)) + slideInVertically(
                            animationSpec = tween(300, easing = FastOutSlowInEasing),
                            initialOffsetY = { fullHeight -> fullHeight / 3 }
                        )).togetherWith(
                            fadeOut(animationSpec = tween(180)) + slideOutVertically(
                                animationSpec = tween(200, easing = FastOutSlowInEasing),
                                targetOffsetY = { fullHeight -> -fullHeight / 4 }
                            )
                        )
                    },
                    label = "sheet_state_transition"
                ) { targetState ->
                    when (targetState) {
                        AssistantOverlayState.LISTENING -> {
                            ListeningContent(
                                statusMessage = data.statusMessage,
                                recognizedText = data.recognizedText
                            )
                        }

                        AssistantOverlayState.STT -> {
                            RecognizedTextContent(
                                recognizedText = data.recognizedText
                            )
                        }

                        AssistantOverlayState.FAST_PATH -> {
                            RecognizedTextContent(
                                recognizedText = data.recognizedText
                            )
                        }

                        AssistantOverlayState.ANALYZING,
                        AssistantOverlayState.GGUF_LOADING -> {
                            AnalyzingContent(
                                recognizedText = data.recognizedText
                            )
                        }

                        AssistantOverlayState.CONFIRM_ACTION -> {
                            ConfirmActionContent(
                                recognizedText = data.recognizedText,
                                actionTitle = data.actionTitle,
                                actionDescription = data.actionDescription,
                                actionIconType = data.actionIconType,
                                onConfirm = data.onConfirm,
                                onCancel = data.onCancel
                            )
                        }

                        AssistantOverlayState.CONFIRM_CALL -> {
                            ConfirmActionContent(
                                recognizedText = data.recognizedText,
                                actionTitle = "Xác nhận cuộc gọi?",
                                actionDescription = "Gọi tới ${data.targetName.ifBlank { "liên hệ" }}",
                                actionIconType = OverlayActionIconType.CALL,
                                onConfirm = data.onConfirm,
                                onCancel = data.onCancel
                            )
                        }

                        AssistantOverlayState.MAP_CONFIRM -> {
                            ConfirmActionContent(
                                recognizedText = data.recognizedText,
                                actionTitle = "Xác nhận mở bản đồ?",
                                actionDescription = "Mở bản đồ chỉ đường tới ${data.targetName.ifBlank { "địa điểm yêu cầu" }}",
                                actionIconType = OverlayActionIconType.MAP,
                                onConfirm = data.onConfirm,
                                onCancel = data.onCancel
                            )
                        }
                    }
                }
            }
        }

        // Thanh điều hướng chỉ báo cử chỉ Android (Bottom Gesture Indicator Bar)
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .width(128.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(Color.White.copy(alpha = 0.7f))
        )
    }
}

/**
 * Header trên cùng của Overlay Sheet.
 * Hiển thị logo logo_ai.png nhịp thở nhẹ, tuyệt đối không có huy hiệu kỹ thuật.
 */
@Composable
private fun SheetHeader(isIdle: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_ai),
                contentDescription = "Logo ViDroidCall",
                modifier = Modifier
                    .size(36.dp)
                    .then(if (isIdle) Modifier.scale(logoScale) else Modifier)
            )

            Text(
                text = "ViDroidCall",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15182A),
                letterSpacing = (-0.2).sp
            )
        }
    }
}

/**
 * Trạng thái đang lắng nghe câu lệnh.
 * - Khi vừa mở lên: hiện "Hãy nói gì đó..."
 * - Khi người dùng bắt đầu nói: chuyển thành "Đang lắng nghe..."
 * Dòng chữ ở bên trái, sóng âm thanh thu nhỏ thanh mảnh đặt ở bên phải.
 */
@Composable
private fun ListeningContent(
    statusMessage: String = "",
    recognizedText: String = ""
) {
    val displayText = when {
        recognizedText.isNotBlank() -> "“$recognizedText”"
        statusMessage == "Đang lắng nghe..." || statusMessage == "Đang lắng nghe câu lệnh..." -> "Đang lắng nghe..."
        statusMessage == "Hãy nói gì đó..." -> "Hãy nói gì đó..."
        else -> "Hãy nói gì đó..."
    }

    val isWaiting = displayText == "Hãy nói gì đó..."
    val isSpeaking = !isWaiting

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedContent(
            targetState = displayText,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + slideInVertically(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetY = { fullHeight -> fullHeight / 3 }
                )).togetherWith(
                    fadeOut(animationSpec = tween(160)) + slideOutVertically(
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                        targetOffsetY = { fullHeight -> -fullHeight / 3 }
                    )
                )
            },
            label = "listening_text_anim",
            modifier = Modifier.weight(1f, fill = false)
        ) { text ->
            val isItemWaiting = text == "Hãy nói gì đó..."
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = if (isItemWaiting) FontWeight.SemiBold else FontWeight.Bold,
                color = if (isItemWaiting) Color(0xFF64748B) else Color(0xFF15182A),
                letterSpacing = (-0.3).sp,
                lineHeight = 26.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Sóng âm nhỏ mỏng bên tay phải:
        // - Khi ở "Hãy nói gì đó...": Sóng âm tĩnh (đứng yên hoàn toàn)
        // - Khi cất giọng nói ("Đang lắng nghe..."): Sóng âm động (nhảy nhót sống động)
        AnimatedWaveformVisualizer(isSpeaking = isSpeaking)
    }
}

/**
 * Hiệu ứng sóng âm thanh mảnh, nhỏ gọn đặt ở bên tay phải.
 * - isSpeaking = false ("Hãy nói gì đó..."): Sóng âm tĩnh, đứng yên thư thái
 * - isSpeaking = true ("Đang lắng nghe..."): Sóng âm 7 cột nhảy nhót sống động
 */
@Composable
private fun AnimatedWaveformVisualizer(isSpeaking: Boolean = true) {
    Crossfade(
        targetState = isSpeaking,
        animationSpec = tween(220),
        label = "waveform_speaking_crossfade"
    ) { speaking ->
        if (speaking) {
            DynamicWaveform()
        } else {
            StaticWaveform()
        }
    }
}

/**
 * Sóng âm tĩnh (Static Waveform): Đứng yên hoàn toàn ở trạng thái chờ khi ở "Hãy nói gì đó..."
 */
@Composable
private fun StaticWaveform() {
    val staticBars = listOf(
        Pair(5.dp, Color(0xFFBFDBFE)),
        Pair(9.dp, Color(0xFF93C5FD)),
        Pair(14.dp, Color(0xFF60A5FA)),
        Pair(20.dp, Color(0xFF0866FF)),
        Pair(14.dp, Color(0xFF60A5FA)),
        Pair(9.dp, Color(0xFF93C5FD)),
        Pair(5.dp, Color(0xFFBFDBFE))
    )

    Row(
        modifier = Modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        staticBars.forEach { (height, color) ->
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(height)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Sóng âm động (Dynamic Waveform): 7 cột sóng nhảy nhót theo giọng nói khi ở "Đang lắng nghe..."
 */
@Composable
private fun DynamicWaveform() {
    val transition = rememberInfiniteTransition(label = "WaveformBars")

    val b1 by transition.animateFloat(
        initialValue = 8f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1"
    )
    val b2 by transition.animateFloat(
        initialValue = 12f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(560, delayMillis = 80, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2"
    )
    val b3 by transition.animateFloat(
        initialValue = 16f,
        targetValue = 34f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, delayMillis = 140, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3"
    )
    val b4 by transition.animateFloat(
        initialValue = 18f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, delayMillis = 60, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b4"
    )
    val b5 by transition.animateFloat(
        initialValue = 16f,
        targetValue = 34f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, delayMillis = 110, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b5"
    )
    val b6 by transition.animateFloat(
        initialValue = 12f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(580, delayMillis = 70, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b6"
    )
    val b7 by transition.animateFloat(
        initialValue = 8f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 130, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b7"
    )

    val bars = listOf(
        Pair(b1, Color(0xFFBFDBFE)),
        Pair(b2, Color(0xFF93C5FD)),
        Pair(b3, Color(0xFF60A5FA)),
        Pair(b4, Color(0xFF0866FF)),
        Pair(b5, Color(0xFF60A5FA)),
        Pair(b6, Color(0xFF93C5FD)),
        Pair(b7, Color(0xFFBFDBFE))
    )

    Row(
        modifier = Modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        bars.forEach { (height, color) ->
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(height.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Trạng thái nhận diện giọng nói STT vừa nói xong.
 * Hiển thị câu nói của người dùng với animation trượt dọc,
 * chấm phát sáng thở nhẹ báo hiệu chuẩn bị chuyển sang xác nhận.
 */
@Composable
private fun RecognizedTextContent(recognizedText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_stt_dot")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (recognizedText.isNotBlank()) "“$recognizedText”" else "“...”",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.3).sp,
                lineHeight = 26.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Chấm xanh động nhỏ nhịp nháy xác nhận đang chuyển tiếp
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .scale(dotScale)
                    .clip(CircleShape)
                    .background(Color(0xFF0866FF).copy(alpha = dotAlpha))
            )
        }
    }
}

/**
 * Trạng thái "AI đang phân tích..." khi Fast-Path không khớp.
 * Bám sát phong cách trong app: hiển thị trực tiếp câu nói người dùng,
 * box phân tích gọn gàng với logo thở nhẹ và chữ xanh thương hiệu.
 */
@Composable
private fun AnalyzingContent(recognizedText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "halo_anim")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (recognizedText.isNotBlank()) {
            Text(
                text = "“$recognizedText”",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.2).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Box phân tích gọn gàng bám sát UI màn hình trong app
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0866FF).copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color(0xFF0866FF).copy(alpha = 0.25f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Vùng logo hào quang thở gọn gàng
                Box(
                    modifier = Modifier.size(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .scale(haloScale)
                            .clip(CircleShape)
                            .background(Color(0xFF0866FF).copy(alpha = haloAlpha))
                    )

                    Image(
                        painter = painterResource(id = R.drawable.logo_ai),
                        contentDescription = "AI analyzing",
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "AI đang phân tích câu lệnh...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0866FF),
                    letterSpacing = (-0.2).sp
                )
            }
        }
    }
}

/**
 * Khung xác nhận hành động trực quan áp dụng cho MỌI intent.
 * Hiển thị trực tiếp câu nói người dùng + Thẻ tóm tắt việc (Icon, Tiêu đề, Mô tả) + 2 nút [Hủy] và [Xác nhận].
 */
@Composable
private fun ConfirmActionContent(
    recognizedText: String,
    actionTitle: String,
    actionDescription: String,
    actionIconType: OverlayActionIconType,
    onConfirm: (() -> Unit)?,
    onCancel: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (recognizedText.isNotBlank()) {
            Text(
                text = "“$recognizedText”",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.2).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Thẻ tóm tắt việc trực quan
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Biểu tượng loại hành động
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0866FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getActionIcon(actionIconType),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = actionTitle.ifBlank { "Xác nhận thực hiện?" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = actionDescription.ifBlank { "Thực hiện hành động" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hàng 2 nút: [Hủy] và [Xác nhận]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Nút HỦY (phụ)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                onClick = { onCancel?.invoke() }
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hủy",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }
            }

            // Nút XÁC NHẬN (chính, màu xanh)
            Surface(
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0866FF),
                shadowElevation = 3.dp,
                onClick = { onConfirm?.invoke() }
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Xác nhận",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Ánh xạ loại biểu tượng theo loại hành động
 */
private fun getActionIcon(type: OverlayActionIconType): ImageVector {
    return when (type) {
        OverlayActionIconType.CALL -> Icons.Rounded.Call
        OverlayActionIconType.SMS -> Icons.AutoMirrored.Rounded.Message
        OverlayActionIconType.OPEN_APP -> Icons.Rounded.Apps
        OverlayActionIconType.ALARM -> Icons.Rounded.Alarm
        OverlayActionIconType.TIMER -> Icons.Rounded.Timer
        OverlayActionIconType.MAP -> Icons.Rounded.LocationOn
        OverlayActionIconType.SEARCH -> Icons.Rounded.Search
        OverlayActionIconType.YOUTUBE -> Icons.Rounded.PlayArrow
        OverlayActionIconType.MUSIC -> Icons.Rounded.MusicNote
        OverlayActionIconType.GENERIC -> Icons.Rounded.TouchApp
    }
}
