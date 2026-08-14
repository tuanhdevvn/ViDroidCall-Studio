package com.example.emma_vidroidcall.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emma_vidroidcall.R
import com.example.emma_vidroidcall.ui.component.CustomBottomMenuBar
import com.example.emma_vidroidcall.ui.component.NavTab
import com.example.emma_vidroidcall.ui.component.bounceClick
import com.example.emma_vidroidcall.ui.theme.AppBackground
import com.example.emma_vidroidcall.ui.theme.AppPrimary
import com.example.emma_vidroidcall.ui.theme.EmmaViDroidCallTheme

// Model lịch sử lệnh
data class CommandHistoryItem(
    val id: String,
    val commandText: String,
    val time: String,
    val status: String,
    val category: String
)

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(NavTab.ASSISTANT) }
    var isListening by remember { mutableStateOf(false) }
    var speechText by remember { mutableStateOf("") }

    // Dữ liệu mẫu Lịch sử câu lệnh
    val historyItems = remember {
        listOf(
            CommandHistoryItem("1", "Gọi điện cho Mẹ", "10:45 AM", "Thành công", "Cuộc gọi"),
            CommandHistoryItem("2", "Mở ứng dụng Zalo", "09:30 AM", "Thành công", "Ứng dụng"),
            CommandHistoryItem("3", "Nhắn tin cho Nam: 'Tôi đang tới'", "Hôm qua", "Thành công", "Tin nhắn"),
            CommandHistoryItem("4", "Đặt báo thức 06:30 sáng", "Hôm qua", "Thành công", "Hệ thống"),
            CommandHistoryItem("5", "Bật chế độ tiết kiệm pin", "12/08/2026", "Thành công", "Hệ thống")
        )
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(AppBackground),
        containerColor = AppBackground,
        bottomBar = {
            CustomBottomMenuBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> selectedTab = tab },
                onMicClick = {
                    isListening = !isListening
                    speechText = if (isListening) "Đang lắng nghe..." else ""
                },
                isListening = isListening,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                NavTab.ASSISTANT -> AssistantContent(
                    isListening = isListening,
                    speechText = speechText,
                    onToggleListening = {
                        isListening = !isListening
                        speechText = if (isListening) "Đang lắng nghe..." else ""
                    }
                )

                NavTab.HISTORY -> HistoryContent(
                    historyItems = historyItems
                )

                NavTab.SETTINGS -> SettingsContent()
            }
        }
    }
}

/**
 * Giao diện Trang Hỏi đáp (Assistant / Voice)
 * Chứa lời chào, thẻ trợ lý ảo và các gợi ý.
 */
