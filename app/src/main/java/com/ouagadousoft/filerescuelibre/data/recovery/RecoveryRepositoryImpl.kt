package com.ouagadousoft.filerescuelibre.data.recovery

import com.ouagadousoft.filerescuelibre.data.root.RootShell
import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.ouagadousoft.filerescuelibre.domain.model.ScanSource
import com.ouagadousoft.filerescuelibre.domain.repository.RecoveryRepository

class RecoveryException(message: String) : Exception(message)

private const val RECOVERY_DIR = "/storage/emulated/0/FileRescueLibre/Recupere"

/** Préfixe ajouté par la corbeille MediaStore (ex. ".trashed-1699999999-photo.jpg") à retirer à la récupération. */
private val TRASHED_PREFIX = Regex("^\\.trashed-\\d+-")

/**
 * Copie un fichier trouvé (corbeille ou carving) vers [RECOVERY_DIR], un dossier normal
 * visible par la galerie et les gestionnaires de fichiers — l'original n'est jamais
 * déplacé ni supprimé. Passe entièrement par root (libsu) : les fichiers carvés vivent
 * dans le stockage privé de l'app et les fichiers de corbeille sous des chemins que le
 * stockage cloisonné (scoped storage) ne laisse pas écrire sans droits étendus.
 */
class RecoveryRepositoryImpl : RecoveryRepository {

    override suspend fun recover(file: RecoverableFile): String {
        RootShell.exec("mkdir -p \"$RECOVERY_DIR\"")

        val destinationPath = uniqueDestinationPath(destinationNameFor(file))
        val copyOutput = RootShell.exec(
            "cp -f \"${file.path}\" \"$destinationPath\" && echo OK"
        )
        if (copyOutput.lastOrNull()?.trim() != "OK") {
            throw RecoveryException("Échec de la copie vers $destinationPath")
        }

        // Best-effort : rend le fichier visible dans la galerie sans attendre un redémarrage.
        RootShell.exec(
            "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d \"file://$destinationPath\""
        )

        return destinationPath
    }

    private fun destinationNameFor(file: RecoverableFile): String = when (file.source) {
        ScanSource.TRASHED_FILE -> file.name.replaceFirst(TRASHED_PREFIX, "")
        ScanSource.APP_TRASH_BIN, ScanSource.CARVED_BLOCK -> file.name
    }

    private suspend fun uniqueDestinationPath(name: String): String {
        var candidate = "$RECOVERY_DIR/$name"
        if (!pathExists(candidate)) return candidate

        val dotIndex = name.lastIndexOf('.')
        val base = if (dotIndex > 0) name.substring(0, dotIndex) else name
        val extension = if (dotIndex > 0) name.substring(dotIndex) else ""

        var index = 1
        do {
            candidate = "$RECOVERY_DIR/$base ($index)$extension"
            index++
        } while (pathExists(candidate))
        return candidate
    }

    private suspend fun pathExists(path: String): Boolean =
        RootShell.exec("test -e \"$path\" && echo EXISTS").lastOrNull()?.trim() == "EXISTS"
}
