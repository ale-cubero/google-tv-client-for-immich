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

import com.immichtv.data.prefs.SettingsRepository
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rewrites the placeholder base URL Retrofit is built with to the user-supplied server host,
 * preserving the relative API path (and any base sub-path, e.g. a reverse-proxied `/immich`).
 */
@Singleton
class HostSelectionInterceptor @Inject constructor(
    private val settings: SettingsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val base = settings.snapshot.baseUrl.toHttpUrlOrNull()
            ?: return chain.proceed(request) // not configured yet — let it fail loudly

        val relativePath = request.url.encodedPath.trimStart('/')
        val newUrl = base.newBuilder()
            .addPathSegments(relativePath)
            .encodedQuery(request.url.encodedQuery)
            .build()

        return chain.proceed(request.newBuilder().url(newUrl).build())
    }
}

/**
 * Adds the `x-api-key` header — but ONLY when the request's origin (scheme + host + port) matches
 * the configured Immich server. Registered as a *network* interceptor so it runs per redirect hop:
 * OkHttp does not strip custom headers on cross-host redirects, so an application interceptor would
 * forward the bearer token to any host the server redirects to (e.g. a CDN or a malicious location).
 * Scoping to the configured origin prevents that credential leak.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val settings: SettingsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val apiKey = settings.snapshot.apiKey
        val base = settings.snapshot.baseUrl.toHttpUrlOrNull()
        if (apiKey.isBlank() || base == null || !request.url.isSameOrigin(base)) {
            return chain.proceed(request)
        }
        return chain.proceed(request.newBuilder().header("x-api-key", apiKey).build())
    }

    private fun HttpUrl.isSameOrigin(other: HttpUrl): Boolean =
        scheme == other.scheme && host.equals(other.host, ignoreCase = true) && port == other.port
}
