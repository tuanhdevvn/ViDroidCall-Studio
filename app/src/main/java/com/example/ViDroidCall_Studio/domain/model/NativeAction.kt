// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

package com.example.ViDroidCall_Studio.domain.model

import com.example.ViDroidCall_Studio.data.model.NluResult
import org.json.JSONObject
import java.util.UUID

/**
 * Các loại hành động Native Android được hỗ trợ bởi hệ thống
 */
sealed class NativeAction {
    abstract val actionId: String
    abstract val intentName: String
    abstract val requiresConfirmation: Boolean

    /**
     * Mở ứng dụng (YouTube, Zalo, Facebook, Settings, Camera, ...)
     */
    data class OpenApp(
        override val actionId: String = UUID.randomUUID().toString(),
        val appName: String,
        override val requiresConfirmation: Boolean = false
    ) : NativeAction() {
        override val intentName: String = "open_app"
    }

    /**
     * Thực hiện cuộc gọi (Mở màn hình quay số ACTION_DIAL)
     */
    data class CallContact(
        override val actionId: String = UUID.randomUUID().toString(),
        val contact: String,
        val phoneNumber: String,
        override val requiresConfirmation: Boolean = true
    ) : NativeAction() {
        override val intentName: String = "call_contact"
    }

    /**
     * Gửi tin nhắn SMS: tìm contact/số rồi mở trang soạn tin (ACTION_SENDTO).
     * Có message thì dán sẵn; không có message vẫn mở khung soạn trống.
     */
    data class SendSms(
        override val actionId: String = UUID.randomUUID().toString(),
        val contact: String,
        val phoneNumber: String,
        val message: String,
        override val requiresConfirmation: Boolean = true
    ) : NativeAction() {
        override val intentName: String = "send_sms"
    }

    /**
     * Mở bản đồ chỉ đường (Google Maps geo:0,0?q=...)
     */
    data class OpenMap(
        override val actionId: String = UUID.randomUUID().toString(),
        val destination: String,
        override val requiresConfirmation: Boolean = false
    ) : NativeAction() {
        override val intentName: String = "open_map"
    }

    /**
     * Đặt báo thức (AlarmClock.ACTION_SET_ALARM)
     */
    data class SetAlarm(
        override val actionId: String = UUID.randomUUID().toString(),
        val hour: Int,
        val minute: Int,
        val label: String = "Báo thức AI",
        override val requiresConfirmation: Boolean = false
    ) : NativeAction() {
        override val intentName: String = "set_alarm"
    }

    /**
     * Hẹn giờ đếm ngược (AlarmClock.ACTION_SET_TIMER)
     */
    data class SetTimer(
        override val actionId: String = UUID.randomUUID().toString(),
        val durationSeconds: Int,
        val displayDuration: Int,
        val unitText: String,
        val label: String = "Hẹn giờ",
        override val requiresConfirmation: Boolean = false
    ) : NativeAction() {
        override val intentName: String = "set_timer"
    }

    /**
     * Tìm kiếm video trên YouTube
     */
    data class SearchVideo(
        override val actionId: String = UUID.randomUUID().toString(),
        val query: String,
        override val requiresConfirmation: Boolean = false
    ) : NativeAction() {
        override val intentName: String = "search_video"
    }

    /**
     * Phát nhạc
     */
    data class PlayMusic(
        override val actionId: String = UUID.randomUUID().toString(),
        val songName: String,
        val artist: String,
        val genre: String,
        val musicQuery: String,
        override val requiresConfirmation: Boolean = false
    ) : NativeAction() {
        override val intentName: String = "play_music"
    }

    /**
     * Tìm kiếm thông tin trên Web (Google Search)
     */
    data class SearchWeb(
        override val actionId: String = UUID.randomUUID().toString(),
        val query: String,
        override val requiresConfirmation: Boolean = false
    ) : NativeAction() {
        override val intentName: String = "search_web"
    }

    /**
     * Phản hồi thông tin (Chào hỏi, tạm biệt, làm rõ thông tin)
     */
    data class Informational(
        override val actionId: String = UUID.randomUUID().toString(),
        override val intentName: String,
        val message: String,
        val speechText: String
    ) : NativeAction() {
        override val requiresConfirmation: Boolean = false
    }