@Composable
private fun AssistantContent(
    isListening: Boolean,
    speechText: String,
    onToggleListening: () -> Unit
) {
    val suggestions = listOf(
        "📞 Gọi cho Mẹ",
        "💬 Nhắn Zalo",
        "⏰ Báo thức 06:30",
        "🎵 Phát nhạc",
        "🗺️ Chỉ đường"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // 1. Header Chào mừng
        item {
            HeaderSection()
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 2. Thẻ hiển thị Trợ lý Micro giọng nói & Vòng sóng nhịp thở
        item {
            VoiceAssistantCard(
                isListening = isListening,
                speechText = speechText,
                onToggleListening = onToggleListening
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 3. Tiêu đề Gợi ý câu lệnh nhanh
        item {
            Text(
                text = "Gợi ý câu lệnh nhanh",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15182A)
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Danh sách gợi ý xếp dọc
        items(suggestions) { suggestion ->
            SuggestionChipItem(text = suggestion)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Giao diện Lịch sử câu lệnh độc lập
 */
@Composable
private fun HistoryContent(
    historyItems: List<CommandHistoryItem>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = AppPrimary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lịch sử câu lệnh",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF15182A)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(historyItems) { item ->
            HistoryCardRow(item = item)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Top Header với thiết kế giao diện trắng hiện đại
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

    // Animation state cho Bong bóng chat
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

    // Hiệu ứng gõ chữ
    val fullText = "Tôi có thể giúp gì cho bạn?"
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        isBubbleVisible = true
        delay(300) // Đợi bong bóng hiện ra
        for (i in fullText.indices) {
            displayedText += fullText[i]
            delay(40) // Tốc độ gõ chữ
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ảnh Mascot Emma có hiệu ứng lơ lửng
        Image(
            painter = painterResource(id = R.drawable.emma_mascot),
            contentDescription = "Emma Mascot",
            modifier = Modifier
                .size(100.dp)
                .offset(y = offsetY.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Bong bóng chat
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
                topStart = 24.dp,
                topEnd = 24.dp,
                bottomEnd = 24.dp,
                bottomStart = 4.dp
            ),
            color = AppPrimary.copy(alpha = 0.08f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Xin chào! Mình là Emma",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF15182A)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = displayedText,
                    fontSize = 15.sp,
                    color = Color(0xFF555767),
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

/**
 * Thẻ hiển thị Trợ lý giọng nói chính (Hình 2)
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
                elevation = 12.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = AppPrimary.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(28.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Vòng sóng âm thanh tròn trung tâm
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = AppPrimary.copy(alpha = 0.06f),
                        radius = size.minDimension / 2
                    )
                    drawCircle(
                        color = AppPrimary.copy(alpha = if (isListening) ringAlpha1 else 0.12f),
                        radius = (size.minDimension / 2) * (if (isListening) ringScale1 else 0.85f),
                        style = Stroke(width = 3f)
                    )
                    drawCircle(
                        color = AppPrimary.copy(alpha = 0.18f),
                        radius = size.minDimension * 0.36f
                    )
                }

                // Nút Micro chính ở giữa
                Box(
                    modifier = Modifier
                        .size(104.dp)
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
                            modifier = Modifier.size(50.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Microphone",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isListening) "Đang nghe bạn nói..." else "Nhấn microphone để bắt đầu",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (isListening) AppPrimary else Color(0xFF555767)
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
                        color = AppPrimary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = speechText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Nút bấm "Nhấn để nói"
            Box(
                modifier = Modifier
                    .background(AppPrimary.copy(alpha = 0.1f), CircleShape)
                    .bounceClick(onClick = onToggleListening)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        tint = AppPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isListening) "Dừng lại" else "Nhấn để nói",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppPrimary
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
private fun SuggestionChipItem(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.96f, onClick = {})
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = AppPrimary.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF15182A),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = AppPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Dòng hiển thị item lịch sử lệnh
 */
@Composable
private fun HistoryCardRow(item: CommandHistoryItem) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.96f, onClick = {})
            .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(AppPrimary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.category) {
                            "Cuộc gọi" -> Icons.Rounded.Call
                            "Tin nhắn" -> Icons.Rounded.AutoAwesome
                            else -> Icons.Rounded.CheckCircle
                        },
                        contentDescription = null,
                        tint = AppPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = item.commandText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15182A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${item.category} • ${item.time}",
                        fontSize = 12.sp,
                        color = Color(0xFF8A94A6)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFF0F4FC), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Chạy lại",
                    tint = AppPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Tab Màn hình Cài đặt
 */
@Composable
private fun SettingsContent() {
    var autoListen by remember { mutableStateOf(true) }
    var voiceFeedback by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Cài đặt hệ thống",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF15182A)
        )
        Text(
            text = "Tùy chỉnh Trợ lý giọng nói Emma",
            fontSize = 14.sp,
            color = Color(0xFF8A94A6)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingToggleRow(
                    title = "Nhận diện từ khóa 'Hey Emma'",
                    description = "Tự động lắng nghe khi bạn gọi",
                    checked = autoListen,
                    onCheckedChange = { autoListen = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                SettingToggleRow(
                    title = "Phản hồi bằng giọng nói",
                    description = "Phát âm phản hồi sau mỗi lệnh",
                    checked = voiceFeedback,
                    onCheckedChange = { voiceFeedback = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingOptionRow(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    title = "Giọng nói trợ lý",
                    subtitle = "Emma Vi - Nữ miền Nam (Mặc định)"
                )
                Spacer(modifier = Modifier.height(14.dp))
                SettingOptionRow(
                    icon = Icons.Rounded.Settings,
                    title = "Độ nhạy micro",
                    subtitle = "Mức độ trung bình"
                )
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF15182A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF8A94A6)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AppPrimary
            )
        )
    }
}

@Composable
private fun SettingOptionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.97f, onClick = {}),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(AppPrimary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AppPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15182A)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF8A94A6)
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF8A94A6),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    EmmaViDroidCallTheme(dynamicColor = false) {
        HomeScreen()
    }
}
