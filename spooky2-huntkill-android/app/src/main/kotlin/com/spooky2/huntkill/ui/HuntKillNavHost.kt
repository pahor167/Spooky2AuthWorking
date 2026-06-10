package com.spooky2.huntkill.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spooky2.huntkill.ui.connect.ConnectScreen
import com.spooky2.huntkill.ui.hunt.HitsScreen
import com.spooky2.huntkill.ui.hunt.HuntConfigScreen
import com.spooky2.huntkill.ui.hunt.HuntViewModel
import com.spooky2.huntkill.ui.hunt.KillScreen
import com.spooky2.huntkill.ui.hunt.LiveScanScreen
import com.spooky2.huntkill.ui.log.LogScreen

/** Navigation routes for the Connect → Hunt → Live → Hits → Kill flow, plus Logs. */
object Routes {
    const val GRAPH = "huntkill"
    const val CONNECT = "connect"
    const val HUNT = "hunt"
    const val LIVE = "live"
    const val HITS = "hits"
    const val KILL = "kill"
    const val LOG = "logs"
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HuntKillNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spooky2 Hunt & Kill") },
                actions = {
                    // Persistent Logs button visible on every flow screen (hidden on the
                    // Logs screen itself, which has its own back navigation).
                    if (currentRoute != Routes.LOG) {
                        IconButton(onClick = { navController.navigate(Routes.LOG) }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Logs")
                        }
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CONNECT,
            route = Routes.GRAPH,
            modifier = Modifier.fillMaxSize().padding(padding),
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
            composable(Routes.LOG) {
                LogScreen(onBack = { navController.popBackStack() })
            }
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
