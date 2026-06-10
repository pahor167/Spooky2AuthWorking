package com.spooky2.huntkill.ui.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.spooky2.huntkill.ui.common.DisclaimerBanner

@Composable
fun ConnectScreen(
    onConnected: () -> Unit,
    viewModel: ConnectViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isConnected) {
        if (state.isConnected) onConnected()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Spooky2 Hunt & Kill", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Connect to the generator to begin.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        when (state.status) {
            ConnectStatus.Connecting -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
                Text("Handshaking…")
            }
            ConnectStatus.Connected -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Handshake success", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Generator: ${state.generatorType ?: "—"}")
                        Text("Baud: ${state.baudRate ?: "—"}")
                        state.authToken?.let {
                            Text(
                                "Auth token: $it",
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
            ConnectStatus.Error -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Connection failed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.errorMessage ?: "Connection failed",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            ConnectStatus.Idle -> Unit
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = viewModel::connect,
            enabled = state.status != ConnectStatus.Connecting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.status == ConnectStatus.Error) "Retry Connect (Demo)" else "Connect (Demo)")
        }

        Spacer(Modifier.height(16.dp))
        if (state.usbDevices.isEmpty()) {
            Text("No USB generator attached", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = viewModel::refreshUsbDevices,
                enabled = state.status != ConnectStatus.Connecting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Refresh")
            }
        } else {
            Text("Select generator", style = MaterialTheme.typography.titleSmall)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    state.usbDevices.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = option.deviceName == state.selectedDeviceName,
                                    onClick = { viewModel.selectUsbDevice(option.deviceName) },
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = option.deviceName == state.selectedDeviceName,
                                onClick = { viewModel.selectUsbDevice(option.deviceName) },
                            )
                            Text(
                                option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = viewModel::connectUsb,
                enabled = state.status != ConnectStatus.Connecting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Connect (USB)")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = viewModel::refreshUsbDevices,
                enabled = state.status != ConnectStatus.Connecting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Refresh")
            }
        }

        Spacer(Modifier.height(24.dp))
        DisclaimerBanner()
    }
}
