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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.adalseno.bottlechecker.data.BcrsApi
import io.github.adalseno.bottlechecker.data.BcrsRepository
import io.github.adalseno.bottlechecker.data.createHttpClient
import io.github.adalseno.bottlechecker.domain.BarcodeValidator
import io.github.adalseno.bottlechecker.domain.Eligibility
import io.github.adalseno.bottlechecker.domain.ErrorKind
import io.github.adalseno.bottlechecker.scanner.ScanResult
import io.github.adalseno.bottlechecker.scanner.UnavailableReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CheckerUiState(
    /** What's in the manual-entry field (digits only). */
    val input: String = "",
    val inputError: BarcodeValidator.InvalidReason? = null,
    val scanNotice: ScanNotice? = null,
    val check: CheckState = CheckState.Idle,
)

sealed interface CheckState {
    data object Idle : CheckState
    data class Loading(val barcode: String) : CheckState
    data class Done(val barcode: String, val eligible: Boolean) : CheckState
    data class Failed(val barcode: String, val kind: ErrorKind) : CheckState
}

/** Something to tell the user after a scan attempt that didn't produce a barcode. */
sealed interface ScanNotice {
    data class Unavailable(val reason: UnavailableReason) : ScanNotice
    data object Failed : ScanNotice
}

class CheckerViewModel(
    private val repository: BcrsRepository,
    vararg closeables: AutoCloseable,
) : ViewModel(*closeables) {

    private val _state = MutableStateFlow(CheckerUiState())
    val state: StateFlow<CheckerUiState> = _state.asStateFlow()

    private var checkJob: Job? = null

    fun onInputChange(text: String) {
        val digits = text.filter { it in '0'..'9' }.take(MAX_LENGTH)
        _state.update { it.copy(input = digits, inputError = null) }
    }

    fun onCheckInput() = check(_state.value.input)

    fun onScanResult(result: ScanResult) {
        when (result) {
            is ScanResult.Success -> {
                _state.update { it.copy(input = result.code, inputError = null, scanNotice = null) }
                check(result.code)
            }
            ScanResult.Cancelled -> _state.update { it.copy(scanNotice = null) }
            is ScanResult.Unavailable ->
                _state.update { it.copy(scanNotice = ScanNotice.Unavailable(result.reason)) }
            is ScanResult.Failure -> _state.update { it.copy(scanNotice = ScanNotice.Failed) }
        }
    }

    fun onRetry() {
        val failed = _state.value.check as? CheckState.Failed ?: return
        check(failed.barcode)
    }

    private fun check(raw: String) {
        when (val validation = BarcodeValidator.validate(raw)) {
            is BarcodeValidator.Result.Invalid ->
                _state.update { it.copy(inputError = validation.reason, check = CheckState.Idle) }
            is BarcodeValidator.Result.Valid -> {
                val barcode = validation.code
                checkJob?.cancel()
                _state.update {
                    it.copy(inputError = null, scanNotice = null, check = CheckState.Loading(barcode))
                }
                checkJob = viewModelScope.launch {
                    val next = when (val result = repository.check(barcode)) {
                        Eligibility.Eligible -> CheckState.Done(barcode, eligible = true)
                        Eligibility.NotEligible -> CheckState.Done(barcode, eligible = false)
                        is Eligibility.Error -> CheckState.Failed(barcode, result.kind)
                    }
                    _state.update { it.copy(check = next) }
                }
            }
        }
    }

    companion object {
        private const val MAX_LENGTH = 13

        /** Production wiring: real HTTP client, closed when the ViewModel is cleared. */
        fun create(): CheckerViewModel {
            val client = createHttpClient()
            return CheckerViewModel(BcrsRepository(BcrsApi(client)), client)
        }
    }
}
