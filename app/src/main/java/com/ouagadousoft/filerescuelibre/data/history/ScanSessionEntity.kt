package com.ouagadousoft.filerescuelibre.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Nom de [com.ouagadousoft.filerescuelibre.domain.model.ScanType]. */
    val type: String,
    /** Nom de [com.ouagadousoft.filerescuelibre.domain.model.ScanZone], `null` pour un scan approfondi. */
    val zoneName: String?,
    val timestampEpochSeconds: Long,
    val resultCount: Int,
)
