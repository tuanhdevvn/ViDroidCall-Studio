// SPDX-License-Identifier: Apache-2.0
// Copyright (c) Xiaomi Corporation
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

package com.k2fsa.sherpa.onnx

import android.content.res.AssetManager

data class SpokenLanguageIdentificationWhisperConfig(
    var encoder: String = "",
    var decoder: String = "",
    var tailPaddings: Int = -1,
)

data class SpokenLanguageIdentificationConfig(
    var whisper: SpokenLanguageIdentificationWhisperConfig = SpokenLanguageIdentificationWhisperConfig(),
    var numThreads: Int = 1,
    var debug: Boolean = false,
    var provider: String = "cpu",
)

class SpokenLanguageIdentification(
    assetManager: AssetManager? = null,
    config: SpokenLanguageIdentificationConfig,
) {
    private var ptr: Long

    init {
        ptr = if (assetManager != null) {
            newFromAsset(assetManager, config)
        } else {
            newFromFile(config)
        }
        require(ptr != 0L) {
            "Invalid SpokenLanguageIdentificationConfig: failed to create native SpokenLanguageIdentification"
        }
    }

    protected fun finalize() {
        if (ptr != 0L) {
            delete(ptr)
            ptr = 0
        }
    }

    fun release() = finalize()

    fun createStream(): OfflineStream {
        val p = createStream(ptr)
        return OfflineStream(p)
    }

    fun compute(stream: OfflineStream) = compute(ptr, stream.ptr)

    private external fun newFromAsset(
        assetManager: AssetManager,
        config: SpokenLanguageIdentificationConfig,
    ): Long

    private external fun newFromFile(
        config: SpokenLanguageIdentificationConfig,
    ): Long

    private external fun delete(ptr: Long)

    private external fun createStream(ptr: Long): Long

    private external fun compute(ptr: Long, streamPtr: Long): String

    companion object {
        init {
            System.loadLibrary("sherpa-onnx-jni")
        }
    }
}

// please refer to
// https://k2-fsa.github.io/sherpa/onnx/spolken-language-identification/pretrained_models.html#whisper
// to download more models
fun getSpokenLanguageIdentificationConfig(
    type: Int,
    numThreads: Int = 1
): SpokenLanguageIdentificationConfig? {
    when (type) {
        0 -> {
            val modelDir = "sherpa-onnx-whisper-tiny"
            return SpokenLanguageIdentificationConfig(
                whisper = SpokenLanguageIdentificationWhisperConfig(
                    encoder = "$modelDir/tiny-encoder.int8.onnx",
                    decoder = "$modelDir/tiny-decoder.int8.onnx",
                ),
                numThreads = numThreads,
                debug = true,
            )
        }

        1 -> {
            val modelDir = "sherpa-onnx-whisper-base"
            return SpokenLanguageIdentificationConfig(
                whisper = SpokenLanguageIdentificationWhisperConfig(
                    encoder = "$modelDir/base-encoder.int8.onnx",
                    decoder = "$modelDir/base-decoder.int8.onnx",
                ),
                numThreads = 1,
                debug = true,
            )
        }
    }
    return null
}
