/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */

package at.bitfire.dav4jvm.ktor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow

internal actual fun <T> collectorFlow(block: suspend (FlowCollector<T>) -> Unit): Flow<T> = flow {
    var failure: Throwable? = null
    channelFlow {
        try {
            block(FlowCollector { send(it) })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            failure = e
        }
    }.collect { emit(it) }
    failure?.let { throw it }
}
