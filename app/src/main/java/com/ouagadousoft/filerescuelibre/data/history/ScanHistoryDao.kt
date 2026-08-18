package com.ouagadousoft.filerescuelibre.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Insert
    suspend fun insertSession(session: ScanSessionEntity): Long

    @Insert
    suspend fun insertFiles(files: List<RecoveredFileEntity>)

    @Query("SELECT * FROM scan_sessions ORDER BY timestampEpochSeconds DESC")
    fun observeSessions(): Flow<List<ScanSessionEntity>>

    @Query("SELECT * FROM recovered_files WHERE sessionId = :sessionId")
    suspend fun getFilesForSession(sessionId: Long): List<RecoveredFileEntity>

    @Query("DELETE FROM scan_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)
}
