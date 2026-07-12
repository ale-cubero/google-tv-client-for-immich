/*
 * TV client for Immich — a read-only Android TV client for a self-hosted Immich server.
 * Copyright (C) 2026 Alejandro Cubero
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.immichtv.ui.common

import com.immichtv.ui.i18n.Strings
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Maps a thrown error into a short, actionable, localized message. An offline or unreachable server
 * reads as a clear "can't reach your server" rather than a raw exception string.
 */
fun Throwable.toUserMessage(strings: Strings): String = when (this) {
    is UnknownHostException -> strings.errUnknownHost
    is SocketTimeoutException -> strings.errTimeout
    is ConnectException -> strings.errUnreachable
    is SSLException -> strings.errTls
    is HttpException -> when (code()) {
        401, 403 -> strings.errApiKey
        in 500..599 -> strings.errServer(code())
        else -> strings.errServerGeneric(code())
    }
    // Any other I/O failure (no route to host, connection reset, etc.) is almost always reachability.
    is IOException -> strings.errUnreachable
    else -> message?.takeIf { it.isNotBlank() } ?: strings.errGeneric
}
