package com.ouagadousoft.filerescuelibre.domain.repository

import com.ouagadousoft.filerescuelibre.domain.model.DeepScanEvent
import kotlinx.coroutines.flow.Flow

interface DeepScanRepository {
    /** Émet la progression (octets lus) et chaque fichier reconstruit par carving. */
    fun deepScan(): Flow<DeepScanEvent>
}
