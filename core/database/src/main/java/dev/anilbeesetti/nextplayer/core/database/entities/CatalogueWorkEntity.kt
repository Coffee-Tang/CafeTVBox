package dev.anilbeesetti.nextplayer.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "catalogue_work",
    foreignKeys = [
        ForeignKey(
            entity = CatalogueLibraryEntity::class,
            parentColumns = ["id"],
            childColumns = ["library_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("library_id"),
        Index(value = ["library_id", "kind", "work_key"], unique = true),
    ],
)
data class CatalogueWorkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "library_id")
    val libraryId: Long,
    @ColumnInfo(name = "work_key")
    val workKey: String,
    @ColumnInfo(name = "kind")
    val kind: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "other_title")
    val otherTitle: String? = null,
    @ColumnInfo(name = "year")
    val year: Int? = null,
    /** The catalogue's id for this work, and null until a match is bound. */
    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int? = null,
    @ColumnInfo(name = "poster_path")
    val posterPath: String? = null,
    @ColumnInfo(name = "overview")
    val overview: String? = null,
    /**
     * `AUTO` when a unique certain match bound it, `USER` when the viewer did, and null when
     * nothing has been bound. A user binding is not overwritten by a later scan.
     */
    @ColumnInfo(name = "bound_by")
    val boundBy: String? = null,
)
