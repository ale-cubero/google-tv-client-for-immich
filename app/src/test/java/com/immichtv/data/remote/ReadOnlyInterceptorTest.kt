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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proves the read-only guarantee: only GET/HEAD and the two allowlisted non-mutating POSTs are
 * permitted; every write verb (and any non-allowlisted POST) is rejected.
 */
class ReadOnlyInterceptorTest {

    @Test
    fun `reads are allowed`() {
        assertTrue(ReadOnlyInterceptor.isReadOnly("GET", "/api/albums"))
        assertTrue(ReadOnlyInterceptor.isReadOnly("GET", "/api/assets/123/thumbnail"))
        assertTrue(ReadOnlyInterceptor.isReadOnly("HEAD", "/api/assets/123/video/playback"))
        assertTrue(ReadOnlyInterceptor.isReadOnly("get", "/api/timeline/buckets")) // case-insensitive
    }

    @Test
    fun `allowlisted read POSTs are allowed`() {
        assertTrue(ReadOnlyInterceptor.isReadOnly("POST", "/api/auth/validateToken"))
        assertTrue(ReadOnlyInterceptor.isReadOnly("POST", "/api/search/metadata"))
        assertTrue(ReadOnlyInterceptor.isReadOnly("POST", "/api/search/smart"))
        // still allowed behind a reverse-proxy sub-path
        assertTrue(ReadOnlyInterceptor.isReadOnly("POST", "/immich/api/search/metadata"))
    }

    @Test
    fun `write verbs are blocked`() {
        assertFalse(ReadOnlyInterceptor.isReadOnly("DELETE", "/api/assets/123"))
        assertFalse(ReadOnlyInterceptor.isReadOnly("PUT", "/api/assets/123"))
        assertFalse(ReadOnlyInterceptor.isReadOnly("PATCH", "/api/albums/123"))
    }

    @Test
    fun `non-allowlisted POSTs are blocked`() {
        assertFalse(ReadOnlyInterceptor.isReadOnly("POST", "/api/assets")) // upload
        assertFalse(ReadOnlyInterceptor.isReadOnly("POST", "/api/albums")) // create album
        assertFalse(ReadOnlyInterceptor.isReadOnly("POST", "/api/assets/123/favorite"))
    }

    @Test
    fun `crafted paths embedding an allowlisted suffix are blocked`() {
        // a write route that merely ends with an allowlisted path must NOT pass
        assertFalse(ReadOnlyInterceptor.isReadOnly("POST", "/api/assets/api/search/metadata"))
        assertFalse(ReadOnlyInterceptor.isReadOnly("POST", "/api/x/api/auth/validateToken"))
        // and a path with no /api/ segment at all
        assertFalse(ReadOnlyInterceptor.isReadOnly("POST", "/search/metadata"))
    }
}
