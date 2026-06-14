package com.spooky2.huntkill.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spooky2.huntkill.core.lookup.LookupMatch
import com.spooky2.huntkill.ui.theme.SLOnErrorContainer
import com.spooky2.huntkill.ui.theme.SLOnSecondaryContainer
import com.spooky2.huntkill.ui.theme.SLErrorContainer
import com.spooky2.huntkill.ui.theme.SLSecondaryContainer

/**
 * A condition label shown as a small inline box next to a hit frequency when its
 * reverse-lookup matches name a notable program (e.g. C19, FLU). Add new conditions
 * by appending to [CONDITIONS] — each carries its own keyword matcher and colors.
 */
data class ConditionTag(
    val label: String,
    val container: Color,
    val content: Color,
    val matches: (String) -> Boolean,
)

private val COVID_RE = Regex(
    """covid|sars[- ]?cov|corona[- ]?virus|ncov|c-?19""",
    RegexOption.IGNORE_CASE,
)
// Word-bounded so "flu" doesn't fire on fluke/fluid/reflux/fluor…; the "influenz*"
// stem covers Influenza / Influenzum / Influenzinum (homeopathic nosodes).
private val FLU_RE = Regex("""\b(flu|influenz\w*)\b""", RegexOption.IGNORE_CASE)

/** Tag specs, in display order. Keyed off the program name of each lookup match. */
val CONDITIONS: List<ConditionTag> = listOf(
    ConditionTag("C19", SLErrorContainer, SLOnErrorContainer) { COVID_RE.containsMatchIn(it) },
    ConditionTag("FLU", SLSecondaryContainer, SLOnSecondaryContainer) { FLU_RE.containsMatchIn(it) },
)

/** Which condition tags apply to [matches] (any match's program name triggers a tag). */
fun conditionTagsFor(matches: List<LookupMatch>?): List<ConditionTag> {
    if (matches.isNullOrEmpty()) return emptyList()
    return CONDITIONS.filter { tag -> matches.any { tag.matches(it.programName) } }
}

/** Render the applicable condition tags as small inline boxes. No-op when none apply. */
@Composable
fun ConditionTags(matches: List<LookupMatch>?, modifier: Modifier = Modifier) {
    val tags = conditionTagsFor(matches)
    if (tags.isEmpty()) return
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        tags.forEach { tag ->
            Surface(
                color = tag.container,
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, tag.content.copy(alpha = 0.4f)),
            ) {
                Text(
                    tag.label,
                    color = tag.content,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
    }
}
