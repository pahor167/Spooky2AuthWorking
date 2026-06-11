package com.spooky2.huntkill.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spooky2.huntkill.ui.connect.ConnectScreen
import com.spooky2.huntkill.ui.history.HistoryDetailScreen
import com.spooky2.huntkill.ui.history.HistoryScreen
import com.spooky2.huntkill.ui.history.HistoryViewModel
import com.spooky2.huntkill.ui.hunt.HitsScreen
import com.spooky2.huntkill.ui.hunt.HuntConfigScreen
import com.spooky2.huntkill.ui.hunt.HuntViewModel
import com.spooky2.huntkill.ui.hunt.KillScreen
import com.spooky2.huntkill.ui.hunt.LiveScanScreen
import com.spooky2.huntkill.ui.log.LogScreen
import com.spooky2.huntkill.ui.theme.SLPrimary
import kotlinx.coroutines.launch

/** Navigation routes for the Connect → Hunt → Live → Hits → Kill flow, plus Logs. */
object Routes {
    const val GRAPH          = "huntkill"
    const val CONNECT        = "connect"
    const val HUNT           = "hunt"
    const val LIVE           = "live"
    const val HITS           = "hits"
    const val KILL           = "kill"
    const val LOG            = "logs"
    const val HISTORY        = "history"
    const val HISTORY_DETAIL = "history/{id}"

    fun historyDetail(id: String): String = "history/$id"
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HuntKillNavHost(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope          = rememberCoroutineScope()

    // No back arrow on LIVE: terminal phases (Error/Cancelled/Done/Hits) auto-navigate
    // away, and an arrow while running would bypass the stop-confirm dialog.
    val backAction: (() -> Unit)? = when (currentRoute) {
        Routes.HUNT             -> { { navController.popBackStack(Routes.CONNECT, inclusive = false) } }
        Routes.HITS             -> { { navController.popBackStack(Routes.HUNT, inclusive = false) } }
        Routes.LOG              -> { { navController.popBackStack() } }
        Routes.HISTORY, Routes.HISTORY_DETAIL -> { { navController.popBackStack() } }
        else -> null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "HUNT & KILL",
                        style = MaterialTheme.typography.titleSmall,
                        color = SLPrimary,
                    )
                },
                navigationIcon = {
                    backAction?.let { action ->
                        IconButton(onClick = action) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    val onHistoryScreens =
                        currentRoute == Routes.HISTORY || currentRoute == Routes.HISTORY_DETAIL
                    if (!onHistoryScreens) {
                        TextButton(
                            onClick = { navController.navigate(Routes.HISTORY) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 8.dp,
                                vertical   = 0.dp,
                            ),
                        ) {
                            Text(
                                "History",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (currentRoute != Routes.LOG) {
                        IconButton(onClick = { navController.navigate(Routes.LOG) }) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = "Logs",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor              = MaterialTheme.colorScheme.surface,
                    titleContentColor           = SLPrimary,
                    navigationIconContentColor  = MaterialTheme.colorScheme.onSurfaceVariant,
                    actionIconContentColor      = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Routes.CONNECT,
            route            = Routes.GRAPH,
            modifier         = Modifier.fillMaxSize().padding(padding),
        ) {
            composable(Routes.CONNECT) {
                ConnectScreen(onConnected = { navController.navigate(Routes.HUNT) })
            }
            composable(Routes.HUNT) {
                val viewModel = sharedHuntViewModel(navController)
                ObserveEvents(viewModel) { msg -> scope.launch { snackbarHostState.showMessage(msg) } }
                HuntConfigScreen(
                    viewModel    = viewModel,
                    onStartHunt  = { navController.navigate(Routes.LIVE) },
                )
            }
            composable(Routes.LIVE) {
                val viewModel = sharedHuntViewModel(navController)
                ObserveEvents(viewModel) { msg -> scope.launch { snackbarHostState.showMessage(msg) } }
                LiveScanScreen(
                    viewModel  = viewModel,
                    onKilling  = {
                        navController.navigate(Routes.KILL) {
                            popUpTo(Routes.LIVE) { inclusive = true }
                        }
                    },
                    onDone     = {
                        navController.navigate(Routes.HITS) {
                            popUpTo(Routes.LIVE) { inclusive = true }
                        }
                    },
                    onCancelled = { navController.popBackStack(Routes.HUNT, inclusive = false) },
                    onDropouts  = {
                        navController.navigate(Routes.HITS) {
                            popUpTo(Routes.LIVE) { inclusive = true }
                        }
                    },
                    onError = { navController.popBackStack(Routes.HUNT, inclusive = false) },
                )
            }
            composable(Routes.HITS) {
                val viewModel = sharedHuntViewModel(navController)
                ObserveEvents(viewModel) { msg -> scope.launch { snackbarHostState.showMessage(msg) } }
                HitsScreen(
                    viewModel    = viewModel,
                    onRunAgain   = { navController.popBackStack(Routes.HUNT, inclusive = false) },
                    onDisconnect = {
                        navController.navigate(Routes.CONNECT) {
                            popUpTo(Routes.GRAPH) { inclusive = false }
                        }
                    },
                    onKilling    = {
                        navController.navigate(Routes.KILL) {
                            popUpTo(Routes.HITS) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.KILL) {
                val viewModel = sharedHuntViewModel(navController)
                ObserveEvents(viewModel) { msg -> scope.launch { snackbarHostState.showMessage(msg) } }
                KillScreen(
                    viewModel = viewModel,
                    onDone    = {
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
            composable(Routes.HISTORY) {
                val historyViewModel = hiltViewModel<HistoryViewModel>()
                HistoryScreen(
                    viewModel  = historyViewModel,
                    onOpenRun  = { id -> navController.navigate(Routes.historyDetail(id)) },
                )
            }
            composable(
                Routes.HISTORY_DETAIL,
                arguments = listOf(navArgument("id") { type = NavType.StringType }),
            ) { entry ->
                val runId            = entry.arguments?.getString("id").orEmpty()
                val historyViewModel = hiltViewModel<HistoryViewModel>()
                val huntViewModel    = sharedHuntViewModel(navController)
                HistoryDetailScreen(
                    runId     = runId,
                    viewModel = historyViewModel,
                    onBack    = { navController.popBackStack() },
                    onReRun   = { run ->
                        val started = huntViewModel.startKillFromFrequencies(
                            freqs          = run.hits.map { it.frequency },
                            dwellSeconds   = run.dwellSeconds,
                            amplitudeCv    = run.targetAmplitudeCv,
                            deviations     = run.hits.map { it.deviation },
                        )
                        if (started) {
                            navController.navigate(Routes.KILL)
                        } else {
                            scope.launch { snackbarHostState.showMessage("Connect a generator first") }
                            navController.navigate(Routes.CONNECT) {
                                popUpTo(Routes.GRAPH) { inclusive = false }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ObserveEvents(viewModel: HuntViewModel, onEvent: (String) -> Unit) {
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.events.collect { onEvent(it) }
    }
}

private suspend fun SnackbarHostState.showMessage(message: String) {
    currentSnackbarData?.dismiss()
    showSnackbar(message)
}

@Composable
private fun sharedHuntViewModel(navController: NavHostController): HuntViewModel {
    val parentEntry = remember(navController) { navController.getBackStackEntry(Routes.GRAPH) }
    return hiltViewModel(parentEntry)
}
