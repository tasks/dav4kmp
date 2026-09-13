/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.dav4jvm

fun String.toLatin1() = ByteArray(length) { this[it].code.toByte() }

fun String.toUtf16(bigEndian: Boolean): ByteArray {
    val result = ByteArray(length * 2)
    for (i in indices) {
        val code = this[i].code
        val hi = (code shr 8).toByte()
        val lo = (code and 0xFF).toByte()
        if (bigEndian) {
            result[2*i] = hi
            result[2*i + 1] = lo
        } else {
            result[2*i] = lo
            result[2*i + 1] = hi
        }
    }
    return result
}
