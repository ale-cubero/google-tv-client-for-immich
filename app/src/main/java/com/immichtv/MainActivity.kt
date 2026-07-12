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
package com.immichtv

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.immichtv.data.prefs.SettingsRepository
import com.immichtv.ui.collection.CollectionScreen
import com.immichtv.ui.home.HomeScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.immichtv.ui.i18n.Language
import com.immichtv.ui.i18n.LocalStrings
import com.immichtv.ui.i18n.stringsFor
import com.immichtv.ui.search.SearchScreen
import com.immichtv.ui.setup.SetupScreen
import com.immichtv.ui.theme.ImmichTvTheme
import com.immichtv.ui.theme.ThemeMode
import com.immichtv.ui.timeline.TimelineScreen
import com.immichtv.ui.viewer.ViewerScreen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val TIMELINE = "timeline"
    const val SEARCH = "search"
    const val VIEWER = "viewer"
    const val COLLECTION = "collection/{source}/{id}?title={title}"
    // id is encoded too: album/person ids are UUIDs, but a place id is a city name (may have spaces).
    fun collection(source: String, id: String, title: String) =
        "collection/${Uri.encode(source)}/${Uri.encode(id)}?title=${Uri.encode(title)}"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRoot()
        }
    }
}

/** Decides the first screen based on whether the server is already configured, and the theme. */
@HiltViewModel
class AppViewModel @Inject constructor(
    settings: SettingsRepository,
) : ViewModel() {
    val startConfigured: Boolean = settings.snapshot.isConfigured
    val themeMode: StateFlow<ThemeMode> =
        settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)
    val language: StateFlow<Language> =
        settings.language.stateIn(viewModelScope, SharingStarted.Eagerly, Language.ENGLISH)
}

@Composable
private fun AppRoot(appViewModel: AppViewModel = hiltViewModel()) {
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
    val language by appViewModel.language.collectAsStateWithLifecycle()
    ImmichTvTheme(mode = themeMode) {
        // One string catalog for the whole tree; changing the language re-provides it live.
        CompositionLocalProvider(LocalStrings provides stringsFor(language)) {
            AppContent(appViewModel)
        }
    }
}

@Composable
private fun AppContent(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val start = if (appViewModel.startConfigured) Routes.HOME else Routes.SETUP

    NavHost(
        navController = navController,
        startDestination = start,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        composable(Routes.SETUP) {
            SetupScreen(
                onConfigured = {
                    // Clear the whole back stack so Home is the sole destination — works for first
                    // launch, "change server", and re-login after logout alike.
                    navController.navigate(Routes.HOME) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenTimeline = { navController.navigate(Routes.TIMELINE) },
                onOpenSearch = { navController.navigate(Routes.SEARCH) },
                onOpenViewer = { navController.navigate(Routes.VIEWER) },
                onOpenAlbum = { id, title -> navController.navigate(Routes.collection("album", id, title)) },
                onOpenPerson = { id, name -> navController.navigate(Routes.collection("person", id, name)) },
                onOpenPlace = { city -> navController.navigate(Routes.collection("place", city, city)) },
                // Reopen setup with current values prefilled (Back cancels, returning Home).
                onChangeServer = { navController.navigate(Routes.SETUP) },
                // After logout the settings are already cleared; drop Home from the stack.
                onLoggedOut = {
                    navController.navigate(Routes.SETUP) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.TIMELINE) {
            TimelineScreen(onOpenViewer = { navController.navigate(Routes.VIEWER) })
        }
        composable(Routes.SEARCH) {
            SearchScreen(onOpenViewer = { navController.navigate(Routes.VIEWER) })
        }
        composable(
            route = Routes.COLLECTION,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
            ),
        ) {
            CollectionScreen(onOpenViewer = { navController.navigate(Routes.VIEWER) })
        }
        composable(Routes.VIEWER) {
            ViewerScreen()
        }
    }
}
