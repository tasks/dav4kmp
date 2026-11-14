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
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.http.auth.parseAuthorizationHeader
import io.ktor.http.encodedPath
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DigestAuthProviderTest {

    private val rfc2617Challenge = parseAuthorizationHeader(
        """Digest realm="testrealm@host.com", qop="auth,auth-int", nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093", opaque="5ccc069c403ebaf9f0171e9517f40e41""""
    )!!

    private val rfc7616Challenge = parseAuthorizationHeader(
        """Digest realm="http-auth@example.org", qop="auth, auth-int", algorithm=SHA-256, nonce="7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v", opaque="FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS""""
    )!!

    private fun request(path: String = "/dir/index.html", method: HttpMethod = HttpMethod.Get) =
        HttpRequestBuilder().apply {
            this.method = method
            url.protocol = URLProtocol.HTTP
            url.host = "www.example.org"
            url.encodedPath = path
        }

    private fun HttpRequestBuilder.digestHeader() =
        parseAuthorizationHeader(headers[HttpHeaders.Authorization]!!) as HttpAuthHeader.Parameterized


    @Test
    fun rfc2617_example() = runTest {
        val provider = DigestAuthProvider("Mufasa", "Circle Of Life") { "0a4f113b" }
        assertTrue(provider.isApplicable(rfc2617Challenge))

        val request = request()
        provider.addRequestHeaders(request, rfc2617Challenge)

        val digest = request.digestHeader()
        assertEquals("Mufasa", digest.parameter("username"))
        assertEquals("testrealm@host.com", digest.parameter("realm"))
        assertEquals("dcd98b7102dd2f0e8b11d0f600bfb0c093", digest.parameter("nonce"))
        assertEquals("/dir/index.html", digest.parameter("uri"))
        assertEquals("auth", digest.parameter("qop"))
        assertEquals("00000001", digest.parameter("nc"))
        assertEquals("0a4f113b", digest.parameter("cnonce"))
        assertEquals("5ccc069c403ebaf9f0171e9517f40e41", digest.parameter("opaque"))
        assertEquals("MD5", digest.parameter("algorithm"))
        assertEquals("6629fae49393a05397450978507c4ef1", digest.parameter("response"))
    }

    @Test
    fun rfc7616_sha256_example() = runTest {
        val provider = DigestAuthProvider("Mufasa", "Circle of Life") { "f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ" }
        assertTrue(provider.isApplicable(rfc7616Challenge))

        val request = request()
        provider.addRequestHeaders(request, rfc7616Challenge)

        val digest = request.digestHeader()
        assertEquals("SHA-256", digest.parameter("algorithm"))
        assertEquals("753927fa0e85d155564e2e272a28d1802ca10daf4496794697cf8db5856cb6c1", digest.parameter("response"))
    }

    @Test
    fun rfc7616_md5_example() = runTest {
        val challenge = parseAuthorizationHeader(
            """Digest realm="http-auth@example.org", qop="auth, auth-int", algorithm=MD5, nonce="7ypf/xlj9XXwfDPEoM4URrv/xwf94BcCAzFZH4GiTo0v", opaque="FQhe/qaU925kfnzjCev0ciny7QMkPqMAFRtzCUYo5tdS""""
        )!!
        val provider = DigestAuthProvider("Mufasa", "Circle of Life") { "f2/wE4q74E6zIJEtWaHKaf5wv/H5QzzpXusqGemxURZJ" }
        assertTrue(provider.isApplicable(challenge))

        val request = request()
        provider.addRequestHeaders(request, challenge)

        assertEquals("8ca523f5e9506fed4657c9700eebdbec", request.digestHeader().parameter("response"))
    }

    @Test
    fun withoutQop_rfc2069() = runTest {
        val challenge = parseAuthorizationHeader("""Digest realm="testrealm@host.com", nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093"""")!!
        val provider = DigestAuthProvider("Mufasa", "Circle Of Life") { "0a4f113b" }
        assertTrue(provider.isApplicable(challenge))

        val request = request()
        provider.addRequestHeaders(request, challenge)

        val digest = request.digestHeader()
        assertNull(digest.parameter("qop"))
        assertNull(digest.parameter("nc"))
        assertNull(digest.parameter("cnonce"))
        assertEquals("670fd8c2df070c60b045671b8b24ff02", digest.parameter("response"))
    }

    @Test
    fun md5Sess() = runTest {
        val challenge = parseAuthorizationHeader(
            """Digest realm="testrealm@host.com", qop="auth", algorithm=MD5-sess, nonce="dcd98b7102dd2f0e8b11d0f600bfb0c093""""
        )!!
        val provider = DigestAuthProvider("Mufasa", "Circle Of Life") { "0a4f113b" }
        assertTrue(provider.isApplicable(challenge))

        val request = request()
        provider.addRequestHeaders(request, challenge)

        val digest = request.digestHeader()
        assertEquals("MD5-sess", digest.parameter("algorithm"))
        assertEquals("8e3825c57e897f5a0dec6c2d4e5059d0", digest.parameter("response"))
    }

    @Test
    fun nonceCountIncrementsAndResetsWithNewNonce() = runTest {
        val provider = DigestAuthProvider("Mufasa", "Circle Of Life") { "0a4f113b" }
        assertTrue(provider.isApplicable(rfc2617Challenge))

        val first = request(); provider.addRequestHeaders(first, rfc2617Challenge)
        val second = request(); provider.addRequestHeaders(second, null)
        assertEquals("00000001", first.digestHeader().parameter("nc"))
        assertEquals("00000002", second.digestHeader().parameter("nc"))

        val newChallenge = parseAuthorizationHeader("""Digest realm="testrealm@host.com", qop="auth", nonce="new-nonce"""")!!
        assertTrue(provider.isApplicable(newChallenge))
        val third = request(); provider.addRequestHeaders(third, newChallenge)
        assertEquals("new-nonce", third.digestHeader().parameter("nonce"))
        assertEquals("00000001", third.digestHeader().parameter("nc"))
    }

    @Test
    fun sameNonceInAnotherChallengeKeepsCounting() = runTest {
        val provider = DigestAuthProvider("Mufasa", "Circle Of Life") { "0a4f113b" }
        assertTrue(provider.isApplicable(rfc2617Challenge))

        val first = request(); provider.addRequestHeaders(first, rfc2617Challenge)
        val second = request(); provider.addRequestHeaders(second, rfc2617Challenge)
        assertEquals("00000001", first.digestHeader().parameter("nc"))
        assertEquals("00000002", second.digestHeader().parameter("nc"))
    }

    @Test
    fun replacesExistingAuthorizationHeader() = runTest {
        val provider = DigestAuthProvider("Mufasa", "Circle Of Life") { "0a4f113b" }
        assertTrue(provider.isApplicable(rfc2617Challenge))

        val request = request().apply { headers.append(HttpHeaders.Authorization, "Digest stale") }
        provider.addRequestHeaders(request, rfc2617Challenge)

        val values = request.headers.getAll(HttpHeaders.Authorization)!!
        assertEquals(1, values.size)
        assertEquals("00000001", request.digestHeader().parameter("nc"))
    }

    @Test
    fun neverPreemptiveOnItsOwn() = runTest {
        val provider = DigestAuthProvider("user", "password")
        val request = request()
        assertFalse(provider.sendWithoutRequest(request))

        provider.addRequestHeaders(request, null)
        assertNull(request.headers[HttpHeaders.Authorization])

        assertTrue(provider.isApplicable(rfc2617Challenge))
        assertFalse(provider.sendWithoutRequest(request))
        assertFalse(provider.sendWithoutRequest(request().apply { url.host = "other.example.com" }))
    }

    @Test
    fun signsWithoutChallengeAfterChallenge() = runTest {
        val provider = DigestAuthProvider("Mufasa", "Circle Of Life") { "0a4f113b" }
        assertTrue(provider.isApplicable(rfc2617Challenge))
        provider.addRequestHeaders(request(), rfc2617Challenge)

        val request = request()
        provider.addRequestHeaders(request, null)
        assertEquals("dcd98b7102dd2f0e8b11d0f600bfb0c093", request.digestHeader().parameter("nonce"))
        assertEquals("00000002", request.digestHeader().parameter("nc"))
    }

    @Test
    fun retryIsSignedWithItsOwnChallenge() = runTest {
        val provider = DigestAuthProvider("Mufasa", "Circle Of Life") { "0a4f113b" }
        val otherChallenge = parseAuthorizationHeader("""Digest realm="other@host.com", qop="auth", nonce="other-nonce"""")!!

        assertTrue(provider.isApplicable(rfc2617Challenge))
        assertTrue(provider.isApplicable(otherChallenge))

        val first = request(); provider.addRequestHeaders(first, rfc2617Challenge)
        assertEquals("dcd98b7102dd2f0e8b11d0f600bfb0c093", first.digestHeader().parameter("nonce"))
        assertEquals("testrealm@host.com", first.digestHeader().parameter("realm"))
        assertEquals("6629fae49393a05397450978507c4ef1", first.digestHeader().parameter("response"))

        val second = request(); provider.addRequestHeaders(second, otherChallenge)
        assertEquals("other-nonce", second.digestHeader().parameter("nonce"))
        assertEquals("other@host.com", second.digestHeader().parameter("realm"))
    }

    @Test
    fun emptyPathIsSignedAsSlash() = runTest {
        val provider = DigestAuthProvider("user", "password") { "0a4f113b" }
        assertTrue(provider.isApplicable(rfc2617Challenge))

        val request = request(path = "")
        provider.addRequestHeaders(request, rfc2617Challenge)

        assertEquals("/", request.digestHeader().parameter("uri"))
        assertEquals("", request.url.encodedPath)
    }

    @Test
    fun uriIncludesQuery() = runTest {
        val provider = DigestAuthProvider("user", "password") { "0a4f113b" }
        assertTrue(provider.isApplicable(rfc2617Challenge))

        val request = request(path = "/dir/").apply { url.parameters.append("a", "b") }
        provider.addRequestHeaders(request, rfc2617Challenge)

        assertEquals("/dir/?a=b", request.digestHeader().parameter("uri"))
    }

    @Test
    fun quotesInCredentialsAreEscaped() = runTest {
        val provider = DigestAuthProvider("us\"er", "password") { "0a4f113b" }
        assertTrue(provider.isApplicable(rfc2617Challenge))

        val request = request()
        provider.addRequestHeaders(request, rfc2617Challenge)

        assertTrue(request.headers[HttpHeaders.Authorization]!!.contains("""username="us\"er""""))
    }

    @Test
    fun notApplicable() {
        val provider = DigestAuthProvider("user", "password")
        assertFalse(provider.isApplicable(parseAuthorizationHeader("""Basic realm="realm"""")!!))
        assertFalse(provider.isApplicable(parseAuthorizationHeader("""Digest nonce="abc"""")!!))
        assertFalse(provider.isApplicable(parseAuthorizationHeader("""Digest realm="realm"""")!!))
        assertFalse(provider.isApplicable(parseAuthorizationHeader("""Digest realm="realm", nonce="abc", algorithm=SHA-512-256""")!!))
        assertFalse(provider.isApplicable(parseAuthorizationHeader("""Digest realm="realm", nonce="abc", qop="auth-int"""")!!))
        assertTrue(provider.isApplicable(parseAuthorizationHeader("""Digest realm="realm", nonce="abc", qop="auth-int,auth", algorithm=sha-256""")!!))
    }

}
