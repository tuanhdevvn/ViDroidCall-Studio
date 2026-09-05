// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log

/**
 * Tiện ích định tuyến và phân giải ứng dụng Android dựa trên tên gọi tiếng Việt / tiếng Anh
 */
object AppResolver {
    private const val TAG = "AppResolver"

    // Danh mục mapping các ứng dụng phổ biến và aliases tại Việt Nam
    private val KNOWN_APP_PACKAGES = mapOf(
        "youtube" to listOf("com.google.android.youtube"),
        "yt" to listOf("com.google.android.youtube"),
        "zalo" to listOf("com.zing.zalo"),
        "facebook" to listOf("com.facebook.katana", "com.facebook.lite"),
        "fb" to listOf("com.facebook.katana", "com.facebook.lite"),
        "tiktok" to listOf("com.ss.android.ugc.trill", "com.zhiliaoapp.musically"),
        "chrome" to listOf("com.android.chrome"),
        "google maps" to listOf("com.google.android.apps.maps"),
        "google map" to listOf("com.google.android.apps.maps"),
        "maps" to listOf("com.google.android.apps.maps"),
        "bản đồ" to listOf("com.google.android.apps.maps"),
        "map" to listOf("com.google.android.apps.maps"),
        "gmail" to listOf("com.google.android.gm"),
        "messenger" to listOf("com.facebook.orca", "com.facebook.mlite"),
        "telegram" to listOf("org.telegram.messenger", "org.telegram.plus"),
        "spotify" to listOf("com.spotify.music"),
        "shopee" to listOf("com.shopee.vn"),
        "lazada" to listOf("com.lazada.android"),
        "momo" to listOf("com.mservice.momopay"),
        "viber" to listOf("com.viber.voip"),
        "instagram" to listOf("com.instagram.android"),
        "ch play" to listOf("com.android.vending"),
        "playstore" to listOf("com.android.vending"),
        "play store" to listOf("com.android.vending"),
        "cửa hàng play" to listOf("com.android.vending"),
        "calculator" to listOf("com.google.android.calculator", "com.sec.android.app.popupcalculator")
    )

    /**
     * Chuẩn hóa tên ứng dụng người dùng nhập/phát âm
     */
    fun cleanAppName(raw: String): String {
        return raw.lowercase()
            .replace("ứng dụng", "")
            .replace("app", "")
            .replace("mở", "")
            .trim()
    }

    /**
     * Lấy tên ứng dụng hiển thị đẹp (Viết hoa đúng chuẩn, dịch sang tiếng Việt)
     */
    fun getDisplayAppName(raw: String): String {
        val clean = cleanAppName(raw)
        return when (clean) {
            "google_maps", "google map", "google maps" -> "Google Maps"
            "ch play", "playstore", "play store" -> "CH Play"
            "youtube", "yt" -> "YouTube"
            "facebook", "fb" -> "Facebook"
            "tiktok" -> "TikTok"
            "zalo" -> "Zalo"
            "shopee" -> "Shopee"
            "lazada" -> "Lazada"
            "gallery" -> "Bộ sưu tập"
            "camera" -> "Máy ảnh"
            "calculator" -> "Máy tính"
            "contacts" -> "Danh bạ"
            "clock" -> "Đồng hồ"
            "settings" -> "Cài đặt"
            "recorder" -> "Ghi âm"
            "files" -> "Quản lý tệp"
            else -> raw.replace("_", " ")
        }
    }

    /**
     * Tìm package name của ứng dụng đã được cài đặt trên thiết bị
     */
    fun resolvePackageName(context: Context, appName: String): String? {
        val cleanName = cleanAppName(appName)
        if (cleanName.isBlank()) return null

        val pm = context.packageManager

        // 1. Tra cứu theo danh sách package đã biết
        val candidates = KNOWN_APP_PACKAGES[cleanName]
        if (candidates != null) {
            for (pkg in candidates) {
                if (isPackageInstalled(pm, pkg)) {
                    return pkg
                }
            }
        }

        // 2. Tra cứu động qua danh sách Launcher Activity đã cài đặt trên thiết bị
        return findPackageByInstalledLabels(context, cleanName)
    }

    /**
     * Kiểm tra xem package có tồn tại và được cài trên thiết bị hay không
     */
    fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Phân giải các Intent gốc của hệ thống Android cho các tính năng cơ bản
     */
    fun resolveSystemIntent(cleanName: String): Intent? {
        return when (cleanName) {
            "camera", "máy ảnh", "chụp ảnh", "chụp hình" -> Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            "gallery", "bộ sưu tập", "thư viện", "thư viện ảnh", "album ảnh" -> Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            "calculator", "máy tính", "bàn tính" -> Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_APP_CALCULATOR) }
            "contacts", "danh bạ", "số điện thoại", "danh sách gọi" -> Intent(Intent.ACTION_VIEW, Uri.parse("content://contacts/people"))
            "clock", "đồng hồ", "báo thức", "đồng hồ báo thức" -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
            "settings", "cài đặt", "thiết lập", "cài đặt máy", "cài đặt điện thoại" -> Intent(Settings.ACTION_SETTINGS)
            "recorder", "ghi âm", "máy ghi âm", "thu âm" -> Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION)
            "files", "quản lý tệp", "tệp tin", "file của bạn", "quản lý file" -> Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)
            "playstore", "ch play", "cửa hàng", "tải ứng dụng", "cửa hàng ứng dụng" -> Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms"))
            "chrome", "trình duyệt", "mở trình duyệt", "web" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                setPackage("com.android.chrome")
            }
            "gọi điện", "điện thoại" -> Intent(Intent.ACTION_DIAL)
            "tin nhắn", "sms" -> Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MESSAGING)
            }
            else -> null
        }
    }

    /**
     * Tra cứu động qua danh sách ứng dụng đã cài đặt trên máy bằng việc so khớp nhãn hiển thị
     */
    private fun findPackageByInstalledLabels(context: Context, cleanAppName: String): String? {
        return try {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            var exactMatch: String? = null
            var partialMatch: String? = null

            for (info in resolveInfos) {
                val label = info.loadLabel(pm).toString().lowercase().trim()
                if (label == cleanAppName) {
                    exactMatch = info.activityInfo.packageName
                    break
                }
                if (label.contains(cleanAppName) || cleanAppName.contains(label)) {
                    partialMatch = info.activityInfo.packageName
                }
            }
            exactMatch ?: partialMatch
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tìm kiếm package theo tên: ${e.message}")
            null
        }
    }
}
