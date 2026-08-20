package dev.anilbeesetti.nextplayer.core.database.entities

import androidx.room.ColumnInfo

/**
 * One file of one episode, with how far playback reached in it.
 *
 * [uri] is null for an episode whose files have not been written yet, and the playback columns are
 * null for a file that has never been played.
 */
data class ItemPlayback(
    @ColumnInfo(name = "item_id")
    val itemId: Long,
    @ColumnInfo(name = "season")
    val season: Int,
    @ColumnInfo(name = "episode")
    val episode: Int,
    @ColumnInfo(name = "uri")
    val uri: String?,
    @ColumnInfo(name = "playback_position")
    val playbackPosition: Long?,
    @ColumnInfo(name = "last_played_time")
    val lastPlayedTime: Long?,
)
