package com.ouagadousoft.filerescuelibre.data.carving

import com.ouagadousoft.filerescuelibre.domain.model.DeepScanEvent
import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.ouagadousoft.filerescuelibre.domain.model.ScanSource
import com.ouagadousoft.filerescuelibre.domain.repository.DeepScanRepository
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.UUID

class DeepScanException(message: String) : Exception(message)

private const val CHUNK_SIZE = 4 * 1024 * 1024

/** Marge de recouvrement entre deux blocs lus, pour ne jamais rater une signature à cheval. */
private const val CHUNK_OVERLAP = 32

/**
 * Scan approfondi (F4) : file carving par signatures sur la partition data brute,
 * inspiré de la logique de PhotoRec. Lit le périphérique bloc par blocs (streaming,
 * jamais l'intégralité en mémoire), extrait chaque fichier détecté vers le stockage
 * privé de l'application au fur et à mesure.
 */
class DeepScanRepositoryImpl(
    private val outputDir: File,
) : DeepScanRepository {

    override fun deepScan(): Flow<DeepScanEvent> = flow {
        val devicePath = BlockDeviceLocator.resolveUserdataDevice()
            ?: throw DeepScanException(
                "Impossible de localiser la partition data brute sur cet appareil."
            )
        val totalBytes = BlockDeviceLocator.deviceSizeBytes(devicePath)
            ?: throw DeepScanException(
                "Impossible de déterminer la taille de la partition ($devicePath)."
            )

        outputDir.mkdirs()

        SuFileInputStream.open(devicePath).use { input ->
            val channel = input.channel
            val source = channelRandomAccessSource(channel)

            var position = 0L
            while (position < totalBytes) {
                val positionAtLoopStart = position
                val toRead = minOf(CHUNK_SIZE.toLong(), totalBytes - position).toInt()
                val buffer = source.readFullyAt(position, toRead)
                if (buffer == null) {
                    // Fin de lecture prématurée (device plus court que rapporté, erreur I/O) :
                    // on arrête proprement le scan plutôt que de boucler indéfiniment.
                    break
                }

                val match = findEarliestSignature(buffer)
                if (match == null) {
                    position += (toRead - CHUNK_OVERLAP).coerceAtLeast(1)
                    emit(DeepScanEvent.Progress(position.coerceAtMost(totalBytes), totalBytes))
                    continue
                }

                val headerOffset = (position + match.bufferIndex - match.anchor.headerBackOffset())
                    .coerceAtLeast(0)
                val remaining = totalBytes - headerOffset
                val outcome = carve(source, headerOffset, match.anchor, remaining)

                if (outcome != null) {
                    val savedFile = writeCarvedFile(source, headerOffset, outcome.length, outcome.format.extension)
                    if (savedFile != null) {
                        emit(
                            DeepScanEvent.FileFound(
                                RecoverableFile(
                                    path = savedFile.absolutePath,
                                    name = savedFile.name,
                                    sizeBytes = outcome.length,
                                    lastModifiedEpochSeconds = System.currentTimeMillis() / 1000,
                                    category = outcome.format.category,
                                    reliability = outcome.reliability,
                                    source = ScanSource.CARVED_BLOCK,
                                )
                            )
                        )
                    }
                    position = headerOffset + outcome.length
                } else {
                    position = headerOffset + 1
                }
                // Garde-fou : garantir une progression même si un carving valide mais très
                // court retombe pile sur la position de départ (bord de chevauchement).
                position = maxOf(position, positionAtLoopStart + 1)

                emit(DeepScanEvent.Progress(position.coerceAtMost(totalBytes), totalBytes))
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun writeCarvedFile(
        source: RandomAccessSource,
        headerOffset: Long,
        length: Long,
        extension: String,
    ): File? {
        val file = File(outputDir, "${UUID.randomUUID()}.$extension")
        return try {
            FileOutputStream(file).use { out ->
                var remaining = length
                var offset = headerOffset
                val buf = ByteArray(CHUNK_SIZE)
                while (remaining > 0) {
                    val n = source.readAt(offset, buf, 0, minOf(buf.size.toLong(), remaining).toInt())
                    if (n <= 0) break
                    out.write(buf, 0, n)
                    offset += n
                    remaining -= n
                }
            }
            file
        } catch (e: IOException) {
            file.delete()
            null
        }
    }

    private fun channelRandomAccessSource(channel: FileChannel): RandomAccessSource =
        RandomAccessSource { offset, buffer, bufferOffset, length ->
            val target = ByteBuffer.wrap(buffer, bufferOffset, length)
            channel.position(offset)
            channel.read(target)
        }
}
