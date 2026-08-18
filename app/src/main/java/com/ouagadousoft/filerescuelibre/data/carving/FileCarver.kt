package com.ouagadousoft.filerescuelibre.data.carving

import com.ouagadousoft.filerescuelibre.domain.model.FileCategory
import com.ouagadousoft.filerescuelibre.domain.model.ReliabilityLevel

/**
 * Moteur de carving par signatures, inspiré de la logique de PhotoRec (pas son code) :
 * recherche de magic numbers dans un flux d'octets brut, puis détermination de la
 * longueur du fichier soit par un champ de taille du conteneur (déterministe), soit
 * par recherche d'un footer connu.
 *
 * GIF et MKV sont volontairement absents de cette V1 : leur fin de fichier ne peut
 * pas être déterminée de façon fiable avec une simple recherche de signature (trailer
 * GIF trop court pour être discriminant, EBML de Matroska nécessitant un vrai parseur).
 * Les inclure aurait produit des fichiers tronqués présentés comme récupérés.
 */
enum class CarveFormat(val category: FileCategory, val extension: String) {
    JPEG(FileCategory.IMAGE, "jpg"),
    PNG(FileCategory.IMAGE, "png"),
    BMP(FileCategory.IMAGE, "bmp"),
    WEBP(FileCategory.IMAGE, "webp"),
    HEIC(FileCategory.IMAGE, "heic"),
    MP4(FileCategory.VIDEO, "mp4"),
    MOV(FileCategory.VIDEO, "mov"),
    THREE_GP(FileCategory.VIDEO, "3gp"),
    AVI(FileCategory.VIDEO, "avi"),
}

data class CarveOutcome(
    val length: Long,
    val reliability: ReliabilityLevel,
    val format: CarveFormat,
)

internal data class SignatureMatch(val bufferIndex: Int, val anchor: Anchor)

internal enum class Anchor(val pattern: ByteArray) {
    JPEG_HEADER(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())),
    PNG_HEADER(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)),
    BMP_HEADER(byteArrayOf(0x42, 0x4D)),
    RIFF_WEBP("WEBP".toByteArray(Charsets.US_ASCII)),
    RIFF_AVI("AVI ".toByteArray(Charsets.US_ASCII)),
    ISOBMFF_FTYP("ftyp".toByteArray(Charsets.US_ASCII)),
}

fun interface RandomAccessSource {
    fun readAt(offset: Long, buffer: ByteArray, bufferOffset: Int, length: Int): Int
}

internal fun RandomAccessSource.readFullyAt(offset: Long, length: Int): ByteArray? {
    if (offset < 0 || length <= 0) return null
    val buffer = ByteArray(length)
    var totalRead = 0
    while (totalRead < length) {
        val n = readAt(offset + totalRead, buffer, totalRead, length - totalRead)
        if (n <= 0) return null
        totalRead += n
    }
    return buffer
}

private fun ByteArray.indexOfSubArray(pattern: ByteArray, fromIndex: Int = 0): Int {
    if (pattern.isEmpty() || pattern.size > size) return -1
    val lastPossible = size - pattern.size
    var i = fromIndex.coerceAtLeast(0)
    outer@ while (i <= lastPossible) {
        for (j in pattern.indices) {
            if (this[i + j] != pattern[j]) {
                i++
                continue@outer
            }
        }
        return i
    }
    return -1
}

internal fun findEarliestSignature(buffer: ByteArray): SignatureMatch? {
    var best: SignatureMatch? = null
    for (anchor in Anchor.entries) {
        val idx = buffer.indexOfSubArray(anchor.pattern)
        if (idx >= 0 && (best == null || idx < best.bufferIndex)) {
            best = SignatureMatch(idx, anchor)
        }
    }
    return best
}

/** Décalage du début réel du fichier par rapport à la position de l'ancre trouvée. */
internal fun Anchor.headerBackOffset(): Int = when (this) {
    Anchor.RIFF_WEBP, Anchor.RIFF_AVI -> 8 // "RIFF" (4) + taille (4) précèdent le tag
    Anchor.ISOBMFF_FTYP -> 4 // le champ de taille de la box précède "ftyp"
    Anchor.JPEG_HEADER, Anchor.PNG_HEADER, Anchor.BMP_HEADER -> 0
}

