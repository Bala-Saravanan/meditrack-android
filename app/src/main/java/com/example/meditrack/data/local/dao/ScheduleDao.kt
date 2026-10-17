package com.example.meditrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.data.local.model.FullScheduleDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY createdAt DESC")
    fun getAllSchedules(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE status = :status ORDER BY createdAt DESC")
    fun getSchedulesByStatus(status: String): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE id = :id")
    fun getScheduleById(id: Long): Flow<Schedule?>

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getScheduleByIdOnce(id: Long): Schedule?

    @Query("SELECT COUNT(*) FROM schedules WHERE status = 'Active'")
    fun getActiveSchedulesCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    fun getFullScheduleDetails(scheduleId: Long): Flow<FullScheduleDetails?>

    @Transaction
    @Query("SELECT * FROM schedules WHERE id = :scheduleId")
    suspend fun getFullScheduleDetailsOnce(scheduleId: Long): FullScheduleDetails?

    @Transaction
    @Query("SELECT * FROM schedules ORDER BY createdAt DESC")
    fun getAllFullSchedules(): Flow<List<FullScheduleDetails>>

    @Transaction
    @Query("SELECT * FROM schedules WHERE status = 'Active' ORDER BY createdAt DESC")
    suspend fun getAllActiveFullSchedulesOnce(): List<FullScheduleDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule): Long

    @Update
    suspend fun updateSchedule(schedule: Schedule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleMedicine(scheduleMedicine: ScheduleMedicine): Long

    @Update
    suspend fun updateScheduleMedicine(scheduleMedicine: ScheduleMedicine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleMedicineTime(time: ScheduleMedicineTime): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleMedicineTimes(times: List<ScheduleMedicineTime>): List<Long>

    @Transaction
    suspend fun insertScheduleWithMedicinesAndTimes(
        schedule: Schedule,
        medicinesWithTimes: List<Pair<ScheduleMedicine, List<ScheduleMedicineTime>>>
    ): Long {
        val scheduleId = insertSchedule(schedule)
        for ((medicine, times) in medicinesWithTimes) {
            val assignedMedicine = medicine.copy(scheduleId = scheduleId)
            val scheduleMedicineId = insertScheduleMedicine(assignedMedicine)
            val assignedTimes = times.map { it.copy(scheduleMedicineId = scheduleMedicineId) }
            insertScheduleMedicineTimes(assignedTimes)
        }
        return scheduleId
    }

    @Update
    suspend fun updateScheduleMedicineTime(time: ScheduleMedicineTime)

    @Query("SELECT * FROM schedule_medicines WHERE scheduleId = :scheduleId")
    suspend fun getScheduleMedicinesForScheduleOnce(scheduleId: Long): List<ScheduleMedicine>

    @Query("SELECT * FROM schedule_medicine_times WHERE scheduleMedicineId = :scheduleMedicineId")
    suspend fun getScheduleMedicineTimesOnce(scheduleMedicineId: Long): List<ScheduleMedicineTime>

    @Transaction
    suspend fun updateScheduleWithMedicinesAndTimes(
        schedule: Schedule,
        medicinesWithTimes: List<Pair<ScheduleMedicine, List<ScheduleMedicineTime>>>
    ) {
        updateSchedule(schedule)
        val existingMedicines = getScheduleMedicinesForScheduleOnce(schedule.id)
        val newMedicineIds = medicinesWithTimes.map { it.first.medicineId }.toSet()

        // Deactivate medicines no longer assigned
        for (existing in existingMedicines) {
            if (existing.medicineId !in newMedicineIds) {
                disableScheduleMedicine(existing.id)
                disableScheduleMedicineTimes(existing.id)
                val times = getScheduleMedicineTimesOnce(existing.id)
                for (t in times) {
                    deletePendingDosesForTime(t.id)
                }
            }
        }

        // Insert or update medicines and times
        for ((medDraft, timesDraft) in medicinesWithTimes) {
            val existing = existingMedicines.find { it.medicineId == medDraft.medicineId }
            val scheduleMedicineId = if (existing != null) {
                val updated = existing.copy(
                    foodInstruction = medDraft.foodInstruction,
                    doseQuantity = medDraft.doseQuantity,
                    isActive = true
                )
                updateScheduleMedicine(updated)
                existing.id
            } else {
                insertScheduleMedicine(medDraft.copy(scheduleId = schedule.id, isActive = true))
            }

            val existingTimes = getScheduleMedicineTimesOnce(scheduleMedicineId)
            val newPeriods = timesDraft.map { it.period }.toSet()

            for (time in existingTimes) {
                if (time.period !in newPeriods) {
                    updateScheduleMedicineTime(time.copy(isEnabled = false))
                    deletePendingDosesForTime(time.id)
                }
            }

            for (timeDraft in timesDraft) {
                val existingTime = existingTimes.find { it.period == timeDraft.period }
                if (existingTime != null) {
                    val timeChanged = existingTime.minutesSinceMidnight != timeDraft.minutesSinceMidnight
                    val becameDisabled = existingTime.isEnabled && !timeDraft.isEnabled
                    if (timeChanged || becameDisabled) {
                        deletePendingDosesForTime(existingTime.id)
                    }
                    updateScheduleMedicineTime(
                        existingTime.copy(
                            minutesSinceMidnight = timeDraft.minutesSinceMidnight,
                            isEnabled = timeDraft.isEnabled
                        )
                    )
                } else {
                    insertScheduleMedicineTime(
                        timeDraft.copy(scheduleMedicineId = scheduleMedicineId)
                    )
                }
            }
        }
    }

    @Query("DELETE FROM schedule_medicine_times WHERE scheduleMedicineId = :scheduleMedicineId")
    suspend fun deleteScheduleMedicineTimes(scheduleMedicineId: Long)

    @Query("UPDATE schedule_medicine_times SET isEnabled = 0 WHERE scheduleMedicineId = :scheduleMedicineId")
    suspend fun disableScheduleMedicineTimes(scheduleMedicineId: Long)

    @Query("UPDATE schedule_medicines SET isActive = 0 WHERE id = :scheduleMedicineId")
    suspend fun disableScheduleMedicine(scheduleMedicineId: Long)

    @Query("UPDATE schedules SET status = :newStatus WHERE id = :scheduleId")
    suspend fun updateScheduleStatus(scheduleId: Long, newStatus: String)

    @Query("DELETE FROM dose_records WHERE scheduleMedicineTimeId = :timeId AND status = 'PENDING' AND recordedAt IS NULL")
    suspend fun deletePendingDosesForTime(timeId: Long)
}
