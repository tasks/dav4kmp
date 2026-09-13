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
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader

/**
 * Represents a [NS_WEBDAV_PUSH]`:content-update` property.
 *
 * Experimental! See https://github.com/bitfireAT/webdav-push/
 */
data class SupportedTriggers(
    val contentUpdate: ContentUpdate? = null,
    val propertyUpdate: PropertyUpdate? = null
): Property {

    object Factory: PropertyFactory {

        override fun getName() = WebDAVPush.SupportedTriggers

        override fun create(parser: XmlReader): SupportedTriggers {
            var supportedTriggers = SupportedTriggers()

            val depth = parser.depth
            var eventType = parser.eventType
            while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && parser.depth == depth)) {
                if (eventType == EventType.START_ELEMENT && parser.depth == depth + 1) {
                    when (parser.propertyName()) {
                        WebDAVPush.ContentUpdate -> supportedTriggers = supportedTriggers.copy(
                            contentUpdate = ContentUpdate.Factory.create(parser)
                        )
                        WebDAVPush.PropertyUpdate -> supportedTriggers = supportedTriggers.copy(
                            propertyUpdate = PropertyUpdate.Factory.create(parser)
                        )
                    }
                }
                eventType = parser.next()
            }

            return supportedTriggers
        }

    }

}
