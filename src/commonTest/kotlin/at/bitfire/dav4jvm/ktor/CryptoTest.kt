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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CryptoTest {

    @Test
    fun md5_empty() {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", md5(ByteArray(0)).toHexString())
    }

    @Test
    fun md5_abc() {
        assertEquals("900150983cd24fb0d6963f7d28e17f72", md5("abc".encodeToByteArray()).toHexString())
    }

    @Test
    fun md5_multipleBlocks() {
        val input = "12345678901234567890123456789012345678901234567890123456789012345678901234567890"
        assertEquals("57edf4a22be3c955ac49da2e2107b67a", md5(input.encodeToByteArray()).toHexString())
    }

    @Test
    fun sha256_empty() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256(ByteArray(0)).toHexString())
    }

    @Test
    fun sha256_multipleBlocks() {
        val input = "abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"
        assertEquals("248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1", sha256(input.encodeToByteArray()).toHexString())
    }

    @Test
    fun sha256_abc() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256("abc".encodeToByteArray()).toHexString())
    }

    @Test
    fun secureRandomBytes_sizeAndVariance() {
        val a = secureRandomBytes(16)
        val b = secureRandomBytes(16)
        assertEquals(16, a.size)
        assertNotEquals(a.toList(), b.toList())
        assertEquals(0, secureRandomBytes(0).size)
    }

}
