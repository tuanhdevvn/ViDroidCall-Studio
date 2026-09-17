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

class VersionInfo {
    companion object {
        init {
            System.loadLibrary("sherpa-onnx-jni")
        }

        val version: String
            get() = getVersionStr2()

        val gitSha1: String
            get() = getGitSha12()

        val gitDate: String
            get() = getGitDate2()

        val onnxruntimeVersion: String
            get() = getOnnxruntimeVersionStr2()

        external fun getVersionStr2(): String
        external fun getGitSha12(): String
        external fun getGitDate2(): String
        external fun getOnnxruntimeVersionStr2(): String
    }
}
