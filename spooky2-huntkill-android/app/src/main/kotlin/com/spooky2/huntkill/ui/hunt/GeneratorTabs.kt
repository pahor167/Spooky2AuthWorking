package com.spooky2.huntkill.ui.hunt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.spooky2.huntkill.ui.common.formatElapsed
import com.spooky2.huntkill.ui.theme.SLActive
import com.spooky2.huntkill.ui.theme.SLError
import com.spooky2.huntkill.ui.theme.SLOnSurfaceVariant
import com.spooky2.huntkill.ui.theme.SLOutline
import com.spooky2.huntkill.ui.theme.SLSecondary
import com.spooky2.huntkill.ui.theme.SLSurface

/**
 * Real Material tab bar — one tab per connected generator, with the selected tab
 * underlined in the cold-blue accent. Designed to sit at the BOTTOM of the run
 * screens (a top divider separates it from the content above). Hidden when only one
 * generator is connected, so single-generator devices are unaffected.
 */
@Composable
fun GeneratorTabs(
    tabs: List<GeneratorTabInfo>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.size <= 1) return
    val selected = activeIndex.coerceIn(0, tabs.lastIndex)

    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = SLOutline)
        TabRow(
            selectedTabIndex = selected,
            containerColor   = SLSurface,
            contentColor     = SLActive,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(positions[selected]),
                    color = SLActive,
                )
            },
            divider = {},
        ) {
            tabs.forEachIndexed { i, tab ->
                GeneratorTab(tab = tab, selected = i == selected, onSelect = { onSelect(i) })
            }
        }
    }
}

@Composable
private fun GeneratorTab(
    tab: GeneratorTabInfo,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Tab(
        selected = selected,
        onClick  = onSelect,
        selectedContentColor   = SLActive,
        unselectedContentColor = SLOnSurfaceVariant,
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector        = phaseIcon(tab.phase, tab.isPaused),
                contentDescription = null,
                tint               = phaseColor(tab.phase, tab.isPaused),
                modifier           = Modifier.size(18.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text  = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) SLActive else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text  = statusLine(tab),
                    style = MaterialTheme.typography.labelSmall,
                    color = phaseColor(tab.phase, tab.isPaused),
                )
            }
        }
    }
}

/** "HUNT · 19:10", "KILL · 02:00", "PAUSED", "IDLE", … */
private fun statusLine(tab: GeneratorTabInfo): String {
    val label = phaseShortLabel(tab.phase, tab.isPaused)
    val running = tab.phase == HuntPhase.Hunting || tab.phase == HuntPhase.Killing
    return if (running && tab.timeLeftSeconds > 0) "$label · ${formatElapsed(tab.timeLeftSeconds)}" else label
}

/** Short uppercase label for the phase badge inside the tab. */
internal fun phaseShortLabel(phase: HuntPhase, isPaused: Boolean): String = when {
    isPaused                                 -> "PAUSED"
    phase == HuntPhase.Hunting               -> "HUNT"
    phase == HuntPhase.Killing               -> "KILL"
    phase == HuntPhase.HitsReady             -> "HITS"
    phase == HuntPhase.HitsReadyWithDropouts -> "HITS"
    phase == HuntPhase.Done                  -> "DONE"
    phase == HuntPhase.Cancelled             -> "STOP"
    phase == HuntPhase.Error                 -> "ERR"
    else                                     -> "IDLE"
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
    isPaused                     -> Icons.Filled.Refresh
    phase == HuntPhase.Hunting   -> Icons.Filled.Search
    phase == HuntPhase.Killing   -> Icons.Filled.PlayArrow
    phase == HuntPhase.Done      -> Icons.Filled.Done
    phase == HuntPhase.Cancelled -> Icons.Filled.Close
    phase == HuntPhase.Error     -> Icons.Filled.Warning
    else                         -> Icons.Filled.Done
}
