package com.spooky2.huntkill.ui.log

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.spooky2.huntkill.log.FileLogWriter
import com.spooky2.huntkill.log.LogBus
import com.spooky2.huntkill.log.LogEntry
import com.spooky2.huntkill.ui.theme.MonoNumberSmall
import com.spooky2.huntkill.ui.theme.SLPrimary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(
    val logBus: LogBus,
    private val fileWriter: FileLogWriter,
) : ViewModel() {
    suspend fun exportZip(): File = withContext(Dispatchers.IO) { fileWriter.exportZip() }
}

@Composable
fun LogScreen(
    onBack: () -> Unit,
    viewModel: LogViewModel = hiltViewModel(),
) {
    val logBus            = viewModel.logBus
    val entries           by logBus.entries.collectAsState()
    val listState         = rememberLazyListState()
    val clipboard: ClipboardManager = LocalClipboardManager.current
    val context           = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.scrollToItem(entries.size - 1)
    }

    // Compact action row replaces the inner TopAppBar; the outer NavHost Scaffold
    // supplies the shared TopAppBar (title + back arrow) for this route.
    LazyColumn(
        state    = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
    ) {
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(logBus.dump()))
                        scope.launch { snackbarHostState.showSnackbar("Logs copied") }
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 8.dp,
                        vertical   = 0.dp,
                    ),
                ) { Text("Copy", style = MaterialTheme.typography.labelMedium, color = SLPrimary) }
                TextButton(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, logBus.dump())
                        }
                        context.startActivity(Intent.createChooser(send, "Share logs"))
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 8.dp,
                        vertical   = 0.dp,
                    ),
                ) { Text("Share", style = MaterialTheme.typography.labelMedium, color = SLPrimary) }
                TextButton(
                    onClick = {
                        scope.launch {
                            runCatching {
                                logBus.flush()
                                val zip = viewModel.exportZip()
                                shareZip(context, zip)
                            }.onFailure {
                                snackbarHostState.showSnackbar("Export failed: ${it.message ?: "error"}")
                            }
                        }
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 8.dp,
                        vertical   = 0.dp,
                    ),
                ) { Text("Export", style = MaterialTheme.typography.labelMedium, color = SLPrimary) }
                TextButton(
                    onClick = { logBus.clear() },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 8.dp,
                        vertical   = 0.dp,
                    ),
                ) {
                    Text(
                        "Clear",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            SnackbarHost(snackbarHostState)
        }
        items(entries) { entry ->
            Text(
                text  = formatLine(entry),
                style = MonoNumberSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun shareZip(context: Context, zip: File) {
    val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", zip)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Export logs"))
}

private val lineTimeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

private fun formatLine(entry: LogEntry): String =
    "${lineTimeFormat.format(Date(entry.timestampMs))} ${entry.level}/${entry.tag}: ${entry.message}"
