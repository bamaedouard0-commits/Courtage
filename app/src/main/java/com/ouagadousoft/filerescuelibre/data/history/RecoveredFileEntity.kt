package com.ouagadousoft.filerescuelibre.data.history

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recovered_files",
    foreignKeys = [
        ForeignKey(
            entity = ScanSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class RecoveredFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val lastModifiedEpochSeconds: Long,
    /** Nom de [com.ouagadousoft.filerescuelibre.domain.model.FileCategory]. */
    val category: String,
    /** Nom de [com.ouagadousoft.filerescuelibre.domain.model.ReliabilityLevel]. */
    val reliability: String,
    /** Nom de [com.ouagadousoft.filerescuelibre.domain.model.ScanSource]. */
    val source: String,
)
