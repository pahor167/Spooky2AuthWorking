package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.ui.common.DisclaimerBanner

@Composable
fun HuntConfigScreen(
    viewModel: HuntViewModel,
    onStartHunt: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val params = state.params
    val validationError = params.validationError()
    val canStart = validationError == null && !state.isSwitchingGenerator

    // Returning to this screen resets a finished/cancelled run so the config isn't
    // stuck and refreshes the connected-generator chip.
    LaunchedEffect(Unit) { viewModel.prepareForConfig() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Hunt Configuration", style = MaterialTheme.typography.headlineSmall)

        state.generator?.let { GeneratorSection(it, state.isSwitchingGenerator, viewModel::switchGenerator) }

        Text("Scan range", style = MaterialTheme.typography.titleSmall)
        NumberField(
            "Start frequency (Hz)",
            params.startFrequencyText,
            "Scan range 41 kHz – 1.8 MHz",
            viewModel::updateStartFrequency,
        )
        NumberField(
            "End frequency (Hz)",
            params.endFrequencyText,
            "Upper bound of the resonance sweep",
            viewModel::updateEndFrequency,
        )

        Text("Treatment", style = MaterialTheme.typography.titleSmall)
        NumberField(
            "Kill dwell (seconds)",
            params.dwellSecondsText,
            "Dwell: time spent treating each found frequency",
            viewModel::updateDwellSeconds,
        )
        NumberField(
            "Target amplitude (cV)",
            params.targetAmplitudeCvText,
            "Output amplitude in centivolts (2000 = 20.00 V)",
            viewModel::updateTargetAmplitude,
        )

        (state.errorMessage ?: validationError)?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.startHunt()
                onStartHunt()
            },
            enabled = canStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start Hunt")
        }

        Spacer(Modifier.height(8.dp))
        DisclaimerBanner()
    }
}

@Composable
private fun GeneratorSection(
    generator: GeneratorInfo,
    isSwitching: Boolean,
    onSwitch: (Int) -> Unit,
) {
    val portLabel = generator.portIndex?.let { "Generator ${it + 1}" } ?: generator.generatorType
    val portsText = if (generator.portCount != null && generator.portIndex != null) {
        " · port ${generator.portIndex + 1} of ${generator.portCount}"
    } else {
        ""
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text("$portLabel$portsText · ${generator.baudRate} baud") },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
        ),
    )

    if (generator.hasMultiplePorts && generator.portCount != null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                for (index in 0 until generator.portCount) {
                    SegmentedButton(
                        selected = generator.portIndex == index,
                        onClick = { onSwitch(index) },
                        enabled = !isSwitching,
                        shape = SegmentedButtonDefaults.itemShape(index, generator.portCount),
                    ) {
                        Text("Generator ${index + 1}")
                    }
                }
            }
            if (isSwitching) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp).padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, helper: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        supportingText = { Text(helper) },
        isError = value.isNotEmpty() && value.toDoubleOrNull() == null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
