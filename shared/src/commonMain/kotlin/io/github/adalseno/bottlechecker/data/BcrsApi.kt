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

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Talks to the (unofficial) barcode lookup used by the BCRS Malta website.
 *
 * Everything about the endpoint lives here, so switching to an official API later
 * only touches this class.
 */
class BcrsApi(
    private val client: HttpClient,
    private val endpoint: String = DEFAULT_ENDPOINT,
) {
    /**
     * @throws BcrsHttpException on a non-2xx HTTP status.
     * @throws kotlinx.serialization.SerializationException if the body isn't the expected JSON.
     * @throws kotlinx.io.IOException (and Ktor timeout exceptions) on network problems.
     */
    suspend fun lookup(barcode: String): BcrsResponse {
        val response = client.submitForm(
            url = endpoint,
            formParameters = parameters {
                append("action", "barcode_data_fetch")
                append("keyword", barcode)
            },
        )
        if (!response.status.isSuccess()) throw BcrsHttpException(response.status.value)
        return json.decodeFromString<BcrsResponse>(response.bodyAsText().trim())
    }

    companion object {
        const val DEFAULT_ENDPOINT = "https://bcrsmalta.mt/wp-admin/admin-ajax.php"
        const val USER_AGENT = "BottleChecker/0.1 (+https://github.com/adalseno/mobileapp-recycle)"
        const val TIMEOUT_MILLIS = 10_000L

        private val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
data class BcrsResponse(
    val status: String? = null,
    val message: String? = null,
)

class BcrsHttpException(val statusCode: Int) : Exception("HTTP $statusCode")

/**
 * Creates the HTTP client. Without an [engine], Ktor picks the platform one
 * (OkHttp on Android, Darwin on iOS). Tests pass a `MockEngine`.
 */
fun createHttpClient(
    engine: HttpClientEngine? = null,
    timeoutMillis: Long = BcrsApi.TIMEOUT_MILLIS,
): HttpClient {
    val config: HttpClientConfig<*>.() -> Unit = {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMillis
            connectTimeoutMillis = timeoutMillis
            socketTimeoutMillis = timeoutMillis
        }
        install(UserAgent) {
            agent = BcrsApi.USER_AGENT
        }
    }
    return if (engine == null) HttpClient(config) else HttpClient(engine, config)
}
