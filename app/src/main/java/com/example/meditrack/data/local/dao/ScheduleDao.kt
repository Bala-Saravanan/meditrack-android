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

    @Query("DELETE FROM schedule_medicine_times WHERE scheduleMedicineId = :scheduleMedicineId")
    suspend fun deleteScheduleMedicineTimes(scheduleMedicineId: Long)

    @Query("UPDATE schedule_medicine_times SET isEnabled = 0 WHERE scheduleMedicineId = :scheduleMedicineId")
    suspend fun disableScheduleMedicineTimes(scheduleMedicineId: Long)

    @Query("UPDATE schedule_medicines SET isActive = 0 WHERE id = :scheduleMedicineId")
    suspend fun disableScheduleMedicine(scheduleMedicineId: Long)

    @Query("UPDATE schedules SET status = :newStatus WHERE id = :scheduleId")
    suspend fun updateScheduleStatus(scheduleId: Long, newStatus: String)
}
