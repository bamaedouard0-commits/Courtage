package com.ouagadousoft.filerescuelibre.domain.repository

import com.ouagadousoft.filerescuelibre.domain.model.DeepScanEvent
import kotlinx.coroutines.flow.Flow

interface DeepScanRepository {
    /**
     * Émet la progression (octets lus) et chaque fichier reconstruit par carving.
     *
     * [startOffset] reprend le scan à cette position plutôt que depuis le début — mais
     * uniquement si [expectedDevicePath] correspond au périphérique effectivement résolu ;
     * sinon le scan repart de zéro (voir [DeepScanEvent.Started]).
     */
    fun deepScan(startOffset: Long = 0, expectedDevicePath: String? = null): Flow<DeepScanEvent>
}
