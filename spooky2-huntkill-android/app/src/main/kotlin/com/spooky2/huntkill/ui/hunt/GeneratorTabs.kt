package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.ui.common.formatElapsed
import com.spooky2.huntkill.ui.theme.SLActive
import com.spooky2.huntkill.ui.theme.SLActiveContainer
import com.spooky2.huntkill.ui.theme.SLError
import com.spooky2.huntkill.ui.theme.SLOnActiveContainer
import com.spooky2.huntkill.ui.theme.SLOnSurfaceVariant
import com.spooky2.huntkill.ui.theme.SLOutline
import com.spooky2.huntkill.ui.theme.SLSecondary
import com.spooky2.huntkill.ui.theme.SLSurfaceVariant
import com.spooky2.huntkill.ui.theme.SectionLabel

/**
 * Horizontal scrollable tab strip — one tab per connected generator.
 * Hidden when only one generator is available so single-generator devices look
 * exactly as before.
 */
@Composable
fun GeneratorTabs(
    tabs: List<GeneratorTabInfo>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.size <= 1) return

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEach { tab ->
            GeneratorTab(
                tab        = tab,
                isActive   = tab.index == activeIndex,
                onSelect   = { onSelect(tab.index) },
            )
        }
    }
}

@Composable
private fun GeneratorTab(
    tab: GeneratorTabInfo,
    isActive: Boolean,
    onSelect: () -> Unit,
) {
    val icon       = phaseIcon(tab.phase, tab.isPaused)
    val iconColor  = phaseColor(tab.phase, tab.isPaused)
    val shortLabel = phaseShortLabel(tab.phase, tab.isPaused)

    val containerColor = if (isActive) SLActiveContainer else SLSurfaceVariant
    val border = if (isActive) {
        BorderStroke(1.dp, SLActive.copy(alpha = 0.7f))
    } else {
        BorderStroke(1.dp, SLOutline)
    }

    Surface(
        onClick       = onSelect,
        shape         = RoundedCornerShape(10.dp),
        color         = containerColor,
        border        = border,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = shortLabel,
                    tint               = iconColor,
                    modifier           = Modifier.size(14.dp),
                )
                Text(
                    text  = shortLabel,
                    style = SectionLabel,
                    color = iconColor,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text  = tab.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isActive) SLOnActiveContainer else MaterialTheme.colorScheme.onSurface,
                )
                val isRunning = tab.phase == HuntPhase.Hunting || tab.phase == HuntPhase.Killing
                if (isRunning && tab.timeLeftSeconds > 0) {
                    Text(
                        text  = formatElapsed(tab.timeLeftSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Short uppercase label for the phase badge inside the tab. */
internal fun phaseShortLabel(phase: HuntPhase, isPaused: Boolean): String = when {
    isPaused                           -> "PAUSED"
    phase == HuntPhase.Hunting         -> "HUNT"
    phase == HuntPhase.Killing         -> "KILL"
    phase == HuntPhase.HitsReady       -> "HITS"
    phase == HuntPhase.HitsReadyWithDropouts -> "HITS"
    phase == HuntPhase.Done            -> "DONE"
    phase == HuntPhase.Cancelled       -> "STOP"
    phase == HuntPhase.Error           -> "ERR"
    else                               -> "IDLE"
}

/** Tint color for the phase icon/label inside the tab. */
internal fun phaseColor(phase: HuntPhase, isPaused: Boolean): Color = when {
    isPaused                   -> SLSecondary
    phase == HuntPhase.Hunting -> SLActive
    phase == HuntPhase.Killing -> SLError
    phase == HuntPhase.Error   -> SLError
    else                       -> SLOnSurfaceVariant
}

/** Icon representing the current phase, chosen from material-icons-core. */
private fun phaseIcon(phase: HuntPhase, isPaused: Boolean): ImageVector = when {
    isPaused                   -> Icons.Filled.Refresh
    phase == HuntPhase.Hunting -> Icons.Filled.Search
    phase == HuntPhase.Killing -> Icons.Filled.PlayArrow
    phase == HuntPhase.Done    -> Icons.Filled.Done
    phase == HuntPhase.Cancelled -> Icons.Filled.Close
    phase == HuntPhase.Error   -> Icons.Filled.Warning
    else                       -> Icons.Filled.Done
}
