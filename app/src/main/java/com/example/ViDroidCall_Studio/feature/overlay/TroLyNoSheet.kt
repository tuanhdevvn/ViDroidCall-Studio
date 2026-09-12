// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.overlay

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ViDroidCall_Studio.domain.model.NativeAction

/**
 * Hộp thoại Trợ lý nổi (Overlay Bottom Sheet)
 * Render hộp thoại trợ lý ảo bám đáy (bottom-pinned) theo thiết kế Stitch.
 * Tuyệt đối KHÔNG chứa fake HomeScreen hay wallpaper giả.
 */
@Composable
fun TroLyNoSheet(
    state: TroLyNoState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Chuẩn thiết kế Stitch: Frosted white glassmorphism card
    val cardBackground = Color(0xF5FFFFFF)
    val cardBorder = Color.White.copy(alpha = 0.8f)
    val textPrimary = Color(0xFF0F172A)
    val textSecondary = Color(0xFF475569)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color(0x35000000)
            ),
        shape = RoundedCornerShape(28.dp),
        color = cardBackground,
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header row: Branding & status badge
            HeaderRow(state = state, textPrimary = textPrimary, textSecondary = textSecondary)

            // 2. Nội dung theo từng trạng thái (Listening, STT, FastPath, Analyzing, Confirmation, Clarify)
            when (state) {
                is TroLyNoState.Listening -> {
                    ListeningContent(
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }

                is TroLyNoState.Stt -> {
                    SttContent(
                        transcript = state.text,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }

                is TroLyNoState.FastPath -> {
                    FastPathContent(
                        text = state.text,
                        action = state.action,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }

                is TroLyNoState.Analyzing -> {
                    AnalyzingContent(
                        text = "Đang nhận diện...",
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }

                is TroLyNoState.Confirmation -> {
                    ConfirmationContent(
                        action = state.action,
                        onConfirm = onConfirm,
                        onCancel = onCancel,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }

                is TroLyNoState.Clarify -> {
                    ClarifyContent(
                        text = state.text,
                        missing = state.missing,
                        onCancel = onCancel,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                }
            }
        }
    }
}

/**
 * Thanh tiêu đề thương hiệu ViDroidCall và Status Pill
 */
@Composable
private fun HeaderRow(
    state: TroLyNoState,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo & Tên ứng dụng
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF0866FF), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFF0866FF), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = "ViDroidCall",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                letterSpacing = (-0.2).sp
            )
        }

        // Status Badge Pill
        when (state) {
            is TroLyNoState.Listening, is TroLyNoState.Stt -> {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFD1FAE5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PulsingIndicator(color = Color(0xFF10B981))
                        Text(
                            text = "Trợ lý AI đã sẵn sàng",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF065F46)
                        )
                    }
                }
            }

            is TroLyNoState.FastPath -> {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFDBEAFE),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Text(
                        text = "⚡ Khớp tức thì",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0866FF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            is TroLyNoState.Analyzing -> {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                ) {
                    Text(
                        text = "Đang phân tích",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0866FF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            is TroLyNoState.Confirmation -> {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                ) {
                    Text(
                        text = "Xác nhận",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0866FF),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            is TroLyNoState.Clarify -> {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A))
                ) {
                    Text(
                        text = "Cần làm rõ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Trạng thái Listening: hiển thị tiêu đề, waveform, phụ đề hướng dẫn
 */
@Composable
private fun ListeningContent(
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Đang lắng nghe câu lệnh...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.weight(1f)
            )
            SoundWaveAnimation()
        }
        Text(
            text = "Hãy nói gì đó...",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textSecondary
        )
    }
}

/**
 * Trạng thái STT: hiển thị transcript hiện tại cập nhật theo thời gian thực
 */
@Composable
private fun SttContent(
    transcript: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (transcript.isNotBlank()) "“$transcript”" else "Đang lắng nghe...",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            SoundWaveAnimation()
        }
        Text(
            text = "Đang nhận diện giọng nói...",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textSecondary
        )
    }
}

/**
 * Trạng thái Fast-Path: khớp nhanh quy tắc
 */
