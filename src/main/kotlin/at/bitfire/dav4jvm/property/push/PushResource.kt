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
import at.bitfire.dav4jvm.XmlReader
import io.ktor.http.Url
import io.ktor.http.parseUrl
import org.xmlpull.v1.XmlPullParser

/**
 * Represents a [NS_WEBDAV_PUSH]`:push-resource` property.
 *
 * Experimental! See https://github.com/bitfireAT/webdav-push/
 *
 * @property uri    absolute URL of the push resource; `null` if the element didn't contain one
 *                  (a push resource is always an absolute URL, so relative references and
 *                  opaque URIs are not silently resolved against a default host)
 */
data class PushResource(
    val uri: Url? = null
): Property {

    object Factory: PropertyFactory {

        override fun getName() = WebDAVPush.PushResource

        override fun create(parser: XmlPullParser): PushResource =
            PushResource(
                uri = XmlReader(parser).readText()?.let { parseUrl(it) }
            )

    }

}