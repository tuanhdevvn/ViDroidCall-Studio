// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.example.ViDroidCall_Studio.data.local.habit

import com.example.ViDroidCall_Studio.domain.model.NativeAction
import com.example.ViDroidCall_Studio.util.AppResolver
import org.json.JSONObject
import java.util.Locale

/**
 * Serialize NativeAction để chạy lại lối tắt mà không qua Fast-Path / GGUF.
 * Nhãn và slot theo intent, không lấy câu STT.
 */
object NativeActionCodec {

    private val VI = Locale.forLanguageTag("vi-VN")

    private val EXCLUDED_INTENTS = setOf(
        "greeting",
        "goodbye",
        "clarify",
        "unsupported"
    )

    fun isEligible(action: NativeAction): Boolean {
        return when (action) {
            is NativeAction.Informational, is NativeAction.Unsupported -> false
            else -> action.intentName !in EXCLUDED_INTENTS && slotKey(action) != null
        }
    }

    fun slotKey(action: NativeAction): String? {
        val slot = when (action) {
            is NativeAction.CallContact -> targetSlot(action.contact, action.phoneNumber)
            is NativeAction.SendSms -> targetSlot(action.contact, action.phoneNumber)
            is NativeAction.OpenApp -> action.appName.trim().lowercase(VI)
            is NativeAction.OpenMap -> action.destination.trim().lowercase(VI)
            is NativeAction.SetAlarm -> "%02d:%02d".format(action.hour, action.minute)
            is NativeAction.SetTimer -> "${action.displayDuration}|${action.unitText.trim().lowercase(VI)}"
            is NativeAction.SearchVideo -> action.query.trim().lowercase(VI)
            is NativeAction.SearchWeb -> action.query.trim().lowercase(VI)
            is NativeAction.PlayMusic -> action.musicQuery.trim().lowercase(VI).ifBlank { "default" }
            is NativeAction.Informational, is NativeAction.Unsupported -> null
        } ?: return null
        if (slot.isBlank()) return null
        return "${action.intentName}|$slot"
    }

    fun quickLabel(action: NativeAction): String {
        return when (action) {
            is NativeAction.CallContact -> "Gọi ${displayTarget(action.contact, action.phoneNumber)}"
            is NativeAction.SendSms -> "Nhắn ${displayTarget(action.contact, action.phoneNumber)}"
            is NativeAction.OpenApp -> "Mở ${AppResolver.getDisplayAppName(action.appName)}"
            is NativeAction.OpenMap -> "Bản đồ ${action.destination.trim()}"
            is NativeAction.SetAlarm -> {
                if (action.minute > 0) {
                    "Báo thức ${action.hour} giờ ${action.minute} phút"
                } else {
                    "Báo thức ${action.hour} giờ"
                }
            }
            is NativeAction.SetTimer -> "Hẹn giờ ${action.displayDuration} ${action.unitText}"
            is NativeAction.SearchVideo -> "Video ${action.query.trim()}"
            is NativeAction.SearchWeb -> "Tìm ${action.query.trim()}"
            is NativeAction.PlayMusic -> {
                if (action.musicQuery.isNotBlank()) "Nhạc ${action.musicQuery.trim()}" else "Mở nhạc"
            }
            is NativeAction.Informational -> action.message
            is NativeAction.Unsupported -> action.message
        }
    }

    fun toJson(action: NativeAction): String {
        val json = JSONObject()
        json.put("type", action.intentName)
        json.put("requiresConfirmation", action.requiresConfirmation)
        when (action) {
            is NativeAction.CallContact -> {
                json.put("contact", action.contact)
                json.put("phoneNumber", action.phoneNumber)
            }
            is NativeAction.SendSms -> {
                json.put("contact", action.contact)
                json.put("phoneNumber", action.phoneNumber)
                json.put("message", action.message)
            }
            is NativeAction.OpenApp -> json.put("appName", action.appName)
            is NativeAction.OpenMap -> json.put("destination", action.destination)
            is NativeAction.SetAlarm -> {
                json.put("hour", action.hour)
                json.put("minute", action.minute)
                json.put("label", action.label)
            }
            is NativeAction.SetTimer -> {
                json.put("durationSeconds", action.durationSeconds)
                json.put("displayDuration", action.displayDuration)
                json.put("unitText", action.unitText)
                json.put("label", action.label)
            }
            is NativeAction.SearchVideo -> json.put("query", action.query)
            is NativeAction.SearchWeb -> json.put("query", action.query)
            is NativeAction.PlayMusic -> {
                json.put("songName", action.songName)
                json.put("artist", action.artist)
                json.put("genre", action.genre)
                json.put("musicQuery", action.musicQuery)
            }
            is NativeAction.Informational, is NativeAction.Unsupported -> {
                json.put("message", (action as? NativeAction.Informational)?.message ?: (action as NativeAction.Unsupported).message)
            }
        }
        return json.toString()
    }

    fun fromJson(raw: String): NativeAction? {
        return try {
            val json = JSONObject(raw)
            val type = json.optString("type")
            val confirm = json.optBoolean("requiresConfirmation", false)
            when (type) {
                "call_contact" -> NativeAction.CallContact(
                    contact = json.optString("contact"),
                    phoneNumber = json.optString("phoneNumber"),
                    requiresConfirmation = json.optBoolean("requiresConfirmation", true)
                )
                "send_sms" -> NativeAction.SendSms(
                    contact = json.optString("contact"),
                    phoneNumber = json.optString("phoneNumber"),
                    message = json.optString("message"),
                    requiresConfirmation = json.optBoolean("requiresConfirmation", true)
                )
                "open_app" -> NativeAction.OpenApp(
                    appName = json.optString("appName"),
                    requiresConfirmation = confirm
                )
                "open_map" -> NativeAction.OpenMap(
                    destination = json.optString("destination"),
                    requiresConfirmation = confirm
                )
                "set_alarm" -> NativeAction.SetAlarm(
                    hour = json.optInt("hour"),
                    minute = json.optInt("minute"),
                    label = json.optString("label", "Báo thức AI"),
                    requiresConfirmation = confirm
                )
                "set_timer" -> NativeAction.SetTimer(
                    durationSeconds = json.optInt("durationSeconds"),
                    displayDuration = json.optInt("displayDuration"),
                    unitText = json.optString("unitText"),
                    label = json.optString("label", "Hẹn giờ"),
                    requiresConfirmation = confirm
                )
                "search_video" -> NativeAction.SearchVideo(
                    query = json.optString("query"),
                    requiresConfirmation = confirm
                )
                "search_web" -> NativeAction.SearchWeb(
                    query = json.optString("query"),
                    requiresConfirmation = confirm
                )
                "play_music" -> NativeAction.PlayMusic(
                    songName = json.optString("songName"),
                    artist = json.optString("artist"),
                    genre = json.optString("genre"),
                    musicQuery = json.optString("musicQuery"),
                    requiresConfirmation = confirm
                )
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun targetSlot(contact: String, phoneNumber: String): String? {
        val name = contact.trim().lowercase(VI)
        if (name.isNotBlank()) return name
        val phone = phoneNumber.trim()
        return phone.ifBlank { null }
    }

    private fun displayTarget(contact: String, phoneNumber: String): String {
        val name = contact.trim()
        if (name.isNotBlank()) return name
        val phone = phoneNumber.trim()
        if (phone.length >= 6) {
            return phone.replaceRange(3, phone.length - 3, "****")
        }
        return phone.ifBlank { "liên hệ" }
    }
}
