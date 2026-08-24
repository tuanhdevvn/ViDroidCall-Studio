package com.example.ViDroidCall_Studio.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ViDroidCall_Studio.R
import com.example.ViDroidCall_Studio.data.local.FontSizePreferences
import com.example.ViDroidCall_Studio.ui.component.bounceClick
import com.example.ViDroidCall_Studio.ui.theme.AppPrimary
import kotlin.math.roundToInt

/**
 * Màn hình Cài đặt Cỡ chữ - Giao diện Thân thiện, Ấm áp và Dễ dùng cho mọi lứa tuổi
 */
@Composable
fun FontSizeSettingsScreen(
    currentScale: Float,
    onScaleChange: (Float) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember(currentScale) { mutableFloatStateOf(currentScale) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 1. Header thân thiện với nút Quay lại tròn trịa
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .bounceClick(scaleDown = 0.85f, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = "Cỡ chữ",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Thẻ Xem Trước Mẫu (Clean Preview Card)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Xem trước",
                        fontSize = (15 * sliderValue).sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "“Gọi điện cho Mẹ”",
                    fontSize = (20 * sliderValue).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Đặt báo thức 7 giờ sáng",
                    fontSize = (16 * sliderValue).sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Khung Điều chỉnh Cỡ chữ Thân thiện (Friendly Font Size Controller)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Tiêu đề mức kích thước
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kích thước chữ",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = FontSizePreferences.getScaleDescription(sliderValue),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppPrimary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Thanh trượt mượt mà với 2 chữ 'A' mềm mại
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "A",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { newValue ->
                            sliderValue = ((newValue * 20).roundToInt() / 20f)
                            onScaleChange(sliderValue)
                        },
                        valueRange = FontSizePreferences.MIN_FONT_SCALE..FontSizePreferences.MAX_FONT_SCALE,
                        steps = 4,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = AppPrimary,
                            activeTrackColor = AppPrimary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "A",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppPrimary
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Thanh chọn nhanh 4 mức kiểu Segmented Bar bo tròn mềm mại
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val presets = listOf(
                            0.85f to "Nhỏ",
                            1.0f to "Vừa",
                            1.15f to "Lớn",
                            1.30f to "Rất lớn"
                        )
                        presets.forEach { (scale, label) ->
                            val isSelected = (sliderValue - scale).let { it in -0.05f..0.05f }
                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                                label = "presetBg"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) AppPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "presetText"
                            )

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClick(scaleDown = 0.92f, onClick = {
                                        sliderValue = scale
                                        onScaleChange(scale)
                                    }),
                                shape = RoundedCornerShape(12.dp),
                                color = bgColor,
                                shadowElevation = if (isSelected) 3.dp else 0.dp
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 9.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Nút Khôi phục mặc định thân thiện
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .bounceClick(onClick = {
                    sliderValue = FontSizePreferences.DEFAULT_FONT_SCALE
                    onScaleChange(FontSizePreferences.DEFAULT_FONT_SCALE)
                }),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 13.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Khôi phục cỡ chữ mặc định",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