private const val MAX_SIMPLE_SIZE = 500L * 1024 * 1024
private const val MAX_CONTAINER_SIZE = 2L * 1024 * 1024 * 1024
private const val JPEG_SEARCH_WINDOW = 50L * 1024 * 1024
private const val PNG_SEARCH_WINDOW = 150L * 1024 * 1024

private val RIFF_TAG = "RIFF".byteArrayLiteral()
private val VALID_DIB_HEADER_SIZES = setOf(12L, 40L, 52L, 56L, 64L, 108L, 124L)

private fun String.byteArrayLiteral() = toByteArray(Charsets.US_ASCII)

/**
 * Tente de déterminer la longueur du fichier commençant à [headerOffset] dans [source].
 * Retourne `null` si l'ancre était un faux positif (structure invalide) ou si aucune
 * longueur fiable n'a pu être établie — le fichier n'est alors pas extrait.
 */
fun carve(source: RandomAccessSource, headerOffset: Long, anchor: Anchor, remainingBytes: Long): CarveOutcome? {
    if (headerOffset < 0 || remainingBytes <= 0) return null

    return when (anchor) {
        Anchor.JPEG_HEADER -> carveByFooter(
            source, headerOffset,
            footer = byteArrayOf(0xFF.toByte(), 0xD9.toByte()),
            footerTrailingBytes = 0,
            searchWindow = minOf(JPEG_SEARCH_WINDOW, remainingBytes),
            format = CarveFormat.JPEG,
        )
        Anchor.PNG_HEADER -> carveByFooter(
            source, headerOffset,
            footer = "IEND".byteArrayLiteral(),
            footerTrailingBytes = 4, // CRC 32 bits suivant le marqueur IEND
            searchWindow = minOf(PNG_SEARCH_WINDOW, remainingBytes),
            format = CarveFormat.PNG,
        )
        Anchor.BMP_HEADER -> carveBmp(source, headerOffset, remainingBytes)
        Anchor.RIFF_WEBP -> carveRiff(source, headerOffset, remainingBytes, CarveFormat.WEBP)
        Anchor.RIFF_AVI -> carveRiff(source, headerOffset, remainingBytes, CarveFormat.AVI)
        Anchor.ISOBMFF_FTYP -> carveIsoBmff(source, headerOffset, remainingBytes)
    }
}

private fun readUInt32LE(b: ByteArray, offset: Int): Long =
    (b[offset].toLong() and 0xFF) or
        ((b[offset + 1].toLong() and 0xFF) shl 8) or
        ((b[offset + 2].toLong() and 0xFF) shl 16) or
        ((b[offset + 3].toLong() and 0xFF) shl 24)

private fun readUInt32BE(b: ByteArray, offset: Int): Long =
    ((b[offset].toLong() and 0xFF) shl 24) or
        ((b[offset + 1].toLong() and 0xFF) shl 16) or
        ((b[offset + 2].toLong() and 0xFF) shl 8) or
        (b[offset + 3].toLong() and 0xFF)

private fun readUInt64BE(b: ByteArray, offset: Int): Long {
    var result = 0L
    for (i in 0 until 8) {
        result = (result shl 8) or (b[offset + i].toLong() and 0xFF)
    }
    return result
}

private fun carveBmp(source: RandomAccessSource, headerOffset: Long, remainingBytes: Long): CarveOutcome? {
    val header = source.readFullyAt(headerOffset, 18) ?: return null
    val declaredSize = readUInt32LE(header, 2)
    val dibHeaderSize = readUInt32LE(header, 14)
    if (dibHeaderSize !in VALID_DIB_HEADER_SIZES) return null
    val minPlausibleSize = 14 + dibHeaderSize
    if (declaredSize < minPlausibleSize || declaredSize > MAX_SIMPLE_SIZE || declaredSize > remainingBytes) return null
    return CarveOutcome(declaredSize, ReliabilityLevel.INTACT, CarveFormat.BMP)
}

