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

    @Query("SELECT * FROM live_source ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<LiveSourceEntity>>

    @Query("SELECT * FROM live_source WHERE id = :id")
    suspend fun getById(id: Long): LiveSourceEntity?

    @Query("DELETE FROM live_source WHERE id = :id")
    suspend fun deleteById(id: Long)
}
