// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Component giao diện hiển thị bảng trợ lý nổi (Assistant Sheet).
 * Tuân thủ tuyệt đối quy chuẩn thiết kế Pixel-faithful từ dự án Stitch.
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
        // Khung thẻ nổi Google Assistant-style (28dp corners, frosted translucent, shadow)
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
            color = Color(0xFFFFFFFF).copy(alpha = 0.88f),
            border = BorderStroke(1.dp, Color(0xFFFFFFFF).copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                // Hàng Header trên cùng: Logo thương hiệu + Huy hiệu trạng thái
                SheetHeader(data = data)

                Spacer(modifier = Modifier.height(14.dp))

                // Nội dung động tương ứng từng trạng thái của Trợ lý
                when (data.state) {
                    AssistantOverlayState.LISTENING -> {
                        ListeningContent()
                    }

                    AssistantOverlayState.STT -> {
                        RecognizedTextContent(
                            recognizedText = data.recognizedText
                        )
                    }

                    AssistantOverlayState.FAST_PATH -> {
                        InstantMatchContent(
                            recognizedText = data.recognizedText,
                            intentName = data.intentName
                        )
                    }

                    AssistantOverlayState.ANALYZING -> {
                        AnalyzingContent(
                            recognizedText = data.recognizedText
                        )
                    }

                    AssistantOverlayState.GGUF_LOADING -> {
                        GgufLoadingContent(
                            recognizedText = data.recognizedText
                        )
                    }

                    AssistantOverlayState.CONFIRM_CALL -> {
                        ConfirmCallContent(
                            targetName = data.targetName,
                            onConfirm = data.onConfirm,
                            onCancel = data.onCancel
                        )
                    }

                    AssistantOverlayState.MAP_CONFIRM -> {
                        ConfirmMapContent(
                            targetName = data.targetName,
                            onConfirm = data.onConfirm,
                            onCancel = data.onCancel
                        )
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
 * Header hàng trên của Overlay Sheet theo chuẩn Stitch.
 */
@Composable
private fun SheetHeader(data: AssistantOverlayData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bên trái: Chấm xanh + Biểu tượng Mic hình lục giác + Tên ViDroidCall
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0866FF))
            )

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0866FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }

            Text(
                text = "ViDroidCall",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15182A),
                letterSpacing = (-0.2).sp
            )
        }

        // Bên phải: Huy hiệu trạng thái động theo Stitch (maxLines = 1, không rớt dòng)
        when (data.state) {
            AssistantOverlayState.LISTENING -> {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse_green")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_scale"
                )

                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "Trợ lý AI đã sẵn sàng",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF047857),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            AssistantOverlayState.STT, AssistantOverlayState.FAST_PATH -> {
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE).copy(alpha = 0.8f))
                ) {
                    Text(
                        text = "Khớp tức thì",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0866FF),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            AssistantOverlayState.ANALYZING -> {
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Text(
                        text = "Chuyển tiếp AI nâng cao",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB45309),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            AssistantOverlayState.GGUF_LOADING -> {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse_amber")
                val amberPulse by infiniteTransition.animateFloat(
                    initialValue = 0.7f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "amber_scale"
                )

                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .scale(amberPulse)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                        )
                        Text(
                            text = "Trợ lý AI đang nạp...",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF78350F),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            AssistantOverlayState.CONFIRM_CALL -> {
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Text(
                        text = data.sourceLabel.ifBlank { "⚡ Fast-Path" },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0866FF),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            AssistantOverlayState.MAP_CONFIRM -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = Color(0xFFFAF5FF),
                        border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                    ) {
                        Text(
                            text = "🧠 GGUF",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7E22CE),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text(
                            text = "open_map",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Stitch A1 / B1: Trạng thái đang lắng nghe câu lệnh.
 */
@Composable
private fun ListeningContent() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Đang lắng nghe câu lệnh...",
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF15182A),
            letterSpacing = (-0.3).sp,
            lineHeight = 27.sp
        )

        Text(
            text = "Hãy nói gì đó...",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Bộ 5 thanh sóng âm động chuẩn màu Stitch (#A9C9FF, #6EA2FF, #0866FF)
        WaveformVisualization()
    }
}

/**
 * Bộ 5 thanh sóng âm động theo chuẩn Stitch.
 */
@Composable
private fun WaveformVisualization() {
    val transition = rememberInfiniteTransition(label = "waveform")

    val h1 by transition.animateFloat(
        initialValue = 10.dp.value,
        targetValue = 22.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w1"
    )

    val h2 by transition.animateFloat(
        initialValue = 16.dp.value,
        targetValue = 30.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(750, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w2"
    )

    val h3 by transition.animateFloat(
        initialValue = 24.dp.value,
        targetValue = 36.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w3"
    )

    val h4 by transition.animateFloat(
        initialValue = 16.dp.value,
        targetValue = 30.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(750, delayMillis = 150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w4"
    )

    val h5 by transition.animateFloat(
        initialValue = 8.dp.value,
        targetValue = 18.dp.value,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "w5"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WaveBar(height = h1.dp, color = Color(0xFFA9C9FF))
            WaveBar(height = h2.dp, color = Color(0xFF6EA2FF))
            WaveBar(height = h3.dp, color = Color(0xFF0866FF))
            WaveBar(height = h4.dp, color = Color(0xFF6EA2FF))
            WaveBar(height = h5.dp, color = Color(0xFFA9C9FF))
        }
    }
}

@Composable
private fun WaveBar(height: androidx.compose.ui.unit.Dp, color: Color) {
    Box(
        modifier = Modifier
            .width(6.dp)
            .height(height)
            .clip(RoundedCornerShape(9999.dp))
            .background(color)
    )
}

/**
 * Trạng thái nhận diện giọng nói STT dở dang hoặc vừa hoàn tất.
 */
@Composable
private fun RecognizedTextContent(recognizedText: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "ĐÃ NHẬN DIỆN",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B),
            letterSpacing = 1.sp
        )

        Text(
            text = if (recognizedText.isNotBlank()) "“$recognizedText”" else "“...”",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A),
            letterSpacing = (-0.3).sp,
            lineHeight = 28.sp
        )
    }
}

