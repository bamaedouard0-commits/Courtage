package com.ouagadousoft.filerescuelibre.domain.repository

import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.ouagadousoft.filerescuelibre.domain.model.ScanZone
import kotlinx.coroutines.flow.Flow

interface QuickScanRepository {
    /** Émet chaque fichier récupérable au fur et à mesure de sa détection dans [zone]. */
    fun quickScan(zone: ScanZone): Flow<RecoverableFile>
}
