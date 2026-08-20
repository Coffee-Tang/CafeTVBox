package dev.anilbeesetti.nextplayer.feature.player

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.anilbeesetti.nextplayer.core.common.extensions.isTelevision
import dev.anilbeesetti.nextplayer.core.model.ControlButtonsPosition
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.model.WorkPickerDirection
import dev.anilbeesetti.nextplayer.core.model.shouldOpenWorkPickerOnDown
import dev.anilbeesetti.nextplayer.core.ui.R as coreUiR
import dev.anilbeesetti.nextplayer.core.ui.components.requestFocusUntilLanded
import dev.anilbeesetti.nextplayer.core.ui.components.thenIf
import dev.anilbeesetti.nextplayer.core.ui.extensions.copy
import dev.anilbeesetti.nextplayer.feature.player.buttons.NextButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayPauseButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButton
import dev.anilbeesetti.nextplayer.feature.player.buttons.PreviousButton
import dev.anilbeesetti.nextplayer.feature.player.extensions.explanationOrNull
import dev.anilbeesetti.nextplayer.feature.player.extensions.formatted
import dev.anilbeesetti.nextplayer.feature.player.extensions.nameRes
import dev.anilbeesetti.nextplayer.feature.player.state.ControlsVisibilityState
import dev.anilbeesetti.nextplayer.feature.player.state.LiveLinesState
import dev.anilbeesetti.nextplayer.feature.player.state.PlaybackFailureResponse
import dev.anilbeesetti.nextplayer.feature.player.state.VerticalGesture
import dev.anilbeesetti.nextplayer.feature.player.state.rememberBrightnessState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberControlsVisibilityState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberErrorState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberLiveLinesState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberLiveRetryState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberMediaPresentationState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberMetadataState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberPictureInPictureState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberRotationState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberSeekGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberTapGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberVideoZoomAndContentScaleState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberVolumeAndBrightnessGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberVolumeState
import dev.anilbeesetti.nextplayer.feature.player.state.responseToPlaybackFailure
import dev.anilbeesetti.nextplayer.feature.player.state.seekAmountFormatted
import dev.anilbeesetti.nextplayer.feature.player.state.seekToPositionFormated
import dev.anilbeesetti.nextplayer.feature.player.ui.DoubleTapIndicator
import dev.anilbeesetti.nextplayer.feature.player.ui.OverlayShowView
import dev.anilbeesetti.nextplayer.feature.player.ui.OverlayView
import dev.anilbeesetti.nextplayer.feature.player.ui.SubtitleConfiguration
import dev.anilbeesetti.nextplayer.feature.player.ui.VerticalProgressView
import dev.anilbeesetti.nextplayer.feature.player.ui.WorkPickerKeySink
import dev.anilbeesetti.nextplayer.feature.player.ui.controls.ControlsBottomView
import dev.anilbeesetti.nextplayer.feature.player.ui.controls.ControlsTopView
import dev.anilbeesetti.nextplayer.feature.player.ui.isBackKey
import dev.anilbeesetti.nextplayer.feature.player.ui.isWorkPickerConfirmKey
import dev.anilbeesetti.nextplayer.feature.player.ui.isWorkPickerHandledKey
import dev.anilbeesetti.nextplayer.feature.player.ui.setDescendantSurfaceFocusable
import dev.anilbeesetti.nextplayer.feature.player.ui.shouldDisableSurfaceFocus
import dev.anilbeesetti.nextplayer.feature.player.ui.shouldDismissOverlayOnBack
import dev.anilbeesetti.nextplayer.feature.player.ui.shouldForwardChromeKeys
import dev.anilbeesetti.nextplayer.feature.player.ui.shouldHandleHiddenPlayerKey
import dev.anilbeesetti.nextplayer.feature.player.ui.shouldHideControlsOnBack
import dev.anilbeesetti.nextplayer.feature.player.ui.shouldShowControlScrim
import dev.anilbeesetti.nextplayer.feature.player.ui.shouldSwallowDismissingBackUp
import dev.anilbeesetti.nextplayer.feature.player.ui.workPickerDirectionOf
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

