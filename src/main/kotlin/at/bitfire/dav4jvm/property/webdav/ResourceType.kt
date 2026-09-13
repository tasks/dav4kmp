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
import at.bitfire.dav4jvm.property.caldav.CalDAV
import at.bitfire.dav4jvm.property.carddav.CardDAV
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader

class ResourceType(
    val types: Set<Property.Name> = emptySet()
): Property {

    companion object {


    }


    object Factory: PropertyFactory {

        override fun getName() = WebDAV.ResourceType

        override fun create(parser: XmlReader): ResourceType {
            val types = mutableSetOf<Property.Name>()

            val depth = parser.depth
            var eventType = parser.eventType
            while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && parser.depth == depth)) {
                if (eventType == EventType.START_ELEMENT && parser.depth == depth + 1) {
                    // use static objects to allow types.contains()
                    var typeName = Property.Name(parser.namespaceURI, parser.localName)
                    when (typeName) {       // if equals(), replace by our instance
                        WebDAV.Collection -> typeName = WebDAV.Collection
                        WebDAV.Principal -> typeName = WebDAV.Principal
                        CardDAV.Addressbook -> typeName = CardDAV.Addressbook
                        CalDAV.Calendar -> typeName = CalDAV.Calendar
                        CalDAV.CalendarProxyRead -> typeName = CalDAV.CalendarProxyRead
                        CalDAV.CalendarProxyWrite -> typeName = CalDAV.CalendarProxyWrite
                        CalDAV.Subscribed -> typeName = CalDAV.Subscribed
                    }
                    types.add(typeName)
                }
                eventType = parser.next()
            }

            return ResourceType(types)
        }

    }

}
