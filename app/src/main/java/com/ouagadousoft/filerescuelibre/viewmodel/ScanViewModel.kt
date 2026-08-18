package com.ouagadousoft.filerescuelibre.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ouagadousoft.filerescuelibre.data.carving.DeepScanRepositoryImpl
import com.ouagadousoft.filerescuelibre.data.history.AppDatabase
import com.ouagadousoft.filerescuelibre.data.history.ScanHistoryRepositoryImpl
import com.ouagadousoft.filerescuelibre.data.recovery.RecoveryRepositoryImpl
import com.ouagadousoft.filerescuelibre.data.scan.QuickScanRepositoryImpl
import com.ouagadousoft.filerescuelibre.domain.model.DeepScanEvent
import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.ouagadousoft.filerescuelibre.domain.model.ScanHistoryEntry
import com.ouagadousoft.filerescuelibre.domain.model.ScanType
import com.ouagadousoft.filerescuelibre.domain.model.ScanZone
import com.ouagadousoft.filerescuelibre.domain.repository.DeepScanRepository
import com.ouagadousoft.filerescuelibre.domain.repository.QuickScanRepository
import com.ouagadousoft.filerescuelibre.domain.repository.RecoveryRepository
import com.ouagadousoft.filerescuelibre.domain.repository.ScanHistoryRepository
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Idle : ScanUiState

    /** [progressFraction] est `null` pour un scan sans mesure de progression fiable (scan rapide). */
    data class Scanning(val foundCount: Int, val progressFraction: Float? = null) : ScanUiState
    data class Completed(val results: List<RecoverableFile>) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

sealed interface RecoveryStatus {
    data object InProgress : RecoveryStatus
    data class Success(val savedPath: String) : RecoveryStatus
    data class Error(val message: String) : RecoveryStatus
}

// Instanciation directe faute de framework d'injection de dépendances pour le moment ;
// à remplacer si le projet adopte Hilt/Koin par la suite. AndroidViewModel est nécessaire
// pour obtenir un répertoire de sortie privé (filesDir) pour les fichiers carvés.
class ScanViewModel @JvmOverloads constructor(
    application: Application,
    private val quickScanRepository: QuickScanRepository = QuickScanRepositoryImpl(),
    private val deepScanRepository: DeepScanRepository =
        DeepScanRepositoryImpl(File(application.filesDir, "carved")),
    private val recoveryRepository: RecoveryRepository = RecoveryRepositoryImpl(),
    private val scanHistoryRepository: ScanHistoryRepository =
        ScanHistoryRepositoryImpl(AppDatabase.getInstance(application).scanHistoryDao()),
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    /** État de récupération par [RecoverableFile.path], affiché sur chaque ligne de résultat. */
    private val _recoveryStatuses = MutableStateFlow<Map<String, RecoveryStatus>>(emptyMap())
    val recoveryStatuses: StateFlow<Map<String, RecoveryStatus>> = _recoveryStatuses.asStateFlow()

    fun startQuickScan(zone: ScanZone) {
        if (_uiState.value is ScanUiState.Scanning) return

        viewModelScope.launch {
            _uiState.value = ScanUiState.Scanning(0)
            val results = mutableListOf<RecoverableFile>()
            try {
                quickScanRepository.quickScan(zone).collect { file ->
                    results.add(file)
                    _uiState.value = ScanUiState.Scanning(results.size)
                }
                _uiState.value = ScanUiState.Completed(results.toList())
                scanHistoryRepository.saveScan(ScanType.QUICK, zone, results)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ScanUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun startDeepScan() {
        if (_uiState.value is ScanUiState.Scanning) return

        viewModelScope.launch {
            _uiState.value = ScanUiState.Scanning(0, progressFraction = 0f)
            val results = mutableListOf<RecoverableFile>()
            try {
                deepScanRepository.deepScan().collect { event ->
                    when (event) {
                        is DeepScanEvent.Progress -> {
                            val fraction = if (event.totalBytes > 0) {
                                (event.bytesScanned.toFloat() / event.totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                null
                            }
                            _uiState.value = ScanUiState.Scanning(results.size, fraction)
                        }
                        is DeepScanEvent.FileFound -> {
                            results.add(event.file)
                            val currentFraction = (_uiState.value as? ScanUiState.Scanning)?.progressFraction
                            _uiState.value = ScanUiState.Scanning(results.size, currentFraction)
                        }
                    }
                }
                _uiState.value = ScanUiState.Completed(results.toList())
                scanHistoryRepository.saveScan(ScanType.DEEP, zone = null, results = results)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ScanUiState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun reset() {
        _uiState.value = ScanUiState.Idle
        _recoveryStatuses.value = emptyMap()
    }

    /** Recharge les résultats d'un scan passé (historique) sans relancer de scan ni le ré-enregistrer. */
    fun loadHistoryEntry(entry: ScanHistoryEntry) {
        if (_uiState.value is ScanUiState.Scanning) return

        viewModelScope.launch {
            _recoveryStatuses.value = emptyMap()
            val results = scanHistoryRepository.resultsForScan(entry.id)
            _uiState.value = ScanUiState.Completed(results)
        }
    }

    fun recoverFile(file: RecoverableFile) {
        if (_recoveryStatuses.value[file.path] is RecoveryStatus.InProgress) return

        viewModelScope.launch {
            _recoveryStatuses.value += file.path to RecoveryStatus.InProgress
            val status = try {
                val savedPath = recoveryRepository.recover(file)
                RecoveryStatus.Success(savedPath)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                RecoveryStatus.Error(e.message ?: "Erreur inconnue")
            }
            _recoveryStatuses.value += file.path to status
        }
    }
}
