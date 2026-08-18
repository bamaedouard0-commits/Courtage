package com.ouagadousoft.filerescuelibre

import android.app.Application
import com.topjohnwu.superuser.Shell

class FileRescueApplication : Application() {

    companion object {
        init {
            Shell.enableVerboseLogging = BuildConfig.DEBUG
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
        }
    }
}