val LocalControlsVisibilityState = compositionLocalOf<ControlsVisibilityState?> { null }

@OptIn(UnstableApi::class)
@Composable
fun MediaPlayerScreen(
    player: Player?,
    viewModel: PlayerViewModel,
    playerPreferences: PlayerPreferences,
    liveLines: List<String> = emptyList(),
    workId: Long? = null,
    openWorkPicker: Boolean = false,
    modifier: Modifier = Modifier,
    onSelectSubtitleClick: () -> Unit,
    onBackClick: () -> Unit,
    onPlayInBackgroundClick: () -> Unit,
) {
    val volumeState = rememberVolumeState(
        player = player,
        showVolumePanelIfHeadsetIsOn = playerPreferences.showSystemVolumePanel,
    )
    player ?: return
    val metadataState = rememberMetadataState(player)
    val mediaPresentationState = rememberMediaPresentationState(player)
    val controlsVisibilityState = rememberControlsVisibilityState(
        player = player,
        hideAfter = playerPreferences.controllerAutoHideTimeout.seconds,
    )
    val tapGestureState = rememberTapGestureState(
        player = player,
        doubleTapGesture = playerPreferences.doubleTapGesture,
        seekIncrementMillis = playerPreferences.seekIncrement.seconds.inWholeMilliseconds,
        useLongPressGesture = playerPreferences.useLongPressControls,
        longPressSpeed = playerPreferences.longPressControlsSpeed,
    )
    val seekGestureState = rememberSeekGestureState(
        player = player,
        sensitivity = playerPreferences.seekSensitivity,
        enableSeekGesture = playerPreferences.useSeekControls,
    )
    val pictureInPictureState = rememberPictureInPictureState(
        player = player,
        autoEnter = playerPreferences.autoPip,
    )
    val videoZoomAndContentScaleState = rememberVideoZoomAndContentScaleState(
        player = player,
        initialContentScale = playerPreferences.playerVideoZoom,
        enableZoomGesture = playerPreferences.useZoomControls,
        enablePanGesture = playerPreferences.enablePanGesture,
        onEvent = viewModel::onVideoZoomEvent,
    )
    val brightnessState = rememberBrightnessState()
    val volumeAndBrightnessGestureState = rememberVolumeAndBrightnessGestureState(
        volumeState = volumeState,
        brightnessState = brightnessState,
        enableVolumeGesture = playerPreferences.enableVolumeSwipeGesture,
        enableBrightnessGesture = playerPreferences.enableBrightnessSwipeGesture,
        volumeGestureSensitivity = playerPreferences.volumeGestureSensitivity,
        brightnessGestureSensitivity = playerPreferences.brightnessGestureSensitivity,
    )
    val rotationState = rememberRotationState(
        player = player,
        screenOrientation = playerPreferences.playerScreenOrientation,
    )
    val errorState = rememberErrorState(player = player)
    val liveLinesState = rememberLiveLinesState(
        player = player,
        lines = liveLines,
        onLinePlaying = { line ->
            player.currentMediaItem?.mediaId?.let { viewModel.rememberLine(mediaKey = it, line = line) }
        },
    )

    val liveRetryState = rememberLiveRetryState(player = player)
    val failureResponse = errorState.error?.let {
        responseToPlaybackFailure(
            isLive = mediaPresentationState.isLive,
            mayRetryLine = liveRetryState.mayRetryLine,
            hasAnotherLine = liveLinesState.hasAnotherLine,
        )
    }

    LaunchedEffect(errorState.error) {
        when (failureResponse) {
            PlaybackFailureResponse.RETRY_AT_LIVE_EDGE -> {
                errorState.dismiss()
                liveRetryState.retryAtLiveEdge()
            }
            PlaybackFailureResponse.SWITCH_LINE -> {
                if (liveLinesState.switchToNextLine()) errorState.dismiss()
            }
            PlaybackFailureResponse.GIVE_UP, null -> Unit
        }
    }

    // However a line was arrived at, it starts out owing the viewer nothing for the one before it.
    LaunchedEffect(liveLinesState.lineInUse) { liveRetryState.onLineChanged() }

    LaunchedEffect(pictureInPictureState.isInPictureInPictureMode) {
        if (pictureInPictureState.isInPictureInPictureMode) {
            controlsVisibilityState.hideControls()
        }
    }

    LaunchedEffect(tapGestureState.isLongPressGestureInAction) {
        if (tapGestureState.isLongPressGestureInAction) {
            controlsVisibilityState.hideControls()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (playerPreferences.rememberPlayerBrightness) {
            brightnessState.setBrightness(playerPreferences.playerBrightness)
        }
    }

    LaunchedEffect(brightnessState.currentBrightness) {
        if (playerPreferences.rememberPlayerBrightness) {
            viewModel.updatePlayerBrightness(brightnessState.currentBrightness)
        }
    }

    var overlayView by remember {
        mutableStateOf(if (openWorkPicker && workId != null) OverlayView.WORK_PICKER else null)
    }
    LaunchedEffect(overlayView) {
        if (overlayView == OverlayView.WORK_PICKER) {
            controlsVisibilityState.hideControls()
        }
    }

    val context = LocalContext.current
    val isTv = remember { context.isTelevision }
    val playPauseFocusRequester = remember { FocusRequester() }
    val seekBarFocusRequester = remember { FocusRequester() }
    val unlockFocusRequester = remember { FocusRequester() }
    var isPlayPauseFocused by remember { mutableStateOf(false) }
    var isUnlockFocused by remember { mutableStateOf(false) }
    val seekIncrementMs = playerPreferences.seekIncrement.seconds.inWholeMilliseconds

    val workPickerKeys = remember { WorkPickerKeySink() }
    val activity = LocalActivity.current as? PlayerActivity
    val composeView = LocalView.current
    val playerKeyDispatch = remember { PlayerKeyDispatch() }
    var swallowDismissingBackUp by remember { mutableStateOf(false) }

    var dpadSeekOffsetMs by remember { mutableLongStateOf(0L) }
    var dpadSeekTargetMs by remember { mutableLongStateOf(0L) }
    var dpadSeekActive by remember { mutableStateOf(false) }
    var dpadSeekTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(dpadSeekTick) {
        if (!dpadSeekActive) return@LaunchedEffect
        delay(1.seconds)
        dpadSeekActive = false
    }
    val showDpadSeekFeedback: (Long) -> Unit = { deltaMs ->
        if (!dpadSeekActive) dpadSeekOffsetMs = 0L
        dpadSeekOffsetMs += deltaMs
        dpadSeekTargetMs = player?.currentPosition ?: 0L
        dpadSeekActive = true
        dpadSeekTick++
    }

    SideEffect {
        playerKeyDispatch.handle = { event ->
            val isDown = event.action == android.view.KeyEvent.ACTION_DOWN
            val isUp = event.action == android.view.KeyEvent.ACTION_UP
            val direction = workPickerDirectionOf(event.keyCode)
            val hidden = shouldHandleHiddenPlayerKey(
                overlayOpen = overlayView != null,
                controlsVisible = controlsVisibilityState.controlsVisible,
            )
            when {
                shouldSwallowDismissingBackUp(
                    swallowArmed = swallowDismissingBackUp,
                    isBack = isBackKey(event.keyCode),
                    isActionUp = isUp,
                ) -> {
                    swallowDismissingBackUp = false
                    true
                }
                shouldDismissOverlayOnBack(overlayOpen = overlayView != null) &&
                    isBackKey(event.keyCode) -> {
                    if (isDown) {
                        overlayView = null
                        swallowDismissingBackUp = true
                    }
                    true
                }
                overlayView == OverlayView.WORK_PICKER -> {
                    workPickerKeys.onKeyCode(event.keyCode, isDown)
                }
                shouldHideControlsOnBack(
                    overlayOpen = overlayView != null,
                    controlsVisible = controlsVisibilityState.controlsVisible,
                ) &&
                    isBackKey(event.keyCode) -> {
                    if (isDown) controlsVisibilityState.hideControls()
                    true
                }
                hidden &&
                    shouldOpenWorkPickerOnDown(
                        controlsVisible = false,
                        hasWork = workId != null,
                    ) &&
                    direction == WorkPickerDirection.DOWN -> {
                    if (isDown) {
                        controlsVisibilityState.hideControls()
                        overlayView = OverlayView.WORK_PICKER
                    }
                    true
                }
                hidden && isWorkPickerConfirmKey(event.keyCode) -> {
                    if (isDown) {
                        player?.let { if (it.isPlaying) it.pause() else it.play() }
                    }
                    true
                }
                hidden && direction == WorkPickerDirection.UP -> {
                    if (isDown) controlsVisibilityState.showControls()
                    true
                }
                hidden &&
                    (direction == WorkPickerDirection.LEFT || direction == WorkPickerDirection.RIGHT) -> {
                    if (isDown) {
                        player?.let { currentPlayer ->
                            val deltaMs = if (direction == WorkPickerDirection.LEFT) {
                                -seekIncrementMs
                            } else {
                                seekIncrementMs
                            }
                            val duration = currentPlayer.duration
                            val target = (currentPlayer.currentPosition + deltaMs).coerceAtLeast(0)
                            currentPlayer.seekTo(
                                if (duration > 0) target.coerceAtMost(duration) else target,
                            )
                            showDpadSeekFeedback(deltaMs)
                        }
                    }
                    true
                }
                shouldForwardChromeKeys(
                    overlayIsWorkPicker = overlayView == OverlayView.WORK_PICKER,
                    chromeVisible = controlsVisibilityState.controlsVisible || overlayView != null,
                ) &&
                    isWorkPickerHandledKey(event.keyCode) &&
                    !isWorkPickerConfirmKey(event.keyCode) -> {
                    composeView.dispatchKeyEvent(event)
                    true
                }
                else -> false
            }
        }
    }
    DisposableEffect(activity) {
        activity?.dispatchKeyInterceptor = { playerKeyDispatch.handle(it) }
        onDispose { activity?.dispatchKeyInterceptor = null }
    }

    val chromeTakesSurfaceFocus = shouldDisableSurfaceFocus(
        overlayOpen = overlayView != null,
        controlsVisible = controlsVisibilityState.controlsVisible,
    )
    SideEffect {
        setDescendantSurfaceFocusable(composeView, focusable = !chromeTakesSurfaceFocus)
    }
    LaunchedEffect(chromeTakesSurfaceFocus, composeView) {
        repeat(10) {
            if (setDescendantSurfaceFocusable(composeView, focusable = !chromeTakesSurfaceFocus) > 0) {
                return@LaunchedEffect
            }
            delay(50)
        }
    }

    if (isTv) {
        LaunchedEffect(controlsVisibilityState.controlsVisible, controlsVisibilityState.controlsLocked, overlayView) {
            if (overlayView != null || !controlsVisibilityState.controlsVisible) return@LaunchedEffect
            val locked = controlsVisibilityState.controlsLocked
            val target = if (locked) unlockFocusRequester else playPauseFocusRequester
            target.requestFocusUntilLanded(attempts = 20) { if (locked) isUnlockFocused else isPlayPauseFocused }
        }
    }

    CompositionLocalProvider(LocalControlsVisibilityState provides controlsVisibilityState) {
        Box {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black),
            ) {
                PlayerContentFrame(
                    player = player,
                    pictureInPictureState = pictureInPictureState,
                    controlsVisibilityState = controlsVisibilityState,
                    tapGestureState = tapGestureState,
                    seekGestureState = seekGestureState,
                    videoZoomAndContentScaleState = videoZoomAndContentScaleState,
                    volumeAndBrightnessGestureState = volumeAndBrightnessGestureState,
                    subtitleConfiguration = SubtitleConfiguration(
                        useSystemCaptionStyle = playerPreferences.useSystemCaptionStyle,
                        showBackground = playerPreferences.subtitleBackground,
                        font = playerPreferences.subtitleFont,
                        textSize = playerPreferences.subtitleTextSize,
                        textBold = playerPreferences.subtitleTextBold,
                        applyEmbeddedStyles = playerPreferences.applyEmbeddedStyles,
                    ),
                )

                val showControlScrim = shouldShowControlScrim(
                    controlsVisible = controlsVisibilityState.controlsVisible,
                    controlsLocked = controlsVisibilityState.controlsLocked,
                    overlayOpen = overlayView != null,
                )
                AnimatedVisibility(
                    visible = showControlScrim,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                    )
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    if (mediaPresentationState.isBuffering) {
                        CircularProgressIndicator(modifier = Modifier.size(72.dp))
                    }
                    LineNote(liveLinesState = liveLinesState)
                }

                DoubleTapIndicator(tapGestureState = tapGestureState)

                DpadSeekIndicator(
                    visible = dpadSeekActive && dpadSeekOffsetMs != 0L,
                    offsetMs = dpadSeekOffsetMs,
                    positionMs = dpadSeekTargetMs,
                )

                AnimatedVisibility(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .align(Alignment.TopCenter),
                    visible = tapGestureState.isLongPressGestureInAction,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Surface(shape = CircleShape) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                        ) {
                            Text(
                                text = stringResource(coreUiR.string.fast_playback_speed, tapGestureState.longPressSpeed),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }

                if (controlsVisibilityState.controlsVisible && controlsVisibilityState.controlsLocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .safeDrawingPadding()
                            .padding(top = 24.dp),
                    ) {
                        PlayerButton(
                            modifier = Modifier.thenIf(isTv) {
                                focusRequester(unlockFocusRequester)
                                    .onFocusChanged { isUnlockFocused = it.hasFocus }
                            },
                            containerColor = Color.Black.copy(0.5f),
                            onClick = { controlsVisibilityState.unlockControls() },
                        ) {
                            Icon(
                                painter = painterResource(coreUiR.drawable.ic_lock),
                                contentDescription = stringResource(coreUiR.string.controls_unlock),
                            )
                        }
                    }
                } else {
                    PlayerControlsView(
                        topView = {
                            AnimatedVisibility(
                                visible = controlsVisibilityState.controlsVisible && overlayView == null,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                ControlsTopView(
                                    title = metadataState.title ?: "",
                                    isLive = mediaPresentationState.isLive,
                                    lineCount = liveLinesState.lineCount,
                                    onLinesClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.LIVE_LINES
                                    },
                                    onAudioClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.AUDIO_SELECTOR
                                    },
                                    onSubtitleClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.SUBTITLE_SELECTOR
                                    },
                                    onPlaybackSpeedClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.PLAYBACK_SPEED
                                    },
                                    onPlaylistClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = if (workId != null) {
                                            OverlayView.WORK_PICKER
                                        } else {
                                            OverlayView.PLAYLIST
                                        }
                                    },
                                    onBackClick = onBackClick,
                                )
                            }
                        },
                        middleView = {
                            when {
                                seekGestureState.seekAmount != null -> InfoView(info = "${seekGestureState.seekAmountFormatted}\n[${seekGestureState.seekToPositionFormated}]")
                                videoZoomAndContentScaleState.isZooming -> InfoView(info = "${(videoZoomAndContentScaleState.zoom * 100).toInt()}%")
                                videoZoomAndContentScaleState.showContentScaleIndicator -> InfoView(info = stringResource(videoZoomAndContentScaleState.videoContentScale.nameRes()))
                                controlsVisibilityState.controlsVisible && overlayView == null -> ControlsMiddleView(
                                    player = player,
                                    isLive = mediaPresentationState.isLive,
                                    playPauseModifier = Modifier.thenIf(isTv) {
                                        focusRequester(playPauseFocusRequester)
                                            .onFocusChanged { isPlayPauseFocused = it.hasFocus }
                                    },
                                )
                                else -> Unit
                            }
                        },
                        bottomView = {
                            AnimatedVisibility(
                                visible = controlsVisibilityState.controlsVisible &&
                                    !controlsVisibilityState.controlsLocked &&
                                    overlayView == null,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                val context = LocalContext.current
                                ControlsBottomView(
                                    player = player,
                                    mediaPresentationState = mediaPresentationState,
                                    controlsAlignment = when (playerPreferences.controlButtonsPosition) {
                                        ControlButtonsPosition.LEFT -> Alignment.Start
                                        ControlButtonsPosition.RIGHT -> Alignment.End
                                    },
                                    videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                                    isPipSupported = pictureInPictureState.isPipSupported,
                                    isLive = mediaPresentationState.isLive,
                                    seekBarModifier = Modifier.thenIf(isTv) {
                                        focusRequester(seekBarFocusRequester)
                                            .focusProperties { up = playPauseFocusRequester }
                                    },
                                    onSeek = seekGestureState::onSeek,
                                    onSeekEnd = seekGestureState::onSeekEnd,
                                    onRotateClick = rotationState::rotate,
                                    onPlayInBackgroundClick = onPlayInBackgroundClick,
                                    onLockControlsClick = {
                                        controlsVisibilityState.showControls()
                                        controlsVisibilityState.lockControls()
                                    },
                                    onVideoContentScaleClick = {
                                        controlsVisibilityState.showControls()
                                        videoZoomAndContentScaleState.switchToNextVideoContentScale()
                                    },
                                    onVideoContentScaleLongClick = {
                                        controlsVisibilityState.hideControls()
                                        overlayView = OverlayView.VIDEO_CONTENT_SCALE
                                    },
                                    onPictureInPictureClick = {
                                        if (!pictureInPictureState.hasPipPermission) {
                                            Toast.makeText(context, coreUiR.string.enable_pip_from_settings, Toast.LENGTH_SHORT).show()
                                            pictureInPictureState.openPictureInPictureSettings()
                                        } else {
                                            pictureInPictureState.enterPictureInPictureMode()
                                        }
                                    },
                                )
                            }
                        },
                    )
                }

                val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .displayCutoutPadding()
                        .padding(systemBarsPadding.copy(top = 0.dp, bottom = 0.dp))
                        .padding(24.dp),
                ) {
                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.CenterStart),
                        visible = volumeAndBrightnessGestureState.activeGesture == VerticalGesture.VOLUME,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        VerticalProgressView(
                            value = volumeState.volumePercentage,
                            maxValue = volumeState.maxVolumePercentage,
                            icon = painterResource(coreUiR.drawable.ic_volume),
                        )
                    }

                    AnimatedVisibility(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        visible = volumeAndBrightnessGestureState.activeGesture == VerticalGesture.BRIGHTNESS,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        VerticalProgressView(
                            value = brightnessState.brightnessPercentage,
                            icon = painterResource(coreUiR.drawable.ic_brightness),
                        )
                    }
                }
            }

            OverlayShowView(
                player = player,
                overlayView = overlayView,
                videoContentScale = videoZoomAndContentScaleState.videoContentScale,
                lineCount = liveLinesState.lineCount,
                lineInUse = liveLinesState.lineInUse,
                onDismiss = { overlayView = null },
                onSelectSubtitleClick = onSelectSubtitleClick,
                onSubtitleOptionEvent = viewModel::onSubtitleOptionEvent,
                onVideoContentScaleChanged = { videoZoomAndContentScaleState.onVideoContentScaleChanged(it) },
                onLineClick = { liveLinesState.switchToLine(it) },
                workId = workId,
                viewModel = viewModel,
                workPickerKeys = workPickerKeys,
            )
        }
    }

    // A channel still being asked again, or with a line left to try, is being seen to; saying it
    // failed while either is under way would be premature.
    errorState.error?.takeIf { failureResponse == PlaybackFailureResponse.GIVE_UP }?.let { error ->
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(text = stringResource(coreUiR.string.error_playing_video))
            },
            text = {
                val explanation = error.explanationOrNull(
                    context = context,
                    mediaUri = player.currentMediaItem?.localConfiguration?.uri,
                )
                Text(
                    text = explanation?.let { stringResource(it) }
                        ?: error.message
                        ?: stringResource(coreUiR.string.unknown_error),
                )
            },
            confirmButton = {
                if (player.hasNextMediaItem()) {
                    TextButton(
                        onClick = {
                            errorState.dismiss()
                            player.seekToNext()
                            player.play()
                        },
                    ) {
                        Text(text = stringResource(coreUiR.string.play_next_video))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        errorState.dismiss()
                        onBackClick()
                    },
                ) {
                    Text(text = stringResource(coreUiR.string.exit))
                }
            },
        )
    }

    BackHandler {
        when {
            overlayView != null -> overlayView = null
            isTv && controlsVisibilityState.controlsVisible -> controlsVisibilityState.hideControls()
            else -> onBackClick()
        }
    }
}

