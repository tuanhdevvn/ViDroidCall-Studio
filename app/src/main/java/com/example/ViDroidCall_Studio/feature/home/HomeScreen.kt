package com.example.ViDroidCall_Studio.feature.home

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ViDroidCall_Studio.data.local.history.CommandHistoryRepository
import com.example.ViDroidCall_Studio.data.nlu.NluActionDispatcher
import com.example.ViDroidCall_Studio.data.nlu.NluEngineManager
import com.example.ViDroidCall_Studio.data.nlu.NluModelState
import com.example.ViDroidCall_Studio.domain.model.NativeAction
import com.example.ViDroidCall_Studio.feature.assistant.AssistantScreen
import com.example.ViDroidCall_Studio.feature.history.HistoryScreen
import com.example.ViDroidCall_Studio.feature.settings.SettingsScreen
import com.example.ViDroidCall_Studio.feature.speech.rememberSpeechToText
import com.example.ViDroidCall_Studio.feature.speech.rememberTextToSpeech
import com.example.ViDroidCall_Studio.ui.component.CustomBottomMenuBar
import com.example.ViDroidCall_Studio.ui.component.NavTab
import com.example.ViDroidCall_Studio.ui.component.StoragePermissionDialog
import com.example.ViDroidCall_Studio.ui.theme.ViDroidCallTheme
import com.example.ViDroidCall_Studio.util.StoragePermissionHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedTab by remember { mutableStateOf(NavTab.ASSISTANT) }

    // Quản lý phản hồi giọng nói TTS (Text-To-Speech)
    val textToSpeech = rememberTextToSpeech()

    // Quản lý Lịch sử câu lệnh ngoại tuyến (SQLite Repository)
    val historyRepository = remember { CommandHistoryRepository(context.applicationContext) }
    val historyItems by historyRepository.historyFlow.collectAsState(initial = emptyList())

    // Quản lý NLU Engine
    val nluEngineManager = remember { NluEngineManager(context.applicationContext) }
    
    // Quản lý trạng thái nạp Mô hình AI & Quyền bộ nhớ
    val modelState by nluEngineManager.modelState.collectAsState()
    val isNluProcessing by nluEngineManager.isGenerating.collectAsState()
    val nluResult by nluEngineManager.lastResult.collectAsState()

    var hasStoragePermission by remember {
        mutableStateOf(StoragePermissionHelper.hasStoragePermission(context))
    }
    var showStoragePermissionDialog by remember { mutableStateOf(false) }
    var hasAutoPromptedPermission by remember { mutableStateOf(false) }

    // Danh sách quyền hệ thống thiết yếu cần yêu cầu khi người dùng khởi động vào App
    val initialPermissions = remember {
        listOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS
        )
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Kết quả cấp quyền ban đầu đã được hệ thống ghi nhận
    }

    // Tự động kiểm tra và hiện hộp thoại xin cấp các quyền cần thiết ngay khi vào ứng dụng
    LaunchedEffect(Unit) {
        val permissionsToRequest = initialPermissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            multiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Tự động kiểm tra lại quyền và quét nạp model khi user quay lại App (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = StoragePermissionHelper.hasStoragePermission(context)
                hasStoragePermission = granted
                if (granted && modelState !is NluModelState.Ready && modelState !is NluModelState.Loading) {
                    nluEngineManager.autoDetectAndLoadModel()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Tự động phát hiện thiếu quyền bộ nhớ khi chưa có model và mở hộp thoại thông báo
    LaunchedEffect(modelState, hasStoragePermission) {
        if (!hasStoragePermission && modelState is NluModelState.ModelNotFound && !hasAutoPromptedPermission) {
            hasAutoPromptedPermission = true
            showStoragePermissionDialog = true
        }
    }

    // Quản lý Trạng thái Action Dispatcher & Dialog xác nhận
    var pendingAction by remember { mutableStateOf<NativeAction?>(null) }
    var pendingPermissionAction by remember { mutableStateOf<NativeAction?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var lastProcessedResultId by remember { mutableStateOf<String?>(null) }
    var actionDispatcherRef by remember { mutableStateOf<NluActionDispatcher?>(null) }

    // Launcher yêu cầu quyền runtime hệ thống Android (READ_CONTACTS, v.v.)
    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(context, "Đã cấp quyền thành công", Toast.LENGTH_SHORT).show()
            val action = pendingPermissionAction
            pendingPermissionAction = null
            if (action != null) {
                // Tự động thực thi tiếp hành động đang chờ sau khi người dùng nhấn Cho phép
                actionDispatcherRef?.executeNativeAction(action)
            }
        } else {
            Toast.makeText(context, "Bạn đã từ chối cấp quyền", Toast.LENGTH_SHORT).show()
            pendingPermissionAction = null
        }
    }

    // Quản lý Điều phối hành động Native & phát phản hồi thoại TTS
    val actionDispatcher = remember(textToSpeech) {
        NluActionDispatcher(
            context = context,
            enableAppLaunch = true,
            onActionError = {},
            onSpeakFeedback = { speechText ->
                textToSpeech.speak(speechText)
            },
            onRequestPermission = { permission ->
                runtimePermissionLauncher.launch(permission)
            }
        ).also { actionDispatcherRef = it }
    }

    // Tự động lưu câu lệnh vào Lịch sử, điều phối hành động Native và phát phản hồi giọng nói
    LaunchedEffect(nluResult) {
        val result = nluResult
        val query = nluEngineManager.currentQuery.value
        if (result != null && query.isNotBlank()) {
            val resultId = "${result.intent}_${result.rawJson.hashCode()}"
            if (resultId != lastProcessedResultId) {
                lastProcessedResultId = resultId
                historyRepository.addFromNluResult(query = query, nluResult = result)

                val action = NativeAction.fromNluResult(result)
                val speech = action.getSpeechFeedbackText()
                if (speech.isNotBlank()) {
                    textToSpeech.speak(speech)
                }

                if (result.status == "success") {
                    if (action.requiresConfirmation) {
                        pendingAction = action
                        showConfirmationDialog = true
                    } else if (action !is NativeAction.Informational && action !is NativeAction.Unsupported) {
                        // Delay 800ms cho hành động an toàn không cần xác nhận
                        delay(800)
                        pendingPermissionAction = action
                        actionDispatcher?.executeNativeAction(action)
                    }
                }
            }
        }
    }

    val handleConfirmAction = {
        val action = pendingAction
        showConfirmationDialog = false
        pendingAction = null
        if (action != null) {
            pendingPermissionAction = action
            actionDispatcher?.executeNativeAction(action)
        }
    }

    val handleCancelAction = {
        showConfirmationDialog = false
        pendingAction = null
    }

    val speechToText = rememberSpeechToText(
        onSpeechResult = { recognizedText ->
            nluEngineManager.processQuery(recognizedText)
        }
    )

    // Xử lý bật tắt thu âm an toàn: Chặn kích hoạt khi AI đang bận suy luận
    val handleToggleListeningSafe: () -> Unit = {
        if (isNluProcessing) {
            Toast.makeText(context, "AI đang phân tích câu lệnh, vui lòng đợi...", Toast.LENGTH_SHORT).show()
        } else {
            // Nếu TTS đang phát thì ngắt phát giọng nói để lắng nghe câu lệnh mới
            if (textToSpeech.isSpeaking) {
                textToSpeech.stop()
            }
            speechToText.toggleListening()
        }
    }

    val handleCancelListening: () -> Unit = {
        speechToText.cancelListening()
    }

    val handleOpenStorageSettings: () -> Unit = {
        showStoragePermissionDialog = false
        StoragePermissionHelper.openStoragePermissionSettings(context)
    }

    val handleRescanModel: () -> Unit = {
        nluEngineManager.autoDetectAndLoadModel()
        Toast.makeText(context, "Đang quét lại file mô hình GGUF...", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            CustomBottomMenuBar(
                selectedTab = selectedTab,
                onTabSelected = { tab -> selectedTab = tab },
                onMicClick = handleToggleListeningSafe,
                isListening = speechToText.isListening,
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
                NavTab.ASSISTANT -> AssistantScreen(
                    isListening = speechToText.isListening,
                    speechText = speechToText.speechText,
                    onToggleListening = handleToggleListeningSafe,
                    onCancelListening = handleCancelListening,
                    nluResult = nluResult,
                    isNluProcessing = isNluProcessing,
                    modelState = modelState,
                    hasStoragePermission = hasStoragePermission,
                    onRequestStoragePermission = handleOpenStorageSettings,
                    onRescanModel = handleRescanModel,
                    isTtsSpeaking = textToSpeech.isSpeaking,
                    onSuggestionClick = { prompt ->
                        if (!isNluProcessing) {
                            nluEngineManager.processQuery(prompt)
                        }
                    },
                    pendingAction = pendingAction,
                    showConfirmationDialog = showConfirmationDialog,
                    onConfirmAction = handleConfirmAction,
                    onCancelAction = handleCancelAction
                )

                NavTab.HISTORY -> HistoryScreen(
                    historyItems = historyItems,
                    onRerunCommand = { query ->
                        nluEngineManager.processQuery(query)
                        selectedTab = NavTab.ASSISTANT
                    },
                    onDeleteItem = { id ->
                        scope.launch { historyRepository.deleteItem(id) }
                    },
                    onClearAll = {
                        scope.launch { historyRepository.clearHistory() }
                    }
                )

                NavTab.SETTINGS -> SettingsScreen(modelState = modelState)
            }

            // Hộp thoại tự động nhắc cấp quyền truy cập bộ nhớ
            if (showStoragePermissionDialog) {
                StoragePermissionDialog(
                    onOpenSettings = handleOpenStorageSettings,
                    onDismiss = { showStoragePermissionDialog = false }
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeScreenPreview() {
    ViDroidCallTheme(dynamicColor = false) {
        HomeScreen()
    }
}
