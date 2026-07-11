package com.immichtv.ui.common

import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Maps a thrown error into a short, actionable message for the user. The goal is that an offline or
 * unreachable server reads as a clear "can't reach your server" rather than a raw exception string
 * like "Failed to connect to /192.168.1.10:2283" or "Unable to resolve host".
 */
fun Throwable.toUserMessage(): String = when (this) {
    is UnknownHostException ->
        "Can't find your Immich server.\nCheck the server address and your network connection."
    is SocketTimeoutException ->
        "Your Immich server isn't responding.\nIt may be offline or unreachable on your network."
    is ConnectException ->
        "Can't reach your Immich server.\nMake sure it's online and on the same network."
    is SSLException ->
        "Secure connection failed.\nIf your server uses a self-signed certificate, enable " +
            "\"Allow insecure connection\" in Settings → Change server."
    is HttpException -> when (code()) {
        401, 403 -> "Your API key was rejected.\nUpdate it in Settings → Change server."
        in 500..599 -> "Your Immich server reported an error (HTTP ${code()}).\nTry again shortly."
        else -> "Your Immich server returned an error (HTTP ${code()})."
    }
    // Any other I/O failure (no route to host, connection reset, etc.) is almost always reachability.
    is IOException ->
        "Can't reach your Immich server.\nMake sure it's online and on the same network."
    else -> message?.takeIf { it.isNotBlank() } ?: "Something went wrong. Please try again."
}
