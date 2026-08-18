package com.ouagadousoft.filerescuelibre.domain.model

/**
 * Catégories de fichiers reconnues. Le cahier des charges §4.1 ne ciblait que photos/vidéos
 * pour le MVP ; AUDIO et DOCUMENT étendent la couverture vers l'ambition affichée du README
 * (musiques, documents/PDF) — voir `SupportedFormats` et `FileCarver` pour ce qui est
 * effectivement détecté (scan rapide) vs carvé de façon fiable (scan approfondi).
 */
enum class FileCategory {
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
}
