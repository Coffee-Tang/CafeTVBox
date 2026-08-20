package dev.anilbeesetti.nextplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.anilbeesetti.nextplayer.core.database.entities.CatalogueItemEntity
import dev.anilbeesetti.nextplayer.core.database.entities.CatalogueItemFileEntity
import dev.anilbeesetti.nextplayer.core.database.entities.CatalogueLibraryEntity
import dev.anilbeesetti.nextplayer.core.database.entities.CatalogueWorkEntity
import dev.anilbeesetti.nextplayer.core.database.entities.ItemPlayback
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogueDao {

    @Upsert
    suspend fun upsertLibrary(library: CatalogueLibraryEntity): Long

    @Query("SELECT * FROM catalogue_library ORDER BY created_at ASC, id ASC")
    fun observeLibraries(): Flow<List<CatalogueLibraryEntity>>

    @Query("SELECT * FROM catalogue_library ORDER BY created_at ASC, id ASC")
    suspend fun getLibraries(): List<CatalogueLibraryEntity>

    @Query("SELECT * FROM catalogue_library WHERE id = :id")
    suspend fun getLibrary(id: Long): CatalogueLibraryEntity?

    @Query("DELETE FROM catalogue_library WHERE id = :id")
    suspend fun deleteLibrary(id: Long)

    @Query("SELECT * FROM catalogue_library WHERE connection_id = :connectionId AND root = :root")
    suspend fun findLibrary(connectionId: Long, root: String): CatalogueLibraryEntity?

    @Upsert
    suspend fun upsertWork(work: CatalogueWorkEntity): Long

    @Query("SELECT * FROM catalogue_work ORDER BY title COLLATE NOCASE")
    fun observeWorks(): Flow<List<CatalogueWorkEntity>>

    @Query("SELECT * FROM catalogue_work WHERE library_id = :libraryId")
    suspend fun getWorks(libraryId: Long): List<CatalogueWorkEntity>

    @Query("SELECT * FROM catalogue_work WHERE id = :id")
    fun observeWork(id: Long): Flow<CatalogueWorkEntity?>

    @Query("SELECT * FROM catalogue_work WHERE library_id = :libraryId AND kind = :kind AND work_key = :workKey")
    suspend fun findWork(libraryId: Long, kind: String, workKey: String): CatalogueWorkEntity?

    @Query("DELETE FROM catalogue_work WHERE library_id = :libraryId")
    suspend fun deleteWorksOfLibrary(libraryId: Long)

    @Query("DELETE FROM catalogue_work WHERE library_id = :libraryId AND id NOT IN (:keepIds)")
    suspend fun deleteWorksNotIn(libraryId: Long, keepIds: List<Long>)

    @Upsert
    suspend fun upsertItem(item: CatalogueItemEntity): Long

    @Query("SELECT * FROM catalogue_item WHERE id = :id")
    suspend fun getItem(id: Long): CatalogueItemEntity?

    @Query("SELECT * FROM catalogue_item WHERE work_id = :workId")
    suspend fun getItems(workId: Long): List<CatalogueItemEntity>

    @Query("SELECT * FROM catalogue_item WHERE work_id = :workId ORDER BY season ASC, episode ASC")
    fun observeItems(workId: Long): Flow<List<CatalogueItemEntity>>

    @Query("SELECT * FROM catalogue_item WHERE work_id = :workId AND season = :season AND episode = :episode")
    suspend fun findItem(workId: Long, season: Int, episode: Int): CatalogueItemEntity?

    @Query("DELETE FROM catalogue_item WHERE work_id = :workId AND id NOT IN (:keepIds)")
    suspend fun deleteItemsNotIn(workId: Long, keepIds: List<Long>)

    @Upsert
    suspend fun upsertFile(file: CatalogueItemFileEntity): Long

    @Query("SELECT * FROM catalogue_item_file WHERE item_id = :itemId")
    suspend fun getFiles(itemId: Long): List<CatalogueItemFileEntity>

    @Query("DELETE FROM catalogue_item_file WHERE item_id = :itemId")
    suspend fun deleteFilesOfItem(itemId: Long)

    /**
     * Every file of a work's episodes, with how far playback reached in it.
     *
     * The catalogue asks this rather than keeping playback state of its own, so that where an
     * episode was left is the same fact whether it is read from history, from the work page, or
     * from the episode picker. Emits again whenever any of the three tables changes.
     */
    @Query(
        "SELECT i.id AS item_id, i.season AS season, i.episode AS episode, f.uri AS uri, " +
            "ms.playback_position AS playback_position, ms.last_played_time AS last_played_time " +
            "FROM catalogue_item i " +
            "LEFT JOIN catalogue_item_file f ON f.item_id = i.id " +
            "LEFT JOIN media_state ms ON ms.uri = f.uri " +
            "WHERE i.work_id = :workId " +
            "ORDER BY i.season ASC, i.episode ASC",
    )
    fun observeWorkPlayback(workId: Long): Flow<List<ItemPlayback>>
}