@Composable
fun InfoView(
    modifier: Modifier = Modifier,
    info: String,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = info,
            style = textStyle,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Shows the cumulative amount skipped by repeated D-pad left/right seeks while the controls are
 * hidden, along with the resulting position. Fades out shortly after the last seek.
 */
/**
 * Says which of a channel's lines is being tried, so that a wait reads as an attempt rather than as
 * the player having stopped caring.
 */
@OptIn(UnstableApi::class)
@Composable
private fun LineNote(liveLinesState: LiveLinesState) {
    val note = when {
        liveLinesState.hasGivenUp -> stringResource(coreUiR.string.live_no_line_reachable)
        liveLinesState.isSwitching -> stringResource(
            coreUiR.string.live_trying_line,
            liveLinesState.lineInUse,
            liveLinesState.lineCount,
        )
        else -> null
    } ?: return

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Black.copy(alpha = 0.6f),
    ) {
        Text(
            text = note,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
        )
    }
}

@Composable
fun BoxScope.DpadSeekIndicator(
    visible: Boolean,
    offsetMs: Long,
    positionMs: Long,
) {
    AnimatedVisibility(
        modifier = Modifier.align(Alignment.Center),
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.6f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    painter = painterResource(coreUiR.drawable.ic_fast),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(if (offsetMs < 0) 180f else 0f),
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${if (offsetMs >= 0) "+" else "-"}${abs(offsetMs).milliseconds.inWholeSeconds}s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Text(
                        text = positionMs.milliseconds.formatted(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
fun ControlsMiddleView(
    modifier: Modifier = Modifier,
    player: Player,
    isLive: Boolean = false,
    playPauseModifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(40.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A channel is opened on its own, so there is nothing to step through.
        if (!isLive) {
            PreviousButton(player = player)
        }
        PlayPauseButton(player = player, modifier = playPauseModifier)
        if (!isLive) {
            NextButton(player = player)
        }
    }
}

private class PlayerKeyDispatch {
    var handle: (android.view.KeyEvent) -> Boolean = { false }
}

@Composable
fun PlayerControlsView(
    modifier: Modifier = Modifier,
    topView: @Composable () -> Unit,
    middleView: @Composable BoxScope.() -> Unit,
    bottomView: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            topView()
            Spacer(modifier = Modifier.weight(1f))
            bottomView()
        }

        middleView()
    }
}
