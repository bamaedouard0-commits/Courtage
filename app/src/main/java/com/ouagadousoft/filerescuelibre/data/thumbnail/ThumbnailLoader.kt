package com.ouagadousoft.filerescuelibre.data.thumbnail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import com.ouagadousoft.filerescuelibre.domain.model.FileCategory
import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.topjohnwu.superuser.io.SuFileInputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_DIMENSION_PX = 160

/**
 * Décodage de miniatures basse résolution, en pur Bitmap/BitmapFactory/MediaMetadataRetriever
 * (pas de dépendance Coil/Glide pour cette première version). Lit via root
 * (`SuFileInputStream`, comme le moteur de carving) car les fichiers de corbeille du scan
 * rapide ne sont pas accessibles par les API de fichiers normales sous scoped storage — y
 * compris pour les vidéos, via [SuFileMediaDataSource] qui fait le pont entre l'accès
 * aléatoire root et l'API `MediaDataSource` attendue par `MediaMetadataRetriever`.
 */
object ThumbnailLoader {

    /** Retourne `null` si le décodage échoue (fichier carvé tronqué/corrompu, codec non supporté). */
    suspend fun load(file: RecoverableFile): Bitmap? = withContext(Dispatchers.IO) {
        try {
            when (file.category) {
                FileCategory.IMAGE -> loadImageThumbnail(file.path)
                FileCategory.VIDEO -> loadVideoThumbnail(file)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun loadImageThumbnail(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        SuFileInputStream.open(path).use { BitmapFactory.decodeStream(it, null, bounds) }

        val options = BitmapFactory.Options().apply {
            inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
        }
        return SuFileInputStream.open(path).use { BitmapFactory.decodeStream(it, null, options) }
    }

    /** Extrait une frame représentative de la vidéo, puis la réduit à [MAX_DIMENSION_PX]. */
    private fun loadVideoThumbnail(file: RecoverableFile): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            SuFileMediaDataSource(file.path, file.sizeBytes).use { dataSource ->
                retriever.setDataSource(dataSource)
                retriever.getFrameAtTime()?.downscaledTo(MAX_DIMENSION_PX)
            }
        } finally {
            retriever.release()
        }
    }

    private fun computeSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sampleSize = 1
        while (width / (sampleSize * 2) >= maxDimension && height / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun Bitmap.downscaledTo(maxDimension: Int): Bitmap {
        if (width <= maxDimension && height <= maxDimension) return this
        val scale = maxDimension.toFloat() / maxOf(width, height)
        val targetWidth = (width * scale).toInt().coerceAtLeast(1)
        val targetHeight = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }
}

/**
 * Pont entre la lecture par accès aléatoire root (comme [com.ouagadousoft.filerescuelibre.data.carving.DeepScanRepositoryImpl])
 * et l'API `MediaDataSource` attendue par `MediaMetadataRetriever.setDataSource`, pour les
 * fichiers non lisibles par chemin direct sous scoped storage. [length] est déjà connue (issue
 * du scan initial) plutôt que relue via `channel.size()`, qui ne serait pas fiable pour tous
 * les cas d'usage de ce flux root.
 */
private class SuFileMediaDataSource(path: String, private val length: Long) : MediaDataSource() {
    private val input = SuFileInputStream.open(path)
    private val channel = input.channel

    override fun getSize(): Long = length

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= length) return -1
        channel.position(position)
        return channel.read(ByteBuffer.wrap(buffer, offset, size))
    }

    override fun close() {
        input.close()
    }
}
