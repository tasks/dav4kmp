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

import at.bitfire.dav4jvm.Error
import at.bitfire.dav4jvm.Property
import io.ktor.http.HttpStatusCode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HttpExceptionSerializationTest {

    @Test
    fun `is Java-serializable`() {
        val ex = HttpException(
            status = HttpStatusCode.InternalServerError,
            requestExcerpt = "Request Body",
            responseExcerpt = "Response Body",
            errors = listOf(
                Error(Property.Name("Serialized", "Name"))
            )
        )

        // serialize (Java-style as in Serializable interface, not Kotlin serialization)
        val blob = ByteArrayOutputStream().use { baos ->
            ObjectOutputStream(baos).use { oos ->
                oos.writeObject(ex)
            }
            baos.toByteArray()
        }

        // deserialize
        ByteArrayInputStream(blob).use { bais ->
            ObjectInputStream(bais).use { ois ->
                val actual = ois.readObject() as HttpException
                assertEquals(ex.message, actual.message)
                assertEquals(ex.statusCode, actual.statusCode)
                assertEquals(ex.requestExcerpt, actual.requestExcerpt)
                assertEquals(ex.responseExcerpt, actual.responseExcerpt)
                assertEquals(ex.errors, actual.errors)
                assertNull(actual.cause)
            }
        }
    }

}
