/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.dav4jvm.property.caldav

import at.bitfire.dav4jvm.Property
import at.bitfire.dav4jvm.PropertyFactory
import at.bitfire.dav4jvm.XmlReader
import org.xmlpull.v1.XmlPullParser
import java.util.logging.Level
import java.util.logging.Logger

data class CalendarColor(
    val color: Int?
): Property {

    companion object {

        private val PATTERN = Regex("#?([0-9a-fA-F]{6})([0-9a-fA-F]{2})?")

        /**
         * Converts a WebDAV color from one of these formats:
         *   #RRGGBB     (alpha = 0xFF)
         *   RRGGBB      (alpha = 0xFF)
         *   #RRGGBBAA
         *   RRGGBBAA
         *  to an [Int] with alpha.
         */
        @Throws(IllegalArgumentException::class)
        fun parseARGBColor(davColor: String): Int {
            val m = PATTERN.find(davColor)
            if (m != null) {
                val color_rgb = m.groupValues[1].toInt(16)
                val color_alpha = m.groups[2]?.value?.let { it.toInt(16) and 0xFF } ?: 0xFF
                return (color_alpha shl 24) or color_rgb
            } else
                throw IllegalArgumentException("Couldn't parse color value: $davColor")
        }
    }


    object Factory: PropertyFactory {

        override fun getName() = CalDAV.CalendarColor

        override fun create(parser: XmlPullParser): CalendarColor {
            XmlReader(parser).readText()?.let {
                try {
                    return CalendarColor(parseARGBColor(it))
                } catch (e: IllegalArgumentException) {
                    val logger = Logger.getLogger(javaClass.name)
                    logger.log(Level.WARNING, "Couldn't parse color, ignoring", e)
                }
            }
            return CalendarColor(null)
        }

    }

}