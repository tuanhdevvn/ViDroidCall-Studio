// SPDX-License-Identifier: Apache-2.0

package com.example.ViDroidCall_Studio.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import java.text.Normalizer
import java.util.regex.Pattern

/**
 * Tiện ích nhận diện số điện thoại và tra cứu Danh bạ hệ thống Android (ContactsContract)
 */
object ContactResolver {
    private const val TAG = "ContactResolver"

    data class ContactInfo(
        val name: String,
        val phoneNumber: String,
        val allPhoneNumbers: List<String> = emptyList()
    )

    sealed interface ContactSearchResult {
        data class Success(val contact: ContactInfo) : ContactSearchResult
        data class MultipleNumbers(val name: String, val phoneNumbers: List<String>) : ContactSearchResult
        data class NotFound(val query: String) : ContactSearchResult
        data class NoPhoneNumber(val name: String) : ContactSearchResult
        data object PermissionDenied : ContactSearchResult
    }

    /**
     * Phân biệt số điện thoại trực tiếp hoặc số khẩn cấp với tên danh bạ
     */
    fun isPhoneNumber(query: String): Boolean {
        val clean = query.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace(".", "")
            .replace("(", "")
            .replace(")", "")
        if (clean.isEmpty()) return false

        // Số khẩn cấp hoặc đầu số dịch vụ thông dụng
        val emergencyNumbers = setOf(
            "111", "112", "113", "114", "115", "119", "911", "1800", "1900"
        )
        if (emergencyNumbers.contains(clean)) return true

        // Chuỗi chỉ chứa số (có thể có dấu + ở đầu), độ dài từ 3 đến 15 chữ số
        val phoneRegex = Regex("^\\+?[0-9]{3,15}$")
        return phoneRegex.matches(clean)
    }

    /**
     * Chuẩn hóa văn bản tiếng Việt: Chữ thường, xóa dấu thanh và ký tự đặc biệt
     */
    fun normalizeText(input: String): String {
        val nfd = Normalizer.normalize(input.trim().lowercase(), Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfd).replaceAll("")
            .replace("đ", "d")
            .replace("Đ", "d")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Tra cứu số điện thoại từ Danh bạ hệ thống theo tên liên hệ
     */
    fun searchContact(context: Context, name: String): ContactSearchResult {
        // 1. Kiểm tra Runtime Permission READ_CONTACTS
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            Log.w(TAG, "Ứng dụng chưa được cấp quyền READ_CONTACTS")
            return ContactSearchResult.PermissionDenied
        }

        val targetClean = name.trim()
        if (targetClean.isEmpty()) {
            return ContactSearchResult.NotFound(name)
        }

        val targetNormalized = normalizeText(targetClean)

        // 2. Query ContactsContract.CommonDataKinds.Phone
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
        )

        val cursor: Cursor? = try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException khi query danh bạ: ${e.message}", e)
            return ContactSearchResult.PermissionDenied
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi query danh bạ: ${e.message}", e)
            return ContactSearchResult.NotFound(name)
        }

        if (cursor == null) {
            return ContactSearchResult.NotFound(name)
        }

        // 3. Tập hợp danh sách số điện thoại theo từng Tên hiển thị
        val contactMap = mutableMapOf<String, MutableList<String>>()

        cursor.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val displayName = if (nameIndex >= 0) it.getString(nameIndex) ?: "" else ""
                val number = if (numberIndex >= 0) it.getString(numberIndex) ?: "" else ""
                val cleanNumber = number.replace(" ", "").replace("-", "").trim()

                if (displayName.isNotBlank() && cleanNumber.isNotBlank()) {
                    val list = contactMap.getOrPut(displayName) { mutableListOf() }
                    if (!list.contains(cleanNumber)) {
                        list.add(cleanNumber)
                    }
                }
            }
        }

        if (contactMap.isEmpty()) {
            return ContactSearchResult.NotFound(name)
        }

        // 4. Thuật toán tìm kiếm theo độ ưu tiên:
        // Cấp 1: Exact match (Chính xác tuyệt đối bao gồm cả dấu tiếng Việt)
        val exactMatch = contactMap.entries.firstOrNull {
            it.key.equals(targetClean, ignoreCase = true)
        }
        if (exactMatch != null) {
            return returnMatch(exactMatch.key, exactMatch.value)
        }

        // Cấp 2: Exact match sau khi Normalize không dấu (ví dụ: "me" -> "Mẹ", "bo" -> "Bố", "chi" -> "Chị")
        val normalizedMatch = contactMap.entries.firstOrNull {
            normalizeText(it.key) == targetNormalized
        }
        if (normalizedMatch != null) {
            return returnMatch(normalizedMatch.key, normalizedMatch.value)
        }

        // Cấp 3: Contains match (Tên danh bạ chứa từ khóa hoặc từ khóa chứa tên danh bạ)
        val containsMatch = contactMap.entries.firstOrNull {
            val contactNorm = normalizeText(it.key)
            contactNorm.contains(targetNormalized) || targetNormalized.contains(contactNorm)
        }
        if (containsMatch != null) {
            return returnMatch(containsMatch.key, containsMatch.value)
        }

        return ContactSearchResult.NotFound(name)
    }

    private fun returnMatch(name: String, numbers: List<String>): ContactSearchResult {
        if (numbers.isEmpty()) {
            return ContactSearchResult.NoPhoneNumber(name)
        }
        val primaryNumber = numbers.first()
        return ContactSearchResult.Success(
            ContactInfo(
                name = name,
                phoneNumber = primaryNumber,
                allPhoneNumbers = numbers
            )
        )
    }
}
