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
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.auth.HttpAuthHeader

/**
 * An [AuthProvider] that tries Basic auth preemptively and switches to Digest auth (remembered
 * for subsequent requests) once the server challenges for it.
 *
 * Wrap in [DomainAuthProvider] to restrict it to a certain domain.
 *
 * On the JVM, Digest auth is done by Ktor's `DigestAuthProvider`; on iOS (where Ktor's
 * `DigestAuthProvider` isn't supported) by an own implementation.
 *
 * @param username the username to authenticate with
 * @param password the password to authenticate with
 */
expect class PreemptiveBasicDigestAuthProvider(
    username: String,
    password: String
) : AuthProvider {

    @Suppress("OverridingDeprecatedMember", "DEPRECATION_ERROR")
    @Deprecated("Please use sendWithoutRequest function instead", level = DeprecationLevel.ERROR)
    override val sendWithoutRequest: Boolean

    override fun sendWithoutRequest(request: HttpRequestBuilder): Boolean

    override fun isApplicable(auth: HttpAuthHeader): Boolean

    override suspend fun addRequestHeaders(request: HttpRequestBuilder, authHeader: HttpAuthHeader?)

}
