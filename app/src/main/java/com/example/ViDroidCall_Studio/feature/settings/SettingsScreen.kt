// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.feature.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.ViDroidCall_Studio.data.local.AppTheme
import com.example.ViDroidCall_Studio.data.local.FontSizePreferences
import com.example.ViDroidCall_Studio.data.local.ThemePreferences
import com.example.ViDroidCall_Studio.data.local.TroLyNoiPreferences
import com.example.ViDroidCall_Studio.ui.component.IosStyleSwitch
import com.example.ViDroidCall_Studio.ui.component.OverlayPermissionDialog
import com.example.ViDroidCall_Studio.util.TroLyNoiPermissions
import com.example.ViDroidCall_Studio.data.local.feedback.NluFeedbackEntry
import com.example.ViDroidCall_Studio.data.local.feedback.NluFeedbackLogRepository
import com.example.ViDroidCall_Studio.data.nlu.NluModelState
import com.example.ViDroidCall_Studio.ui.component.bounceClick
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Màn hình Cài đặt Thiết lập Ứng dụng - Tinh tế, Hiện đại & Thân thiện
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modelState: NluModelState = NluModelState.Uninitialized,
    feedbackRepository: NluFeedbackLogRepository? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePreferences = remember { ThemePreferences(context) }
    val fontSizePreferences = remember { FontSizePreferences(context) }
    val troLyNoiPreferences = remember { TroLyNoiPreferences(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val currentTheme by themePreferences.themeFlow.collectAsState(initial = AppTheme.LIGHT)
    val currentFontScale by fontSizePreferences.fontScaleFlow.collectAsState(initial = FontSizePreferences.DEFAULT_FONT_SCALE)
    val troLyNoiEnabled by troLyNoiPreferences.enabledFlow.collectAsState(initial = false)
    var isSwitchBusy by remember { mutableStateOf(false) }

    var awaitingOverlaySettings by remember { mutableStateOf(false) }
    var showOverlayPermissionDialog by remember { mutableStateOf(false) }
    val continueEnableHolder = remember { object { var invoke: () -> Unit = {} } }

    fun disableTroLyNoi() {
        awaitingOverlaySettings = false
        showOverlayPermissionDialog = false
        scope.launch {
            troLyNoiPreferences.setEnabled(false)
            @Suppress("DEPRECATION")
            troLyNoiPreferences.setWakeWordEnabled(false)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) continueEnableHolder.invoke() else {
            Toast.makeText(context, "Chưa cấp micro, Trợ lý nổi vẫn tắt", Toast.LENGTH_SHORT).show()
            disableTroLyNoi()
        }
    }
    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) continueEnableHolder.invoke() else {
            Toast.makeText(context, "Chưa cấp thông báo, Trợ lý nổi vẫn tắt", Toast.LENGTH_SHORT).show()
            disableTroLyNoi()
        }
    }

    continueEnableHolder.invoke = {
        when (
            TroLyNoiPermissions.nextMissingPermission(
                hasRecordAudio = TroLyNoiPermissions.hasRecordAudio(context),
                notificationsRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                hasNotifications = TroLyNoiPermissions.hasNotifications(context),
                canDrawOverlays = TroLyNoiPermissions.canDrawOverlays(context),
            )
        ) {
            TroLyNoiPermissions.Gate.RECORD_AUDIO ->
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            TroLyNoiPermissions.Gate.POST_NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            TroLyNoiPermissions.Gate.SYSTEM_ALERT_WINDOW -> {
                showOverlayPermissionDialog = true
            }
            TroLyNoiPermissions.Gate.NONE -> {
                awaitingOverlaySettings = false
                showOverlayPermissionDialog = false
                scope.launch {
                    troLyNoiPreferences.setEnabled(true)
                    @Suppress("DEPRECATION")
                    troLyNoiPreferences.setWakeWordEnabled(true)
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, troLyNoiEnabled, awaitingOverlaySettings) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            val hasAll = TroLyNoiPermissions.hasAllRequired(context)
            if (troLyNoiEnabled && !hasAll) {
                disableTroLyNoi()
                return@LifecycleEventObserver
            }
            if (awaitingOverlaySettings) {
                if (TroLyNoiPermissions.canDrawOverlays(context)) {
                    continueEnableHolder.invoke()
                } else {
                    Toast.makeText(
                        context,
                        "Chưa cho phép hiện trên ứng dụng khác, Trợ lý nổi vẫn tắt",
                        Toast.LENGTH_SHORT
                    ).show()
                    disableTroLyNoi()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var sliderValue by remember(currentFontScale) { mutableFloatStateOf(currentFontScale) }

    var feedbackCount by remember { mutableIntStateOf(0) }
    var feedbackEntries by remember { mutableStateOf<List<NluFeedbackEntry>>(emptyList()) }
    var showClearFeedbackDialog by remember { mutableStateOf(false) }
    var pendingDeleteEntry by remember { mutableStateOf<NluFeedbackEntry?>(null) }
    var feedbackRefreshKey by remember { mutableIntStateOf(0) }
    val clipboardManager = LocalClipboardManager.current

    fun refreshFeedbackLog() {
        scope.launch {
            feedbackCount = feedbackRepository?.count() ?: 0
            feedbackEntries = feedbackRepository?.readAll().orEmpty().reversed()
        }
    }

    fun shareFeedbackLog() {
        val file = feedbackRepository?.getLogFile()
        if (file == null || !file.exists()) {
            Toast.makeText(context, "Chưa có file mẫu sai để chia sẻ", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, NluFeedbackLogRepository.LOG_FILE_NAME)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Chia sẻ mẫu sai NLU"))
        } catch (error: Exception) {
            Toast.makeText(context, "Không chia sẻ được: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(feedbackRepository, feedbackRefreshKey) {
        refreshFeedbackLog()
    }

    if (showClearFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showClearFeedbackDialog = false },
            title = { Text("Xóa toàn bộ mẫu sai?") },
            text = { Text("Thao tác này sẽ xóa $feedbackCount mẫu đã lưu và không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearFeedbackDialog = false
                        scope.launch {
                            feedbackRepository?.clearAll()
                                ?.onSuccess {
                                    feedbackRefreshKey++
                                    Toast.makeText(context, "Đã xóa toàn bộ mẫu sai", Toast.LENGTH_SHORT).show()
                                }
                                ?.onFailure { error ->
                                    Toast.makeText(context, "Xóa thất bại: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                ) {
                    Text("Xóa", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearFeedbackDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    pendingDeleteEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDeleteEntry = null },
            title = { Text("Xóa mẫu này?") },
            text = { Text("Câu “${entry.sttText}” sẽ bị xóa khỏi file JSONL.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDeleteEntry = null
                        scope.launch {
                            feedbackRepository?.deleteByIndex(entry.index)
                                ?.onSuccess {
                                    feedbackRefreshKey++
                                    Toast.makeText(context, "Đã xóa mẫu", Toast.LENGTH_SHORT).show()
                                }
                                ?.onFailure { error ->
                                    Toast.makeText(context, "Xóa thất bại: ${error.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                ) {
                    Text("Xóa", color = Color(0xFFDC2626))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteEntry = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showOverlayPermissionDialog) {
        OverlayPermissionDialog(
            onOpenSettings = {
                showOverlayPermissionDialog = false
                awaitingOverlaySettings = true
                try {
                    context.startActivity(TroLyNoiPermissions.overlaySettingsIntent(context))
                } catch (_: Exception) {
                    awaitingOverlaySettings = false
                    Toast.makeText(context, "Không mở được Cài đặt quyền overlay", Toast.LENGTH_SHORT).show()
                    disableTroLyNoi()
                }
            },
            onDismiss = {
                showOverlayPermissionDialog = false
                Toast.makeText(
                    context,
                    "Chưa cho phép hiện trên ứng dụng khác, Trợ lý nổi vẫn tắt",
                    Toast.LENGTH_SHORT
                ).show()
                disableTroLyNoi()
            }
        )
    }

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
                    .settingsCardShadow(RoundedCornerShape(24.dp)),
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
                    .settingsCardShadow(RoundedCornerShape(24.dp)),
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

                    // Thanh trượt nút tròn cao cấp với các điểm mốc tinh tế, không bị bóng ma hay đè vòng tròn
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

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val surfaceColor = MaterialTheme.colorScheme.surface
                            val trackBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            val inactiveDotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

                            Slider(
                                value = stepFloatValue,
                                onValueChange = { newIdx ->
                                    stepFloatValue = newIdx
                                    val targetIdx = newIdx.roundToInt().coerceIn(0, 3)
                                    val targetScale = presetScales[targetIdx]
                                    sliderValue = targetScale
                                },
                                onValueChangeFinished = {
                                    val targetIdx = stepFloatValue.roundToInt().coerceIn(0, 3)
                                    stepFloatValue = targetIdx.toFloat()
                                    val targetScale = presetScales[targetIdx]
                                    sliderValue = targetScale
                                    scope.launch {
                                        fontSizePreferences.setFontScale(targetScale)
                                    }
                                },
                                valueRange = 0f..3f,
                                thumb = {
                                    Surface(
                                        shape = CircleShape,
                                        color = surfaceColor,
                                        shadowElevation = 5.dp,
                                        border = BorderStroke(3.dp, primaryColor),
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .background(primaryColor, CircleShape)
                                            )
                                        }
                                    }
                                },
                                track = {
                                    androidx.compose.foundation.Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                    ) {
                                        val trackH = 8.dp.toPx()
                                        val topOff = (size.height - trackH) / 2f
                                        val radius = androidx.compose.ui.geometry.CornerRadius(trackH / 2f, trackH / 2f)

                                        val thumbRadius = 13.dp.toPx()
                                        val usableWidth = (size.width - thumbRadius * 2f).coerceAtLeast(0f)
                                        val thumbX = thumbRadius + usableWidth * (stepFloatValue / 3f).coerceIn(0f, 1f)

                                        // 1. Ray nền toàn phần bo tròn
                                        drawRoundRect(
                                            color = trackBgColor,
                                            topLeft = androidx.compose.ui.geometry.Offset(0f, topOff),
                                            size = androidx.compose.ui.geometry.Size(size.width, trackH),
                                            cornerRadius = radius
                                        )

                                        // 2. Đoạn đã kéo qua (ôm trọn đến tâm nút trượt)
                                        if (thumbX > 0f) {
                                            drawRoundRect(
                                                color = primaryColor,
                                                topLeft = androidx.compose.ui.geometry.Offset(0f, topOff),
                                                size = androidx.compose.ui.geometry.Size((thumbX + thumbRadius * 0.5f).coerceAtMost(size.width), trackH),
                                                cornerRadius = radius
                                            )
                                        }

                                        // 3. Bốn điểm mốc tính toán chuẩn xác theo quỹ đạo của tâm nút trượt
                                        val dotR = 2.5.dp.toPx()
                                        for (i in 0..3) {
                                            val dotX = thumbRadius + usableWidth * (i / 3f)
                                            val dist = kotlin.math.abs(thumbX - dotX)

                                            // Ẩn điểm mốc khi nút trượt đè lên để không bị lỗi 2 vòng tròn
                                            if (dist > 15.dp.toPx()) {
                                                val isPassed = i.toFloat() <= stepFloatValue
                                                drawCircle(
                                                    color = if (isPassed) surfaceColor.copy(alpha = 0.9f) else inactiveDotColor,
                                                    radius = dotR,
                                                    center = androidx.compose.ui.geometry.Offset(dotX, size.height / 2f)
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

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

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .settingsCardShadow(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    // Hàng 1: Trợ lý nổi (Master Switch)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Trợ lý nổi",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Nói “Trợ lý ơi” hiện hộp thoại nổi đè lên ứng dụng khác",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IosStyleSwitch(
                            checked = troLyNoiEnabled,
                            onCheckedChange = { turnOn ->
                                if (isSwitchBusy) return@IosStyleSwitch
                                isSwitchBusy = true
                                scope.launch {
                                    kotlinx.coroutines.delay(800)
                                    isSwitchBusy = false
                                }
                                if (turnOn) {
                                    continueEnableHolder.invoke()
                                } else {
                                    disableTroLyNoi()
                                }
                            },
                            enabled = !isSwitchBusy,
                            contentDescription = if (troLyNoiEnabled) {
                                "Tắt Trợ lý nổi"
                            } else {
                                "Bật Trợ lý nổi"
                            }
                        )
                    }
                }
            }
        }

        // 5. Thẻ mẫu sai NLU (R&D: STT + JSON GGUF)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .settingsCardShadow(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFDC2626).copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BookmarkAdd,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Mẫu sai NLU",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Câu STT + JSON GGUF đã lưu để export",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDC2626).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$feedbackCount mẫu",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (feedbackEntries.isEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Chưa có mẫu sai. Sau câu On-Device AI (GGUF), bấm “Lưu mẫu sai” trên thẻ Kết quả phân tích.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClick(scaleDown = 0.94f, onClick = { shareFeedbackLog() })
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Share file",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFDC2626).copy(alpha = 0.12f),
                                modifier = Modifier
                                    .weight(1f)
                                    .bounceClick(scaleDown = 0.94f, onClick = { showClearFeedbackDialog = true })
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteSweep,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Xóa hết",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626),
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            feedbackEntries.forEach { entry ->
                                FeedbackEntryRow(
                                    entry = entry,
                                    onDelete = { pendingDeleteEntry = entry },
                                    onCopyJson = {
                                        clipboardManager.setText(AnnotatedString(entry.modelOutputJson))
                                        Toast.makeText(context, "Đã sao chép JSON", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Thẻ Mô Hình AI (Tách riêng hàng Tiêu đề & Hộp hiển thị đầy đủ tên Model)
        item {
            val modelName = when (modelState) {
                is NluModelState.Ready -> modelState.modelPath
                is NluModelState.Loading -> "Đang nạp mô hình..."
                is NluModelState.ModelNotFound -> "Chưa có file mô hình trong Download"
                is NluModelState.Error -> "Lỗi mô hình"
                is NluModelState.Uninitialized -> "Đang khởi tạo..."
            }
            val statusColor = when (modelState) {
                is NluModelState.Ready -> Color(0xFF10B981)
                is NluModelState.Loading -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .settingsCardShadow(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Header: Tiêu đề + Badge Trạng thái
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Mô hình AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = statusColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = when (modelState) {
                                    is NluModelState.Ready -> "Đã sẵn sàng"
                                    is NluModelState.Loading -> "Đang nạp"
                                    else -> "Chưa nạp"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Khối hiển thị Tên Model tách biệt rõ ràng, không bị cắt chữ
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = modelName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (modelState is NluModelState.Ready) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // 6. Thông tin phiên bản (App Info Footer)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .settingsCardShadow(RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
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

@Composable
private fun FeedbackEntryRow(
    entry: NluFeedbackEntry,
    onDelete: () -> Unit,
    onCopyJson: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val savedAtLabel = remember(entry.savedAt) { formatFeedbackSavedAt(entry.savedAt) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick(scaleDown = 0.98f, onClick = { expanded = !expanded })
                ) {
                    Text(
                        text = entry.sttText.ifBlank { "(trống)" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (expanded) 4 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = savedAtLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.ExpandMore,
                            contentDescription = if (expanded) "Thu gọn JSON" else "Xem JSON",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(if (expanded) 180f else 0f)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Xóa mẫu",
                    tint = Color(0xFFDC2626),
                    modifier = Modifier
                        .size(22.dp)
                        .bounceClick(scaleDown = 0.9f, onClick = onDelete)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = entry.modelOutputJson,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF38BDF8),
                            lineHeight = 18.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.bounceClick(scaleDown = 0.9f, onClick = onCopyJson)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Copy JSON",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFeedbackSavedAt(savedAt: Long): String {
    if (savedAt <= 0L) return "Chưa rõ thời gian"
    return SimpleDateFormat("HH:mm · dd/MM", Locale.forLanguageTag("vi-VN")).format(Date(savedAt))
}

private fun Modifier.settingsCardShadow(shape: RoundedCornerShape): Modifier = shadow(
    elevation = 6.dp,
    shape = shape,
    clip = false,
    ambientColor = Color.Black.copy(alpha = 0.08f),
    spotColor = Color.Black.copy(alpha = 0.10f),
)
