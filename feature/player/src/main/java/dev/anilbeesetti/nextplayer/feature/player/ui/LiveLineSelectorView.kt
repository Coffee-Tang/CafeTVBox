package dev.anilbeesetti.nextplayer.feature.player.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.ui.R

/**
 * Lets the viewer put a channel on another of its lines.
 *
 * The lines are the servers carrying one station, in the order they are fallen back on, and are
 * told apart by nothing a viewer could recognise, so they are offered by position alone.
 */
@Composable
fun BoxScope.LiveLineSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    lineCount: Int,
    lineInUse: Int,
    onLineClick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    OverlayView(
        modifier = modifier,
        show = show,
        title = stringResource(R.string.live_lines),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .padding(horizontal = 24.dp)
                .selectableGroup(),
        ) {
            (1..lineCount).forEach { line ->
                RadioButtonRow(
                    selected = line == lineInUse,
                    text = stringResource(R.string.live_line, line),
                    onClick = {
                        onLineClick(line)
                        onDismiss()
                    },
                )
            }
        }
    }
}
