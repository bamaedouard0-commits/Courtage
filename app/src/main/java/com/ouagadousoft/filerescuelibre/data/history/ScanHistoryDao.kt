package com.ouagadousoft.filerescuelibre.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Insert
    suspend fun insertSession(session: ScanSessionEntity): Long

    @Insert
    suspend fun insertFiles(files: List<RecoveredFileEntity>)

    @Insert
    suspend fun insertFile(file: RecoveredFileEntity)

    @Query("UPDATE scan_sessions SET resultCount = resultCount + 1 WHERE id = :sessionId")
    suspend fun incrementResultCount(sessionId: Long)

    @Query("SELECT * FROM scan_sessions ORDER BY timestampEpochSeconds DESC")
    fun observeSessions(): Flow<List<ScanSessionEntity>>

    @Query("SELECT * FROM recovered_files WHERE sessionId = :sessionId")
    suspend fun getFilesForSession(sessionId: Long): List<RecoveredFileEntity>

    @Query("DELETE FROM scan_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    /** Supprime les scans au-delà des [limit] plus récents (les fichiers liés suivent en cascade). */
    @Query(
        """
        DELETE FROM scan_sessions WHERE id NOT IN
        (SELECT id FROM scan_sessions ORDER BY timestampEpochSeconds DESC LIMIT :limit)
        """
    )
    suspend fun pruneSessions(limit: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDeepScanProgress(progress: DeepScanProgressEntity)

    @Query("SELECT * FROM deep_scan_progress WHERE id = 0")
    suspend fun getDeepScanProgress(): DeepScanProgressEntity?

    @Query("UPDATE deep_scan_progress SET position = :position WHERE id = 0")
    suspend fun updateDeepScanProgressPosition(position: Long)

    @Query("DELETE FROM deep_scan_progress")
    suspend fun clearDeepScanProgress()
}
