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

import com.example.ViDroidCall_Studio.data.nlu.NluModelLoadGate
import com.example.ViDroidCall_Studio.data.nlu.NluModelState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NluModelLoadGateTest {

    @Test
    fun skipsWhenAlreadyLoadingEvenIfForce() {
        assertFalse(
            NluModelLoadGate.shouldStartLoad(
                state = NluModelState.Loading,
                force = false,
                isReady = false
            )
        )
        assertFalse(
            NluModelLoadGate.shouldStartLoad(
                state = NluModelState.Loading,
                force = true,
                isReady = false
            )
        )
    }

    @Test
    fun skipsWhenReadyUnlessForce() {
        val ready = NluModelState.Ready("qwen3-nlu-run-Q4_K_M.gguf")
        assertFalse(NluModelLoadGate.shouldStartLoad(ready, force = false, isReady = true))
        assertTrue(NluModelLoadGate.shouldStartLoad(ready, force = true, isReady = true))
    }

    @Test
    fun startsFromUninitializedNotFoundOrError() {
        assertTrue(
            NluModelLoadGate.shouldStartLoad(NluModelState.Uninitialized, force = false, isReady = false)
        )
        assertTrue(
            NluModelLoadGate.shouldStartLoad(NluModelState.ModelNotFound, force = false, isReady = false)
        )
        assertTrue(
            NluModelLoadGate.shouldStartLoad(
                NluModelState.Error("fail"),
                force = false,
                isReady = false
            )
        )
    }
}