private fun carveRiff(
    source: RandomAccessSource,
    headerOffset: Long,
    remainingBytes: Long,
    format: CarveFormat,
): CarveOutcome? {
    val header = source.readFullyAt(headerOffset, 8) ?: return null
    if (!header.copyOfRange(0, 4).contentEquals(RIFF_TAG)) return null
    val declaredChunkSize = readUInt32LE(header, 4)
    val total = declaredChunkSize + 8
    if (total < 20 || total > MAX_SIMPLE_SIZE || total > remainingBytes) return null
    return CarveOutcome(total, ReliabilityLevel.INTACT, format)
}

private fun isPlausibleBoxType(bytes: ByteArray, offset: Int): Boolean {
    for (i in 0 until 4) {
        val c = bytes[offset + i].toInt() and 0xFF
        if (c !in 0x20..0x7E) return false
    }
    return true
}

private val HEIC_BRANDS = setOf("heic", "heix", "hevc", "hevx", "mif1", "msf1")
private val THREE_GP_BRANDS = setOf("3gp4", "3gp5", "3gp6", "3g2a")

private fun carveIsoBmff(source: RandomAccessSource, headerOffset: Long, remainingBytes: Long): CarveOutcome? {
    val brandHeader = source.readFullyAt(headerOffset, 12) ?: return null
    if (!isPlausibleBoxType(brandHeader, 4) || String(brandHeader, 4, 4, Charsets.US_ASCII) != "ftyp") {
        return null
    }
    val majorBrand = String(brandHeader, 8, 4, Charsets.US_ASCII).trim().lowercase()

    val format = when {
        majorBrand in HEIC_BRANDS -> CarveFormat.HEIC
        majorBrand in THREE_GP_BRANDS -> CarveFormat.THREE_GP
        majorBrand == "qt" -> CarveFormat.MOV
        else -> CarveFormat.MP4
    }

    var offset = headerOffset
    var boxCount = 0
    var reachedNaturalEnd = false
    val limit = minOf(headerOffset + MAX_CONTAINER_SIZE, headerOffset + remainingBytes)

    while (boxCount < 5000 && offset < limit) {
        val boxHeader = source.readFullyAt(offset, 16) ?: break
        if (!isPlausibleBoxType(boxHeader, 4)) {
            reachedNaturalEnd = true
            break
        }
        val boxSize32 = readUInt32BE(boxHeader, 0)
        val boxSize = when (boxSize32) {
            0L -> break // taille "jusqu'à la fin du fichier" : inconnue en carving, on s'arrête (partiel)
            1L -> readUInt64BE(boxHeader, 8)
            else -> boxSize32
        }
        if (boxSize < 8 || offset + boxSize > limit) break
        offset += boxSize
        boxCount++
    }

    val length = offset - headerOffset
    if (length <= 0) return null

    val reliability = if (reachedNaturalEnd) ReliabilityLevel.INTACT else ReliabilityLevel.PARTIAL
    return CarveOutcome(length, reliability, format)
}

private fun carveByFooter(
    source: RandomAccessSource,
    headerOffset: Long,
    footer: ByteArray,
    footerTrailingBytes: Int,
    searchWindow: Long,
    format: CarveFormat,
): CarveOutcome? {
    val readChunkSize = 1 * 1024 * 1024
    var searchStart = headerOffset
    val limit = headerOffset + searchWindow

    while (searchStart < limit) {
        val readLen = minOf(readChunkSize.toLong(), limit - searchStart).toInt()
        val buffer = source.readFullyAt(searchStart, readLen) ?: break
        val idx = buffer.indexOfSubArray(footer)
        if (idx >= 0) {
            val length = (searchStart + idx + footer.size + footerTrailingBytes) - headerOffset
            return CarveOutcome(length, ReliabilityLevel.INTACT, format)
        }
        searchStart += (readLen - (footer.size - 1)).coerceAtLeast(1)
    }
    return null
}
