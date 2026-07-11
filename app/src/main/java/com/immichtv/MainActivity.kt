package com.immichtv

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.immichtv.data.prefs.SettingsRepository
import com.immichtv.ui.collection.CollectionScreen
import com.immichtv.ui.home.HomeScreen
import com.immichtv.ui.setup.SetupScreen
import com.immichtv.ui.theme.ImmichTvTheme
import com.immichtv.ui.timeline.TimelineScreen
import com.immichtv.ui.viewer.ViewerScreen
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val TIMELINE = "timeline"
    const val VIEWER = "viewer"
    const val COLLECTION = "collection/{source}/{id}?title={title}"
    fun collection(source: String, id: String, title: String) =
        "collection/$source/$id?title=${Uri.encode(title)}"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImmichTvTheme {
                AppRoot()
            }
        }
    }
}

/** Decides the first screen based on whether the server is already configured. */
@HiltViewModel
class AppViewModel @Inject constructor(
    settings: SettingsRepository,
) : ViewModel() {
    val startConfigured: Boolean = settings.snapshot.isConfigured
}

@Composable
private fun AppRoot(appViewModel: AppViewModel = hiltViewModel()) {
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
                onOpenViewer = { navController.navigate(Routes.VIEWER) },
                onOpenAlbum = { id, title -> navController.navigate(Routes.collection("album", id, title)) },
                onOpenPerson = { id, name -> navController.navigate(Routes.collection("person", id, name)) },
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
