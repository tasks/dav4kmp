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

internal actual fun platformLog(loggerName: String, level: Level, msg: String, thrown: Throwable?) {
    val logger = KtorSimpleLogger(loggerName)
    when {
        level.intValue >= Level.SEVERE.intValue ->
            if (thrown != null) logger.error(msg, thrown) else logger.error(msg)
        level.intValue >= Level.WARNING.intValue ->
            if (thrown != null) logger.warn(msg, thrown) else logger.warn(msg)
        level.intValue >= Level.INFO.intValue ->
            if (thrown != null) logger.info(msg, thrown) else logger.info(msg)
        level.intValue >= Level.FINE.intValue ->
            if (thrown != null) logger.debug(msg, thrown) else logger.debug(msg)
        else ->
            if (thrown != null) logger.trace(msg, thrown) else logger.trace(msg)
    }
}
