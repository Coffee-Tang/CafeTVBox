package dev.anilbeesetti.nextplayer.core.data.live

import dev.anilbeesetti.nextplayer.core.model.LiveProgramme
import dev.anilbeesetti.nextplayer.core.model.channelKey

/**
 * Arranges the entries of a guide under [channelKey], leaving out the filler ones.
 *
 * The key is what lets a playlist find its channels here, as a guide names a station its own way.
 *
 * Guides pad the channels they hold no listings for with an hourly entry that names no programme.
 * Dropping it leaves such a channel looking empty, which is honest, rather than showing a title
 * that carries no information.
 */
internal fun List<LiveProgramme>.toGuide(): Map<String, List<LiveProgramme>> =
    filterNot { it.isFiller() }.groupBy { channelKey(it.channelName) }

private fun LiveProgramme.isFiller(): Boolean =
    title in fillerTitles || fillerMarkers.any { title.contains(it) }

private val fillerTitles = setOf("精彩节目", "无节目信息")
private val fillerMarkers = listOf("暂未提供", "暂无节目")