    /**
     * Ý định chưa được hỗ trợ
     */
    data class Unsupported(
        override val actionId: String = UUID.randomUUID().toString(),
        override val intentName: String,
        val message: String = "Tính năng này chưa được hỗ trợ."
    ) : NativeAction() {
        override val requiresConfirmation: Boolean = false
    }

    /**
     * Lấy tiêu đề hiển thị cho Hộp thoại Xác nhận
     */
    fun getConfirmationTitle(): String {
        return when (this) {
            is CallContact -> "Xác nhận thực hiện cuộc gọi?"
            is SendSms -> "Xác nhận gửi tin nhắn?"
            is SetAlarm -> "Xác nhận đặt báo thức?"
            is SetTimer -> "Xác nhận hẹn giờ?"
            is OpenApp -> "Xác nhận mở ứng dụng?"
            is OpenMap -> "Xác nhận mở bản đồ?"
            is SearchWeb -> "Xác nhận tìm kiếm trên web?"
            else -> "Xác nhận thực hiện thao tác?"
        }
    }

    /**
     * Lấy nội dung chi tiết mô tả thao tác trong Hộp thoại Xác nhận
     */
    fun getConfirmationDescription(): String {
        return when (this) {
            is CallContact -> {
                val target = if (contact.isNotBlank()) contact else phoneNumber
                val phoneInfo = if (phoneNumber.isNotBlank() && contact.isNotBlank() && phoneNumber != contact) {
                    " ($phoneNumber)"
                } else {
                    ""
                }
                "Bạn có muốn gọi tới $target$phoneInfo không?"
            }
            is SendSms -> {
                val target = if (contact.isNotBlank()) contact else phoneNumber
                val msgPreview = if (message.isNotBlank()) message else "(Trống)"
                if (message.isNotBlank()) {
                    "Bạn có muốn gửi tin nhắn cho $target với nội dung '$msgPreview' không?"
                } else {
                    "Bạn có muốn soạn tin nhắn gửi cho $target không?"
                }
            }
            is SetAlarm -> {
                val timeStr = if (minute > 0) "$hour giờ $minute phút" else "$hour giờ"
                "Bạn có muốn đặt báo thức lúc $timeStr không?"
            }
            is SetTimer -> {
                "Bạn có muốn hẹn giờ $displayDuration $unitText không?"
            }
            is OpenApp -> {
                "Bạn có muốn mở ứng dụng $appName không?"
            }
            is OpenMap -> {
                "Bạn có muốn tìm đường tới $destination không?"
            }
            is SearchWeb -> {
                "Bạn có muốn tìm kiếm '$query' trên Google không?"
            }
            is SearchVideo -> {
                "Bạn có muốn tìm kiếm video '$query' trên YouTube không?"
            }
            is PlayMusic -> {
                if (musicQuery.isNotBlank()) "Bạn có muốn phát '$musicQuery' không?" else "Bạn có muốn mở trình phát nhạc không?"
            }
            else -> "Bạn có chắc chắn muốn thực hiện hành động này?"
        }
    }

    /**
     * Tiêu đề ngắn gọn dùng cho hộp thoại xác nhận
     */
    fun getActionTitle(): String {
        return when (this) {
            is CallContact -> "Xác nhận cuộc gọi?"
            is SendSms -> "Xác nhận gửi tin nhắn?"
            is SetAlarm -> "Xác nhận đặt báo thức?"
            is SetTimer -> "Xác nhận hẹn giờ?"
            is OpenApp -> "Xác nhận mở ứng dụng?"
            is OpenMap -> "Xác nhận mở bản đồ?"
            is SearchVideo -> "Xác nhận tìm video?"
            is SearchWeb -> "Xác nhận tìm kiếm?"
            is PlayMusic -> "Xác nhận phát nhạc?"
            else -> "Xác nhận thực hiện?"
        }
    }

