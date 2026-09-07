package com.example.taptect.data

import kotlinx.coroutines.flow.Flow

class TapHistoryRepository(private val tapDao: TapDao) {
    val allRecords: Flow<List<TapRecord>> = tapDao.getAllRecords()

    suspend fun insert(record: TapRecord) {
        tapDao.insertRecord(record)
    }

    suspend fun clearHistory() {
        tapDao.deleteAll()
    }
}
