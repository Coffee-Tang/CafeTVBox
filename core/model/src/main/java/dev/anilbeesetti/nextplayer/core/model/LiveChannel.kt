package dev.anilbeesetti.nextplayer.core.model

import java.io.Serializable

/**
 * A single live TV channel parsed from an m3u playlist.
 *
 * [group] comes from the `group-title` attribute and is used to categorise channels. [logoUrl] is
 * the `tvg-logo` attribute when present.
 */
data class LiveChannel(
    val name: String,
    val url: String,
    val group: String = "",
    val logoUrl: String? = null,
    val tvgId: String? = null,
) : Serializable {

    companion object {
        const val UNGROUPED = ""
    }
}
