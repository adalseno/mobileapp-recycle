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
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BcrsRepositoryTest {

    @Test
    fun sendsTheExpectedRequest() = runTest {
        var seen: io.ktor.client.request.HttpRequestData? = null
        val repo = repositoryWith { request ->
            seen = request
            jsonReply(SUCCESS_JSON)(this, request)
        }
        repo.check("5449000000996")

        val request = requireNotNull(seen)
        assertEquals(HttpMethod.Post, request.method)
        assertEquals(BcrsApi.DEFAULT_ENDPOINT, request.url.toString())
        assertEquals(BcrsApi.USER_AGENT, request.headers[HttpHeaders.UserAgent])
        val form = assertIs<FormDataContent>(request.body).formData
        assertEquals("barcode_data_fetch", form["action"])
        assertEquals("5449000000996", form["keyword"])
    }

    @Test
    fun successIsEligible() = runTest {
        assertEquals(Eligibility.Eligible, repositoryWith(handler = jsonReply(SUCCESS_JSON)).check("5449000000996"))
    }

    @Test
    fun failIsNotEligible() = runTest {
        assertEquals(Eligibility.NotEligible, repositoryWith(handler = jsonReply(FAIL_JSON)).check("1234567890128"))
    }

    @Test
    fun jsonServedAsHtmlStillParses() = runTest {
        val repo = repositoryWith { respond(SUCCESS_JSON, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "text/html")) }
        assertEquals(Eligibility.Eligible, repo.check("5449000000996"))
    }

    @Test
    fun unknownStatusIsAnError() = runTest {
        val result = repositoryWith(handler = jsonReply("""{"status":"maybe","message":"?"}""")).check("5449000000996")
        assertEquals(ErrorKind.UnexpectedResponse, assertIs<Eligibility.Error>(result).kind)
    }

    @Test
    fun missingStatusIsAnError() = runTest {
        val result = repositoryWith(handler = jsonReply("""{"message":"?"}""")).check("5449000000996")
        assertEquals(ErrorKind.UnexpectedResponse, assertIs<Eligibility.Error>(result).kind)
    }

    @Test
    fun htmlPageIsAnError() = runTest {
        val repo = repositoryWith { respond("<html><body>Blocked</body></html>", HttpStatusCode.OK) }
        assertEquals(ErrorKind.UnexpectedResponse, assertIs<Eligibility.Error>(repo.check("5449000000996")).kind)
    }

    @Test
    fun wordpressZeroIsAnError() = runTest {
        // admin-ajax.php answers "0" when the action is unknown.
        val repo = repositoryWith { respond("0", HttpStatusCode.OK) }
        assertEquals(ErrorKind.UnexpectedResponse, assertIs<Eligibility.Error>(repo.check("5449000000996")).kind)
    }

    @Test
    fun http500IsAServerError() = runTest {
        val repo = repositoryWith { respondError(HttpStatusCode.InternalServerError) }
        assertEquals(ErrorKind.Server, assertIs<Eligibility.Error>(repo.check("5449000000996")).kind)
    }

    @Test
    fun http403IsAServerError() = runTest {
        val repo = repositoryWith { respondError(HttpStatusCode.Forbidden) }
        assertEquals(ErrorKind.Server, assertIs<Eligibility.Error>(repo.check("5449000000996")).kind)
    }

    @Test
    fun slowServerTimesOut() = runTest {
        val repo = repositoryWith(timeoutMillis = 1_000) {
            delay(5_000)
            jsonReply(SUCCESS_JSON)(this, it)
        }
        assertEquals(ErrorKind.Timeout, assertIs<Eligibility.Error>(repo.check("5449000000996")).kind)
    }

    @Test
    fun connectionFailureIsANetworkError() = runTest {
        val repo = repositoryWith { throw IOException("Unable to resolve host") }
        assertEquals(ErrorKind.Network, assertIs<Eligibility.Error>(repo.check("5449000000996")).kind)
    }
}
