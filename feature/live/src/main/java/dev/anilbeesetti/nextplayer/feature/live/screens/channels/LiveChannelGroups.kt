package dev.anilbeesetti.nextplayer.feature.live.screens.channels

import androidx.annotation.StringRes
import dev.anilbeesetti.nextplayer.core.model.ChannelCategory
import dev.anilbeesetti.nextplayer.core.model.LiveChannel
import dev.anilbeesetti.nextplayer.core.model.channelCategory
import dev.anilbeesetti.nextplayer.core.ui.R

/** Groups channels by `group-title`, keeping the order in which groups appear in the playlist. */
internal fun playlistGroups(channels: List<LiveChannel>): List<LiveChannelGroup> =
    channels.groupBy { it.group }
        .map { (group, grouped) -> LiveChannelGroup(name = group, channels = grouped) }

/**
 * Groups the channels of every source by the kind of station they are.
 *
 * Sources disagree about their own `group-title`s, so keeping them would leave dozens of rows in
 * two languages describing overlapping sets of channels. The kinds read from the names are few and
 * mean the same thing whichever source a channel came from. Every channel is listed first so that a
 * channel a category places oddly is still one row away, and a category no channel belongs to is
 * left out.
 */
internal fun categoryGroups(channels: List<LiveChannel>): List<LiveChannelGroup> {
    if (channels.isEmpty()) return emptyList()
    val byCategory = channels.groupBy { channelCategory(it.name) }
    val categories = ChannelCategory.entries.mapNotNull { category ->
        byCategory[category]?.let { grouped ->
            LiveChannelGroup(name = "", labelRes = category.labelRes, channels = grouped)
        }
    }
    return listOf(
        LiveChannelGroup(name = "", labelRes = R.string.live_category_all, channels = channels),
    ) + categories
}

@get:StringRes
private val ChannelCategory.labelRes: Int
    get() = when (this) {
        ChannelCategory.CCTV -> R.string.live_category_cctv
        ChannelCategory.SATELLITE -> R.string.live_category_satellite
        ChannelCategory.OTHER -> R.string.live_category_other
    }
