package com.immichtv.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hard guarantee that the app is READ-ONLY (project requirement): it must never be able to change
 * data on the Immich server. This interceptor blocks every request whose HTTP method could mutate
 * state. Only `GET`/`HEAD` are allowed, plus a tiny allowlist of Immich endpoints that use `POST`
 * but are non-mutating reads (token validation and metadata search).
 *
 * Registered as the FIRST application interceptor, so it vetoes any request — from the repository,
 * Coil, or ExoPlayer, now or in the future — before it reaches the network. A blocked call fails
 * loudly rather than silently writing.
 */
@Singleton
class ReadOnlyInterceptor @Inject constructor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!isReadOnly(request.method, request.url.encodedPath)) {
            throw IOException("Read-only app: blocked ${request.method} ${request.url.encodedPath}")
        }
        return chain.proceed(request)
    }

    internal companion object {
        // POST endpoints that are reads, not writes.
        private val READ_ONLY_POST_PATHS = listOf(
            "/api/auth/validateToken",
            "/api/search/metadata",
        )

        /** True only for requests that cannot mutate server state. */
        fun isReadOnly(method: String, path: String): Boolean = when (method.uppercase()) {
            "GET", "HEAD" -> true
            "POST" -> READ_ONLY_POST_PATHS.any { path.endsWith(it) }
            else -> false // PUT, PATCH, DELETE, and any other write verb
        }
    }
}
