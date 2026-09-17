// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 ViDroidCall Studio contributors

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
