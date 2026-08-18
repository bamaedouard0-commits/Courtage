package com.ouagadousoft.filerescuelibre.domain.model

enum class ScanType {
    QUICK,
    DEEP,
}

/** Résumé d'un scan passé, persisté pour l'historique (F : historique des scans). */
data class ScanHistoryEntry(
    val id: Long,
    val type: ScanType,
    val zone: ScanZone?,
    val timestampEpochSeconds: Long,
    val resultCount: Int,
)
