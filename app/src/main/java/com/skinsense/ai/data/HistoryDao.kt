package com.skinsense.ai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(history: HistoryEntity)

    @Query("SELECT * FROM scan_history WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAll(userId: String): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE id = :id")
    suspend fun getById(id: Int): HistoryEntity?

    @Query("DELETE FROM scan_history WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM scan_history WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}
