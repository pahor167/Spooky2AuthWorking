package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Hunt Configuration", style = MaterialTheme.typography.headlineSmall)

        NumberField("Start frequency (Hz)", params.startFrequencyText, viewModel::updateStartFrequency)
        NumberField("End frequency (Hz)", params.endFrequencyText, viewModel::updateEndFrequency)
        NumberField("Kill dwell (seconds)", params.dwellSecondsText, viewModel::updateDwellSeconds)
        NumberField("Target amplitude (cV)", params.targetAmplitudeCvText, viewModel::updateTargetAmplitude)

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                viewModel.startHunt()
                onStartHunt()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Start Hunt")
        }

        Spacer(Modifier.height(8.dp))
        DisclaimerBanner()
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}
