// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Tiện ích kiểm tra và xử lý cấp quyền truy cập bộ nhớ / quản lý tệp trên mọi phiên bản Android.
 */
object StoragePermissionHelper {
    private const val TAG = "StoragePermissionHelper"

    /**
     * Kiểm tra xem ứng dụng đã có quyền đọc bộ nhớ / quản lý tệp để quét mô hình GGUF chưa.
     * - Android 11+ (API 30+): Kiểm tra Environment.isExternalStorageManager() (MANAGE_EXTERNAL_STORAGE).
     * - Android 10 trở xuống (API < 30): Kiểm tra Manifest.permission.READ_EXTERNAL_STORAGE.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Environment.isExternalStorageManager()
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi kiểm tra isExternalStorageManager: ${e.message}")
                false
            }
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Tự động mở màn hình Cài đặt hệ thống để người dùng cấp quyền truy cập tệp:
     * - Android 11+ (API 30+): Mở màn hình "Quyền truy cập tất cả các tệp" (All files access) cho ViDroidCall Studio.
     * - Android 10 trở xuống: Mở màn hình Chi tiết ứng dụng để người dùng bật quyền Bộ nhớ (Storage).
     */
    fun openStoragePermissionSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Log.i(TAG, "Đã mở ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION")
                } catch (e: Exception) {
                    Log.w(TAG, "ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION thất bại, dùng ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION", e)
                    val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallbackIntent)
                }
            } else {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Log.i(TAG, "Đã mở ACTION_APPLICATION_DETAILS_SETTINGS")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Không thể mở cài đặt quyền: ${e.message}", e)
            try {
                val appSettingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(appSettingsIntent)
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback ACTION_APPLICATION_DETAILS_SETTINGS cũng lỗi: ${ex.message}", ex)
            }
        }
    }
}
