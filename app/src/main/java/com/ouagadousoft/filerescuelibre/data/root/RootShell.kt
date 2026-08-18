package com.ouagadousoft.filerescuelibre.data.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Fine encapsulation de libsu pour exécuter des scripts shell root depuis des coroutines. */
object RootShell {
    suspend fun exec(script: String): List<String> = withContext(Dispatchers.IO) {
        Shell.cmd(script).exec().out
    }
}
