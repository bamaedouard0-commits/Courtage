package com.ouagadousoft.filerescuelibre.domain.model

sealed interface DeepScanEvent {
    data class Progress(val bytesScanned: Long, val totalBytes: Long) : DeepScanEvent
    data class FileFound(val file: RecoverableFile) : DeepScanEvent
}
