package com.example.taptect.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.taptect.data.TapDatabase
import com.example.taptect.data.TapHistoryRepository
import com.example.taptect.data.TapRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TapHistoryRepository
    val allRecords: StateFlow<List<TapRecord>>

    init {
        val tapDao = TapDatabase.getDatabase(application).tapDao()
        repository = TapHistoryRepository(tapDao)
        allRecords = repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun insert(record: TapRecord) = viewModelScope.launch {
        repository.insert(record)
    }

    fun clearHistory() = viewModelScope.launch {
        repository.clearHistory()
    }
}
