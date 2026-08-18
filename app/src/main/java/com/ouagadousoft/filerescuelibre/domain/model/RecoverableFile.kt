package com.ouagadousoft.filerescuelibre.domain.model

enum class ReliabilityLevel {
    INTACT,
    PARTIAL,
}

enum class ScanSource {
    /** Fichier renommé par le mécanisme de corbeille MediaStore (préfixe ".trashed-"). */
    TRASHED_FILE,

    /** Corbeille FUSE par application (dossiers ".Trash-<uid>"). */
    APP_TRASH_BIN,

    /** Fichier reconstruit par carving sur la partition data brute (scan approfondi). */
    CARVED_BLOCK,
}

data class RecoverableFile(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val lastModifiedEpochSeconds: Long,
    val category: FileCategory,
    val reliability: ReliabilityLevel,
    val source: ScanSource,
)
