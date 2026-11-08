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

package io.github.adalseno.bottlechecker.data

import io.github.adalseno.bottlechecker.domain.Eligibility
import io.github.adalseno.bottlechecker.domain.ErrorKind
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/** Turns the raw server reply into an [Eligibility]. Anything unexpected is an error, never "not eligible". */
class BcrsRepository(private val api: BcrsApi) {

    suspend fun check(barcode: String): Eligibility = try {
        val response = api.lookup(barcode)
        when (response.status?.trim()?.lowercase()) {
            "success" -> Eligibility.Eligible
            "fail" -> Eligibility.NotEligible
            else -> Eligibility.Error(ErrorKind.UnexpectedResponse, "status=${response.status}")
        }
    } catch (e: HttpRequestTimeoutException) {
        Eligibility.Error(ErrorKind.Timeout, e.message)
    } catch (e: ConnectTimeoutException) {
        Eligibility.Error(ErrorKind.Timeout, e.message)
    } catch (e: SocketTimeoutException) {
        Eligibility.Error(ErrorKind.Timeout, e.message)
    } catch (e: CancellationException) {
        throw e
    } catch (e: BcrsHttpException) {
        Eligibility.Error(ErrorKind.Server, e.message)
    } catch (e: SerializationException) {
        Eligibility.Error(ErrorKind.UnexpectedResponse, e.message)
    } catch (e: IllegalArgumentException) {
        Eligibility.Error(ErrorKind.UnexpectedResponse, e.message)
    } catch (e: IOException) {
        Eligibility.Error(ErrorKind.Network, e.message)
    } catch (e: Exception) {
        // Engine-specific failures that aren't IOExceptions are still connection problems for the user.
        Eligibility.Error(ErrorKind.Network, e.message)
    }
}
