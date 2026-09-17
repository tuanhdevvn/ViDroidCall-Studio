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

data class OfflinePunctuationModelConfig(
    var ctTransformer: String = "",
    var numThreads: Int = 1,
    var debug: Boolean = false,
    var provider: String = "cpu",
)


data class OfflinePunctuationConfig(
    var model: OfflinePunctuationModelConfig,
)

class OfflinePunctuation(
    assetManager: AssetManager? = null,
    config: OfflinePunctuationConfig,
) {
    private var ptr: Long

    init {
        ptr = if (assetManager != null) {
            newFromAsset(assetManager, config)
        } else {
            newFromFile(config)
        }
        require(ptr != 0L) {
            "Invalid OfflinePunctuationConfig: failed to create native OfflinePunctuation"
        }
    }

    protected fun finalize() {
        if (ptr != 0L) {
            delete(ptr)
            ptr = 0
        }
    }

    fun release() = finalize()

    fun addPunctuation(text: String) = addPunctuation(ptr, text)

    private external fun delete(ptr: Long)

    private external fun addPunctuation(ptr: Long, text: String): String

    private external fun newFromAsset(
        assetManager: AssetManager,
        config: OfflinePunctuationConfig,
    ): Long

    private external fun newFromFile(
        config: OfflinePunctuationConfig,
    ): Long

    companion object {
        init {
            System.loadLibrary("sherpa-onnx-jni")
        }
    }
}
