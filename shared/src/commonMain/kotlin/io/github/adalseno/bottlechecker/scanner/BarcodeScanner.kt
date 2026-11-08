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

package io.github.adalseno.bottlechecker.scanner

/**
 * The only platform-specific piece of the app. Android uses Google Code Scanner,
 * iOS uses VisionKit's DataScannerViewController. Implementations are passed in at
 * app start, so shared code never touches platform APIs.
 */
interface BarcodeScanner {
    suspend fun scan(): ScanResult
}

sealed interface ScanResult {
    data class Success(val code: String) : ScanResult
    data object Cancelled : ScanResult
    data class Unavailable(val reason: UnavailableReason) : ScanResult
    data class Failure(val message: String?) : ScanResult
}

enum class UnavailableReason {
    /** The device can't scan (old hardware, no Play services…). Type the barcode instead. */
    NotSupported,

    /** The user refused camera access. */
    PermissionDenied,

    /** Android: Play services is still downloading the scanner module. Try again shortly. */
    Downloading,
}

/** Used in previews and on devices without a scanner. */
object UnsupportedScanner : BarcodeScanner {
    override suspend fun scan(): ScanResult = ScanResult.Unavailable(UnavailableReason.NotSupported)
}
