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
import at.bitfire.dav4jvm.XmlUtils.propertyName
import at.bitfire.dav4jvm.property.webdav.Depth
import at.bitfire.dav4jvm.property.webdav.WebDAV
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader

/**
 * Represents a [NS_WEBDAV_PUSH]`:property-update` property.
 *
 * Experimental! See https://github.com/bitfireAT/webdav-push/
 */
data class PropertyUpdate(
    val depth: Depth? = null
): Property {

    object Factory: PropertyFactory {

        override fun getName() = WebDAVPush.PropertyUpdate

        override fun create(parser: XmlReader): PropertyUpdate {
            var propertyUpdate = PropertyUpdate()

            val depth = parser.depth
            var eventType = parser.eventType
            while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && parser.depth == depth)) {
                if (eventType == EventType.START_ELEMENT && parser.depth == depth + 1) {
                    when (parser.propertyName()) {
                        WebDAV.Depth -> propertyUpdate = propertyUpdate.copy(
                            depth = Depth.Factory.create(parser)
                        )
                    }
                }
                eventType = parser.next()
            }
            return propertyUpdate
        }

    }

}
