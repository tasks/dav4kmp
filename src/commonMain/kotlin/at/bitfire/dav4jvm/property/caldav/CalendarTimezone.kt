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
import at.bitfire.dav4jvm.readText
import nl.adaptivity.xmlutil.XmlReader

data class CalendarTimezone(
    val vTimeZone: String?
): Property {

    object Factory: PropertyFactory {

        override fun getName() = CalDAV.CalendarTimezone

        override fun create(parser: XmlReader) =
            // <!ELEMENT calendar-timezone (#PCDATA)>
            CalendarTimezone(parser.readText())

    }
}
