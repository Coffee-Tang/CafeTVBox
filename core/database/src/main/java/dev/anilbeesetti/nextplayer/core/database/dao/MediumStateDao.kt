package dev.anilbeesetti.nextplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.anilbeesetti.nextplayer.core.database.entities.MediumStateEntity
import dev.anilbeesetti.nextplayer.core.database.entities.RecentPlayback
import kotlinx.coroutines.flow.Flow

@Dao
interface MediumStateDao {

    @Upsert
    suspend fun upsert(mediumState: MediumStateEntity)

    @Upsert
    suspend fun upsertAll(mediaStates: List<MediumStateEntity>)

    @Query("SELECT * FROM media_state WHERE uri = :uri")
    suspend fun get(uri: String): MediumStateEntity?

    @Query("SELECT * FROM media_state WHERE uri = :uri")
    fun getAsFlow(uri: String): Flow<MediumStateEntity?>

    @Query("SELECT * FROM media_state")
    fun getAll(): Flow<List<MediumStateEntity>>

    /**
     * The most recently played items, newest first, each with the work it belongs to when the
     * catalogue knows of it.
     *
     * A row without a title predates playback history and has nothing to show, so it is left out
     * until the item is played again. No limit is applied here because episodes of one work fold
     * into a single entry afterwards, and a limit taken before that would count them separately.
     */
    @Query(
        "SELECT ms.*, w.id AS work_id, w.title AS work_title, w.kind AS work_kind, " +
            "i.season AS season, i.episode AS episode " +
            "FROM media_state ms " +
            "LEFT JOIN catalogue_item_file f ON f.uri = ms.uri " +
            "LEFT JOIN catalogue_item i ON i.id = f.item_id " +
            "LEFT JOIN catalogue_work w ON w.id = i.work_id " +
            "WHERE ms.last_played_time IS NOT NULL AND ms.title IS NOT NULL " +
            "ORDER BY ms.last_played_time DESC",
    )
    fun getRecentlyPlayed(): Flow<List<RecentPlayback>>

    @Query("DELETE FROM media_state WHERE uri in (:uris)")
    suspend fun delete(uris: List<String>)

    /**
     * Forgets every episode of the work [uri] belongs to, and reports how many rows went.
     *
     * History shows a work once, so forgetting that one entry has to forget the whole work;
     * otherwise the episode before it would simply take its place. Nothing is deleted, and zero
     * returned, when [uri] belongs to no work the catalogue knows.
     */
    @Query(
        "DELETE FROM media_state WHERE uri IN (" +
            "SELECT f.uri FROM catalogue_item_file f " +
            "JOIN catalogue_item i ON i.id = f.item_id " +
            "WHERE i.work_id = (" +
            "SELECT i2.work_id FROM catalogue_item_file f2 " +
            "JOIN catalogue_item i2 ON i2.id = f2.item_id " +
            "WHERE f2.uri = :uri" +
            ")" +
            ")",
    )
    suspend fun deleteWorkOf(uri: String): Int

    /**
     * Forgets every item that has been played, along with where playback reached in it.
     *
     * State that was never played, such as a track choice made without watching, is left alone.
     */
    @Query("DELETE FROM media_state WHERE last_played_time IS NOT NULL")
    suspend fun deleteAllPlayed()
}
