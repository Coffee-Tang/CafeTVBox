package dev.anilbeesetti.nextplayer.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catalogue_library")
data class CatalogueLibraryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    /**
     * The folder this library is, as the connection that holds it names it. Together with
     * [connectionId] it is how a later scan finds the same folder again.
     */
    @ColumnInfo(name = "root")
    val root: String,
    /** `FILM` or `SERIES`. A library is one kind so a scan does not have to guess. */
    @ColumnInfo(name = "kind")
    val kind: String,
    /** The network connection that holds [root], and null when the folder is on the device. */
    @ColumnInfo(name = "connection_id")
    val connectionId: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
)