    /**
     * Tóm tắt hành động cụ thể để hiển thị trên UI xác nhận
     */
    fun getActionSummary(): String {
        return when (this) {
            is CallContact -> {
                val target = if (contact.isNotBlank()) {
                    contact
                } else if (phoneNumber.length >= 6) {
                    phoneNumber.replaceRange(3, phoneNumber.length - 3, "****")
                } else {
                    phoneNumber
                }
                "Gọi tới $target"
            }
            is SendSms -> {
                val target = if (contact.isNotBlank()) {
                    contact
                } else if (phoneNumber.length >= 6) {
                    phoneNumber.replaceRange(3, phoneNumber.length - 3, "****")
                } else {
                    phoneNumber
                }
                if (message.isNotBlank()) "Gửi tin nhắn tới $target: “$message”" else "Soạn tin nhắn gửi $target"
            }
            is SetAlarm -> {
                val timeStr = if (minute > 0) "$hour giờ $minute phút" else "$hour giờ"
                "Đặt báo thức lúc $timeStr"
            }
            is SetTimer -> {
                "Hẹn giờ $displayDuration $unitText"
            }
            is OpenApp -> {
                val displayName = com.example.ViDroidCall_Studio.util.AppResolver.getDisplayAppName(appName)
                "Mở ứng dụng $displayName"
            }
            is OpenMap -> {
                "Mở bản đồ tới $destination"
            }
            is SearchVideo -> {
                "Tìm kiếm video “$query” trên YouTube"
            }
            is SearchWeb -> {
                "Tìm kiếm “$query” trên Google"
            }
            is PlayMusic -> {
                if (musicQuery.isNotBlank()) "Phát “$musicQuery”" else "Mở trình phát nhạc"
            }
            is Informational -> message
            is Unsupported -> message
        }
    }

    /**
     * Định danh loại icon đại diện cho hành động
     */
    fun getActionIconType(): String {
        return when (this) {
            is CallContact -> "CALL"
            is SendSms -> "SMS"
            is SetAlarm -> "ALARM"
            is SetTimer -> "TIMER"
            is OpenApp -> "OPEN_APP"
            is OpenMap -> "MAP"
            is SearchVideo -> "YOUTUBE"
            is SearchWeb -> "SEARCH"
            is PlayMusic -> "MUSIC"
            else -> "GENERIC"
        }
    }

    /**
     * Lấy câu thoại phản hồi bằng giọng nói (TTS)
     */
    fun getSpeechFeedbackText(): String {
        return when (this) {
            is OpenApp -> {
                val displayName = com.example.ViDroidCall_Studio.util.AppResolver.getDisplayAppName(appName)
                "Đang mở ứng dụng $displayName"
            }
            is CallContact -> {
                val target = if (contact.isNotBlank()) contact else phoneNumber
                if (target.isNotBlank()) "Đang thực hiện cuộc gọi tới $target" else "Đang mở ứng dụng cuộc gọi"
            }
            is SendSms -> {
                val target = if (contact.isNotBlank()) contact else phoneNumber
                if (target.isNotBlank()) "Đang mở ứng dụng gửi tin nhắn tới $target" else "Đang mở ứng dụng tin nhắn"
            }
            is OpenMap -> {
                if (destination.isNotBlank()) "Đang mở bản đồ chỉ đường tới $destination" else "Đang mở ứng dụng bản đồ"
            }
            is SetAlarm -> {
                if (minute > 0) "Đang đặt báo thức lúc $hour giờ $minute phút" else "Đang đặt báo thức lúc $hour giờ"
            }
            is SetTimer -> {
                "Đang hẹn giờ $displayDuration $unitText"
            }
            is SearchVideo -> "Đang tìm video $query trên YouTube"
            is SearchWeb -> "Đang tìm: $query"
            is PlayMusic -> {
                if (musicQuery.isNotBlank()) "Đang phát $musicQuery" else "Đang mở trình phát nhạc"
            }
            is Informational -> speechText
            is Unsupported -> "Xin lỗi, tôi chưa hỗ trợ tính năng này."
        }
    }

