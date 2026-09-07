package com.example.taptect.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TapDao {
    @Query("SELECT * FROM tap_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<TapRecord>>

    @Insert
    suspend fun insertRecord(record: TapRecord)

    @Query("DELETE FROM tap_records")
    suspend fun deleteAll()
}
