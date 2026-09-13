/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.dav4jvm.ktor.exception

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpExceptionTest {

    private val sampleUrl = "https://example.com"

    @Test
    fun message() = runTest {
        val mockEngine = MockEngine {
            respond(
                status = HttpStatusCode.Forbidden,
                content = "Your Information"
            )
        }
        val httpClient = HttpClient(mockEngine)
        val response = httpClient.get(sampleUrl)
        val result = HttpException.fromResponse(response)

        assertEquals("HTTP 403 Forbidden", result.message)
    }

    @Test
    fun isRedirect() = runTest {
        val mockEngine = MockEngine {
            respond(
                status = HttpStatusCode.Found,
                content = "Your Information",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            )
        }
        val httpClient = HttpClient(mockEngine)
        val response = httpClient.get(sampleUrl)
        val result = HttpException.fromResponse(response)

        assertTrue(result.isRedirect)
        assertFalse(result.isClientError)
        assertFalse(result.isServerError)
    }

    @Test
    fun isClientError() = runTest {
        val mockEngine = MockEngine {
            respond(
                status = HttpStatusCode.NotFound,
                content = "Your Information",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            )
        }
        val httpClient = HttpClient(mockEngine)
        val response = httpClient.get(sampleUrl)
        val result = HttpException.fromResponse(response)

        assertFalse(result.isRedirect)
        assertTrue(result.isClientError)
        assertFalse(result.isServerError)
    }

    @Test
    fun isServerError() = runTest {
        val mockEngine = MockEngine {
            respond(
                status = HttpStatusCode.InternalServerError,
                content = "Your Information",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
            )
        }
        val httpClient = HttpClient(mockEngine)
        val response = httpClient.get(sampleUrl)
        val result = HttpException.fromResponse(response)

        assertFalse(result.isRedirect)
        assertFalse(result.isClientError)
        assertTrue(result.isServerError)
    }

    @Test
    fun `reasonPhrase - default phrase fallback`() {
        assertEquals("OK", HttpException.reasonPhrase(HttpStatusCode(200, "")))
    }

    @Test
    fun `reasonPhrase - from HttpStatusCode`() {
        assertEquals("Custom OK", HttpException.reasonPhrase(HttpStatusCode(200, "Custom OK")))
    }

}