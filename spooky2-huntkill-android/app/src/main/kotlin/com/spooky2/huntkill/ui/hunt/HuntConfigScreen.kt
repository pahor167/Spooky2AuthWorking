package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.ui.common.DisclaimerBanner
import com.spooky2.huntkill.ui.theme.MonoNumberSmall
import com.spooky2.huntkill.ui.theme.SLActive
import com.spooky2.huntkill.ui.theme.SLActiveContainer
import com.spooky2.huntkill.ui.theme.SLOnActiveContainer
import com.spooky2.huntkill.ui.theme.SLOutline
import com.spooky2.huntkill.ui.theme.SLPrimary
import com.spooky2.huntkill.ui.theme.SectionLabel

@Composable
fun HuntConfigScreen(
    viewModel: HuntViewModel,
    onStartHunt: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val params = state.params
    val validationError = params.validationError()
    val canStart = validationError == null && state.busyAction == null

    LaunchedEffect(Unit) { viewModel.prepareForConfig() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val tabs by viewModel.tabs.collectAsState()
        val activeTabIndex by viewModel.activeIndex.collectAsState()

        Text("Hunt Configuration", style = MaterialTheme.typography.headlineSmall)

        state.generator?.let { GeneratorSection(it) }

        // Scan-range + treatment fields are defaults for almost every run — kept in
        // a collapsible section so the screen leads with what the user actually
        // touches (generator choice, refine, Start). A summary row shows the values
        // while collapsed; any validation error auto-expands it.
        var settingsExpanded by remember { mutableStateOf(false) }
        LaunchedEffect(validationError) { if (validationError != null) settingsExpanded = true }
        val startVal = params.startFrequencyText.toDoubleOrNull()
        val endVal   = params.endFrequencyText.toDoubleOrNull()
        val dwellVal = params.dwellSecondsText.toDoubleOrNull()
        val ampVal   = params.targetAmplitudeCvText.toIntOrNull()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { settingsExpanded = !settingsExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("SCAN SETTINGS", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!settingsExpanded) {
                    Text(
                        "${params.startFrequencyText}–${params.endFrequencyText} Hz · " +
                            "dwell ${params.dwellSecondsText} s · ${params.targetAmplitudeCvText} cV · " +
                            "${params.scanSpeed.label} ~${params.scanSpeed.estMinutes}m",
                        style = MonoNumberSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                if (settingsExpanded) "Hide" else "Edit",
                style = MaterialTheme.typography.labelMedium,
                color = SLPrimary,
            )
        }

        if (settingsExpanded) {
            Text("SCAN RANGE", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            NumberField(
                label    = "Start frequency (Hz)",
                value    = params.startFrequencyText,
                helper   = "Scan range 41 kHz – 1.8 MHz",
                onChange = viewModel::updateStartFrequency,
                isError  = params.startFrequencyText.isNotEmpty() &&
                    (startVal == null || startVal <= 0),
            )
            NumberField(
                label    = "End frequency (Hz)",
                value    = params.endFrequencyText,
                helper   = "Upper bound of the resonance sweep",
                onChange = viewModel::updateEndFrequency,
                isError  = params.endFrequencyText.isNotEmpty() &&
                    (endVal == null || endVal <= 0 || (startVal != null && endVal <= startVal)),
            )

            Text("TREATMENT", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            NumberField(
                label    = "Kill dwell (seconds)",
                value    = params.dwellSecondsText,
                helper   = "Time spent treating each found frequency",
                onChange = viewModel::updateDwellSeconds,
                isError  = params.dwellSecondsText.isNotEmpty() &&
                    (dwellVal == null || dwellVal < 0),
            )
            NumberField(
                label    = "Target amplitude (cV)",
                value    = params.targetAmplitudeCvText,
                helper   = "Output amplitude in centivolts (2000 = 20.00 V)",
                onChange = viewModel::updateTargetAmplitude,
                isError  = params.targetAmplitudeCvText.isNotEmpty() &&
                    (ampVal == null || ampVal <= 0),
            )

            Text("SCAN SPEED", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ScanSpeed.entries.forEach { speed ->
                    val selected = params.scanSpeed == speed
                    FilterChip(
                        selected = selected,
                        onClick  = { viewModel.setScanSpeed(speed) },
                        label    = {
                            Text(
                                "${speed.label} · ~${speed.estMinutes}m",
                                style    = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        },
                        shape  = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SLActiveContainer,
                            selectedLabelColor     = SLOnActiveContainer,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled             = true,
                            selected            = selected,
                            borderColor         = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = SLActive.copy(alpha = 0.5f),
                        ),
                    )
                }
            }
            Text(
                "Standard = validated original timing; Balanced/Fast trade a little " +
                    "fidelity for speed (may miss very narrow hits).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Refinement mode (original "Continue Refining Hits"): after each kill pass,
        // re-scan a narrow window around each hit at a halved step and treat the
        // refined hits — looping until no hits remain or the user stops.
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Refine hits", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Re-scan around each hit at finer steps after every kill pass",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked         = state.refineHits,
                onCheckedChange = { viewModel.toggleRefineHits() },
                colors          = SwitchDefaults.colors(
                    checkedTrackColor = SLActive,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }

        // Review hits before treatment: when on, a clean sweep stops at a review
        // screen (inspect / graph-rescan candidates) instead of going straight to kill.
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Review hits before treatment", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Stop after the sweep to inspect / re-scan candidates before treating",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked         = state.reviewBeforeKill,
                onCheckedChange = { viewModel.setReviewBeforeKill(it) },
                colors          = SwitchDefaults.colors(
                    checkedTrackColor = SLActive,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }

        (state.errorMessage ?: validationError)?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                viewModel.startHunt()
                onStartHunt()
            },
            enabled = canStart,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SLPrimary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            if (state.busyAction != null) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.size(6.dp))
            }
            Text("Start Hunt", maxLines = 1)
        }

        Spacer(Modifier.height(4.dp))
        DisclaimerBanner()
        GeneratorTabs(tabs = tabs, activeIndex = activeTabIndex, onSelect = viewModel::setActiveGenerator)
    }
}

@Composable
private fun GeneratorSection(generator: GeneratorInfo) {
    val portLabel = generator.activeLabel()
    val portsText = if (generator.portCount != null && generator.portIndex != null) {
        " · port ${generator.portIndex + 1} of ${generator.portCount}"
    } else {
        ""
    }
    val infoText = generator.firmwareVersion?.let { " · fw $it" }.orEmpty()

    Text("GENERATOR", style = SectionLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
    AssistChip(
        onClick = {},
        enabled = false,
        label = {
            Text(
                "$portLabel$portsText$infoText · ${generator.baudRate} baud",
                style = MonoNumberSmall,
            )
        },
        shape = RoundedCornerShape(8.dp),
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, SLOutline),
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    helper: String,
    onChange: (String) -> Unit,
    isError: Boolean = value.isNotEmpty() && value.toDoubleOrNull() == null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        supportingText = { Text(helper, style = MaterialTheme.typography.bodySmall) },
        isError = isError,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = MonoNumberSmall,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = SLOutline,
            focusedBorderColor   = SLActive,
        ),
    )
}
