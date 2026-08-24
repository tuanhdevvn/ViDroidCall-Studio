package com.example.ViDroidCall_Studio.feature.assistant

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import com.example.ViDroidCall_Studio.R
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import com.example.ViDroidCall_Studio.data.model.NluResult
import com.example.ViDroidCall_Studio.data.nlu.NluModelState
import com.example.ViDroidCall_Studio.ui.component.bounceClick
import com.example.ViDroidCall_Studio.ui.theme.AppPrimary

/**
 * Giao diện Màn hình Chính (Home / Voice AI Assistant)
 * Chiếm trọn Full màn hình, trực diện, không bị gò bó trong khung card nhỏ.
 */
@Composable
fun AssistantScreen(
    isListening: Boolean,
    speechText: String,
    onToggleListening: () -> Unit,
    nluResult: NluResult?,
    isNluProcessing: Boolean,
    modelState: NluModelState,
    modifier: Modifier = Modifier,
    onSuggestionClick: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Trạng thái Mô hình NLU AI
        ModelEngineStatusBadge(modelState = modelState)

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Khu vực Trung tâm Ra lệnh giọng nói AI (Chiếm trọn Full màn hình)
        VoiceAssistantSection(
            isListening = isListening,
            speechText = speechText,
            isNluProcessing = isNluProcessing,
            modelState = modelState,
            onToggleListening = onToggleListening
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Thẻ Kết Quả Phân Tích Ý Định JSON NLU AI (Khi có kết quả hoặc đang phân tích)
        if (isNluProcessing || nluResult != null) {
            NluJsonResultCard(
                nluResult = nluResult,
                isProcessing = isNluProcessing
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Badge thông báo trạng thái trợ lý AI bằng Tiếng Việt thân thiện và Icon chuyên nghiệp
 */
@Composable
private fun ModelEngineStatusBadge(modelState: NluModelState) {
    val (statusText, badgeColor, iconVector) = when (modelState) {
        is NluModelState.Ready -> Triple("Trợ lý AI đã sẵn sàng", Color(0xFF10B981), Icons.Rounded.CheckCircle)
        is NluModelState.Loading -> Triple("Trợ lý AI đang nạp...", Color(0xFFF59E0B), Icons.Rounded.Sync)
        is NluModelState.ModelNotFound -> Triple("Chưa có mô hình AI", Color(0xFFEF4444), Icons.Rounded.WarningAmber)
        is NluModelState.Error -> Triple("Lỗi trợ lý AI", Color(0xFFEF4444), Icons.Rounded.WarningAmber)
        is NluModelState.Uninitialized -> Triple("Đang khởi động trợ lý AI...", Color(0xFF6B7280), Icons.Rounded.Memory)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = badgeColor.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(badgeColor.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = statusText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

/**
 * Khu vực Trợ lý Micro giọng nói trung tâm (Toàn màn hình, không bị đóng khung card)
 */
@Composable
private fun VoiceAssistantSection(
    isListening: Boolean,
    speechText: String,
    isNluProcessing: Boolean,
    modelState: NluModelState,
    onToggleListening: () -> Unit
) {
    val context = LocalContext.current
    val isAiReady = modelState is NluModelState.Ready

    val infiniteTransition = rememberInfiniteTransition(label = "VoiceRings")
    val ringScale1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (isListening) 1.40f else 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 1500 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringScale1"
    )
    val ringAlpha1 by infiniteTransition.animateFloat(
        initialValue = if (isListening) 0.55f else 0.35f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 1500 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringAlpha1"
    )

    // Animation thở nhịp nhàng của Logo App khi AI đang suy nghĩ
    val aiBrainScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiBrainScale"
    )
    val aiGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.60f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiGlowAlpha"
    )
    // Animation xoay tròn 360 độ kiểu Loading cho Logo App
    val aiLogoRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aiLogoRotation"
    )

    // Xử lý khi nhấn nút: Chặn thao tác nếu AI đang phân tích câu lệnh (thực thi tuần tự)
    val handleActionClick: () -> Unit = {
        if (isNluProcessing) {
            Toast.makeText(context, "AI đang phân tích câu lệnh, vui lòng đợi...", Toast.LENGTH_SHORT).show()
        } else if (isAiReady) {
            onToggleListening()
        } else {
            val msg = when (modelState) {
                is NluModelState.Loading -> "Trợ lý AI đang nạp, vui lòng đợi..."
                is NluModelState.ModelNotFound -> "Chưa có file mô hình AI trong thư mục Download"
                else -> "Trợ lý AI chưa sẵn sàng"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Nút Micro lớn ở trung tâm với hiệu ứng sóng lan toả liên tục kể cả khi chưa nói
        Box(
            modifier = Modifier.size(210.dp),
            contentAlignment = Alignment.Center
        ) {
            val waveColor = if (isAiReady) AppPrimary else AppPrimary.copy(alpha = 0.25f)

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = waveColor.copy(alpha = if (isAiReady) 0.08f else 0.03f),
                    radius = size.minDimension / 2
                )
                if (isAiReady) {
                    // Vòng sóng âm lan toả liên tục khi AI đã sẵn sàng
                    drawCircle(
                        color = waveColor.copy(alpha = ringAlpha1),
                        radius = (size.minDimension / 2) * ringScale1,
                        style = Stroke(width = if (isListening) 4.5f else 3.5f)
                    )
                }
                drawCircle(
                    color = waveColor.copy(alpha = if (isListening) 0.22f else if (isAiReady) 0.12f else 0.05f),
                    radius = size.minDimension * 0.38f
                )
            }

            // Màu gradient của Micro (Nếu chưa sẵn sàng thì làm mờ xanh dương)
            val micGradientColors = if (isAiReady) {
                listOf(
                    Color(0xFF3B82F6),
                    AppPrimary,
                    Color(0xFF0052D6)
                )
            } else {
                listOf(
                    Color(0xFF3B82F6).copy(alpha = 0.40f),
                    AppPrimary.copy(alpha = 0.40f),
                    Color(0xFF0052D6).copy(alpha = 0.40f)
                )
            }

            Box(
                modifier = Modifier
                    .size(126.dp)
                    .shadow(
                        elevation = if (isAiReady) 20.dp else 4.dp,
                        shape = CircleShape,
                        spotColor = AppPrimary
                    )
                    .background(
                        brush = Brush.radialGradient(colors = micGradientColors),
                        shape = CircleShape
                    )
                    .bounceClick(scaleDown = if (isAiReady) 0.88f else 0.96f, onClick = handleActionClick),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isListening -> {
                        // Hiệu ứng sóng âm thanh động 5 cột nhảy nhót nhịp nhàng khi người dùng đang nói
                        AnimatedWaveformVisualizer()
                    }
                    isNluProcessing -> {
                        // Logo App xoay tròn kiểu Loading kết hợp vầng hào quang phát sáng
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Vòng hào quang phát sáng thở màu xanh dịu mắt
                            Box(
                                modifier = Modifier
                                    .size(78.dp)
                                    .scale(aiBrainScale)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF60A5FA).copy(alpha = aiGlowAlpha),
                                                Color(0xFF2563EB).copy(alpha = aiGlowAlpha * 0.5f),
                                                Color.Transparent
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )
                            // Logo App xoay tròn 360 độ mượt mà kiểu Loading
                            Image(
                                painter = painterResource(id = R.drawable.logo_app),
                                contentDescription = "AI đang phân tích",
                                modifier = Modifier
                                    .size(66.dp)
                                    .scale(aiBrainScale)
                                    .rotate(aiLogoRotation)
                                    .clip(CircleShape)
                            )
                        }
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Microphone",
                            tint = if (isAiReady) Color.White else Color.White.copy(alpha = 0.70f),
                            modifier = Modifier.size(58.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dòng trạng thái lớn, dễ đọc
        Text(
            text = when {
                isListening -> "Đang nghe bạn nói..."
                isNluProcessing -> "AI đang phân tích câu lệnh..."
                else -> "Chạm vào Micro để ra lệnh"
            },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = if (!isAiReady) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    else if (isListening || isNluProcessing) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.onSurface
        )

        // Bong bóng hiển thị nội dung nhận dạng giọng nói tức thời
        AnimatedVisibility(
            visible = isAiReady && speechText.isNotBlank() && speechText != "Đang lắng nghe...",
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "“$speechText”",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Nút bấm hành động to, rõ ràng (Tự động ẩn mượt mà khi AI đang phân tích để không bị trùng lặp)
        AnimatedVisibility(
            visible = !isNluProcessing,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val buttonBgColor = when {
                !isAiReady -> MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                isListening -> MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            }
            val buttonTextColor = when {
                !isAiReady -> MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                else -> MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(buttonBgColor, RoundedCornerShape(22.dp))
                    .bounceClick(scaleDown = if (isAiReady) 0.95f else 0.98f, onClick = handleActionClick)
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isListening) "Dừng nghe" else "Nhấn để bắt đầu nói",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = buttonTextColor
                )
            }
        }
    }
}

/**
 * Hiệu ứng 5 cột sóng âm thanh nhảy nhót nhịp nhàng khi người dùng đang nói (Voice Equalizer Waveform)
 */
@Composable
private fun AnimatedWaveformVisualizer() {
    val transition = rememberInfiniteTransition(label = "WaveformBars")

    val bar1Height by transition.animateFloat(
        initialValue = 14f,
        targetValue = 32f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by transition.animateFloat(
        initialValue = 22f,
        targetValue = 48f,
        animationSpec = infiniteRepeatable(
            animation = tween(540, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by transition.animateFloat(
        initialValue = 30f,
        targetValue = 58f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val bar4Height by transition.animateFloat(
        initialValue = 20f,
        targetValue = 46f,
        animationSpec = infiniteRepeatable(
            animation = tween(580, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )
    val bar5Height by transition.animateFloat(
        initialValue = 12f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(460, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar5"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(60.dp)
    ) {
        listOf(bar1Height, bar2Height, bar3Height, bar4Height, bar5Height).forEach { height ->
            Box(
                modifier = Modifier
                    .width(5.5.dp)
                    .height(height.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
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
                shape = RoundedCornerShape(26.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(26.dp),
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
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Kết Quả Phân Tích AI",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (nluResult != null && !isProcessing) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.bounceClick(scaleDown = 0.9f, onClick = {
                            clipboardManager.setText(AnnotatedString(nluResult.rawJson))
                            Toast.makeText(context, "Đã sao chép JSON vào bộ nhớ tạm", Toast.LENGTH_SHORT).show()
                        })
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Copy JSON",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Copy",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isProcessing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "AI đang trích xuất ý định...",
                        fontSize = 17.sp,
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                // Khung JSON Code Monospace
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = nluResult.rawJson,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = Color(0xFF38BDF8),
                        lineHeight = 20.sp,
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
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}




