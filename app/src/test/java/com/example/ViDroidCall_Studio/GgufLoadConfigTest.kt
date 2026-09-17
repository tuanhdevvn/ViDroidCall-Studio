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

import com.example.ViDroidCall_Studio.data.nlu.GgufLoadConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GgufLoadConfigTest {

    @Test
    fun llamaParamsEnablesMmapAndDisablesMlock() {
        val params = GgufLoadConfig.llamaParams("content://model", modelFd = 7)
        assertEquals(true, params["use_mmap"])
        assertEquals(false, params["use_mlock"])
        assertEquals(7, params["model_fd"])
        assertEquals("content://model", params["model"])
        assertEquals(GgufLoadConfig.CONTEXT_LENGTH, params["n_ctx"])
        assertEquals(GgufLoadConfig.N_BATCH, params["n_batch"])
    }

    @Test
    fun hasGgufMagicAcceptsHeaderOnly() {
        val file = File.createTempFile("vidroidcall-gguf", ".gguf")
        try {
            file.writeBytes(byteArrayOf(0x47, 0x47, 0x55, 0x46, 0x00))
            assertTrue(GgufLoadConfig.hasGgufMagic(file))
        } finally {
            file.delete()
        }
    }

    @Test
    fun hasGgufMagicRejectsEmptyAndWrongHeader() {
        val missing = File.createTempFile("vidroidcall-missing", ".gguf")
        val other = File.createTempFile("vidroidcall-bin", ".bin")
        try {
            missing.writeBytes(byteArrayOf())
            other.writeBytes("PK".toByteArray())
            assertFalse(GgufLoadConfig.hasGgufMagic(missing))
            assertFalse(GgufLoadConfig.hasGgufMagic(other))
            assertFalse(GgufLoadConfig.hasGgufMagic(File("/no/such/model.gguf")))
        } finally {
            missing.delete()
            other.delete()
        }
    }
}
