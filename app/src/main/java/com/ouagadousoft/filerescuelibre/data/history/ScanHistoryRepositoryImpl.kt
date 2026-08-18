package com.ouagadousoft.filerescuelibre.data.history

import com.ouagadousoft.filerescuelibre.domain.model.FileCategory
import com.ouagadousoft.filerescuelibre.domain.model.RecoverableFile
import com.ouagadousoft.filerescuelibre.domain.model.ReliabilityLevel
import com.ouagadousoft.filerescuelibre.domain.model.ResumableDeepScan
import com.ouagadousoft.filerescuelibre.domain.model.ScanHistoryEntry
import com.ouagadousoft.filerescuelibre.domain.model.ScanSource
import com.ouagadousoft.filerescuelibre.domain.model.ScanType
import com.ouagadousoft.filerescuelibre.domain.model.ScanZone
import com.ouagadousoft.filerescuelibre.domain.repository.ScanHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanHistoryRepositoryImpl(private val dao: ScanHistoryDao) : ScanHistoryRepository {

    override suspend fun saveScan(type: ScanType, zone: ScanZone?, results: List<RecoverableFile>) {
        val sessionId = dao.insertSession(
            ScanSessionEntity(
                type = type.name,
                zoneName = zone?.name,
                timestampEpochSeconds = System.currentTimeMillis() / 1000,
                resultCount = results.size,
            )
        )
        if (results.isNotEmpty()) {
            dao.insertFiles(results.map { it.toEntity(sessionId) })
        }
    }

    override fun observeHistory(): Flow<List<ScanHistoryEntry>> =
        dao.observeSessions().map { sessions -> sessions.map { it.toDomain() } }

    override suspend fun resultsForScan(scanId: Long): List<RecoverableFile> =
        dao.getFilesForSession(scanId).map { it.toDomain() }

    override suspend fun deleteScan(scanId: Long) {
        dao.deleteSession(scanId)
    }

    override suspend fun getResumableDeepScan(): ResumableDeepScan? {
        val progress = dao.getDeepScanProgress() ?: return null
        return ResumableDeepScan(
            sessionId = progress.sessionId,
            devicePath = progress.devicePath,
            position = progress.position,
            totalBytes = progress.totalBytes,
            existingResults = dao.getFilesForSession(progress.sessionId).map { it.toDomain() },
        )
    }

    override suspend fun beginDeepScanSession(devicePath: String, totalBytes: Long): Long {
        val sessionId = dao.insertSession(
            ScanSessionEntity(
                type = ScanType.DEEP.name,
                zoneName = null,
                timestampEpochSeconds = System.currentTimeMillis() / 1000,
                resultCount = 0,
            )
        )
        dao.upsertDeepScanProgress(
            DeepScanProgressEntity(
                sessionId = sessionId,
                devicePath = devicePath,
                position = 0,
                totalBytes = totalBytes,
            )
        )
        return sessionId
    }

    override suspend fun recordDeepScanFile(sessionId: Long, file: RecoverableFile) {
        dao.insertFile(file.toEntity(sessionId))
        dao.incrementResultCount(sessionId)
    }

    override suspend fun recordDeepScanPosition(sessionId: Long, position: Long) {
        dao.updateDeepScanProgressPosition(position)
    }

    override suspend fun finishDeepScanSession(sessionId: Long) {
        dao.clearDeepScanProgress()
    }
}

private fun ScanSessionEntity.toDomain() = ScanHistoryEntry(
    id = id,
    type = ScanType.valueOf(type),
    zone = zoneName?.let { ScanZone.valueOf(it) },
    timestampEpochSeconds = timestampEpochSeconds,
    resultCount = resultCount,
)

private fun RecoverableFile.toEntity(sessionId: Long) = RecoveredFileEntity(
    sessionId = sessionId,
    path = path,
    name = name,
    sizeBytes = sizeBytes,
    lastModifiedEpochSeconds = lastModifiedEpochSeconds,
    category = category.name,
    reliability = reliability.name,
    source = source.name,
)

private fun RecoveredFileEntity.toDomain() = RecoverableFile(
    path = path,
    name = name,
    sizeBytes = sizeBytes,
    lastModifiedEpochSeconds = lastModifiedEpochSeconds,
    category = FileCategory.valueOf(category),
    reliability = ReliabilityLevel.valueOf(reliability),
    source = ScanSource.valueOf(source),
)
