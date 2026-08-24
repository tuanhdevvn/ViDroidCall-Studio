package com.example.ViDroidCall_Studio.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ViDroidCall_Studio.data.local.AppTheme
import com.example.ViDroidCall_Studio.data.local.FontSizePreferences
import com.example.ViDroidCall_Studio.data.local.ThemePreferences
import com.example.ViDroidCall_Studio.ui.component.bounceClick
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Tab Màn hình Cài đặt All-in-one tinh gọn, đẹp mắt:
 * - Chọn Chủ đề (Theme) Sáng / Tối / Hệ thống trực tiếp.
 * - Điều chỉnh Cỡ chữ với thanh trượt nút tròn thân thiện & chính xác 4 nấc chọn nhanh.
 * - Đã loại bỏ hoàn toàn thẻ xem trước và nút khôi phục mặc định theo yêu cầu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePreferences = remember { ThemePreferences(context) }
    val fontSizePreferences = remember { FontSizePreferences(context) }

    val currentTheme by themePreferences.themeFlow.collectAsState(initial = AppTheme.LIGHT)
    val currentFontScale by fontSizePreferences.fontScaleFlow.collectAsState(initial = FontSizePreferences.DEFAULT_FONT_SCALE)

    var sliderValue by remember(currentFontScale) { mutableFloatStateOf(currentFontScale) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header Cài đặt tinh gọn
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Cài đặt",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 2. Thẻ Chọn Chủ Đề (Theme Segmented 3 Cards)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Tiêu đề phần Giao diện
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Giao diện",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = when (currentTheme) {
                                AppTheme.LIGHT -> "Sáng"
                                AppTheme.DARK -> "Tối"
                                AppTheme.SYSTEM -> "Hệ thống"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Thanh chọn 3 Chủ đề thiết kế cao cấp (Icon trên - Chữ dưới) không bao giờ bị tràn/xuống dòng
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val themeOptions = listOf(
                            Triple(AppTheme.LIGHT, "Sáng", Icons.Rounded.LightMode),
                            Triple(AppTheme.DARK, "Tối", Icons.Rounded.DarkMode),
                            Triple(AppTheme.SYSTEM, "Hệ thống", Icons.Rounded.PhoneAndroid)
                        )

                        val primaryColor = MaterialTheme.colorScheme.primary

                        themeOptions.forEach { (theme, label, icon) ->
                            val isSelected = currentTheme == theme
                            val accentColor = when (theme) {
                                AppTheme.LIGHT -> Color(0xFFF59E0B)
                                AppTheme.DARK -> Color(0xFF818CF8)
                                AppTheme.SYSTEM -> primaryColor
                            }

                            val cardBg by animateColorAsState(
                                targetValue = if (isSelected) {
                                    primaryColor.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                label = "themeCardBg"
                            )
                            val borderColor by animateColorAsState(
                                targetValue = if (isSelected) {
                                    primaryColor
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                },
                                label = "themeBorder"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) {
                                    primaryColor
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                label = "themeText"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(cardBg, RoundedCornerShape(18.dp))
                                    .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(18.dp))
                                    .bounceClick(scaleDown = 0.90f, onClick = {
                                        scope.launch {
                                            themePreferences.setTheme(theme)
                                        }
                                    })
                                    .padding(vertical = 14.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                color = if (isSelected) accentColor.copy(alpha = 0.18f) else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor,
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Thẻ Điều Chỉnh Cỡ Chữ (Thanh trượt nút tròn thân thiện & 4 nấc chọn nhanh)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Tiêu đề Cỡ chữ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF6366F1).copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FormatSize,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Cỡ chữ",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = FontSizePreferences.getScaleDescription(sliderValue),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Danh sách 4 nấc kích thước chuẩn
                    val presets = listOf(
                        0.85f to "Nhỏ",
                        1.0f to "Vừa",
                        1.15f to "Lớn",
                        1.30f to "Rất lớn"
                    )
                    val presetScales = presets.map { it.first }
                    val currentStepIndex = presetScales.indexOfFirst { (it - sliderValue).let { diff -> diff in -0.05f..0.05f } }.takeIf { it != -1 } ?: 1
                    var stepFloatValue by remember(sliderValue) { mutableFloatStateOf(currentStepIndex.toFloat()) }

                    // Thanh trượt nút tròn thân thiện với chính xác 4 step
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
                            value = stepFloatValue,
                            onValueChange = { newIdx ->
                                stepFloatValue = newIdx
                                val targetIdx = newIdx.roundToInt().coerceIn(0, 3)
                                val targetScale = presetScales[targetIdx]
                                sliderValue = targetScale
                                scope.launch {
                                    fontSizePreferences.setFontScale(targetScale)
                                }
                            },
                            valueRange = 0f..3f,
                            steps = 2, // Đúng chính xác 4 nấc (0, 1, 2, 3) tương ứng 4 nút bấm
                            thumb = {
                                // Nút trượt tròn to, thân thiện với bóng đổ mềm
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .shadow(4.dp, CircleShape)
                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                        .border(3.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            },
                            track = {
                                // Thanh ray với 4 điểm tròn thân thiện
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    // Thanh tiến trình đã kéo
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction = (stepFloatValue / 3f).coerceIn(0f, 1f))
                                            .height(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = "A",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Thanh chọn nhanh 4 mức Segmented Nút Tròn Thân Thiện
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val primaryColor = MaterialTheme.colorScheme.primary

                        presets.forEachIndexed { index, (scale, label) ->
                            val isSelected = (sliderValue - scale).let { it in -0.05f..0.05f }
                            val btnBg by animateColorAsState(
                                targetValue = if (isSelected) {
                                    primaryColor.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f)
                                },
                                label = "presetBg"
                            )
                            val borderCol by animateColorAsState(
                                targetValue = if (isSelected) {
                                    primaryColor
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                },
                                label = "presetBorder"
                            )
                            val textCol by animateColorAsState(
                                targetValue = if (isSelected) {
                                    primaryColor
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                label = "presetText"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(btnBg, RoundedCornerShape(14.dp))
                                    .border(if (isSelected) 1.5.dp else 1.dp, borderCol, RoundedCornerShape(14.dp))
                                    .bounceClick(scaleDown = 0.92f, onClick = {
                                        sliderValue = scale
                                        stepFloatValue = index.toFloat()
                                        scope.launch {
                                            fontSizePreferences.setFontScale(scale)
                                        }
                                    })
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textCol,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Thông tin phiên bản (App Info Footer)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "ViDroidCall Studio",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "v1.0.0",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
