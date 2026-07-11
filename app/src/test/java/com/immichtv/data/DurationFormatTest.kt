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
package com.immichtv.data

import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    @Test fun millisecondsNumber() {
        // Immich v3 returns duration as an integer of milliseconds in search + timeline payloads.
        assertEquals("0:06", formatDuration(JsonPrimitive(6144)))
        assertEquals("0:20", formatDuration(JsonPrimitive(20181)))
        assertEquals("2:34", formatDuration(JsonPrimitive(154_000)))
        assertEquals("1:02:34", formatDuration(JsonPrimitive(3_754_000)))
    }

    @Test fun colonSeparatedString() {
        assertEquals("0:07", formatDuration(JsonPrimitive("0:00:07.174")))
        assertEquals("2:34", formatDuration(JsonPrimitive("0:02:34.000")))
        assertEquals("1:02:34", formatDuration(JsonPrimitive("1:02:34.500")))
    }

    @Test fun unknownOrZeroIsEmpty() {
        assertEquals("", formatDuration(null))
        assertEquals("", formatDuration(JsonPrimitive(0)))
        assertEquals("", formatDuration(JsonPrimitive("")))
        assertEquals("", formatDuration(JsonPrimitive("not-a-duration")))
    }
}
