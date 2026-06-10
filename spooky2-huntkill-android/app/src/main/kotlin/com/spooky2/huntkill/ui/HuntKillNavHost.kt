package com.spooky2.huntkill.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.spooky2.huntkill.ui.connect.ConnectScreen
import com.spooky2.huntkill.ui.hunt.HitsScreen
import com.spooky2.huntkill.ui.hunt.HuntConfigScreen
import com.spooky2.huntkill.ui.hunt.HuntViewModel
import com.spooky2.huntkill.ui.hunt.KillScreen
import com.spooky2.huntkill.ui.hunt.LiveScanScreen

/** Navigation routes for the Connect → Hunt → Live → Hits → Kill flow. */
object Routes {
    const val GRAPH = "huntkill"
    const val CONNECT = "connect"
    const val HUNT = "hunt"
    const val LIVE = "live"
    const val HITS = "hits"
    const val KILL = "kill"
}

@Composable
fun HuntKillNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Routes.CONNECT,
        route = Routes.GRAPH,
    ) {
        composable(Routes.CONNECT) {
            ConnectScreen(onConnected = { navController.navigate(Routes.HUNT) })
        }
        composable(Routes.HUNT) { entry ->
            HuntConfigScreen(
                viewModel = sharedHuntViewModel(navController, entry),
                onStartHunt = { navController.navigate(Routes.LIVE) },
            )
        }
        composable(Routes.LIVE) { entry ->
            LiveScanScreen(
                viewModel = sharedHuntViewModel(navController, entry),
                onHitsReady = { navController.navigate(Routes.HITS) },
                onCancelled = { navController.popBackStack(Routes.HUNT, inclusive = false) },
            )
        }
        composable(Routes.HITS) { entry ->
            HitsScreen(
                viewModel = sharedHuntViewModel(navController, entry),
                onStartKill = { navController.navigate(Routes.KILL) },
            )
        }
        composable(Routes.KILL) { entry ->
            KillScreen(
                viewModel = sharedHuntViewModel(navController, entry),
                onFinished = { navController.popBackStack(Routes.HUNT, inclusive = false) },
            )
        }
    }
}

/**
 * Resolve a single [HuntViewModel] shared across Hunt/Live/Hits/Kill by scoping it to
 * the parent navigation-graph back stack entry. This keeps one running scan coroutine
 * and one state stream across the whole flow (survives recomposition + navigation).
 */
@Composable
private fun sharedHuntViewModel(
    navController: NavHostController,
    entry: NavBackStackEntry,
): HuntViewModel {
    val parentEntry = remember(entry) { navController.getBackStackEntry(Routes.GRAPH) }
    return hiltViewModel(parentEntry)
}
