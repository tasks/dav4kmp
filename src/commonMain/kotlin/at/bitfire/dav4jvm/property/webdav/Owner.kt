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
import at.bitfire.dav4jvm.property.common.HrefListProperty
import at.bitfire.dav4jvm.readTextProperty
import nl.adaptivity.xmlutil.XmlReader

data class Owner(
    val href: String?
): Property {

    object Factory: HrefListProperty.Factory() {

        override fun getName() = WebDAV.Owner

        override fun create(parser: XmlReader): Owner =
            Owner(parser.readTextProperty(WebDAV.Href))

    }

}
