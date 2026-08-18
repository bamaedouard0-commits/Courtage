package com.ouagadousoft.filerescuelibre.domain.repository

import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.ouagadousoft.filerescuelibre.domain.model.ScanHistoryEntry
import com.ouagadousoft.filerescuelibre.domain.model.ScanType
import com.ouagadousoft.filerescuelibre.domain.model.ScanZone
import kotlinx.coroutines.flow.Flow

interface ScanHistoryRepository {
    /** Persiste un scan terminé (type, zone si scan rapide, et fichiers trouvés). */
    suspend fun saveScan(type: ScanType, zone: ScanZone?, results: List<RecoverableFile>)

    /** Historique le plus récent en premier. */
    fun observeHistory(): Flow<List<ScanHistoryEntry>>

    /** Fichiers trouvés lors du scan [scanId], tels qu'enregistrés à l'époque. */
    suspend fun resultsForScan(scanId: Long): List<RecoverableFile>

    suspend fun deleteScan(scanId: Long)
}
