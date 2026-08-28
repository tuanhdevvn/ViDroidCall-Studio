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
import com.example.ViDroidCall_Studio.feature.speech.SpeechToTextManager
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
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

import com.example.ViDroidCall_Studio.domain.model.NativeAction
import com.example.ViDroidCall_Studio.ui.component.ActionConfirmationDialog

/**
 * Giao diện Màn hình Chính (Home / Voice AI Assistant)
 * Chiếm trọn Full màn hình, trực diện, không bị gò bó trong khung card nhỏ.
 */
@Composable
fun AssistantScreen(
    isListening: Boolean,
    speechText: String,
    onToggleListening: () -> Unit,
    onCancelListening: () -> Unit = {},
    nluResult: NluResult?,
    isNluProcessing: Boolean,
    modelState: NluModelState,
    hasStoragePermission: Boolean = true,
    onRequestStoragePermission: () -> Unit = {},
    onRescanModel: () -> Unit = {},
    modifier: Modifier = Modifier,
    isTtsSpeaking: Boolean = false,
    onSuggestionClick: (String) -> Unit = {},
    pendingAction: NativeAction? = null,
    showConfirmationDialog: Boolean = false,
    onConfirmAction: () -> Unit = {},
    onCancelAction: () -> Unit = {}
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
        // 1. Trạng thái Mô hình NLU AI & Quản lý Quyền
        ModelEngineStatusBadge(
            modelState = modelState,
            hasStoragePermission = hasStoragePermission,
            onRequestStoragePermission = onRequestStoragePermission,
            onRescanModel = onRescanModel
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Khu vực Trung tâm Ra lệnh giọng nói AI (Chiếm trọn Full màn hình)
        VoiceAssistantSection(
            isListening = isListening,
            speechText = speechText,
            isNluProcessing = isNluProcessing,
            modelState = modelState,
            isTtsSpeaking = isTtsSpeaking,
            onToggleListening = onToggleListening,
            onCancelListening = onCancelListening
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

        // 4. Hộp thoại Xác nhận thực thi hành động nhạy cảm (Không che mất hay xóa NluJsonResultCard)
        if (showConfirmationDialog && pendingAction != null) {
            ActionConfirmationDialog(
                title = pendingAction.getConfirmationTitle(),
                description = pendingAction.getConfirmationDescription(),
                onConfirm = onConfirmAction,
                onCancel = onCancelAction
            )
        }
    }
}

/**
 * Badge thông báo trạng thái trợ lý AI bằng Tiếng Việt thân thiện, hiển thị nút Cấp quyền / Quét lại khi cần
 */
@Composable
private fun ModelEngineStatusBadge(
    modelState: NluModelState,
    hasStoragePermission: Boolean = true,
    onRequestStoragePermission: () -> Unit = {},
    onRescanModel: () -> Unit = {}
) {
    val (statusText, badgeColor, iconVector) = when (modelState) {
        is NluModelState.Ready -> Triple("Trợ lý AI đã sẵn sàng", Color(0xFF10B981), Icons.Rounded.CheckCircle)
        is NluModelState.Loading -> Triple("Trợ lý AI đang nạp...", Color(0xFFF59E0B), Icons.Rounded.Sync)
        is NluModelState.ModelNotFound -> {
            if (!hasStoragePermission) {
                Triple("Chưa cấp quyền truy cập tệp", Color(0xFFEF4444), Icons.Rounded.FolderShared)
            } else {
                Triple("Chưa có mô hình AI trong Download", Color(0xFFEF4444), Icons.Rounded.WarningAmber)
            }
        }
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
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
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
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }

            // Nút hành động nhanh trên Badge khi chưa có quyền hoặc chưa tìm thấy model
            if (modelState is NluModelState.ModelNotFound) {
                if (!hasStoragePermission) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = badgeColor,
                        modifier = Modifier.bounceClick(scaleDown = 0.92f, onClick = onRequestStoragePermission)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cấp quyền",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = badgeColor.copy(alpha = 0.18f),
                        modifier = Modifier.bounceClick(scaleDown = 0.92f, onClick = onRescanModel)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Quét lại",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                }
            }
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
    isTtsSpeaking: Boolean = false,
    onToggleListening: () -> Unit,
    onCancelListening: () -> Unit = {}
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

    // Animation nhịp thở (breathing/pulse) của Logo App và vầng hào quang khi AI đang phân tích dữ liệu
    val aiBrainScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiBrainScale"
    )
    val aiGlowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiGlowScale"
    )
    val aiGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aiGlowAlpha"
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
                        // Logo App có nhịp thở (breathe) kết hợp vầng hào quang phát sáng lan toả thể hiện AI đang phân tích
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Vòng hào quang phát sáng thở màu xanh dịu mắt
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .scale(aiGlowScale)
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
                            // Logo App nhịp thở êm ái, co giãn tự nhiên khi đang phân tích dữ liệu
                            Image(
                                painter = painterResource(id = R.drawable.logo_app),
                                contentDescription = "AI đang phân tích",
                                modifier = Modifier
                                    .size(68.dp)
                                    .scale(aiBrainScale)
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

        // Khung hiển thị Trạng thái & Giọng nói hợp nhất (Merged Listening & Speech Card)
        val isSpeechPlaceholder = speechText.isBlank() || 
                speechText == "Đang lắng nghe..." || 
                speechText == "Đang lắng nghe câu lệnh..." || 
                speechText == "Đang nghe bạn nói..." ||
                speechText == SpeechToTextManager.LISTENING_PLACEHOLDER

        val isCardVisible = isAiReady && (isListening || isNluProcessing || (speechText.isNotBlank() && !isSpeechPlaceholder))

        if (isCardVisible) {
            val cardText = when {
                isNluProcessing -> "AI đang phân tích câu lệnh..."
                isListening && isSpeechPlaceholder -> "“Đang nghe bạn nói...”"
                speechText.isNotBlank() -> "“$speechText”"
                else -> "“Đang nghe bạn nói...”"
            }

            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = cardText,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }
        } else {
            // Trạng thái chờ: Dòng chữ hướng dẫn chạm micro đơn giản
            Text(
                text = "Chạm vào Micro để ra lệnh",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = if (!isAiReady) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        else MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Nút bấm hành động to, rõ ràng (Tự động ẩn mượt mà khi AI đang phân tích để không bị trùng lặp)
        AnimatedVisibility(
            visible = !isNluProcessing,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            if (isListening) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Nút Dừng nghe (Màu xanh)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .bounceClick(scaleDown = 0.95f, onClick = handleActionClick)
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Dừng nghe",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Nút Hủy cuộc trò chuyện (Màu đỏ nhạt)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFEF4444).copy(alpha = 0.14f),
                                shape = RoundedCornerShape(22.dp)
                            )
                            .bounceClick(scaleDown = 0.95f, onClick = onCancelListening)
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hủy cuộc trò chuyện",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            } else {
                val buttonBgColor = if (!isAiReady) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                }
                val buttonTextColor = if (!isAiReady) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                } else {
                    MaterialTheme.colorScheme.primary
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
                        text = "Nhấn để bắt đầu nói",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = buttonTextColor
                    )
                }
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
                    // Badge Nguồn xử lý (Fast-Path vs On-Device AI)
                    if (nluResult.isFastPath) {
                        NluBadgeChip(
                            label = "⚡ Fast-Path (Bộ dữ liệu)",
                            containerColor = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                            contentColor = Color(0xFF7C3AED)
                        )
                    } else {
                        NluBadgeChip(
                            label = "🧠 On-Device AI (GGUF)",
                            containerColor = Color(0xFF0284C7).copy(alpha = 0.15f),
                            contentColor = Color(0xFF0369A1)
                        )
                    }

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




