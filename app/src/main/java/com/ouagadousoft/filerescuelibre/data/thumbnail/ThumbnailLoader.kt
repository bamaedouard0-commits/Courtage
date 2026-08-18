package com.ouagadousoft.filerescuelibre.data.thumbnail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.ouagadousoft.filerescuelibre.domain.model.FileCategory
import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_DIMENSION_PX = 160

/**
 * Décodage de miniatures basse résolution, en pur Bitmap/BitmapFactory (pas de
 * dépendance Coil/Glide pour cette première version). Lit via root (`SuFileInputStream`,
 * comme le moteur de carving) car les fichiers de corbeille du scan rapide ne sont pas
 * accessibles par les API de fichiers normales sous scoped storage.
 *
 * Vidéos non gérées en V1 : générer une vignette nécessiterait MediaMetadataRetriever
 * avec un accès par descripteur de fichier, plus complexe à faire cohabiter avec la
 * lecture root ; l'UI retombe sur une icône générique pour cette catégorie.
 */
object ThumbnailLoader {

    /** Retourne `null` pour les vidéos, ou si le décodage échoue (fichier carvé tronqué/corrompu). */
    suspend fun load(file: RecoverableFile): Bitmap? {
        if (file.category != FileCategory.IMAGE) return null

        return withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                SuFileInputStream.open(file.path).use { BitmapFactory.decodeStream(it, null, bounds) }

                val options = BitmapFactory.Options().apply {
                    inSampleSize = computeSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION_PX)
                }
                SuFileInputStream.open(file.path).use { BitmapFactory.decodeStream(it, null, options) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
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
}
