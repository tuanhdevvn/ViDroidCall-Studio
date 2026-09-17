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

import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.nehuatl.llamacpp.LlamaContext
import org.nehuatl.llamacpp.LlamaHelper
import java.io.File
import java.util.Random
import kotlin.math.absoluteValue

/**
 * Nạp GGUF qua [LlamaContext] với mmap. Không dùng [LlamaHelper.load] (cứng tắt mmap,
 * copy cả file vào RAM → văng lần nạp đầu trên máy RAM thấp).
 */
internal class GgufLlamaEngine(
    private val scope: CoroutineScope,
    private val events: MutableSharedFlow<LlamaHelper.LLMEvent>
) {
    private var loadJob: Job? = null
    private var completionJob: Job? = null
    private var llamaContext: LlamaContext? = null
    private var tokenCount = 0
    private var allText = ""

    fun load(file: File, loaded: () -> Unit) {
        abort()
        releaseContextQuietly()

        if (!GgufLoadConfig.hasGgufMagic(file)) {
            events.tryEmit(LlamaHelper.LLMEvent.Error("File không phải GGUF: ${file.name}"))
            return
        }

        val pfd = try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: Exception) {
            Log.e(TAG, "Không mở được FD file GGUF", e)
            events.tryEmit(LlamaHelper.LLMEvent.Error("Không mở được file mô hình: ${e.message}"))
            return
        }
        val modelFd = pfd.detachFd()
        val params = GgufLoadConfig.llamaParams(file.absolutePath, modelFd)

        loadJob = scope.launch {
            Log.i(TAG, "Nạp GGUF mmap=true fd=$modelFd path=${file.absolutePath}")
            try {
                val id = Random().nextInt().absoluteValue
                val ctx = LlamaContext(id, params)
                if (ctx.context == 0L) {
                    closeFdQuietly(modelFd)
                    events.tryEmit(LlamaHelper.LLMEvent.Error("Không khởi tạo được mô hình GGUF"))
                    return@launch
                }
                ctx.setTokenCallback { token ->
                    allText += token
                    tokenCount++
                    events.tryEmit(LlamaHelper.LLMEvent.Ongoing(token, tokenCount))
                }
                llamaContext = ctx
                Log.i(TAG, "GGUF mmap sẵn sàng")
                events.tryEmit(LlamaHelper.LLMEvent.Loaded(file.absolutePath))
                loaded()
            } catch (t: Throwable) {
                Log.e(TAG, "LlamaContext mmap thất bại", t)
                closeFdQuietly(modelFd)
                events.tryEmit(
                    LlamaHelper.LLMEvent.Error(
                        "Không khởi tạo được mô hình GGUF: ${t.localizedMessage ?: t.javaClass.simpleName}"
                    )
                )
            }
        }
    }

    fun predict(prompt: String) {
        val ctx = llamaContext ?: throw IllegalStateException("Model was not loaded yet")
        val startTime = System.currentTimeMillis()
        tokenCount = 0
        allText = ""
        val params = mutableMapOf<String, Any>(
            "prompt" to prompt,
            "emit_partial_completion" to true
        )
        completionJob = scope.launch {
            events.tryEmit(LlamaHelper.LLMEvent.Started(prompt))
            ctx.completion(params)
            val duration = System.currentTimeMillis() - startTime
            events.tryEmit(LlamaHelper.LLMEvent.Done(allText, tokenCount, duration))
        }
    }

    fun stopPrediction() {
        val ctx = llamaContext ?: return
        scope.launch {
            try {
                ctx.stopCompletion()
            } catch (t: Throwable) {
                Log.w(TAG, "stopCompletion: ${t.message}")
            }
        }
        completionJob?.cancel()
    }

    fun abort() {
        loadJob?.cancel()
        stopPrediction()
    }

    fun release() {
        abort()
        releaseContextQuietly()
    }

    private fun releaseContextQuietly() {
        val ctx = llamaContext
        llamaContext = null
        if (ctx != null) {
            try {
                ctx.release()
            } catch (t: Throwable) {
                Log.w(TAG, "releaseContext: ${t.message}")
            }
        }
    }

    private fun closeFdQuietly(fd: Int) {
        try {
            ParcelFileDescriptor.adoptFd(fd).close()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val TAG = "GgufLlamaEngine"
    }
}
