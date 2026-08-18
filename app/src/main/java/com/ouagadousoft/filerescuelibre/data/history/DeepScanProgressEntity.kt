package com.ouagadousoft.filerescuelibre.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Ligne unique (id fixe) : un seul scan approfondi peut être "en cours" à la fois dans l'app. */
@Entity(tableName = "deep_scan_progress")
data class DeepScanProgressEntity(
    @PrimaryKey val id: Int = 0,
    val sessionId: Long,
    val devicePath: String,
    val position: Long,
    val totalBytes: Long,
)
