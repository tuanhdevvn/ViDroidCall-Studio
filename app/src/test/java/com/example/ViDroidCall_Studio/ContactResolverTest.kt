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

package com.example.ViDroidCall_Studio

import com.example.ViDroidCall_Studio.util.ContactResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactResolverTest {

    @Test
    fun testIsPhoneNumberDetection() {
        // Direct phone numbers
        assertTrue(ContactResolver.isPhoneNumber("0912345678"))
        assertTrue(ContactResolver.isPhoneNumber("+84912345678"))
        assertTrue(ContactResolver.isPhoneNumber("0243123456"))
        assertTrue(ContactResolver.isPhoneNumber("090 123 4567"))
        assertTrue(ContactResolver.isPhoneNumber("090-123-4567"))
        assertTrue(ContactResolver.isPhoneNumber("(028) 38221234"))

        // Emergency & Service numbers
        assertTrue(ContactResolver.isPhoneNumber("113"))
        assertTrue(ContactResolver.isPhoneNumber("114"))
        assertTrue(ContactResolver.isPhoneNumber("115"))
        assertTrue(ContactResolver.isPhoneNumber("111"))
        assertTrue(ContactResolver.isPhoneNumber("911"))
        assertTrue(ContactResolver.isPhoneNumber("1800"))
        assertTrue(ContactResolver.isPhoneNumber("1900"))

        // Contact names (should return false)
        assertFalse(ContactResolver.isPhoneNumber("mẹ"))
        assertFalse(ContactResolver.isPhoneNumber("bố"))
        assertFalse(ContactResolver.isPhoneNumber("ba"))
        assertFalse(ContactResolver.isPhoneNumber("anh hai"))
        assertFalse(ContactResolver.isPhoneNumber("chị gái"))
        assertFalse(ContactResolver.isPhoneNumber("bác sĩ"))
        assertFalse(ContactResolver.isPhoneNumber("con trai"))
        assertFalse(ContactResolver.isPhoneNumber("con gái"))
        assertFalse(ContactResolver.isPhoneNumber("vợ"))
        assertFalse(ContactResolver.isPhoneNumber("chồng"))
    }

    @Test
    fun testVietnameseAccentNormalization() {
        assertEquals("me", ContactResolver.normalizeText("Mẹ"))
        assertEquals("bo", ContactResolver.normalizeText("Bố"))
        assertEquals("anh hai", ContactResolver.normalizeText("Anh Hai"))
        assertEquals("chi gai", ContactResolver.normalizeText("Chị Gái"))
        assertEquals("bac si", ContactResolver.normalizeText("Bác Sĩ"))
        assertEquals("dang van lam", ContactResolver.normalizeText("Đặng Văn Lâm"))
        assertEquals("ong noi", ContactResolver.normalizeText("Ông Nội"))
    }
}
