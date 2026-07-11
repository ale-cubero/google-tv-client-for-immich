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
        // POST endpoints that are reads, not writes: token validation, metadata search, and
        // smart/CLIP search (semantic "context" search + object recognition). All are query-only.
        private val READ_ONLY_POST_PATHS = listOf(
            "/api/auth/validateToken",
            "/api/search/metadata",
            "/api/search/smart",
        )

        /** True only for requests that cannot mutate server state. */
        fun isReadOnly(method: String, path: String): Boolean = when (method.uppercase()) {
            "GET", "HEAD" -> true
            // Anchored match: the allowlisted path must be the ENTIRE path after an optional
            // reverse-proxy prefix (i.e. from the FIRST "/api/" onward) — a plain endsWith could
            // be fooled by a crafted path embedding the suffix after a write route.
            "POST" -> {
                val apiPath = path.substring(path.indexOf("/api/").coerceAtLeast(0))
                READ_ONLY_POST_PATHS.any { apiPath == it }
            }
            else -> false // PUT, PATCH, DELETE, and any other write verb
        }
    }
}
