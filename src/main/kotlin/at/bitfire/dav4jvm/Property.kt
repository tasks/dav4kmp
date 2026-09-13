/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.dav4jvm

import io.ktor.util.logging.KtorSimpleLogger
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlReader
import java.io.Serializable

/**
 * Represents a WebDAV property.
 *
 * Every [Property] must define a static field (use `@JvmStatic`) called `NAME` of type [Property.Name],
 * which will be accessed by reflection.
 *
 * Every [Property] should be a data class in order to be able to compare it against others, and convert to a useful
 * string for debugging.
 */
interface Property {

    data class Name(
        val namespace: String,
        val name: String
    ): Serializable {

        override fun toString() = "$namespace:$name"

    }

    companion object {

        fun parse(parser: XmlReader): List<Property> {
            val logger = KtorSimpleLogger("at.bitfire.dav4jvm.Property")

            // <!ELEMENT prop ANY >
            val depth = parser.depth
            val properties = ArrayList<Property>()

            var eventType = parser.eventType
            while (eventType != EventType.END_DOCUMENT && !(eventType == EventType.END_ELEMENT && parser.depth == depth)) {
                if (eventType == EventType.START_ELEMENT && parser.depth == depth + 1) {
                    val name = Name(parser.namespaceURI, parser.localName)

                    try {
                        val property = PropertyRegistry.create(name, parser)

                        if (property != null) {
                            properties.add(property)
                        } else
                            logger.debug("Ignoring unknown property $name")
                    } catch (e: Exception) {     // catching generic exception here to avoid a dependency on a specific HTTP library's exception type
                        logger.warn("Ignoring invalid property", e)
                    }
                }

                eventType = parser.next()
            }

            return properties
        }

    }

}
