package com.ouagadousoft.filerescuelibre.domain.repository

import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile

interface RecoveryRepository {
    /** Copie [file] vers un emplacement accessible hors de l'app, sans modifier l'original. Retourne le chemin de destination. */
    suspend fun recover(file: RecoverableFile): String
}
