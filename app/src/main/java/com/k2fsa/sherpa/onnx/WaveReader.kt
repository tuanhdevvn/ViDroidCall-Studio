// SPDX-License-Identifier: Apache-2.0
// Copyright (c) 2023 Xiaomi Corporation
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

data class WaveData(
    val samples: FloatArray,
    val sampleRate: Int,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WaveData

        if (!samples.contentEquals(other.samples)) return false
        if (sampleRate != other.sampleRate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = samples.contentHashCode()
        result = 31 * result + sampleRate
        return result
    }
}

class WaveReader {
    companion object {

        fun readWave(
            assetManager: AssetManager,
            filename: String,
        ): WaveData {
            return readWaveFromAsset(assetManager, filename)
        }

        fun readWave(
            filename: String,
        ): WaveData {
            return readWaveFromFile(filename)
        }

        // Read a mono wave file asset
        external fun readWaveFromAsset(
            assetManager: AssetManager,
            filename: String,
        ): WaveData

        // Read a mono wave file from disk
        external fun readWaveFromFile(
            filename: String,
        ): WaveData

        init {
            System.loadLibrary("sherpa-onnx-jni")
        }
    }
}
