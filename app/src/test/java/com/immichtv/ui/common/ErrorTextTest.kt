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

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import com.immichtv.ui.i18n.En

class ErrorTextTest {

    @Test fun unknownHost_pointsAtAddressAndNetwork() {
        assertTrue(UnknownHostException("host").toUserMessage(En).contains("Can't find your Immich server"))
    }

    @Test fun timeout_readsAsNotResponding() {
        assertTrue(SocketTimeoutException().toUserMessage(En).contains("isn't responding"))
    }

    @Test fun connectRefused_readsAsUnreachable() {
        assertTrue(ConnectException("refused").toUserMessage(En).contains("Can't reach your Immich server"))
    }

    @Test fun genericIo_readsAsUnreachable() {
        assertTrue(IOException("connection reset").toUserMessage(En).contains("Can't reach your Immich server"))
    }

    @Test fun tlsFailure_pointsAtCertificateReview() {
        val msg = SSLHandshakeException("bad cert").toUserMessage(En)
        assertTrue(msg.contains("Change server"))
        assertTrue(msg.contains("trust it"))
    }

    @Test fun unauthorized_pointsAtApiKey() {
        assertTrue(httpException(401).toUserMessage(En).contains("API key was rejected"))
    }

    @Test fun serverError_mentionsServerAndCode() {
        val msg = httpException(503).toUserMessage(En)
        assertTrue(msg.contains("503"))
        assertTrue(msg.contains("server reported an error"))
    }

    @Test fun unknown_fallsBackToMessage() {
        assertEquals("boom", IllegalStateException("boom").toUserMessage(En))
    }

    @Test fun unknown_blankMessage_usesGenericFallback() {
        assertTrue(RuntimeException().toUserMessage(En).contains("Something went wrong"))
    }

    private fun httpException(code: Int): HttpException {
        val response = Response.Builder()
            .code(code)
            .message("err")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/").build())
            .build()
        return HttpException(
            retrofit2.Response.error<Any>("".toResponseBody("text/plain".toMediaType()), response),
        )
    }
}
