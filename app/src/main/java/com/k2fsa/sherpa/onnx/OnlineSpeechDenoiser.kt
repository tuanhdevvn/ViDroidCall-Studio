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

data class OnlineSpeechDenoiserConfig(
    var model: OfflineSpeechDenoiserModelConfig = OfflineSpeechDenoiserModelConfig(),
)

class OnlineSpeechDenoiser(
    assetManager: AssetManager? = null,
    config: OnlineSpeechDenoiserConfig,
) {
    private var ptr: Long

    init {
        ptr = if (assetManager != null) {
            newFromAsset(assetManager, config)
        } else {
            newFromFile(config)
        }
        require(ptr != 0L) {
            "Invalid OnlineSpeechDenoiserConfig: failed to create native OnlineSpeechDenoiser"
        }
    }

    protected fun finalize() {
        if (ptr != 0L) {
            delete(ptr)
            ptr = 0
        }
    }

    fun release() = finalize()

    fun run(samples: FloatArray, sampleRate: Int) = run(ptr, samples, sampleRate)

    fun flush() = flush(ptr)

    fun reset() = reset(ptr)

    val sampleRate
      get() = getSampleRate(ptr)

    val frameShiftInSamples
      get() = getFrameShiftInSamples(ptr)

    private external fun newFromAsset(
        assetManager: AssetManager,
        config: OnlineSpeechDenoiserConfig,
    ): Long

    private external fun newFromFile(
        config: OnlineSpeechDenoiserConfig,
    ): Long

    private external fun delete(ptr: Long)

    private external fun run(ptr: Long, samples: FloatArray, sampleRate: Int): DenoisedAudio

    private external fun flush(ptr: Long): DenoisedAudio

    private external fun reset(ptr: Long)

    private external fun getSampleRate(ptr: Long): Int

    private external fun getFrameShiftInSamples(ptr: Long): Int

    companion object {
        init {
            System.loadLibrary("sherpa-onnx-jni")
        }
    }
}
