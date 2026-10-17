package com.example.meditrack.data.repository

import com.example.meditrack.data.local.dao.ScheduleDao
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.data.local.model.FullScheduleDetails
import kotlinx.coroutines.flow.Flow

class ScheduleRepository(private val scheduleDao: ScheduleDao) {

    val allSchedules: Flow<List<Schedule>> = scheduleDao.getAllSchedules()
    val activeSchedulesCount: Flow<Int> = scheduleDao.getActiveSchedulesCount()
    val allFullSchedules: Flow<List<FullScheduleDetails>> = scheduleDao.getAllFullSchedules()

    fun getSchedulesByStatus(status: String): Flow<List<Schedule>> {
        return scheduleDao.getSchedulesByStatus(status)
    }

    fun getFullScheduleDetails(id: Long): Flow<FullScheduleDetails?> {
        return scheduleDao.getFullScheduleDetails(id)
    }

    suspend fun getFullScheduleDetailsOnce(id: Long): FullScheduleDetails? {
        return scheduleDao.getFullScheduleDetailsOnce(id)
    }

    suspend fun createScheduleWithMedicines(
        schedule: Schedule,
        medicinesWithTimes: List<Pair<ScheduleMedicine, List<ScheduleMedicineTime>>>
    ): Long {
        return scheduleDao.insertScheduleWithMedicinesAndTimes(schedule, medicinesWithTimes)
    }

    suspend fun updateScheduleStatus(scheduleId: Long, status: String) {
        scheduleDao.updateScheduleStatus(scheduleId, status)
    }

    suspend fun updateSchedule(schedule: Schedule) {
        scheduleDao.updateSchedule(schedule)
    }

    suspend fun getScheduleByIdOnce(id: Long): Schedule? {
        return scheduleDao.getScheduleByIdOnce(id)
    }

    suspend fun updateScheduleWithMedicines(
        schedule: Schedule,
        medicinesWithTimes: List<Pair<ScheduleMedicine, List<ScheduleMedicineTime>>>
    ) {
        scheduleDao.updateScheduleWithMedicinesAndTimes(schedule, medicinesWithTimes)
    }

    suspend fun getAllActiveFullSchedulesOnce(): List<FullScheduleDetails> {
        return scheduleDao.getAllActiveFullSchedulesOnce()
    }
}
