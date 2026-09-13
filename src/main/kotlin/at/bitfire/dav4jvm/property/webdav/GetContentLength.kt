/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.dav4jvm.property.webdav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.readLong
import nl.adaptivity.xmlutil.XmlReader

data class GetContentLength(
    val contentLength: Long?
) : Property {

    object Factory: PropertyFactory {

        override fun getName() = WebDAV.GetContentLength

        override fun create(parser: XmlReader) =
            GetContentLength(parser.readLong())

    }

}
