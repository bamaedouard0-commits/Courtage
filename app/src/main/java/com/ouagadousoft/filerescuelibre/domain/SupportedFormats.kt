package com.ouagadousoft.filerescuelibre.domain

import com.ouagadousoft.filerescuelibre.domain.model.FileCategory

/**
 * Formats reconnus par extension pour catégoriser un fichier — utilisé par le scan rapide
 * (détection de fichiers déjà présents sur le disque, aucune contrainte de carving) et pour
 * classer les résultats du scan approfondi une fois un format effectivement carvé.
 *
 * Plus large que ce que `FileCarver` sait carver de façon fiable : le scan rapide n'a pas le
 * problème du scan approfondi (déterminer une longueur fiable sans lire tout le fichier), donc
 * inclut par exemple MP3 ou DOCX alors que `FileCarver` les exclut volontairement. Ne pas
 * supposer que les deux listes doivent rester alignées.
 */
object SupportedFormats {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "heic", "gif", "bmp", "webp")
    private val videoExtensions = setOf("mp4", "mov", "3gp", "avi", "mkv")
    private val audioExtensions = setOf("mp3", "wav", "m4a", "aac", "ogg", "flac")
    private val documentExtensions = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt")

    /** Retourne la catégorie du fichier d'après son extension, ou `null` si hors périmètre reconnu. */
    fun categoryOf(fileName: String): FileCategory? {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return when (extension) {
            in imageExtensions -> FileCategory.IMAGE
            in videoExtensions -> FileCategory.VIDEO
            in audioExtensions -> FileCategory.AUDIO
            in documentExtensions -> FileCategory.DOCUMENT
            else -> null
        }
    }
}
