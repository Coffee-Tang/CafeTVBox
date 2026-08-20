package dev.anilbeesetti.nextplayer.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "catalogue_item",
    foreignKeys = [
        ForeignKey(
            entity = CatalogueWorkEntity::class,
            parentColumns = ["id"],
            childColumns = ["work_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("work_id"),
        Index(value = ["work_id", "season", "episode"], unique = true),
    ],
)
/**
 * One episode of a work, or the single item a film is.
 *
 * The playback columns below are dead weight kept only because dropping a column means recreating
 * the table: playback state lives in `media_state`, keyed by file, and the catalogue reads it from
 * there so that history, the work page and the episode picker cannot disagree about where an
 * episode was left. Nothing reads these.
 */
data class CatalogueItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "work_id")
    val workId: Long,
    @ColumnInfo(name = "season")
    val season: Int,
    @ColumnInfo(name = "episode")
    val episode: Int,
    @ColumnInfo(name = "playback_position", defaultValue = "0")
    val playbackPosition: Long = 0,
    @ColumnInfo(name = "duration")
    val duration: Long? = null,
    @ColumnInfo(name = "last_played_time")
    val lastPlayedTime: Long? = null,
    /** The file of this item that was last played, and null when none has been. */
    @ColumnInfo(name = "last_file_uri")
    val lastFileUri: String? = null,
)
