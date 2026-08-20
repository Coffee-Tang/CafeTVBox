package dev.anilbeesetti.nextplayer.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Embedded

/**
 * A played item together with the work it turned out to be part of.
 *
 * Playback state is kept per file, because that is what a position belongs to, while history is
 * read per work. The work columns are null for anything no library has catalogued: a live channel,
 * a URL played once, a file outside every library.
 */
data class RecentPlayback(
    @Embedded
    val state: MediumStateEntity,
    @ColumnInfo(name = "work_id")
    val workId: Long?,
    @ColumnInfo(name = "work_title")
    val workTitle: String?,
    @ColumnInfo(name = "work_kind")
    val workKind: String?,
    @ColumnInfo(name = "season")
    val season: Int?,
    @ColumnInfo(name = "episode")
    val episode: Int?,
)
