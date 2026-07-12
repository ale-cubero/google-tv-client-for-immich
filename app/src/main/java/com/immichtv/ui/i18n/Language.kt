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
package com.immichtv.ui.i18n

import java.util.Locale

/** UI language, user-selectable and persisted. Drives both string resources and locale-aware formatting. */
enum class Language(val tag: String, val displayName: String) {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    PORTUGUESE("pt", "Português"),
    ITALIAN("it", "Italiano");

    fun toLocale(): Locale = Locale.forLanguageTag(tag)

    companion object {
        fun fromTag(tag: String?): Language = entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}
