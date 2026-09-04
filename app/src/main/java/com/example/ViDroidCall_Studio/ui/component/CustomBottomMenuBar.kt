package com.example.ViDroidCall_Studio.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ViDroidCall_Studio.R
import com.example.ViDroidCall_Studio.ui.theme.AppPrimary

/**
 * Modifier tạo hiệu ứng co giãn (Bounce / Scale In-Out) mượt mà khi chạm vào và nhả ra.
 */
@Composable
fun Modifier.bounceClick(
    scaleDown: Float = 0.85f,
    onClick: () -> Unit
): Modifier {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceClickScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    try {
                        tryAwaitRelease()
                    } finally {
                        isPressed = false
                    }
                },
                onTap = { onClick() }
            )
        }
}

/**
 * Các mục trên thanh menu điều hướng:
 * Bên trái: Lịch sử lệnh
 * Ở giữa: Micro (Phần tròn nút Micro)
 * Bên phải: Cài đặt
 */
enum class NavTab(val title: String, val icon: ImageVector) {
    HISTORY("Lịch sử", Icons.Rounded.History),
    ASSISTANT("Hỏi đáp", Icons.Rounded.Mic),
    SETTINGS("Cài đặt", Icons.Rounded.Settings)
}

class BottomNavCutoutShape(
    private val buttonRadiusDp: Float = 36f,      // Bán kính nút tròn (72dp / 2 = 36dp)
    private val gapDp: Float = 6f,               // Khe hở đều ôm khít xung quanh nút (6dp)
    private val buttonCenterYDp: Float = 8f,     // Tọa độ Y tâm nút so với mép trên bar (8dp)
    private val shoulderWidthDp: Float = 7f,     // Độ rộng chuyển tiếp vai bo gọn gàng (7dp)
    private val cornerRadiusDp: Float = 0f       // Không bo góc trái phải của bar
) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val d = density.density
        val width = size.width
        val height = size.height
        val center = width / 2f

        val cRadius = cornerRadiusDp * d
        val cutR = (buttonRadiusDp + gapDp) * d         // Bán kính vòng cung lõm (42dp)
        val centerY = buttonCenterYDp * d               // Tâm Y của vòng tròn lõm (8dp)
        val shoulder = shoulderWidthDp * d              // Chiều rộng vai lượn (7dp)
        val k = 0.55228475f * cutR                      // Hằng số bezier cho cung tròn 1/4 chuẩn xác

        val startNotch = center - cutR - shoulder       // Điểm bắt đầu lượn xuống
        val endNotch = center + cutR + shoulder         // Điểm kết thúc lượn lên

        val path = androidx.compose.ui.graphics.Path().apply {
            // Bắt đầu từ góc trên bên trái
            moveTo(0f, cRadius)
            if (cRadius > 0f) {
                quadraticTo(0f, 0f, cRadius, 0f)
            } else {
                moveTo(0f, 0f)
            }

            // Đoạn thẳng tới mép bắt đầu của notch
            lineTo(startNotch, 0f)

            // 1. Vai trái: lượn mềm mại từ mép ngang vào xích đạo
            cubicTo(
                startNotch + shoulder * 0.55f, 0f,
                center - cutR, centerY - shoulder * 0.45f,
                center - cutR, centerY
            )

            // 2. Cung tròn 1/4 bên trái: ôm sát nút tròn
            cubicTo(
                center - cutR, centerY + k,
                center - k, centerY + cutR,
                center, centerY + cutR
            )

            // 3. Cung tròn 1/4 bên phải: ôm sát nút tròn
            cubicTo(
                center + k, centerY + cutR,
                center + cutR, centerY + k,
                center + cutR, centerY
            )

            // 4. Vai phải: lượn mềm mại từ xích đạo lên mép ngang
            cubicTo(
                center + cutR, centerY - shoulder * 0.45f,
                endNotch - shoulder * 0.55f, 0f,
                endNotch, 0f
            )

            // Mép trên bên phải
            lineTo(width - cRadius, 0f)
            if (cRadius > 0f) {
                quadraticTo(width, 0f, width, cRadius)
            }

            // Mép dưới bên phải
            lineTo(width, height)

            // Mép dưới bên trái
            lineTo(0f, height)

            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

/**
 * Thanh menu điều hướng phía dưới (Custom Bottom Navigation Bar)
 * - Tông màu chủ đạo: 0xFF0866FF & Theme Surface
 * - Phần hình tròn ở giữa là Nút Micro nổi bật
 * - Bên trái: Lịch sử lệnh
 * - Bên phải: Cài đặt
 * - Hiệu ứng chạm phóng to / thu nhỏ linh hoạt (Bounce Scale animation)
 */
@Composable
fun CustomBottomMenuBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    onMicClick: () -> Unit,
    isListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val notchShape = remember { BottomNavCutoutShape() }
        
        // Thanh Nền Menu chính
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = notchShape,
                    spotColor = Color(0xFF0866FF).copy(alpha = 0.25f),
                    ambientColor = Color(0xFF0866FF).copy(alpha = 0.15f)
                ),
            shape = notchShape,
            color = surfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MỤC BÊN TRÁI: Lịch sử lệnh
                NavItem(
                    tab = NavTab.HISTORY,
                    isSelected = selectedTab == NavTab.HISTORY,
                    onSelect = { onTabSelected(NavTab.HISTORY) },
                    modifier = Modifier.weight(1f)
                )

                // Khoảng trống ở giữa dành cho Nút Micro lõm xuống
                Spacer(modifier = Modifier.width(88.dp))

                // MỤC BÊN PHẢI: Cài đặt
                NavItem(
                    tab = NavTab.SETTINGS,
                    isSelected = selectedTab == NavTab.SETTINGS,
                    onSelect = { onTabSelected(NavTab.SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // NÚT PHẦN TRÒN Ở GIỮA (MICROPHONE FAB)
        CenterMicButton(
            isTabSelected = selectedTab == NavTab.ASSISTANT,
            isListening = isListening && selectedTab == NavTab.ASSISTANT,
            onMicClick = onMicClick,
            modifier = Modifier.offset(y = (-28).dp)
        )
    }
}

private val NavTabSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * Từng item icon trên Menu bar — animation chuyển tab mượt (scale, màu, pill, gạch indicator).
 */
@Composable
private fun NavItem(
    tab: NavTab,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

    val selectionProgress by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = NavTabSpring,
        label = "navSelectionProgress"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = NavTabSpring,
        label = "itemSelectedScale"
    )

    val iconTint by animateColorAsState(
        targetValue = lerp(inactiveColor, activeColor, selectionProgress),
        animationSpec = tween(durationMillis = 220),
        label = "navIconTint"
    )

    val labelColor by animateColorAsState(
        targetValue = lerp(inactiveColor, activeColor, selectionProgress),
        animationSpec = tween(durationMillis = 220),
        label = "navLabelColor"
    )

    Column(
        modifier = modifier
            .bounceClick(scaleDown = 0.88f, onClick = onSelect)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            }
        ) {
            // Pill nền — fade + scale theo tiến trình chọn tab
            Box(
                modifier = Modifier
                    .size(50.dp, 30.dp)
                    .graphicsLayer {
                        alpha = selectionProgress
                        scaleX = 0.75f + 0.25f * selectionProgress
                        scaleY = 0.75f + 0.25f * selectionProgress
                    }
                    .background(
                        color = activeColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(15.dp)
                    )
            )
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = tab.title,
            fontSize = 14.sp,
            fontWeight = if (selectionProgress > 0.5f) FontWeight.Bold else FontWeight.Medium,
            color = labelColor
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Gạch indicator — co giãn ngang khi chọn tab
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(3.dp)
                .graphicsLayer {
                    scaleX = selectionProgress
                    alpha = selectionProgress
                }
                .background(activeColor, RoundedCornerShape(2.dp))
        )
    }
}

