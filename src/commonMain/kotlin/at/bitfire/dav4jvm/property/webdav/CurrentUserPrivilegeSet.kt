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
import at.bitfire.dav4jvm.XmlUtils.propertyName
import at.bitfire.dav4jvm.processTag
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader

data class CurrentUserPrivilegeSet(
    // not all privileges from RFC 3744 are implemented by now
    // feel free to add more if you need them for your project
    val mayRead: Boolean = false,
    val mayWriteProperties: Boolean = false,
    val mayWriteContent: Boolean = false,
    val mayBind: Boolean = false,
    val mayUnbind: Boolean = false
): Property {

    object Factory: PropertyFactory {

        override fun getName() = WebDAV.CurrentUserPrivilegeSet

        override fun create(parser: XmlReader): CurrentUserPrivilegeSet {
            // <!ELEMENT current-user-privilege-set (privilege*)>
            // <!ELEMENT privilege ANY>
            var privs = CurrentUserPrivilegeSet()

            parser.processTag(WebDAV.Privilege) {
                val depth = parser.depth
                var eventType = parser.eventType
                while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && parser.depth == depth)) {
                    if (eventType == EventType.START_ELEMENT && parser.depth == depth + 1)
                        when (parser.propertyName()) {
                            WebDAV.Read ->
                                privs = privs.copy(mayRead = true)
                            WebDAV.Write -> {
                                privs = privs.copy(
                                    mayBind = true,
                                    mayUnbind = true,
                                    mayWriteProperties = true,
                                    mayWriteContent = true
                                )
                            }
                            WebDAV.WriteProperties ->
                                privs = privs.copy(mayWriteProperties = true)
                            WebDAV.WriteContent ->
                                privs = privs.copy(mayWriteContent = true)
                            WebDAV.Bind ->
                                privs = privs.copy(mayBind = true)
                            WebDAV.Unbind ->
                                privs = privs.copy(mayUnbind = true)
                            WebDAV.All -> {
                                privs = privs.copy(
                                    mayRead = true,
                                    mayBind = true,
                                    mayUnbind = true,
                                    mayWriteProperties = true,
                                    mayWriteContent = true
                                )
                            }
                        }
                    eventType = parser.next()
                }
            }

            return privs
        }

    }

}
