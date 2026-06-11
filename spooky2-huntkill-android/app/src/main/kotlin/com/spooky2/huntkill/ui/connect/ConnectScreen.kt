package com.spooky2.huntkill.ui.connect

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spooky2.huntkill.ui.common.DisclaimerBanner
import com.spooky2.huntkill.ui.theme.MonoNumberMedium
import com.spooky2.huntkill.ui.theme.MonoNumberSmall
import com.spooky2.huntkill.ui.theme.SLBackground
import com.spooky2.huntkill.ui.theme.SLPrimary
import com.spooky2.huntkill.ui.theme.SLPrimaryContainer
import com.spooky2.huntkill.ui.theme.SLSurface

@Composable
fun ConnectScreen(
    onConnected: () -> Unit,
    viewModel: ConnectViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isConnected) {
        if (state.isConnected) onConnected()
    }

    // Quiet radial gradient — a depth cue behind the hero text, not a colour splash
    val gradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF0E2A24),   // very dark cyan tint at centre
            SLBackground,        // fades to flat background
        ),
        center = Offset(Float.POSITIVE_INFINITY / 2, 0f),
        radius = 900f,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Hero title in Archivo display weight
            Text(
                "HUNT & KILL",
                style = MaterialTheme.typography.headlineLarge,
                color = SLPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                "Spooky2",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Find resonant frequencies, then treat them — connect your generator to begin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            when (state.status) {
                ConnectStatus.Connecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = SLPrimary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Connecting…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ConnectStatus.Connected -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SLPrimaryContainer),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "CONNECTED",
                                style = MaterialTheme.typography.titleSmall,
                                color = SLPrimary,
                            )
                            Text(
                                "Generator: ${state.generatorType ?: "—"}",
                                style = MonoNumberSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                "Baud: ${state.baudRate ?: "—"}",
                                style = MonoNumberSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            state.authToken?.let {
                                Text(
                                    "Token: $it",
                                    style = MonoNumberSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
                ConnectStatus.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline,
                        ),
                        elevation = CardDefaults.cardElevation(0.dp),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "CONNECTION FAILED",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                state.errorMessage ?: "Connection failed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
                ConnectStatus.Idle -> Unit
            }

            Spacer(Modifier.height(16.dp))

            // Primary CTA — filled cyan with dark text
            Button(
                onClick = viewModel::connectUsb,
                enabled = state.status != ConnectStatus.Connecting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SLPrimary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = SLPrimaryContainer,
                    disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                if (state.status == ConnectStatus.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.size(6.dp))
                }
                Text(
                    if (state.status == ConnectStatus.Error) "Retry Connect (USB)" else "Connect (USB)",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!state.usbAttached) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "No USB generator detected — attach one over USB-OTG, then tap Connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))
            DisclaimerBanner()
        }
    }
}