/**
 * Nút phần tròn Micro ở giữa thanh Bottom Navigation Bar
 * Có hiệu ứng phát sáng, nhịp thở (pulse animation) khi đang lắng nghe và hiệu ứng Chạm (bounceClick scale)
 */
@Composable
private fun CenterMicButton(
    isTabSelected: Boolean,
    isListening: Boolean,
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabSelectionScale by animateFloatAsState(
        targetValue = if (isTabSelected) 1.04f else 0.98f,
        animationSpec = NavTabSpring,
        label = "micTabSelectionScale"
    )

    val micElevation by animateDpAsState(
        targetValue = if (isTabSelected) 12.dp else 8.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "micElevation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "MicPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier.size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        // Vòng hào quang xung quanh CHỈ khi đang lắng nghe (KHÔNG vẽ đĩa trắng khi nghỉ)
        if (isListening) {
            Canvas(modifier = Modifier.size(92.dp)) {
                drawCircle(
                    color = AppPrimary.copy(alpha = pulseAlpha),
                    radius = size.minDimension / 2 * pulseScale
                )
                drawCircle(
                    color = AppPrimary.copy(alpha = 0.3f),
                    radius = size.minDimension / 2 * 1.15f,
                    style = Stroke(width = 2.5f)
                )
            }
        }

        // Khối hình tròn chính màu 0xFF0866FF
        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer {
                    scaleX = tabSelectionScale
                    scaleY = tabSelectionScale
                }
                .shadow(
                    elevation = micElevation,
                    shape = CircleShape,
                    spotColor = AppPrimary,
                    ambientColor = AppPrimary.copy(alpha = 0.4f)
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2B7FFF),
                            AppPrimary,
                            Color(0xFF0052D6)
                        )
                    ),
                    shape = CircleShape
                )
                .bounceClick(scaleDown = 0.75f, onClick = onMicClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_app),
                contentDescription = "Logo App",
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}
