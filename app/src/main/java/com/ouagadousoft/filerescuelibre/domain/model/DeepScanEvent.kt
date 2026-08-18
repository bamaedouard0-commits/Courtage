package com.ouagadousoft.filerescuelibre.domain.model

sealed interface DeepScanEvent {
    /**
     * Émis une fois au tout début. [resumedFromOffset] indique si l'offset de reprise demandé a
     * été honoré (partition résolue identique à celle attendue) ou si le scan est reparti de
     * zéro (partition différente depuis la dernière session — reprendre à un offset sur un autre
     * périphérique serait invalide).
     */
    data class Started(val devicePath: String, val totalBytes: Long, val resumedFromOffset: Boolean) : DeepScanEvent
    data class Progress(val bytesScanned: Long, val totalBytes: Long) : DeepScanEvent
    data class FileFound(val file: RecoverableFile) : DeepScanEvent
}
