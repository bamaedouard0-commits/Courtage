package com.ouagadousoft.filerescuelibre.domain.repository

import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import kotlinx.coroutines.flow.Flow

interface QuickScanRepository {
    /** Émet chaque fichier récupérable au fur et à mesure de sa détection. */
    fun quickScan(): Flow<RecoverableFile>
}
