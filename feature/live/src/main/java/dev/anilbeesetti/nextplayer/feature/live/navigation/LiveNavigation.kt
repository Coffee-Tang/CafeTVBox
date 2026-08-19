package dev.anilbeesetti.nextplayer.feature.live.navigation

import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.metadata
import androidx.navigation3.ui.NavDisplay
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.feature.live.screens.addsource.AddLiveSourceScreenRoute
import dev.anilbeesetti.nextplayer.feature.live.screens.addsource.AddLiveSourceViewModel
import dev.anilbeesetti.nextplayer.feature.live.screens.channels.LiveChannelsScreenRoute
import dev.anilbeesetti.nextplayer.feature.live.screens.channels.LiveChannelsViewModel
import dev.anilbeesetti.nextplayer.feature.live.screens.list.LiveScreenRoute
import kotlinx.serialization.Serializable

@Serializable
object LiveRoute : NavKey

@Serializable
object LiveSourcesRoute : NavKey

@Serializable
data class AddLiveSourceRoute(val sourceId: Long? = null) : NavKey

@Serializable
data class LiveChannelsRoute(val sourceId: Long) : NavKey

fun NavBackStack<NavKey>.navigateToLiveSources() {
    add(LiveSourcesRoute)
}

fun NavBackStack<NavKey>.navigateToAddLiveSource(sourceId: Long? = null) {
    add(AddLiveSourceRoute(sourceId))
}

fun NavBackStack<NavKey>.navigateToLiveChannels(sourceId: Long) {
    add(LiveChannelsRoute(sourceId))
}

/** The Live tab itself: every channel the configured sources offer between them. */
fun EntryProviderScope<NavKey>.liveEntry(
    onManageSources: () -> Unit,
    onSettingsClick: () -> Unit,
    onPlayChannel: (LiveChannel) -> Unit,
) {
    entry<LiveRoute> {
        LiveChannelsScreenRoute(
            onNavigateUp = null,
            onManageSources = onManageSources,
            onSettingsClick = onSettingsClick,
            onPlayChannel = onPlayChannel,
            viewModel = hiltViewModel<LiveChannelsViewModel, LiveChannelsViewModel.Factory>(
                creationCallback = { factory -> factory.create(null) },
            ),
        )
    }
}

fun EntryProviderScope<NavKey>.liveSourcesEntry(
    onNavigateUp: () -> Unit,
    onAddSource: () -> Unit,
    onEditSource: (sourceId: Long) -> Unit,
    onOpenSource: (sourceId: Long) -> Unit,
    onSettingsClick: () -> Unit,
) {
    entry<LiveSourcesRoute> {
        LiveScreenRoute(
            onNavigateUp = onNavigateUp,
            onAddSource = onAddSource,
            onEditSource = onEditSource,
            onOpenSource = onOpenSource,
            onSettingsClick = onSettingsClick,
        )
    }
}

fun EntryProviderScope<NavKey>.addLiveSourceEntry(
    onNavigateUp: () -> Unit,
) {
    entry<AddLiveSourceRoute>(
        metadata = metadata {
            put(NavDisplay.TransitionKey) {
                slideInVertically { it } togetherWith scaleOut(targetScale = 0.95f)
            }
            put(NavDisplay.PopTransitionKey) {
                scaleIn(initialScale = 0.95f) togetherWith slideOutVertically { it }
            }
            put(NavDisplay.PredictivePopTransitionKey) {
                scaleIn(initialScale = 0.95f) togetherWith slideOutVertically { it }
            }
        },
    ) { key ->
        AddLiveSourceScreenRoute(
            onNavigateUp = onNavigateUp,
            viewModel = hiltViewModel<AddLiveSourceViewModel, AddLiveSourceViewModel.Factory>(
                creationCallback = { factory -> factory.create(key.sourceId) },
            ),
        )
    }
}

fun EntryProviderScope<NavKey>.liveChannelsEntry(
    onNavigateUp: () -> Unit,
    onPlayChannel: (LiveChannel) -> Unit,
) {
    entry<LiveChannelsRoute> { key ->
        LiveChannelsScreenRoute(
            onNavigateUp = onNavigateUp,
            onManageSources = null,
            onSettingsClick = null,
            onPlayChannel = onPlayChannel,
            viewModel = hiltViewModel<LiveChannelsViewModel, LiveChannelsViewModel.Factory>(
                creationCallback = { factory -> factory.create(key.sourceId) },
            ),
        )
    }
}
