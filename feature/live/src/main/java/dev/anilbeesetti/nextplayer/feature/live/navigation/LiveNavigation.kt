package dev.anilbeesetti.nextplayer.feature.live.navigation

import android.net.Uri
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
import dev.anilbeesetti.nextplayer.feature.live.screens.addsource.AddLiveSourceScreenRoute
import dev.anilbeesetti.nextplayer.feature.live.screens.addsource.AddLiveSourceViewModel
import dev.anilbeesetti.nextplayer.feature.live.screens.channels.LiveChannelsScreenRoute
import dev.anilbeesetti.nextplayer.feature.live.screens.channels.LiveChannelsViewModel
import dev.anilbeesetti.nextplayer.feature.live.screens.list.LiveScreenRoute
import kotlinx.serialization.Serializable

@Serializable
object LiveRoute : NavKey

@Serializable
data class AddLiveSourceRoute(val sourceId: Long? = null) : NavKey

@Serializable
data class LiveChannelsRoute(val sourceId: Long) : NavKey

fun NavBackStack<NavKey>.navigateToAddLiveSource(sourceId: Long? = null) {
    add(AddLiveSourceRoute(sourceId))
}

fun NavBackStack<NavKey>.navigateToLiveChannels(sourceId: Long) {
    add(LiveChannelsRoute(sourceId))
}

fun EntryProviderScope<NavKey>.liveEntry(
    onAddSource: () -> Unit,
    onEditSource: (sourceId: Long) -> Unit,
    onOpenSource: (sourceId: Long) -> Unit,
    onSettingsClick: () -> Unit,
) {
    entry<LiveRoute> {
        LiveScreenRoute(
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
    onPlayChannel: (uri: Uri, name: String) -> Unit,
) {
    entry<LiveChannelsRoute> { key ->
        LiveChannelsScreenRoute(
            onNavigateUp = onNavigateUp,
            onPlayChannel = onPlayChannel,
            viewModel = hiltViewModel<LiveChannelsViewModel, LiveChannelsViewModel.Factory>(
                creationCallback = { factory -> factory.create(key.sourceId) },
            ),
        )
    }
}
