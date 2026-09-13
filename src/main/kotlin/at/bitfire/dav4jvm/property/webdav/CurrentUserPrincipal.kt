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
import at.bitfire.dav4jvm.processTag
import at.bitfire.dav4jvm.readText
import nl.adaptivity.xmlutil.XmlReader

// see RFC 5397: WebDAV Current Principal Extension

data class CurrentUserPrincipal(
    val href: String?
): Property {

    object Factory: PropertyFactory {

        override fun getName() = WebDAV.CurrentUserPrincipal

        override fun create(parser: XmlReader): CurrentUserPrincipal {
            // <!ELEMENT current-user-principal (unauthenticated | href)>
            var href: String? = null
            parser.processTag(WebDAV.Href) {
                href = readText()
            }
            return CurrentUserPrincipal(href)
        }

    }

}
