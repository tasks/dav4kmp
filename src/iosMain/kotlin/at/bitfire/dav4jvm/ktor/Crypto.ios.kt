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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_MD5
import platform.CoreCrypto.CC_MD5_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.posix.arc4random_buf

@OptIn(ExperimentalForeignApi::class)
internal actual fun md5(data: ByteArray): ByteArray {
    val digest = UByteArray(CC_MD5_DIGEST_LENGTH)
    val input = if (data.isEmpty()) ByteArray(1) else data
    input.usePinned { pinned ->
        digest.usePinned { out ->
            @Suppress("DEPRECATION")
            CC_MD5(pinned.addressOf(0), data.size.convert(), out.addressOf(0))
        }
    }
    return digest.asByteArray()
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun sha256(data: ByteArray): ByteArray {
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
    val input = if (data.isEmpty()) ByteArray(1) else data
    input.usePinned { pinned ->
        digest.usePinned { out ->
            CC_SHA256(pinned.addressOf(0), data.size.convert(), out.addressOf(0))
        }
    }
    return digest.asByteArray()
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun secureRandomBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    if (size > 0)
        bytes.usePinned { pinned ->
            arc4random_buf(pinned.addressOf(0), size.convert())
        }
    return bytes
}
