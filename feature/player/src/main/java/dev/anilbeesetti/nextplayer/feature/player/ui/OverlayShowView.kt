package dev.anilbeesetti.nextplayer.feature.player.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.media3.common.Player
import dev.anilbeesetti.nextplayer.core.model.VideoContentScale
import dev.anilbeesetti.nextplayer.feature.player.PlayerViewModel
import dev.anilbeesetti.nextplayer.feature.player.extensions.noRippleClickable
import dev.anilbeesetti.nextplayer.feature.player.state.SubtitleOptionsEvent

@Composable
fun BoxScope.OverlayShowView(
    player: Player,
    overlayView: OverlayView?,
    videoContentScale: VideoContentScale,
    lineCount: Int = 0,
    lineInUse: Int = 1,
    onDismiss: () -> Unit = {},
    onSelectSubtitleClick: () -> Unit = {},
    onSubtitleOptionEvent: (SubtitleOptionsEvent) -> Unit = {},
    onVideoContentScaleChanged: (VideoContentScale) -> Unit = {},
    onLineClick: (Int) -> Unit = {},
    workId: Long? = null,
    viewModel: PlayerViewModel? = null,
    workPickerKeys: WorkPickerKeySink? = null,
) {
    if (overlayView != null) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .noRippleClickable(onClick = onDismiss)
                .focusProperties { canFocus = false },
        )
    }

    AudioTrackSelectorView(
        show = overlayView == OverlayView.AUDIO_SELECTOR,
        player = player,
        onDismiss = onDismiss,
    )

    SubtitleSelectorView(
        show = overlayView == OverlayView.SUBTITLE_SELECTOR,
        player = player,
        onSelectSubtitleClick = onSelectSubtitleClick,
        onEvent = onSubtitleOptionEvent,
        onDismiss = onDismiss,
    )

    PlaybackSpeedSelectorView(
        show = overlayView == OverlayView.PLAYBACK_SPEED,
        player = player,
    )

    VideoContentScaleSelectorView(
        show = overlayView == OverlayView.VIDEO_CONTENT_SCALE,
        videoContentScale = videoContentScale,
        onVideoContentScaleChanged = onVideoContentScaleChanged,
        onDismiss = onDismiss,
    )

    PlaylistView(
        show = overlayView == OverlayView.PLAYLIST,
        player = player,
    )

    LiveLineSelectorView(
        show = overlayView == OverlayView.LIVE_LINES,
        lineCount = lineCount,
        lineInUse = lineInUse,
        onLineClick = onLineClick,
        onDismiss = onDismiss,
    )

    if (workId != null && viewModel != null) {
        WorkPickerView(
            show = overlayView == OverlayView.WORK_PICKER,
            workId = workId,
            player = player,
            viewModel = viewModel,
            onDismiss = onDismiss,
            keySink = workPickerKeys,
        )
    }
}

val Configuration.isPortrait: Boolean
    get() = orientation == Configuration.ORIENTATION_PORTRAIT

enum class OverlayView {
    AUDIO_SELECTOR,
    SUBTITLE_SELECTOR,
    PLAYBACK_SPEED,
    VIDEO_CONTENT_SCALE,
    PLAYLIST,
    LIVE_LINES,
    WORK_PICKER,
}
