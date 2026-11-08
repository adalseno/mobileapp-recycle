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

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implemented in Swift (iosApp/iosApp/VisionKitScanner.swift), because VisionKit's
 * DataScannerViewController is a Swift-only API that Kotlin can't call directly.
 */
interface IosScannerBridge {
    fun scan(callback: IosScanCallback)
}

/** Exactly one of these is called per [IosScannerBridge.scan]. */
interface IosScanCallback {
    fun onScanned(code: String)
    fun onCancelled()
    fun onNotSupported()
    fun onPermissionDenied()
    fun onFailed(message: String)
}

class IosBarcodeScanner(private val bridge: IosScannerBridge) : BarcodeScanner {
    override suspend fun scan(): ScanResult = suspendCancellableCoroutine { cont ->
        fun finish(result: ScanResult) {
            if (cont.isActive) cont.resume(result)
        }
        bridge.scan(object : IosScanCallback {
            override fun onScanned(code: String) = finish(ScanResult.Success(code))
            override fun onCancelled() = finish(ScanResult.Cancelled)
            override fun onNotSupported() = finish(ScanResult.Unavailable(UnavailableReason.NotSupported))
            override fun onPermissionDenied() = finish(ScanResult.Unavailable(UnavailableReason.PermissionDenied))
            override fun onFailed(message: String) = finish(ScanResult.Failure(message))
        })
    }
}
