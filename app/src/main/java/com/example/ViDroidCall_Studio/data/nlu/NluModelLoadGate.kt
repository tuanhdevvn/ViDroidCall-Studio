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

/**
 * Chỉ một lần `LlamaHelper.load` tại một thời điểm.
 * Gọi trước khi set [NluModelState.Loading] (ngoài coroutine) để init + ON_RESUME không đua.
 */
internal object NluModelLoadGate {
    fun shouldStartLoad(
        state: NluModelState,
        force: Boolean,
        isReady: Boolean
    ): Boolean {
        if (state is NluModelState.Loading) return false
        if (force) return true
        if (isReady) return false
        return true
    }
}
