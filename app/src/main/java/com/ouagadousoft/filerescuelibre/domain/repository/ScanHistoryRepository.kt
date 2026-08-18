package com.ouagadousoft.filerescuelibre.domain.repository

import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.ouagadousoft.filerescuelibre.domain.model.ResumableDeepScan
import com.ouagadousoft.filerescuelibre.domain.model.ScanHistoryEntry
import com.ouagadousoft.filerescuelibre.domain.model.ScanType
import com.ouagadousoft.filerescuelibre.domain.model.ScanZone
import kotlinx.coroutines.flow.Flow

interface ScanHistoryRepository {
    /** Persiste un scan terminé (type, zone si scan rapide, et fichiers trouvés) en un bloc. */
    suspend fun saveScan(type: ScanType, zone: ScanZone?, results: List<RecoverableFile>)

    /** Historique le plus récent en premier. */
    fun observeHistory(): Flow<List<ScanHistoryEntry>>

    /** Fichiers trouvés lors du scan [scanId], tels qu'enregistrés à l'époque. */
    suspend fun resultsForScan(scanId: Long): List<RecoverableFile>

    suspend fun deleteScan(scanId: Long)

    /** Scan approfondi interrompu (F9) prêt à être repris, ou `null` si aucun. */
    suspend fun getResumableDeepScan(): ResumableDeepScan?

    /** Démarre une session de scan approfondi persistée incrémentalement, retourne son id. */
    suspend fun beginDeepScanSession(devicePath: String, totalBytes: Long): Long

    /** Enregistre immédiatement un fichier trouvé pendant un scan approfondi en cours. */
    suspend fun recordDeepScanFile(sessionId: Long, file: RecoverableFile)

    /** Met à jour la position de reprise ; à appeler avec parcimonie (pas à chaque bloc lu). */
    suspend fun recordDeepScanPosition(sessionId: Long, position: Long)

    /** Marque le scan approfondi [sessionId] comme terminé : il n'est plus repris au démarrage. */
    suspend fun finishDeepScanSession(sessionId: Long)
}
