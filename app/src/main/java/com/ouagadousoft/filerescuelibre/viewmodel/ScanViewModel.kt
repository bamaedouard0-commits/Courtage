package com.ouagadousoft.filerescuelibre.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ouagadousoft.filerescuelibre.data.scan.QuickScanRepositoryImpl
import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.ouagadousoft.filerescuelibre.domain.repository.QuickScanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Scanning(val foundCount: Int) : ScanUiState
    data class Completed(val results: List<RecoverableFile>) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

// Instanciation directe faute de framework d'injection de dépendances pour le moment ;
// à remplacer si le projet adopte Hilt/Koin par la suite.
class ScanViewModel @JvmOverloads constructor(
    private val quickScanRepository: QuickScanRepository = QuickScanRepositoryImpl(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun startQuickScan() {
        if (_uiState.value is ScanUiState.Scanning) return

        viewModelScope.launch {
            _uiState.value = ScanUiState.Scanning(0)
            val results = mutableListOf<RecoverableFile>()
            try {
                quickScanRepository.quickScan().collect { file ->
                    results.add(file)
                    _uiState.value = ScanUiState.Scanning(results.size)
                }
                _uiState.value = ScanUiState.Completed(results.toList())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ScanUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun reset() {
        _uiState.value = ScanUiState.Idle
    }
}
