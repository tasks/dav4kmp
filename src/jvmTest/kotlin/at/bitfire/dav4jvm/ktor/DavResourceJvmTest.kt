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

import at.bitfire.dav4jvm.ktor.exception.DavException
import at.bitfire.dav4jvm.property.webdav.WebDAV
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.close
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertIs

class DavResourceJvmTest {

    @Test
    fun `propfind reports a broken response body as DavException`() = runTest {
        val body = ByteChannel()
        launch(Dispatchers.Default) {
            body.writeFully("<multistatus xmlns='DAV:'><response><href>/dav</href>".encodeToByteArray())
            body.flush()
            body.close(IOException("connection reset by peer"))
        }

        val engine = MockEngine {
            respond(
                body, HttpStatusCode.MultiStatus,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Xml.toString())
            )
        }
        val dav = DavResource(HttpClient(engine), Url("https://example.com/dav/"))

        val thrown = assertFails {
            dav.propfind(0, WebDAV.ResourceType).toList()
        }
        assertIs<DavException>(thrown)
    }

}
