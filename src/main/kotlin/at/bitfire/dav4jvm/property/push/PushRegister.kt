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

import at.bitfire.dav4jvm.HttpUtils
import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.XmlUtils.propertyName
import at.bitfire.dav4jvm.readText
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import kotlin.time.Instant

/**
 * Represents a [NS_WEBDAV_PUSH]`:push-register` property.
 *
 * Experimental! See https://github.com/bitfireAT/webdav-push/
 */
data class PushRegister(
    val expires: Instant? = null,
    val subscription: Subscription? = null,
    val trigger: Trigger? = null
): Property {

    object Factory: PropertyFactory {

        override fun getName() = WebDAVPush.PushRegister

        override fun create(parser: XmlReader): PushRegister {
            var register = PushRegister()

            val depth = parser.depth
            var eventType = parser.eventType
            while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && parser.depth == depth)) {
                if (eventType == EventType.START_ELEMENT && parser.depth == depth + 1)
                    when (parser.propertyName()) {
                        WebDAVPush.Expires ->
                            register = register.copy(
                                expires = parser.readText()?.let {
                                    HttpUtils.parseDate(it)
                                }
                            )
                        WebDAVPush.Subscription ->
                            register = register.copy(
                                subscription = Subscription.Factory.create(parser)
                            )
                        WebDAVPush.Trigger ->
                            register = register.copy(
                                trigger = Trigger.Factory.create(parser)
                            )
                    }
                eventType = parser.next()
            }

            return register
        }

    }

}