package com.example.emma_vidroidcall.feature.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.emma_vidroidcall.ui.theme.EmmaViDroidCallTheme
import com.example.emma_vidroidcall.ui.theme.IllustrationRing
import com.example.emma_vidroidcall.ui.theme.IndicatorInactive
import com.example.emma_vidroidcall.ui.theme.MicrophoneGlowInner
import com.example.emma_vidroidcall.ui.theme.MicrophoneGlowMiddle
import com.example.emma_vidroidcall.ui.theme.MicrophoneGlowOuter
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3

/**
 * Màn hình chứa toàn bộ luồng onboarding.
 *
 * onFinished được gọi khi người dùng bấm Bỏ qua hoặc Bắt đầu.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = if (currentPage == 1) {
                Arrangement.Center
            } else {
                Arrangement.End
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (currentPage == 1) {
                PageIndicator(
                    pageCount = PAGE_COUNT,
                    selectedPage = currentPage,
                )
            } else {
                TextButton(onClick = onFinished) {
                    Text(
                        text = "Bỏ qua",
                        color = if (currentPage == PAGE_COUNT - 1) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            when (page) {
                0 -> OnboardingPage(
                    illustration = { VoicePhoneIllustration() },
                    title = "Nói thay vì chạm",
                    description = "Điều khiển điện thoại bằng giọng nói\ntiếng Việt.",
                )

                1 -> OnboardingPage(
                    illustration = { AiUnderstandingIllustration() },
                    title = "AI hiểu bạn",
                    description = "Voice AI phân tích câu lệnh và chuyển\nchúng thành hành động.",
                )

                else -> OnboardingPage(
                    illustration = { ReadyMicrophoneIllustration() },
                    title = "Bạn đã sẵn sàng",
                    description = "Hãy nói câu lệnh đầu tiên.",
                )
            }
        }

        if (currentPage != 1) {
            PageIndicator(
                pageCount = PAGE_COUNT,
                selectedPage = currentPage,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 18.dp),
            )
        } else {
            Spacer(Modifier.height(25.dp))
        }

        Button(
            onClick = {
                if (currentPage == PAGE_COUNT - 1) {
                    onFinished()
                } else {
                    scope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                text = if (currentPage == PAGE_COUNT - 1) "Bắt đầu  →" else "Tiếp tục",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        if (currentPage == 1) {
            TextButton(
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Quay lại", color = MaterialTheme.colorScheme.primary)
            }
        } else {
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun OnboardingPage(
    illustration: @Composable () -> Unit,
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        illustration()
        Spacer(Modifier.height(58.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PageIndicator(
    pageCount: Int,
    selectedPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val width by animateFloatAsState(
                targetValue = if (index == selectedPage) 24f else 8f,
                label = "indicator width",
            )
            Box(
                modifier = Modifier
                    .size(width = width.dp, height = 7.dp)
                    .background(
                        color = if (index == selectedPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            IndicatorInactive
                        },
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun VoicePhoneIllustration() {
    Box(
        modifier = Modifier
            .size(width = 132.dp, height = 170.dp)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.shapes.medium,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = "Biểu tượng microphone",
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun AiUnderstandingIllustration() {
    val infiniteTransition = rememberInfiniteTransition(label = "AI wave")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "Wave progress",
    )
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Center breathing",
    )

    Box(
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Màu nền nhẹ ở trung tâm giống thiết kế.
            drawCircle(
                color = IllustrationRing.copy(alpha = 0.10f),
                radius = size.minDimension * 0.34f,
            )

            // Ba vòng sóng lệch pha, liên tục lan ra rồi mờ dần.
            repeat(3) { index ->
                val progress = (waveProgress + index / 3f) % 1f
                val radius = size.minDimension * (0.30f + progress * 0.18f)
                val alpha = (1f - progress) * 0.48f
                drawCircle(
                    color = IllustrationRing.copy(alpha = alpha),
                    radius = radius,
                    style = Stroke(width = 2.5f),
                )
            }

            // Vòng chính luôn hiển thị để hình không bị trống giữa chu kỳ.
            drawCircle(
                color = IllustrationRing.copy(alpha = 0.70f),
                radius = size.minDimension * 0.34f,
                style = Stroke(width = 3f),
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(68.dp)
                .graphicsLayer {
                    scaleX = breatheScale
                    scaleY = breatheScale
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.GraphicEq,
                    contentDescription = "Phân tích giọng nói",
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(25.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = "Chuyển thành hành động",
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ReadyMicrophoneIllustration() {
    val infiniteTransition = rememberInfiniteTransition(label = "Ready microphone")
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400),
            repeatMode = RepeatMode.Restart,
        ),
        label = "Microphone wave",
    )
    val microphoneScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "Microphone breathing",
    )

    Box(
        modifier = Modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Ánh sáng cố định phía sau giúp microphone luôn nổi bật.
            drawCircle(
                color = MicrophoneGlowOuter.copy(alpha = 0.72f),
                radius = size.minDimension * 0.34f,
            )
            drawCircle(
                color = MicrophoneGlowMiddle.copy(alpha = 0.64f),
                radius = size.minDimension * 0.27f,
            )
            drawCircle(
                color = MicrophoneGlowInner.copy(alpha = 0.56f),
                radius = size.minDimension * 0.20f,
            )

            // Ba vòng sóng lệch pha lan ra từ microphone.
            repeat(3) { index ->
                val progress = (waveProgress + index / 3f) % 1f
                val radius = size.minDimension * (0.22f + progress * 0.25f)
                val alpha = (1f - progress) * 0.42f
                drawCircle(
                    color = IllustrationRing.copy(alpha = alpha),
                    radius = radius,
                    style = Stroke(width = 3f),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(74.dp)
                .graphicsLayer {
                    scaleX = microphoneScale
                    scaleY = microphoneScale
                }
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Mic,
                contentDescription = "Microphone sẵn sàng",
                modifier = Modifier.size(42.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    EmmaViDroidCallTheme(dynamicColor = false) {
        OnboardingScreen(onFinished = {})
    }
}
