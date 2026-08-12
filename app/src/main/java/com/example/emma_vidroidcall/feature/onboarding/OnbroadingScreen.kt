package com.example.emma_vidroidcall.feature.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
//import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.emma_vidroidcall.ui.theme.EmmaViDroidCallTheme
import kotlinx.coroutines.launch

private val OnboardingPurple = Color(0xFF4C49E3)
private val OnboardingViolet = Color(0xFF7B61FF)
private val OnboardingBackground = Color(0xFFF8F8FF)
private val IndicatorInactive = Color(0xFFDDE4F5)
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
            .background(OnboardingBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onFinished) {
                Text(
                    text = "Bỏ qua",
                    color = if (currentPage == PAGE_COUNT - 1) {
                        Color(0xFF5E6170)
                    } else {
                        OnboardingPurple
                    },
                )
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

        PageIndicator(
            pageCount = PAGE_COUNT,
            selectedPage = currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 18.dp),
        )

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
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OnboardingPurple),
        ) {
            Text(
                text = if (currentPage == PAGE_COUNT - 1) "Bắt đầu  →" else "Tiếp tục",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (currentPage == 1) {
            TextButton(
                onClick = {
                    scope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Quay lại", color = OnboardingPurple)
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
            color = Color(0xFF15182A),
            fontSize = 25.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = description,
            color = Color(0xFF555767),
            fontSize = 14.sp,
            lineHeight = 20.sp,
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
                        color = if (index == selectedPage) OnboardingPurple else IndicatorInactive,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun VoicePhoneIllustration() {
    Canvas(modifier = Modifier.size(220.dp, 170.dp)) {
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(size.width * .20f, 0f),
            size = Size(size.width * .60f, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f),
        )
        drawRoundRect(
            color = Color(0xFFE6E8F8),
            topLeft = Offset(size.width * .20f, 0f),
            size = Size(size.width * .60f, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f, 28f),
            style = Stroke(width = 3f),
        )
        drawCircle(OnboardingViolet, radius = 32f, center = center)
        drawLine(Color.White, center + Offset(0f, -13f), center + Offset(0f, 8f), 5f)
        drawArc(
            color = Color.White,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = center + Offset(-13f, -3f),
            size = Size(26f, 22f),
            style = Stroke(4f),
        )
        drawLine(Color.White, center + Offset(0f, 17f), center + Offset(0f, 25f), 4f)
    }
}

@Composable
private fun AiUnderstandingIllustration() {
    Canvas(modifier = Modifier.size(220.dp)) {
        val ringColor = Color(0xFF8994FF)
        listOf(1f, .82f, .63f).forEachIndexed { index, scale ->
            drawCircle(
                color = ringColor.copy(alpha = .28f + index * .16f),
                radius = size.minDimension * .42f * scale,
                style = Stroke(width = 2.5f),
            )
        }
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(size.width * .18f, size.height * .38f),
            size = Size(size.width * .64f, size.height * .24f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f, 50f),
        )
        val x = size.width * .34f
        repeat(5) { i ->
            val h = (12 + (i % 3) * 8).toFloat()
            drawLine(
                color = OnboardingPurple,
                start = Offset(x + i * 7f, center.y - h / 2),
                end = Offset(x + i * 7f, center.y + h / 2),
                strokeWidth = 4f,
            )
        }
        drawLine(Color(0xFF4A4D5D), center + Offset(-2f, 0f), center + Offset(20f, 0f), 3f)
        drawLine(Color(0xFF4A4D5D), center + Offset(14f, -7f), center + Offset(21f, 0f), 3f)
        drawLine(Color(0xFF4A4D5D), center + Offset(14f, 7f), center + Offset(21f, 0f), 3f)
        val bolt = Path().apply {
            moveTo(center.x + 48f, center.y - 20f)
            lineTo(center.x + 35f, center.y + 2f)
            lineTo(center.x + 47f, center.y + 2f)
            lineTo(center.x + 39f, center.y + 22f)
            lineTo(center.x + 61f, center.y - 5f)
            lineTo(center.x + 49f, center.y - 5f)
            close()
        }
        drawPath(bolt, Color(0xFF8429D9))
    }
}

@Composable
private fun ReadyMicrophoneIllustration() {
    Canvas(modifier = Modifier.size(220.dp)) {
        drawCircle(Color(0xFFF0EFFF), radius = 100f)
        drawCircle(Color(0xFFDEDCFF), radius = 82f)
        drawCircle(Color(0xFFC8C4FF), radius = 63f)
        drawCircle(Color(0xFFB1ABFF), radius = 45f)
        drawRoundRect(
            color = Color.White,
            topLeft = center + Offset(-8f, -25f),
            size = Size(16f, 38f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
        )
        drawArc(
            color = Color.White,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = center + Offset(-18f, -5f),
            size = Size(36f, 32f),
            style = Stroke(6f),
        )
        drawLine(Color.White, center + Offset(0f, 27f), center + Offset(0f, 38f), 6f)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun OnboardingScreenPreview() {
    EmmaViDroidCallTheme(dynamicColor = false) {
        OnboardingScreen(onFinished = {})
    }
}
