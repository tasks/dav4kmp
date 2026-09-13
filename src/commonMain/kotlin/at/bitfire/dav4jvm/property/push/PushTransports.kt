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
 * Represents a [NS_WEBDAV_PUSH]`:push-transports` property.
 *
 * Experimental! See https://github.com/bitfireAT/webdav-push/
 */
class PushTransports private constructor(
    val transports: Set<PushTransport>
): Property {

    fun hasWebPush() = transports.any { it is WebPush }


    object Factory: PropertyFactory {

        override fun getName() = WebDAVPush.Transports

        override fun create(parser: XmlReader): PushTransports {
            val transports = mutableListOf<PushTransport>()
            val depth = parser.depth
            var eventType = parser.eventType
            while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && parser.depth == depth)) {
                if (eventType == EventType.START_ELEMENT && parser.depth == depth + 1) {
                    when (parser.propertyName()) {
                        WebDAVPush.WebPush ->
                            transports += WebPush.Factory.create(parser)
                    }
                }
                eventType = parser.next()
            }
            return PushTransports(transports.toSet())
        }

    }

}