/**
 * Stitch A2: Khớp lệnh tức thì Fast-Path kèm các Chip chuẩn.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InstantMatchContent(
    recognizedText: String,
    intentName: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "ĐÃ NHẬN DIỆN",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B),
            letterSpacing = 1.sp
        )

        Text(
            text = if (recognizedText.isNotBlank()) "“$recognizedText”" else "“Gọi cho mẹ”",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A),
            letterSpacing = (-0.3).sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Hàng Chips: Fast-Path + Intent (FlowRow tự động xuống dòng linh hoạt)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Text(
                    text = "⚡ Fast-Path (Bộ dữ liệu)",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0866FF),
                    maxLines = 1,
                    softWrap = false
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Text(
                    text = "Intent: ${intentName.ifBlank { "call_contact" }}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF334155),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Stitch B2: Chuyển tiếp AI nâng cao & Spinner phân tích câu lệnh.
 */
@Composable
private fun AnalyzingContent(recognizedText: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "ĐÃ NHẬN DIỆN",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B),
            letterSpacing = 1.sp
        )

        Text(
            text = if (recognizedText.isNotBlank()) "“$recognizedText”" else "“Tìm giúp tôi đường đi bệnh viện Bạch Mai”",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            letterSpacing = (-0.3).sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFEFF6FF).copy(alpha = 0.7f),
            border = BorderStroke(1.dp, Color(0xFFDBEAFE))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF0866FF),
                    strokeWidth = 2.5.dp
                )

                Text(
                    text = "AI đang phân tích câu lệnh...",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Stitch B3: Nạp mô hình GGUF On-Device kèm thanh tiến trình.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GgufLoadingContent(recognizedText: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    val progressFraction by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress_fraction"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "ĐÃ NHẬN DIỆN",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF64748B),
            letterSpacing = 1.sp
        )

        Text(
            text = if (recognizedText.isNotBlank()) "“$recognizedText”" else "“Tìm giúp tôi đường đi bệnh viện Bạch Mai”",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
            letterSpacing = (-0.3).sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFFBEB).copy(alpha = 0.85f),
            border = BorderStroke(1.dp, Color(0xFFFDE68A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(text = "🧠", fontSize = 14.sp)
                        Text(
                            text = "AI đang trích xuất ý định...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF451A03),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFDE68A).copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = "GGUF On-Device",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF92400E),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Thanh tiến trình tải mô hình
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(Color(0xFFFDE68A).copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(9999.dp))
                            .background(Color(0xFFF59E0B))
                    )
                }
            }
        }
    }
}

/**
 * Stitch A3: Xác nhận thực hiện cuộc gọi.
 */
@Composable
private fun ConfirmCallContent(
    targetName: String,
    onConfirm: (() -> Unit)?,
    onCancel: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Xác nhận thực hiện cuộc gọi?",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A),
            letterSpacing = (-0.3).sp,
            lineHeight = 28.sp
        )

        Text(
            text = "Bạn có muốn gọi tới ${targetName.ifBlank { "mẹ" }} không?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF334155),
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Hai nút hành động: Hủy & Xác nhận
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
                onClick = { onCancel?.invoke() }
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hủy",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0866FF),
                shadowElevation = 4.dp,
                onClick = { onConfirm?.invoke() }
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Xác nhận",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Stitch B4: Xác nhận mở bản đồ.
 */
@Composable
private fun ConfirmMapContent(
    targetName: String,
    onConfirm: (() -> Unit)?,
    onCancel: (() -> Unit)?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Xác nhận mở bản đồ?",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A),
            letterSpacing = (-0.3).sp,
            lineHeight = 28.sp
        )

        Text(
            text = "Bạn có muốn mở bản đồ ${targetName.ifBlank { "bệnh viện Bạch Mai" }} không?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF334155),
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFE2E8F0).copy(alpha = 0.85f),
                onClick = { onCancel?.invoke() }
            ) {
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Hủy",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF0866FF),
                shadowElevation = 4.dp,
                onClick = { onConfirm?.invoke() }
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Xác nhận",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
