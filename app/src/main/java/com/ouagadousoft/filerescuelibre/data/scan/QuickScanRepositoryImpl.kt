package com.ouagadousoft.filerescuelibre.data.scan

import com.ouagadousoft.filerescuelibre.data.root.RootShell
import com.ouagadousoft.filerescuelibre.domain.SupportedFormats
import com.ouagadousoft.filerescuelibre.domain.model.ReliabilityLevel
import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.ouagadousoft.filerescuelibre.domain.model.ScanSource
import com.ouagadousoft.filerescuelibre.domain.model.ScanZone
import com.ouagadousoft.filerescuelibre.domain.repository.QuickScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val STORAGE_ROOT = "/storage/emulated/0"

/**
 * Scan rapide (F3) : recherche des fichiers déjà indexés comme supprimés par le système,
 * sans lecture bas niveau du disque, limité à la zone choisie (F2).
 *
 * Deux sources connues :
 * - fichiers renommés par la corbeille MediaStore (préfixe ".trashed-", Android 11+),
 *   recherchés dans la zone sélectionnée puisqu'ils restent dans leur dossier d'origine
 * - corbeilles FUSE par application (dossiers ".Trash-<uid>", Android 11+), qui vivent
 *   à la racine du stockage et ne peuvent donc être rattachées à une zone précise :
 *   uniquement recherchées quand la zone est "Stockage interne complet"
 */
class QuickScanRepositoryImpl : QuickScanRepository {

    override fun quickScan(zone: ScanZone): Flow<RecoverableFile> = flow {
        val zoneDirs = if (zone.relativePaths.isEmpty()) {
            listOf(STORAGE_ROOT)
        } else {
            zone.relativePaths.map { "$STORAGE_ROOT/$it" }
        }
        val zoneTargets = zoneDirs.joinToString(" ") { "\"$it\"" }

        val trashedFind = "find $zoneTargets -type f -iname \".trashed-*\" 2>/dev/null"
        val appTrashFind = if (zone == ScanZone.FULL_STORAGE) {
            "find \"$STORAGE_ROOT\" -type f -path \"*/.Trash-*/*\" 2>/dev/null"
        } else {
            "true"
        }

        val script = """
            { $trashedFind; $appTrashFind; } | while IFS= read -r f; do
              stat -c '%s|%Y|%n' "${'$'}f" 2>/dev/null
            done
        """.trimIndent()

        val lines = RootShell.exec(script)

        for (line in lines) {
            val parts = line.split("|", limit = 3)
            if (parts.size != 3) continue

            val sizeBytes = parts[0].toLongOrNull() ?: continue
            val lastModified = parts[1].toLongOrNull() ?: continue
            val path = parts[2]
            val name = path.substringAfterLast('/')
            val category = SupportedFormats.categoryOf(name) ?: continue

            emit(
                RecoverableFile(
                    path = path,
                    name = name,
                    sizeBytes = sizeBytes,
                    lastModifiedEpochSeconds = lastModified,
                    category = category,
                    reliability = ReliabilityLevel.INTACT,
                    source = if (path.contains("/.Trash-")) ScanSource.APP_TRASH_BIN else ScanSource.TRASHED_FILE,
                )
            )
        }
    }
}
