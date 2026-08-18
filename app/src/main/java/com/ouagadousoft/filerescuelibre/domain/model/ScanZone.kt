package com.ouagadousoft.filerescuelibre.domain.model

/**
 * Zone de stockage ciblée par le scan rapide (F2). Chemins relatifs à la racine du
 * stockage interne partagé (`/storage/emulated/0`). [FULL_STORAGE] scanne tout.
 *
 * [WHATSAPP] liste deux emplacements possibles car l'app a changé de convention de
 * stockage avec le scoped storage (Android 11+) : l'ancien chemin direct et le
 * nouveau sous `Android/media`. Les deux sont recherchés, celui qui existe est utilisé.
 */
enum class ScanZone(val relativePaths: List<String>) {
    FULL_STORAGE(emptyList()),
    DCIM(listOf("DCIM")),
    PICTURES(listOf("Pictures")),
    MOVIES(listOf("Movies")),
    DOWNLOAD(listOf("Download")),
    WHATSAPP(
        listOf(
            "WhatsApp/Media",
            "Android/media/com.whatsapp/WhatsApp/Media",
        )
    ),
}
