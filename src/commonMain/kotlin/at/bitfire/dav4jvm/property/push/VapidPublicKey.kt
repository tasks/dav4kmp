/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.dav4jvm.property.push

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.readText
import nl.adaptivity.xmlutil.XmlReader

/**
 * Represents a [NS_WEBDAV_PUSH]`:vapid-public-key` property.
 *
 * Experimental! See https://github.com/bitfireAT/webdav-push/
 */
data class VapidPublicKey(
    val type: String? = null,
    val key: String? = null
): Property {

    object Factory : PropertyFactory {

        override fun getName() = WebDAVPush.VapidPublicKey

        override fun create(parser: XmlReader): VapidPublicKey {
            return VapidPublicKey(
                type = parser.getAttributeValue(null, "type"),
                key = parser.readText()
            )
        }

    }

}