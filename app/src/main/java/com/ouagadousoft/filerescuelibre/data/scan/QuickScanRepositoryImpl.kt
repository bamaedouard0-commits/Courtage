package com.ouagadousoft.filerescuelibre.data.scan

import com.ouagadousoft.filerescuelibre.data.root.RootShell
import com.ouagadousoft.filerescuelibre.domain.SupportedFormats
import com.ouagadousoft.filerescuelibre.domain.model.ReliabilityLevel
import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.ouagadousoft.filerescuelibre.domain.model.ScanSource
import com.ouagadousoft.filerescuelibre.domain.repository.QuickScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val SCAN_ROOT = "/storage/emulated/0"

/**
 * Scan rapide (F3) : recherche des fichiers déjà indexés comme supprimés par le système,
 * sans lecture bas niveau du disque.
 *
 * Deux sources connues, lues via une seule invocation shell root pour limiter les
 * démarrages de processus :
 * - fichiers renommés par la corbeille MediaStore (préfixe ".trashed-", Android 11+)
 * - corbeilles FUSE par application (dossiers ".Trash-<uid>", Android 11+)
 */
class QuickScanRepositoryImpl : QuickScanRepository {

    override fun quickScan(): Flow<RecoverableFile> = flow {
        val script = """
            find $SCAN_ROOT -type f \( -iname ".trashed-*" -o -path "*/.Trash-*/*" \) 2>/dev/null | while IFS= read -r f; do
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
