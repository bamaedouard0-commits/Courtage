package com.ouagadousoft.filerescuelibre.domain

import com.ouagadousoft.filerescuelibre.domain.model.FileCategory

/** Formats ciblés par le MVP (cahier des charges §4.1) : uniquement photos et vidéos. */
object SupportedFormats {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "heic", "gif", "bmp", "webp")
    private val videoExtensions = setOf("mp4", "mov", "3gp", "avi", "mkv")

    /** Retourne la catégorie du fichier d'après son extension, ou `null` si hors périmètre MVP. */
    fun categoryOf(fileName: String): FileCategory? {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return when (extension) {
            in imageExtensions -> FileCategory.IMAGE
            in videoExtensions -> FileCategory.VIDEO
            else -> null
        }
    }
}
