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

package com.example.ViDroidCall_Studio.data.nlu

import java.io.File

/**
 * Tham số nạp llama.cpp. [LlamaHelper] cứng `use_mmap=false` nên copy cả GGUF vào RAM
 * (~378 MB) — máy yếu (A07) bị LMK giết process lần nạp đầu. mmap ánh xạ file, không malloc.
 */
internal object GgufLoadConfig {
    const val CONTEXT_LENGTH = 512
    const val N_BATCH = 256
    /** Chờ Compose / hộp quyền ổn định trước khi đụng native. */
    const val SETTLE_DELAY_MS = 500L
    val GGUF_MAGIC = byteArrayOf(0x47, 0x47, 0x55, 0x46) // "GGUF"

    fun llamaParams(modelUri: String, modelFd: Int, contextLength: Int = CONTEXT_LENGTH): Map<String, Any> {
        return mapOf(
            "model" to modelUri,
            "model_fd" to modelFd,
            "use_mmap" to true,
            "use_mlock" to false,
            "n_ctx" to contextLength,
            "embedding" to false,
            "n_batch" to N_BATCH,
            "n_threads" to 0,
            "n_gpu_layers" to 0,
            "vocab_only" to false,
            "lora" to "",
            "lora_scaled" to 1.0,
            "rope_freq_base" to 0.0,
            "rope_freq_scale" to 0.0
        )
    }

    fun hasGgufMagic(file: File): Boolean {
        if (!file.isFile || file.length() < 4) return false
        return try {
            file.inputStream().use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                read == 4 && header.contentEquals(GGUF_MAGIC)
            }
        } catch (_: Exception) {
            false
        }
    }
}
