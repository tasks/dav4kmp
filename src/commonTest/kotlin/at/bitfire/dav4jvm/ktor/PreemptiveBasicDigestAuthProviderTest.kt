/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.dav4jvm.ktor

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.http.encodedPath
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreemptiveBasicDigestAuthProviderTest {

    @Test
    fun `sendWithoutRequest is true before any challenge`() {
        val authProvider = PreemptiveBasicDigestAuthProvider(
            username = "user",
            password = "password"
        )
        val request = HttpRequestBuilder().apply {
            url.protocol = URLProtocol.HTTPS
            url.host = "domain.example"
        }

        assertTrue(authProvider.sendWithoutRequest(request))
    }

    @Test
    fun `sendWithoutRequest stays true after Digest challenge`() {
        val authProvider = PreemptiveBasicDigestAuthProvider(
            username = "user",
            password = "password"
        )
        val authHeader = parseAuthorizationHeader("""Digest algorithm=MD5, realm="realm", nonce="md5-nonce"""")!!
        authProvider.isApplicable(authHeader)

        val request = HttpRequestBuilder().apply {
            url.protocol = URLProtocol.HTTPS
            url.host = "domain.example"
        }

        assertTrue(authProvider.sendWithoutRequest(request))
    }

    @Test
    fun `addRequestHeaders sends Basic before any challenge`() = runTest {
        val authProvider = PreemptiveBasicDigestAuthProvider(
            username = "user",
            password = "password"
        )
        val request = HttpRequestBuilder().apply {
            url.protocol = URLProtocol.HTTPS
            url.host = "domain.example"
        }

        authProvider.addRequestHeaders(request, authHeader = null)

        assertEquals("Basic dXNlcjpwYXNzd29yZA==", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `addRequestHeaders leaves the URL untouched for Basic`() = runTest {
        // the empty-path normalization is a workaround for a ktor Digest bug, so it must not affect Basic requests
        val authProvider = PreemptiveBasicDigestAuthProvider(
            username = "user",
            password = "password"
        )
        val request = HttpRequestBuilder().apply {
            url.protocol = URLProtocol.HTTPS
            url.host = "domain.example"
        }

        authProvider.addRequestHeaders(request, authHeader = null)

        assertEquals("", request.url.encodedPath)
    }

}