    companion object {
        /**
         * Phân tích NluResult thành đối tượng NativeAction cụ thể
         */
        fun fromNluResult(nluResult: NluResult): NativeAction {
            if (!nluResult.isParsedSuccessfully) {
                return Unsupported(intentName = nluResult.intent, message = nluResult.errorMessage ?: "Lỗi parse JSON")
            }

            val args = try {
                JSONObject(nluResult.argumentsJson)
            } catch (e: Exception) {
                JSONObject()
            }

            val status = effectiveStatus(nluResult, args)
            if (status == "needs_clarification") {
                val missingArr = try {
                    args.optJSONArray("missing")
                } catch (e: Exception) { null }
                val isSearchQueryMissing = missingArr != null && missingArr.toString().contains("query")
                val messageText = if (isSearchQueryMissing) {
                    "Bạn muốn tìm kiếm thông tin gì?"
                } else {
                    "Bạn vui lòng cung cấp thêm thông tin."
                }
                val speechText = if (isSearchQueryMissing) {
                    "Bạn muốn tìm kiếm thông tin gì?"
                } else {
                    "Bạn vui lòng cung cấp thêm thông tin"
                }
                return Informational(
                    intentName = nluResult.intent,
                    message = messageText,
                    speechText = speechText
                )
            }

            if (status == "invalid") {
                return Informational(
                    intentName = nluResult.intent,
                    message = "Thời gian bạn yêu cầu không hợp lệ.",
                    speechText = "Thời gian bạn yêu cầu không hợp lệ, vui lòng kiểm tra lại."
                )
            }

            if (status == "unsupported" || status != "success") {
                return Unsupported(
                    intentName = nluResult.intent,
                    message = "Chưa hỗ trợ tính năng này."
                )
            }

            val requiresConf = nluResult.requiresConfirmation

            return when (nluResult.intent) {
                "open_app" -> {
                    val appName = args.optString("app_name").ifBlank {
                        nluResult.slots["app_name"]?.toString() ?: ""
                    }
                    OpenApp(appName = appName, requiresConfirmation = requiresConf)
                }

                "call_contact" -> {
                    val contact = args.optString("contact").ifBlank {
                        nluResult.slots["contact"]?.toString() ?: ""
                    }
                    val phoneNumber = args.optString("phone_number").ifBlank {
                        nluResult.slots["phone_number"]?.toString() ?: ""
                    }
                    CallContact(
                        contact = contact,
                        phoneNumber = phoneNumber,
                        requiresConfirmation = true // Luôn yêu cầu xác nhận cuộc gọi
                    )
                }

                "send_sms" -> {
                    val contact = args.optString("contact").ifBlank {
                        nluResult.slots["contact"]?.toString() ?: ""
                    }
                    val phoneNumber = args.optString("phone_number").ifBlank {
                        nluResult.slots["phone_number"]?.toString() ?: ""
                    }
                    val message = args.optString("message").ifBlank {
                        nluResult.slots["message"]?.toString() ?: ""
                    }
                    SendSms(
                        contact = contact,
                        phoneNumber = phoneNumber,
                        message = message,
                        requiresConfirmation = true // Luôn yêu cầu xác nhận trước khi mở soạn tin
                    )
                }

                "open_map" -> {
                    val destination = args.optString("destination").ifBlank {
                        args.optString("query").ifBlank {
                            nluResult.slots["destination"]?.toString() ?: nluResult.slots["query"]?.toString() ?: ""
                        }
                    }
                    OpenMap(destination = destination, requiresConfirmation = requiresConf)
                }

                "set_alarm" -> {
                    val hour = parseClockInt(args, nluResult.slots, "hour", missing = -1) ?: -1
                    val minute = parseClockInt(args, nluResult.slots, "minute", missing = 0) ?: 0
                    if (hour !in 0..23 || minute !in 0..59) {
                        return Informational(
                            intentName = "set_alarm",
                            message = "Thời gian đặt báo thức không hợp lệ.",
                            speechText = "Thời gian đặt báo thức không hợp lệ"
                        )
                    }
                    val label = args.optString("label").ifBlank {
                        args.optString("message").ifBlank {
                            nluResult.slots["label"]?.toString() ?: nluResult.slots["message"]?.toString() ?: "Báo thức AI"
                        }
                    }
                    SetAlarm(
                        hour = hour,
                        minute = minute,
                        label = label,
                        requiresConfirmation = requiresConf
                    )
                }

                "set_timer" -> {
                    val duration = parseClockInt(args, nluResult.slots, "duration", missing = 0) ?: 0
                    val unit = args.optString("unit").ifBlank {
                        nluResult.slots["unit"]?.toString() ?: "minutes"
                    }
                    val seconds = when (unit) {
                        "hours" -> duration * 3600
                        "minutes" -> duration * 60
                        else -> duration
                    }
                    val unitText = when (unit) {
                        "hours" -> "giờ"
                        "minutes" -> "phút"
                        "seconds" -> "giây"
                        else -> unit
                    }
                    val label = args.optString("label").ifBlank {
                        nluResult.slots["label"]?.toString() ?: "Hẹn giờ"
                    }
                    SetTimer(
                        durationSeconds = seconds,
                        displayDuration = duration,
                        unitText = unitText,
                        label = label,
                        requiresConfirmation = requiresConf
                    )
                }

                "search_video" -> {
                    val query = args.optString("query").ifBlank {
                        nluResult.slots["query"]?.toString() ?: ""
                    }
                    SearchVideo(query = query, requiresConfirmation = requiresConf)
                }

                "play_music" -> {
                    val songName = args.optString("song_name").ifBlank {
                        nluResult.slots["song_name"]?.toString() ?: ""
                    }
                    val artist = args.optString("artist").ifBlank {
                        nluResult.slots["artist"]?.toString() ?: ""
                    }
                    val genre = args.optString("genre").ifBlank {
                        nluResult.slots["genre"]?.toString() ?: ""
                    }
                    val musicQuery = buildString {
                        if (songName.isNotBlank()) append(songName)
                        if (artist.isNotBlank()) {
                            if (isNotEmpty()) append(" ")
                            append(artist)
                        }
                        if (genre.isNotBlank() && isBlank()) append(genre)
                    }.trim()
                    PlayMusic(
                        songName = songName,
                        artist = artist,
                        genre = genre,
                        musicQuery = musicQuery,
                        requiresConfirmation = requiresConf
                    )
                }

                "search_web" -> {
                    val rawArg = if (args.isNull("query")) "" else args.optString("query")
                    val rawSlot = nluResult.slots["query"]?.toString() ?: ""
                    val query = (if (rawArg.isNotBlank() && rawArg.trim() != "null") rawArg else rawSlot)
                        .let { if (it.trim() == "null") "" else it }
                        .trim()
                    if (query.isBlank()) {
                        Informational(
                            intentName = "clarify",
                            message = "Bạn muốn tìm kiếm thông tin gì?",
                            speechText = "Bạn muốn tìm kiếm thông tin gì?"
                        )
                    } else {
                        SearchWeb(query = query, requiresConfirmation = false)
                    }
                }

                "greeting" -> Informational(
                    intentName = "greeting",
                    message = "Xin chào! Tôi có thể giúp gì cho bạn?",
                    speechText = "Xin chào bạn, tôi có thể giúp gì cho bạn?"
                )

                "goodbye" -> Informational(
                    intentName = "goodbye",
                    message = "Tạm biệt và hẹn gặp lại!",
                    speechText = "Tạm biệt bạn, hẹn gặp lại nhé!"
                )

                else -> Unsupported(intentName = nluResult.intent)
            }
        }

        /**
         * GGUF đôi khi gắn status=invalid dù hour/minute nằm trong 0–23 / 0–59
         * (ví dụ 5:00). Chỉ coi là giờ sai khi giá trị thật sự ngoài khoảng.
         */
        private fun effectiveStatus(nluResult: NluResult, args: JSONObject): String {
            val status = nluResult.status
            if (status != "invalid") return status
            return when (nluResult.intent) {
                "set_alarm" -> {
                    val hour = parseClockInt(args, nluResult.slots, "hour", missing = -1) ?: -1
                    val minute = parseClockInt(args, nluResult.slots, "minute", missing = 0) ?: 0
                    if (hour in 0..23 && minute in 0..59) "success" else "invalid"
                }
                "set_timer" -> {
                    val duration = parseClockInt(args, nluResult.slots, "duration", missing = 0) ?: 0
                    if (duration > 0) "success" else "invalid"
                }
                else -> "invalid"
            }
        }

        private fun parseClockInt(
            args: JSONObject,
            slots: Map<String, Any?>,
            key: String,
            missing: Int
        ): Int? {
            if (args.has(key) && !args.isNull(key)) {
                return coerceInt(args.opt(key)) ?: missing
            }
            return coerceInt(slots[key]) ?: missing
        }

        private fun coerceInt(raw: Any?): Int? {
            return when (raw) {
                null, JSONObject.NULL -> null
                is Number -> raw.toInt()
                is String -> raw.trim().toDoubleOrNull()?.toInt()
                else -> null
            }
        }
    }
}
