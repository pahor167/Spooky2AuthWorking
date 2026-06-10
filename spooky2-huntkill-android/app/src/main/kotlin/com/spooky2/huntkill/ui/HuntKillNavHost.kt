package com.spooky2.huntkill.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
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
import kotlinx.coroutines.launch

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // A back arrow is shown on the config and summary screens; running screens use a
    // dedicated Cancel/Stop control (and intercept the system back) so the generator is
    // never left running invisibly.
    val backAction: (() -> Unit)? = when (currentRoute) {
        Routes.HUNT -> { { navController.popBackStack(Routes.CONNECT, inclusive = false) } }
        Routes.HITS -> { { navController.popBackStack(Routes.HUNT, inclusive = false) } }
        else -> null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Spooky2 Hunt & Kill") },
                navigationIcon = {
                    backAction?.let { action ->
                        IconButton(onClick = action) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
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
            composable(Routes.HUNT) {
                val viewModel = sharedHuntViewModel(navController)
                ObserveEvents(viewModel) { msg -> scope.launch { snackbarHostState.showMessage(msg) } }
                HuntConfigScreen(
                    viewModel = viewModel,
                    onStartHunt = { navController.navigate(Routes.LIVE) },
                )
            }
            composable(Routes.LIVE) {
                val viewModel = sharedHuntViewModel(navController)
                ObserveEvents(viewModel) { msg -> scope.launch { snackbarHostState.showMessage(msg) } }
                LiveScanScreen(
                    viewModel = viewModel,
                    // Kill auto-starts after the sweep: jump straight to the Kill screen,
                    // replacing Live so Back doesn't return to the finished sweep.
                    onKilling = {
                        navController.navigate(Routes.KILL) {
                            popUpTo(Routes.LIVE) { inclusive = true }
                        }
                    },
                    onDone = {
                        navController.navigate(Routes.HITS) {
                            popUpTo(Routes.LIVE) { inclusive = true }
                        }
                    },
                    onCancelled = { navController.popBackStack(Routes.HUNT, inclusive = false) },
                )
            }
            composable(Routes.HITS) {
                val viewModel = sharedHuntViewModel(navController)
                ObserveEvents(viewModel) { msg -> scope.launch { snackbarHostState.showMessage(msg) } }
                HitsScreen(
                    viewModel = viewModel,
                    onRunAgain = { navController.popBackStack(Routes.HUNT, inclusive = false) },
                    onDisconnect = {
                        navController.navigate(Routes.CONNECT) {
                            popUpTo(Routes.GRAPH) { inclusive = false }
                        }
                    },
                )
            }
            composable(Routes.KILL) {
                val viewModel = sharedHuntViewModel(navController)
                ObserveEvents(viewModel) { msg -> scope.launch { snackbarHostState.showMessage(msg) } }
                KillScreen(
                    viewModel = viewModel,
                    // After the kill completes show the post-kill summary on the Hits
                    // screen; cancel/error pop back to Hunt config.
                    onDone = {
                        navController.navigate(Routes.HITS) {
                            popUpTo(Routes.KILL) { inclusive = true }
                        }
                    },
                    onStopped = { navController.popBackStack(Routes.HUNT, inclusive = false) },
                )
            }
            composable(Routes.LOG) {
                LogScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

/** Collect one-shot ViewModel events (snackbar messages) for the lifetime of a screen. */
@Composable
private fun ObserveEvents(viewModel: HuntViewModel, onEvent: (String) -> Unit) {
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.events.collect { onEvent(it) }
    }
}

/** Show a snackbar, replacing any currently visible one so messages don't queue up. */
private suspend fun SnackbarHostState.showMessage(message: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(message)
}

/**
 * Resolve the SINGLE [HuntViewModel] shared across Hunt/Live/Hits/Kill.
 *
 * The ViewModel is scoped to the navigation-graph back stack entry ([Routes.GRAPH]),
 * which is owned by the [NavHost] and lives for the whole flow. Every screen resolves
 * the same graph entry, so they all get the exact same instance — one running scan
 * coroutine and one [HuntViewModel.state] stream.
 *
 * Keying the `remember` on the stable graph entry (resolved once, not re-keyed per
 * screen) guarantees all four screens share one instance so Pause/Cancel act on the
 * running scan.
 */
@Composable
private fun sharedHuntViewModel(navController: NavHostController): HuntViewModel {
    val parentEntry = remember(navController) { navController.getBackStackEntry(Routes.GRAPH) }
    return hiltViewModel(parentEntry)
}