@Composable
private fun FastPathContent(
    text: String,
    action: NativeAction,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "ĐÃ NHẬN DIỆN",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textSecondary,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "“$text”",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textPrimary
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Text(
                    text = "⚡ Fast-Path (Bộ dữ liệu)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0866FF),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Text(
                    text = "Intent: ${action.intentName}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/**
 * Trạng thái Analyzing: AI đang phân tích câu lệnh
 */
@Composable
private fun AnalyzingContent(
    text: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "ĐÃ NHẬN DIỆN",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = textSecondary,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "“$text”",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFEFF6FF),
            border = BorderStroke(1.dp, Color(0xFFDBEAFE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color(0xFF0866FF),
                    strokeWidth = 2.5.dp
                )
                Text(
                    text = "AI đang phân tích câu lệnh...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
        }
    }
}

/**
 * Trạng thái Confirmation: Hộp thoại xác nhận trước khi thực thi cuộc gọi, gửi SMS, v.v.
 */
@Composable
private fun ConfirmationContent(
    action: NativeAction,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    textPrimary: Color,
    textSecondary: Color
) {
    val actionIcon = when (action) {
        is NativeAction.CallContact -> Icons.Rounded.Call
        is NativeAction.SendSms -> Icons.AutoMirrored.Rounded.Message
        is NativeAction.OpenMap -> Icons.Rounded.Navigation
        else -> Icons.Rounded.Check
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = action.getConfirmationTitle(),
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textPrimary,
            lineHeight = 28.sp
        )
        Text(
            text = action.getConfirmationDescription(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = textSecondary,
            lineHeight = 22.sp
        )

        if (action is NativeAction.SendSms && action.message.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "“${action.message}”",
                    fontSize = 15.sp,
                    fontStyle = FontStyle.Italic,
                    color = textPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // Action Buttons: Hủy & Xác nhận
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE2E8F0),
                    contentColor = Color(0xFF1E293B)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Hủy",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0866FF),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Xác nhận",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Trạng thái Clarify: làm rõ thông tin
 */
@Composable
private fun ClarifyContent(
    text: String,
    missing: List<String>,
    onCancel: () -> Unit,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Cần thêm thông tin",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Text(
            text = text,
            fontSize = 16.sp,
            color = textSecondary
        )
        if (missing.isNotEmpty()) {
            Text(
                text = "Thông tin còn thiếu: ${missing.joinToString(", ")}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFB45309)
            )
        }
        Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE2E8F0),
                contentColor = Color(0xFF1E293B)
            )
        ) {
            Text(text = "Đóng", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Chấm tròn nhấp nháy chỉ báo trạng thái hoạt động
 */
@Composable
private fun PulsingIndicator(color: Color) {
    val transition = rememberInfiniteTransition(label = "Pulse")
    val alpha = transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )
    Box(
        modifier = Modifier
            .size(6.dp)
            .background(color.copy(alpha = alpha.value), CircleShape)
    )
}

/**
 * Waveform 5 thanh sóng âm thanh chuyển động nhịp nhàng theo phong cách Stitch
 */
@Composable
private fun SoundWaveAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "SoundWave")
    val h1 = infiniteTransition.animateFloat(
        initialValue = 10f, targetValue = 26f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 = infiniteTransition.animateFloat(
        initialValue = 22f, targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(750, delayMillis = 150, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 = infiniteTransition.animateFloat(
        initialValue = 14f, targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(550, delayMillis = 300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 = infiniteTransition.animateFloat(
        initialValue = 24f, targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(800, delayMillis = 100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h4"
    )
    val h5 = infiniteTransition.animateFloat(
        initialValue = 10f, targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(650, delayMillis = 200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h5"
    )

    val heights = listOf(h1, h2, h3, h4, h5)
    val colors = listOf(
        Color(0xFF0866FF),
        Color(0xFF6EA2FF),
        Color(0xFF0866FF),
        Color(0xFFA9C9FF),
        Color(0xFF6EA2FF)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .height(30.dp)
            .padding(horizontal = 4.dp)
    ) {
        heights.forEachIndexed { index, anim ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(anim.value.dp)
                    .background(colors[index], CircleShape)
            )
        }
    }
}
