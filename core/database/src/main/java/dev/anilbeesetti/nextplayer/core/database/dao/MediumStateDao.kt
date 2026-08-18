package dev.anilbeesetti.nextplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.anilbeesetti.nextplayer.core.database.entities.MediumStateEntity
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
     * The most recently played items, newest first.
     *
     * A row without a title predates playback history and has nothing to show, so it is left out
     * until the item is played again.
     */
    @Query(
        "SELECT * FROM media_state WHERE last_played_time IS NOT NULL AND title IS NOT NULL " +
            "ORDER BY last_played_time DESC LIMIT :limit",
    )
    fun getRecentlyPlayed(limit: Int): Flow<List<MediumStateEntity>>

    @Query("DELETE FROM media_state WHERE uri in (:uris)")
    suspend fun delete(uris: List<String>)
}
