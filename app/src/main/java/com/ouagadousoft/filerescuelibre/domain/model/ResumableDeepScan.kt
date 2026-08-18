package com.ouagadousoft.filerescuelibre.domain.model

/** Scan approfondi interrompu (app tuée, crash) et repris à [position] au prochain lancement (F9). */
data class ResumableDeepScan(
    val sessionId: Long,
    val devicePath: String,
    val position: Long,
    val totalBytes: Long,
    val existingResults: List<RecoverableFile>,
)
