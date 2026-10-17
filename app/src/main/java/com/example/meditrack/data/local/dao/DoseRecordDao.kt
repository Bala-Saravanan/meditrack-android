package com.example.meditrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.meditrack.data.local.entity.DoseRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseRecordDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDoseRecord(record: DoseRecord): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDoseRecords(records: List<DoseRecord>): List<Long>

    @Update
    suspend fun updateDoseRecord(record: DoseRecord)

    @Query("SELECT * FROM dose_records WHERE id = :id")
    suspend fun getDoseRecordById(id: Long): DoseRecord?

    @Query("SELECT * FROM dose_records WHERE scheduleMedicineTimeId = :timeId AND scheduledAt = :scheduledAt LIMIT 1")
    suspend fun getDoseByTimeAndScheduledAt(timeId: Long, scheduledAt: Long): DoseRecord?

    @Query("SELECT * FROM dose_records WHERE scheduledAt >= :startTime AND scheduledAt < :endTime ORDER BY scheduledAt ASC")
    fun getDosesForDateRange(startTime: Long, endTime: Long): Flow<List<DoseRecord>>

    @Query("SELECT * FROM dose_records WHERE scheduledAt >= :startTime AND scheduledAt < :endTime ORDER BY scheduledAt ASC")
    suspend fun getDosesForDateRangeOnce(startTime: Long, endTime: Long): List<DoseRecord>

    @Query("SELECT * FROM dose_records WHERE scheduleId = :scheduleId AND scheduledAt >= :startTime AND scheduledAt < :endTime ORDER BY scheduledAt ASC")
    fun getDosesForScheduleAndDateRange(scheduleId: Long, startTime: Long, endTime: Long): Flow<List<DoseRecord>>

    @Query("SELECT * FROM dose_records WHERE scheduleId = :scheduleId AND scheduledAt >= :startTime AND scheduledAt < :endTime ORDER BY scheduledAt ASC")
    suspend fun getDosesForScheduleAndDateRangeOnce(scheduleId: Long, startTime: Long, endTime: Long): List<DoseRecord>

    @Query("SELECT * FROM dose_records WHERE scheduleId = :scheduleId ORDER BY scheduledAt ASC, id ASC")
    fun getDosesForSchedule(scheduleId: Long): Flow<List<DoseRecord>>

    @Query("SELECT * FROM dose_records WHERE scheduleId = :scheduleId ORDER BY scheduledAt ASC, id ASC")
    suspend fun getDosesForScheduleOnce(scheduleId: Long): List<DoseRecord>

    @Query("SELECT * FROM dose_records ORDER BY scheduledAt ASC, id ASC")
    fun getAllDoses(): Flow<List<DoseRecord>>

    @Query("SELECT d.* FROM dose_records d INNER JOIN schedules s ON d.scheduleId = s.id WHERE s.status = 'ACTIVE' AND d.status = 'PENDING' AND d.scheduledAt >= :startTime AND d.scheduledAt < :endTime ORDER BY d.scheduledAt ASC, d.id ASC")
    fun getPendingRemindersForToday(startTime: Long, endTime: Long): Flow<List<DoseRecord>>

    @Query("SELECT d.* FROM dose_records d INNER JOIN schedules s ON d.scheduleId = s.id WHERE s.status = 'ACTIVE' AND d.scheduledAt >= :startTime AND d.scheduledAt < :endTime ORDER BY d.scheduledAt ASC, d.id ASC")
    fun getActiveScheduleDosesForDateRange(startTime: Long, endTime: Long): Flow<List<DoseRecord>>

    @Query("SELECT * FROM dose_records WHERE status = 'PENDING' AND scheduledAt < :thresholdTime")
    suspend fun getExpiredPendingDoses(thresholdTime: Long): List<DoseRecord>

    @Query("UPDATE dose_records SET status = :status, recordedAt = :recordedAt, isLate = :isLate WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, recordedAt: Long?, isLate: Boolean)

    @Query("UPDATE dose_records SET status = 'UNCONFIRMED' WHERE id = :id AND status = 'PENDING'")
    suspend fun markAsUnconfirmed(id: Long)

    @Query("SELECT * FROM dose_records ORDER BY scheduledAt DESC LIMIT :limit")
    fun getRecentDoses(limit: Int): Flow<List<DoseRecord>>

    @Query("DELETE FROM dose_records WHERE scheduleMedicineTimeId = :timeId AND status = 'PENDING' AND recordedAt IS NULL")
    suspend fun deletePendingDosesForTime(timeId: Long)

    @Query("UPDATE dose_records SET status = 'PENDING', recordedAt = NULL, isLate = 0 WHERE scheduledAt > :now AND (status = 'TAKEN' OR status = 'SKIPPED')")
    suspend fun reconcileFutureCompletedDoses(now: Long)
}
