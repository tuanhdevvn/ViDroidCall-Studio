package com.example.ViDroidCall_Studio.feature.assistant

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ViDroidCall_Studio.R
import com.example.ViDroidCall_Studio.data.model.NluResult
import com.example.ViDroidCall_Studio.data.nlu.NluModelState
import com.example.ViDroidCall_Studio.ui.component.bounceClick
import com.example.ViDroidCall_Studio.ui.theme.AppPrimary
import kotlinx.coroutines.delay

/**
 * Giao diện Trang Hỏi đáp (Assistant / Voice)
 * Tích hợp hiển thị kết quả phân tích JSON NLU theo đặc tả kỹ thuật.
 */
@Composable
fun AssistantScreen(
    isListening: Boolean,
    speechText: String,
    onToggleListening: () -> Unit,
    nluResult: NluResult?,
    isNluProcessing: Boolean,
    modelState: NluModelState,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = listOf(
        "📞 Gọi cho Mẹ",
        "💬 Nhắn tin cho Nam là tôi đang tới",
        "⏰ Đặt báo thức 06:30 sáng",
        "⏳ Hẹn giờ 10 phút",
        "🗺️ Chỉ đường đến Hồ Hoàn Kiếm",
        "📱 Mở ứng dụng Zalo"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        // 1. Header Chào mừng Mascot
        item {
            HeaderSection()
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 2. Trạng thái Mô hình NLU
        item {
            ModelEngineStatusBadge(modelState = modelState)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Thẻ hiển thị Trợ lý Micro giọng nói & Vòng sóng nhịp thở
        item {
            VoiceAssistantCard(
                isListening = isListening,
                speechText = speechText,
                onToggleListening = onToggleListening
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 4. Thẻ Kết Quả Phân Tích JSON NLU (Nếu đang xử lý hoặc đã có kết quả)
        if (isNluProcessing || nluResult != null) {
            item {
                NluJsonResultCard(
                    nluResult = nluResult,
                    isProcessing = isNluProcessing
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // 5. Tiêu đề Gợi ý câu lệnh nhanh
        item {
            Text(
                text = "Gợi ý câu lệnh thử nghiệm",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Danh sách gợi ý xếp dọc
        items(suggestions) { suggestion ->
            SuggestionChipItem(
                text = suggestion,
                onClick = {
                    // Lọc bỏ emoji ở đầu để lấy prompt sạch gửi NLU
                    val cleanPrompt = suggestion.replace(Regex("^[\\p{So}\\p{Sk}\\s]+"), "").trim()
                    onSuggestionClick(cleanPrompt)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Badge trạng thái Engine NLU
 */
@Composable
private fun ModelEngineStatusBadge(modelState: NluModelState) {
    val (statusText, badgeColor, iconVector) = when (modelState) {
        is NluModelState.Ready -> {
            if (modelState.isUsingNativeEngine) {
                Triple("Qwen2.5-1.5B NLU (Native GGUF)", Color(0xFF10B981), Icons.Rounded.Memory)
            } else {
                Triple("Emma NLU Engine (Spec Ready)", Color(0xFF3B82F6), Icons.Rounded.Memory)
            }
        }
        is NluModelState.Loading -> Triple("Đang nạp mô hình GGUF...", Color(0xFFF59E0B), Icons.Rounded.Memory)
        is NluModelState.Error -> Triple("Lỗi mô hình: ${modelState.message}", Color(0xFFEF4444), Icons.Rounded.WarningAmber)
        is NluModelState.Uninitialized -> Triple("Mô hình chưa khởi tạo", Color(0xFF6B7280), Icons.Rounded.Memory)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = badgeColor.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = badgeColor
            )
        }
    }
}

/**
 * Thẻ hiển thị Kết quả JSON NLU
 */
@Composable
private fun NluJsonResultCard(
    nluResult: NluResult?,
    isProcessing: Boolean
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Thẻ
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.DataObject,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kết Quả JSON NLU",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (nluResult != null && !isProcessing) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.bounceClick(scaleDown = 0.9f, onClick = {
                            clipboardManager.setText(AnnotatedString(nluResult.rawJson))
                            Toast.makeText(context, "Đã sao chép JSON vào bộ nhớ tạm", Toast.LENGTH_SHORT).show()
                        })
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy JSON",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Copy",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isProcessing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Đang trích xuất ý định (NLU Inference)...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (nluResult != null) {
                // Badges thông tin: Intent, Status, Risk Level
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Badge Intent
                    NluBadgeChip(
                        label = "Intent: ${nluResult.intent}",
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )

                    // Badge Status
                    val (statusBg, statusFg) = when (nluResult.status) {
                        "success" -> Pair(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF059669))
                        "needs_clarification" -> Pair(Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFD97706))
                        "invalid" -> Pair(Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFDC2626))
                        else -> Pair(Color(0xFF6B7280).copy(alpha = 0.15f), Color(0xFF4B5563))
                    }
                    NluBadgeChip(
                        label = "Status: ${nluResult.status}",
                        containerColor = statusBg,
                        contentColor = statusFg
                    )

                    // Badge Risk Level
                    val (riskBg, riskFg) = when (nluResult.riskLevel) {
                        "high" -> Pair(Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFDC2626))
                        "medium" -> Pair(Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFD97706))
                        else -> Pair(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF059669))
                    }
                    NluBadgeChip(
                        label = "Risk: ${nluResult.riskLevel}",
                        containerColor = riskBg,
                        contentColor = riskFg
                    )

                    if (nluResult.requiresConfirmation) {
                        NluBadgeChip(
                            label = "⚠️ Yêu cầu xác nhận",
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.12f),
                            contentColor = Color(0xFFDC2626)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Khung JSON Code Monospace
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = nluResult.rawJson,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Color(0xFF38BDF8),
                        lineHeight = 18.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NluBadgeChip(
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, contentColor.copy(alpha = 0.25f))
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/**
 * Top Header với Mascot Emma và Bong bóng hội thoại
 */
@Composable
private fun HeaderSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "EmmaFloat")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emmaOffset"
    )

    var isBubbleVisible by remember { mutableStateOf(false) }
    val bubbleScale by animateFloatAsState(
        targetValue = if (isBubbleVisible) 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bubbleScale"
    )
    val bubbleAlpha by animateFloatAsState(
        targetValue = if (isBubbleVisible) 1f else 0f,
        animationSpec = tween(500),
        label = "bubbleAlpha"
    )

    val fullText = "Tôi có thể giúp gì cho bạn?"
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isBubbleVisible = true
        delay(300)
        for (i in fullText.indices) {
            displayedText += fullText[i]
            delay(40)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.emma_mascot),
            contentDescription = "Emma Mascot",
            modifier = Modifier
                .size(90.dp)
                .offset(y = offsetY.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Surface(
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = bubbleScale
                    scaleY = bubbleScale
                    alpha = bubbleAlpha
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                },
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomEnd = 22.dp,
                bottomStart = 4.dp
            ),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Xin chào! Mình là Emma",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayedText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * Thẻ hiển thị Trợ lý Micro giọng nói
 */
@Composable
private fun VoiceAssistantCard(
    isListening: Boolean,
    speechText: String,
    onToggleListening: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VoiceRings")
    val ringScale1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale1"
    )
    val ringAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha1"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            ),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = AppPrimary.copy(alpha = 0.08f),
                        radius = size.minDimension / 2
                    )
                    drawCircle(
                        color = AppPrimary.copy(alpha = if (isListening) ringAlpha1 else 0.15f),
                        radius = (size.minDimension / 2) * (if (isListening) ringScale1 else 0.85f),
                        style = Stroke(width = 3f)
                    )
                    drawCircle(
                        color = AppPrimary.copy(alpha = 0.22f),
                        radius = size.minDimension * 0.36f
                    )
                }

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = CircleShape,
                            spotColor = AppPrimary
                        )
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6),
                                    AppPrimary,
                                    Color(0xFF0052D6)
                                )
                            ),
                            shape = CircleShape
                        )
                        .bounceClick(scaleDown = 0.88f, onClick = onToggleListening),
                    contentAlignment = Alignment.Center
                ) {
                    if (isListening) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Waveform",
                            tint = Color.White,
                            modifier = Modifier.size(46.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Microphone",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isListening) "Đang nghe bạn nói..." else "Nhấn microphone để bắt đầu",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = speechText.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = speechText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                    .bounceClick(onClick = onToggleListening)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "Dừng lại" else "Nhấn để nói",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Chip gợi ý câu lệnh nhanh
 */
@Composable
private fun SuggestionChipItem(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.96f, onClick = onClick)
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
