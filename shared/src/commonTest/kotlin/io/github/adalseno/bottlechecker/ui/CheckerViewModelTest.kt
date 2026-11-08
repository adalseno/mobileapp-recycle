/*
 * Copyright 2026 adalseno
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.adalseno.bottlechecker.ui

import io.github.adalseno.bottlechecker.data.FAIL_JSON
import io.github.adalseno.bottlechecker.data.SUCCESS_JSON
import io.github.adalseno.bottlechecker.data.jsonReply
import io.github.adalseno.bottlechecker.data.repositoryWith
import io.github.adalseno.bottlechecker.domain.BarcodeValidator.InvalidReason
import io.github.adalseno.bottlechecker.domain.ErrorKind
import io.github.adalseno.bottlechecker.scanner.ScanResult
import io.github.adalseno.bottlechecker.scanner.UnavailableReason
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class CheckerViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** MockEngine runs on its own dispatcher, so wait until the check settles. */
    private suspend fun CheckerViewModel.awaitSettled(): CheckerUiState {
        repeat(1_000) {
            if (state.value.check !is CheckState.Loading) return state.value
            kotlinx.coroutines.delay(1)
        }
        error("Check never finished")
    }

    @Test
    fun inputKeepsDigitsOnlyUpTo13() {
        val vm = CheckerViewModel(repositoryWith(handler = jsonReply(SUCCESS_JSON)))
        vm.onInputChange("54-49 0000009961234")
        assertEquals("5449000000996", vm.state.value.input)
    }

    @Test
    fun manualCheckEligible() = runTest {
        val vm = CheckerViewModel(repositoryWith(handler = jsonReply(SUCCESS_JSON)))
        vm.onInputChange("5449000000996")
        vm.onCheckInput()
        assertEquals(CheckState.Done("5449000000996", eligible = true), vm.awaitSettled().check)
    }

    @Test
    fun invalidInputDoesNotCallServer() = runTest {
        var calls = 0
        val vm = CheckerViewModel(repositoryWith { calls++; jsonReply(SUCCESS_JSON)(this, it) })
        vm.onInputChange("1234567890123")
        vm.onCheckInput()
        yield()
        assertEquals(InvalidReason.BadCheckDigit, vm.state.value.inputError)
        assertEquals(CheckState.Idle, vm.state.value.check)
        assertEquals(0, calls)
    }

    @Test
    fun scanSuccessFillsInputAndChecks() = runTest {
        val vm = CheckerViewModel(repositoryWith(handler = jsonReply(FAIL_JSON)))
        vm.onScanResult(ScanResult.Success("96385074"))
        val state = vm.awaitSettled()
        assertEquals("96385074", state.input)
        assertEquals(CheckState.Done("96385074", eligible = false), state.check)
    }

    @Test
    fun scanUnavailableShowsNotice() {
        val vm = CheckerViewModel(repositoryWith(handler = jsonReply(SUCCESS_JSON)))
        vm.onScanResult(ScanResult.Unavailable(UnavailableReason.Downloading))
        assertEquals(ScanNotice.Unavailable(UnavailableReason.Downloading), vm.state.value.scanNotice)
        vm.onScanResult(ScanResult.Cancelled)
        assertEquals(null, vm.state.value.scanNotice)
    }

    @Test
    fun serverErrorThenRetrySucceeds() = runTest {
        var calls = 0
        val vm = CheckerViewModel(
            repositoryWith {
                calls++
                if (calls == 1) respondError(HttpStatusCode.ServiceUnavailable) else jsonReply(SUCCESS_JSON)(this, it)
            },
        )
        vm.onInputChange("5449000000996")
        vm.onCheckInput()
        assertEquals(CheckState.Failed("5449000000996", ErrorKind.Server), vm.awaitSettled().check)

        vm.onRetry()
        assertEquals(CheckState.Done("5449000000996", eligible = true), vm.awaitSettled().check)
        assertEquals(2, calls)
    }
}
