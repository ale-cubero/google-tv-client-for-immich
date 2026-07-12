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

import com.immichtv.domain.Asset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Timeline context for a viewer opened from the month grid: the full ordered bucket list and the
 * index of the bucket the initial assets came from, so the viewer can keep paging into adjacent
 * months instead of stopping at the month edge.
 */
data class TimelineNav(
    val buckets: List<String>, // timeBucket keys, newest first (grid order)
    val bucketIndex: Int,      // which bucket the initial assets belong to
)

/**
 * Hands an ordered list of assets to the viewer, decoupling it from where they came from
 * (timeline bucket, album, memory, videos rail, …). The opening screen sets the list + start
 * index, then navigates to the viewer, which reads it back.
 *
 * In-memory by design: on process death the queue is empty and the viewer shows an error — an
 * acceptable trade-off for a TV app during active use (vs. serializing large lists into nav args).
 */
@Singleton
class ViewerQueue @Inject constructor() {
    var assets: List<Asset> = emptyList()
        private set
    var startIndex: Int = 0
        private set
    var timeline: TimelineNav? = null
        private set
    // Opened from the "On this day" memories rail — enables the viewer's "Go to this day" action.
    var fromMemories: Boolean = false
        private set

    fun set(
        assets: List<Asset>,
        startIndex: Int,
        timeline: TimelineNav? = null,
        fromMemories: Boolean = false,
    ) {
        this.assets = assets
        this.startIndex = startIndex.coerceIn(0, (assets.size - 1).coerceAtLeast(0))
        this.timeline = timeline
        this.fromMemories = fromMemories
    }
}
