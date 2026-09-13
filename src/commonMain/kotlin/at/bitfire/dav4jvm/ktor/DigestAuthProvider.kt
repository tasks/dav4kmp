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

import at.bitfire.dav4jvm.QuotedStringUtils
import io.ktor.client.plugins.auth.AuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.auth.AuthScheme
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.encodedPath
import io.ktor.http.fullPath
import io.ktor.util.logging.KtorSimpleLogger
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

/**
 * An [AuthProvider] for HTTP Digest authentication (RFC 7616) that works on all platforms.
 *
 * Supports the `MD5`, `MD5-sess`, `SHA-256` and `SHA-256-sess` algorithms and `qop=auth`.
 *
 * Like Ktor's `DigestAuthProvider`, it never authenticates preemptively on its own
 * ([sendWithoutRequest] is always `false`): a request is only signed after the server has
 * challenged for it. Because the `Authorization` header carries a hash of the password, it must
 * only go to the server that challenged – and Ktor's Auth plugin doesn't know which one that was.
 * The last challenge is remembered though (with an incremented nonce count) so that
 * [addRequestHeaders] can also sign a request without a challenge – for a provider like
 * [PreemptiveBasicDigestAuthProvider] that does authenticate preemptively (wrapped in
 * [DomainAuthProvider] to restrict it to the server's domain).
 *
 * @param username the username to authenticate with
 * @param password the password to authenticate with
 */
@OptIn(ExperimentalAtomicApi::class)
class DigestAuthProvider internal constructor(
    private val username: String,
    private val password: String,
    private val cnonceGenerator: () -> String
) : AuthProvider {

    constructor(username: String, password: String) : this(username, password, ::generateCnonce)

    internal enum class Algorithm(val token: String, val session: Boolean, val hash: (ByteArray) -> ByteArray) {
        MD5("MD5", false, ::md5),
        MD5_SESS("MD5-sess", true, ::md5),
        SHA256("SHA-256", false, ::sha256),
        SHA256_SESS("SHA-256-sess", true, ::sha256);

        companion object {
            fun byToken(token: String) = entries.firstOrNull { it.token.equals(token, ignoreCase = true) }
        }
    }

    private class Challenge(
        val realm: String,
        val nonce: String,
        val opaque: String?,
        val qop: String?,
        val algorithm: Algorithm
    ) {
        val nonceCount = AtomicInt(0)
    }

    @Volatile
    private var challenge: Challenge? = null

    @Suppress("OverridingDeprecatedMember", "DEPRECATION_ERROR")
    @Deprecated("Please use sendWithoutRequest function instead", level = DeprecationLevel.ERROR)
    override val sendWithoutRequest: Boolean
        get() = error("Deprecated")

    override fun sendWithoutRequest(request: HttpRequestBuilder): Boolean = false

    override fun isApplicable(auth: HttpAuthHeader): Boolean = parseChallenge(auth) != null

    /**
     * Signs the request with the challenge from [authHeader] (the one that has been sent for
     * exactly this request, when Ktor retries after a 401), or with the last remembered challenge
     * if there is none (preemptive authentication by an enclosing provider).
     */
    override suspend fun addRequestHeaders(request: HttpRequestBuilder, authHeader: HttpAuthHeader?) {
        val challenge = authHeader?.let { parseChallenge(it) }?.let { parsed ->
            challenge?.takeIf { it.nonce == parsed.nonce } ?: parsed.also { challenge = it }
        } ?: challenge ?: return

        val method = request.method.value.uppercase()
        val url = request.url.build()
        val uri = if (url.encodedPath.isEmpty()) "/" + url.fullPath else url.fullPath
        val nonceCount = challenge.nonceCount.incrementAndFetch().toString(16).padStart(8, '0')
        val cnonce = cnonceGenerator()

        val hash = challenge.algorithm.hash
        fun h(value: String) = hash(value.encodeToByteArray()).toHexString()

        var a1 = h("$username:${challenge.realm}:$password")
        if (challenge.algorithm.session)
            a1 = h("$a1:${challenge.nonce}:$cnonce")
        val a2 = h("$method:$uri")
        val response =
            if (challenge.qop != null)
                h("$a1:${challenge.nonce}:$nonceCount:$cnonce:${challenge.qop}:$a2")
            else
                h("$a1:${challenge.nonce}:$a2")

        val params = buildList {
            add("username" to QuotedStringUtils.asQuotedString(username))
            add("realm" to QuotedStringUtils.asQuotedString(challenge.realm))
            add("nonce" to QuotedStringUtils.asQuotedString(challenge.nonce))
            add("uri" to QuotedStringUtils.asQuotedString(uri))
            add("response" to QuotedStringUtils.asQuotedString(response))
            add("algorithm" to challenge.algorithm.token)
            if (challenge.qop != null) {
                add("qop" to challenge.qop)
                add("nc" to nonceCount)
                add("cnonce" to QuotedStringUtils.asQuotedString(cnonce))
            }
            if (challenge.opaque != null)
                add("opaque" to QuotedStringUtils.asQuotedString(challenge.opaque))
        }
        request.headers[HttpHeaders.Authorization] =
            AuthScheme.Digest + " " + params.joinToString(", ") { (name, value) -> "$name=$value" }
    }

    private fun parseChallenge(auth: HttpAuthHeader): Challenge? {
        if (auth !is HttpAuthHeader.Parameterized || !auth.authScheme.equals(AuthScheme.Digest, ignoreCase = true))
            return null

        val realm = auth.parameter("realm") ?: run {
            logger.debug("Ignoring Digest challenge without realm")
            return null
        }
        val nonce = auth.parameter("nonce") ?: run {
            logger.debug("Ignoring Digest challenge without nonce")
            return null
        }
        val algorithmToken = auth.parameter("algorithm") ?: Algorithm.MD5.token
        val algorithm = Algorithm.byToken(algorithmToken) ?: run {
            logger.debug("Ignoring Digest challenge with unsupported algorithm $algorithmToken")
            return null
        }
        val qop = auth.parameter("qop")?.let { offered ->
            offered.split(',').map { it.trim() }.firstOrNull { it.equals("auth", ignoreCase = true) } ?: run {
                logger.debug("Ignoring Digest challenge with unsupported qop $offered")
                return null
            }
        }

        return Challenge(
            realm = realm,
            nonce = nonce,
            opaque = auth.parameter("opaque"),
            qop = qop,
            algorithm = algorithm
        )
    }


    companion object {

        private val logger = KtorSimpleLogger("at.bitfire.dav4jvm.DigestAuthProvider")

        private fun generateCnonce() = secureRandomBytes(16).toHexString()

    }

}
