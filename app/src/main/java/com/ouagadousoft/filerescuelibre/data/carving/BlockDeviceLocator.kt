package com.ouagadousoft.filerescuelibre.data.carving

import com.ouagadousoft.filerescuelibre.data.root.RootShell

/**
 * Résout le chemin du périphérique bloc brut de la partition data et sa taille.
 * Nécessite root ; les chemins varient selon les fabricants, d'où la liste de
 * candidats connus avant un dernier recours par recherche large.
 */
object BlockDeviceLocator {

    private val candidatePaths = listOf(
        "/dev/block/by-name/userdata",
        "/dev/block/bootdevice/by-name/userdata",
    )

    suspend fun resolveUserdataDevice(): String? {
        for (candidate in candidatePaths) {
            val resolved = RootShell.exec("test -e \"$candidate\" && readlink -f \"$candidate\"")
                .firstOrNull()
                ?.trim()
            if (!resolved.isNullOrBlank()) return resolved
        }

        return RootShell.exec("find /dev/block -iname userdata 2>/dev/null | head -n 1")
            .firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    suspend fun deviceSizeBytes(devicePath: String): Long? {
        return RootShell.exec("blockdev --getsize64 \"$devicePath\"")
            .firstOrNull()
            ?.trim()
            ?.toLongOrNull()
    }
}
