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

import android.content.Context
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Google Code Scanner. The scanning UI is a module inside Google Play services, so the app
 * needs no camera permission. Play services downloads the module on demand (the manifest
 * also asks for it at install time); until it's there we report [UnavailableReason.Downloading].
 */
class AndroidBarcodeScanner(private val context: Context) : BarcodeScanner {

    private val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
        )
        .build()

    override suspend fun scan(): ScanResult {
        val scanner = GmsBarcodeScanning.getClient(context, options)
        val moduleInstall = ModuleInstall.getClient(context)

        val available = try {
            moduleInstall.areModulesAvailable(scanner).await().areModulesAvailable()
        } catch (e: Exception) {
            // Play services missing or too old: the scanner can't work on this device.
            return ScanResult.Unavailable(UnavailableReason.NotSupported)
        }
        if (!available) {
            moduleInstall.installModules(ModuleInstallRequest.newBuilder().addApi(scanner).build())
            return ScanResult.Unavailable(UnavailableReason.Downloading)
        }

        return suspendCancellableCoroutine { cont ->
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val code = barcode.rawValue
                    cont.resumeIfActive(if (code.isNullOrBlank()) ScanResult.Failure("Empty barcode") else ScanResult.Success(code))
                }
                .addOnCanceledListener { cont.resumeIfActive(ScanResult.Cancelled) }
                .addOnFailureListener { e ->
                    val result = if (e is MlKitException && e.errorCode == MlKitException.UNAVAILABLE) {
                        ScanResult.Unavailable(UnavailableReason.Downloading)
                    } else {
                        ScanResult.Failure(e.message)
                    }
                    cont.resumeIfActive(result)
                }
        }
    }
}

private fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) resume(value)
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task ->
        val error = task.exception
        when {
            error != null -> if (cont.isActive) cont.resumeWith(Result.failure(error))
            task.isCanceled -> cont.cancel()
            else -> cont.resumeIfActive(task.result)
        }
    }
}
