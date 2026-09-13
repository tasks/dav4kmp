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

import io.ktor.client.plugins.auth.AuthProvider
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.BasicAuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.auth.AuthScheme
import io.ktor.http.auth.HttpAuthHeader
import kotlin.concurrent.Volatile

/**
 * An [AuthProvider] that tries Basic auth preemptively and switches to Digest auth (remembered
 * for subsequent requests) once the server challenges for it.
 *
 * Handling both schemes in one provider avoids sending both an `Authorization: Basic` and
 * `Authorization: Digest` header on the same request (https://github.com/bitfireAT/dav4jvm/issues/198).
 *
 * Wrap in [DomainAuthProvider] to restrict it to a certain domain.
 *
 * @param username the username to authenticate with
 * @param password the password to authenticate with
 */
class PreemptiveBasicDigestAuthProvider(
    username: String,
    password: String
) : AuthProvider {

    private val basicAuthProvider = BasicAuthProvider(
        credentials = { BasicAuthCredentials(username, password) }
    )
    private val digestAuthProvider = DigestAuthProvider(username, password)

    /**
     * Whether to use Digest auth preemptively (after the server has challenged for it),
     * or Basic auth (the default, until the server challenges for Digest).
     */
    @Volatile
    private var preemptiveDigest = false

    @Suppress("OverridingDeprecatedMember", "DEPRECATION_ERROR")
    @Deprecated("Please use sendWithoutRequest function instead", level = DeprecationLevel.ERROR)
    override val sendWithoutRequest: Boolean
        get() = error("Deprecated")

    override fun sendWithoutRequest(request: HttpRequestBuilder): Boolean = true

    override fun isApplicable(auth: HttpAuthHeader): Boolean =
        basicAuthProvider.isApplicable(auth) || digestAuthProvider.isApplicable(auth)

    override suspend fun addRequestHeaders(request: HttpRequestBuilder, authHeader: HttpAuthHeader?) {
        /* Ktor's Auth plugin retry logic copies all headers from the original request (which
        may include a stale Authorization header set by a previous preemptive attempt)
        before calling this method. Always clear it first so we never send two Authorization headers. */
        request.headers.remove(HttpHeaders.Authorization)

        /* On a retry, authHeader tells us exactly which scheme (basic vs digest) this response challenged for;
        that scheme is then remembered for subsequent requests. Without a challenge, use the remembered scheme. */
        val useDigest = authHeader?.authScheme?.equals(AuthScheme.Digest, ignoreCase = true)
            ?.also { preemptiveDigest = it }
            ?: preemptiveDigest
        val provider = if (useDigest) digestAuthProvider else basicAuthProvider
        provider.addRequestHeaders(request, authHeader)
    }

}
