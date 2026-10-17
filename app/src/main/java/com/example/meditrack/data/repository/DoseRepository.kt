package com.example.meditrack.data.repository

import com.example.meditrack.data.local.dao.DoseRecordDao
import com.example.meditrack.data.local.dao.ScheduleDao
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.model.MedicineAdherenceSummary
import com.example.meditrack.data.local.model.ScheduleAdherenceSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class DoseRepository(
    private val doseRecordDao: DoseRecordDao,
    private val scheduleDao: ScheduleDao
) {

    fun getDosesForDateRange(startTime: Long, endTime: Long): Flow<List<DoseRecord>> {
        return doseRecordDao.getDosesForDateRange(startTime, endTime)
    }

    fun getDosesForScheduleAndDateRange(scheduleId: Long, startTime: Long, endTime: Long): Flow<List<DoseRecord>> {
        return doseRecordDao.getDosesForScheduleAndDateRange(scheduleId, startTime, endTime)
    }

    fun getDosesForSchedule(scheduleId: Long): Flow<List<DoseRecord>> {
        return doseRecordDao.getDosesForSchedule(scheduleId)
    }

    fun getPendingRemindersForToday(): Flow<List<DoseRecord>> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis

        return doseRecordDao.getPendingRemindersForToday(startOfDay, endOfDay)
    }

    fun getTodayDoses(): Flow<List<DoseRecord>> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = cal.timeInMillis

        return doseRecordDao.getDosesForDateRange(startOfDay, endOfDay)
    }

    suspend fun recordDoseTaken(doseId: Long) {
        val dose = doseRecordDao.getDoseRecordById(doseId) ?: return
        val now = System.currentTimeMillis()
        val isLate = DoseRecord.calculateIsLate(dose.scheduledAt, now, DoseRecord.STATUS_TAKEN)
        doseRecordDao.updateStatus(
            id = doseId,
            status = DoseRecord.STATUS_TAKEN,
            recordedAt = now,
            isLate = isLate
        )
    }

    suspend fun recordDoseSkipped(doseId: Long) {
        val dose = doseRecordDao.getDoseRecordById(doseId) ?: return
        val now = System.currentTimeMillis()
        val isLate = DoseRecord.calculateIsLate(dose.scheduledAt, now, DoseRecord.STATUS_SKIPPED)
        doseRecordDao.updateStatus(
            id = doseId,
            status = DoseRecord.STATUS_SKIPPED,
            recordedAt = now,
            isLate = isLate
        )
    }

    suspend fun markExpiredPendingAsUnconfirmed() {
        // Expired if > 2 hours past scheduled time without user interaction
        val twoHoursAgo = System.currentTimeMillis() - (2 * 60 * 60 * 1000L)
        val expired = doseRecordDao.getExpiredPendingDoses(twoHoursAgo)
        for (dose in expired) {
            doseRecordDao.markAsUnconfirmed(dose.id)
        }
    }

    /**
     * Recent Dose Summary (Past N Days) grouped by Schedule
     */
    fun getRecentDoseSummary(daysBack: Int = 4): Flow<List<ScheduleAdherenceSummary>> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfToday = cal.timeInMillis

        cal.add(Calendar.DAY_OF_YEAR, -daysBack)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        return doseRecordDao.getDosesForDateRange(startTime, endOfToday).map { doses ->
            val groupedBySchedule = doses.groupBy { it.scheduleId }
            groupedBySchedule.map { (scheduleId, scheduleDoses) ->
                val schedule = scheduleDao.getScheduleByIdOnce(scheduleId)
                val scheduleName = schedule?.name ?: "Schedule #$scheduleId"

                val taken = scheduleDoses.count { it.status == DoseRecord.STATUS_TAKEN }
                val skipped = scheduleDoses.count { it.status == DoseRecord.STATUS_SKIPPED }
                val unconfirmed = scheduleDoses.count { it.status == DoseRecord.STATUS_UNCONFIRMED }
                val pending = scheduleDoses.count { it.status == DoseRecord.STATUS_PENDING }
                val late = scheduleDoses.count { it.isLate }

                ScheduleAdherenceSummary(
                    scheduleId = scheduleId,
                    scheduleName = scheduleName,
                    totalScheduled = scheduleDoses.size,
                    takenCount = taken,
                    skippedCount = skipped,
                    unconfirmedCount = unconfirmed,
                    pendingCount = pending,
                    lateCount = late
                )
            }
        }
    }

    /**
     * Medicine-level breakdown for a schedule in a date range
     */
    suspend fun getMedicineBreakdownForSchedule(
        scheduleId: Long,
        startTime: Long,
        endTime: Long
    ): List<MedicineAdherenceSummary> {
        val doses = doseRecordDao.getDosesForScheduleAndDateRangeOnce(scheduleId, startTime, endTime)
        val groupedByMedicine = doses.groupBy { it.medicineName }
        return groupedByMedicine.map { (medName, medDoses) ->
            MedicineAdherenceSummary(
                medicineName = medName,
                totalScheduled = medDoses.size,
                takenCount = medDoses.count { it.status == DoseRecord.STATUS_TAKEN },
                skippedCount = medDoses.count { it.status == DoseRecord.STATUS_SKIPPED },
                unconfirmedCount = medDoses.count { it.status == DoseRecord.STATUS_UNCONFIRMED },
                lateCount = medDoses.count { it.isLate }
            )
        }
    }

    /**
     * Generates daily dose occurrences for active schedules for a specific date if not already generated
     */
    suspend fun generateDailyDosesForDate(dateMillis: Long) {
        val activeSchedules = scheduleDao.getAllActiveFullSchedulesOnce()
        val cal = Calendar.getInstance()
        cal.timeInMillis = dateMillis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        val newDoses = mutableListOf<DoseRecord>()

        for (fullSchedule in activeSchedules) {
            val schedule = fullSchedule.schedule
            if (schedule.startDate > startOfDay) continue
            if (schedule.endDate != null && schedule.endDate < startOfDay) continue

            for (medicineWithTimes in fullSchedule.medicinesWithTimes) {
                val sm = medicineWithTimes.scheduleMedicine
                if (!sm.isActive) continue

                for (time in medicineWithTimes.times) {
                    if (!time.isEnabled) continue

                    val scheduledAt = startOfDay + (time.minutesSinceMidnight * 60 * 1000L)
                    val record = DoseRecord(
                        scheduleId = schedule.id,
                        scheduleMedicineId = sm.id,
                        scheduleMedicineTimeId = time.id,
                        medicineName = medicineWithTimes.medicine.name,
                        period = time.period,
                        scheduledAt = scheduledAt,
                        recordedAt = null,
                        status = DoseRecord.STATUS_PENDING,
                        isLate = false,
                        foodInstruction = sm.foodInstruction
                    )
                    newDoses.add(record)
                }
            }
        }

        if (newDoses.isNotEmpty()) {
            doseRecordDao.insertDoseRecords(newDoses)
        }
    }
}
