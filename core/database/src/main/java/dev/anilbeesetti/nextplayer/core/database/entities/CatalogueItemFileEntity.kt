package dev.anilbeesetti.nextplayer.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "catalogue_item_file",
    foreignKeys = [
        ForeignKey(
            entity = CatalogueItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("item_id"),
        Index(value = ["uri"], unique = true),
    ],
)
data class CatalogueItemFileEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "item_id")
    val itemId: Long,
    @ColumnInfo(name = "uri")
    val uri: String,
    @ColumnInfo(name = "path")
    val path: String,
)
