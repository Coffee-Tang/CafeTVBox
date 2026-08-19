package dev.anilbeesetti.nextplayer.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.anilbeesetti.nextplayer.core.database.entities.LiveSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LiveSourceDao {

    @Upsert
    suspend fun upsert(source: LiveSourceEntity): Long

    /** Oldest first: the order sources were added in is the order their lines are tried in. */
    @Query("SELECT * FROM live_source ORDER BY created_at ASC, id ASC")
    fun getAll(): Flow<List<LiveSourceEntity>>

    @Query("SELECT * FROM live_source WHERE id = :id")
    suspend fun getById(id: Long): LiveSourceEntity?

    @Query("DELETE FROM live_source WHERE id = :id")
    suspend fun deleteById(id: Long)
}
