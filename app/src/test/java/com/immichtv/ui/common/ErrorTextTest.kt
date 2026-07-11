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

class ErrorTextTest {

    @Test fun unknownHost_pointsAtAddressAndNetwork() {
        assertTrue(UnknownHostException("host").toUserMessage().contains("Can't find your Immich server"))
    }

    @Test fun timeout_readsAsNotResponding() {
        assertTrue(SocketTimeoutException().toUserMessage().contains("isn't responding"))
    }

    @Test fun connectRefused_readsAsUnreachable() {
        assertTrue(ConnectException("refused").toUserMessage().contains("Can't reach your Immich server"))
    }

    @Test fun genericIo_readsAsUnreachable() {
        assertTrue(IOException("connection reset").toUserMessage().contains("Can't reach your Immich server"))
    }

    @Test fun tlsFailure_hintsAtInsecureToggle() {
        val msg = SSLHandshakeException("bad cert").toUserMessage()
        assertTrue(msg.contains("Allow insecure connection"))
    }

    @Test fun unauthorized_pointsAtApiKey() {
        assertTrue(httpException(401).toUserMessage().contains("API key was rejected"))
    }

    @Test fun serverError_mentionsServerAndCode() {
        val msg = httpException(503).toUserMessage()
        assertTrue(msg.contains("503"))
        assertTrue(msg.contains("server reported an error"))
    }

    @Test fun unknown_fallsBackToMessage() {
        assertEquals("boom", IllegalStateException("boom").toUserMessage())
    }

    @Test fun unknown_blankMessage_usesGenericFallback() {
        assertTrue(RuntimeException().toUserMessage().contains("Something went wrong"))
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
