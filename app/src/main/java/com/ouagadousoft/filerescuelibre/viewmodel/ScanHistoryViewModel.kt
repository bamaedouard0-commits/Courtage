package com.ouagadousoft.filerescuelibre.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ouagadousoft.filerescuelibre.data.history.AppDatabase
import com.ouagadousoft.filerescuelibre.data.history.ScanHistoryRepositoryImpl
import com.ouagadousoft.filerescuelibre.domain.model.ScanHistoryEntry
import com.ouagadousoft.filerescuelibre.domain.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScanHistoryViewModel @JvmOverloads constructor(
    application: Application,
    private val scanHistoryRepository: ScanHistoryRepository =
        ScanHistoryRepositoryImpl(AppDatabase.getInstance(application).scanHistoryDao()),
) : AndroidViewModel(application) {

    val history: StateFlow<List<ScanHistoryEntry>> = scanHistoryRepository.observeHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteScan(entry: ScanHistoryEntry) {
        viewModelScope.launch {
            scanHistoryRepository.deleteScan(entry.id)
        }
    }
}
