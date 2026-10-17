package com.example.meditrack.data.repository

import com.example.meditrack.data.local.dao.DoseRecordDao
import com.example.meditrack.data.local.dao.ScheduleDao
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.model.DateRangeType
import com.example.meditrack.data.local.model.MedicineAdherenceSummary
import com.example.meditrack.data.local.model.ScheduleAdherenceSummary
import com.example.meditrack.data.local.model.ScheduleDetailedReport
import com.example.meditrack.data.local.model.ScheduleReportCardItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
        if (dose.scheduledAt > now) {
            // Future scheduled doses must remain upcoming and cannot be marked taken
            return
        }
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
        if (dose.scheduledAt > now) {
            // Future scheduled doses must remain upcoming and cannot be marked skipped
            return
        }
        val isLate = DoseRecord.calculateIsLate(dose.scheduledAt, now, DoseRecord.STATUS_SKIPPED)
        doseRecordDao.updateStatus(
            id = doseId,
            status = DoseRecord.STATUS_SKIPPED,
            recordedAt = now,
            isLate = isLate
        )
    }

    suspend fun reconcileFutureDoses(now: Long = System.currentTimeMillis()) {
        doseRecordDao.reconcileFutureCompletedDoses(now)
    }

    suspend fun markExpiredPendingAsUnconfirmed(windowMs: Long = DoseRecord.LATE_THRESHOLD_MS) {
        val threshold = System.currentTimeMillis() - windowMs
        val expired = doseRecordDao.getExpiredPendingDoses(threshold)
        for (dose in expired) {
            doseRecordDao.markAsUnconfirmed(dose.id)
        }
    }

    suspend fun getDoseById(doseId: Long): DoseRecord? {
        return doseRecordDao.getDoseRecordById(doseId)
    }

    suspend fun getScheduleById(scheduleId: Long): com.example.meditrack.data.local.entity.Schedule? {
        return scheduleDao.getScheduleByIdOnce(scheduleId)
    }

    suspend fun getFullScheduleDetails(scheduleId: Long): com.example.meditrack.data.local.model.FullScheduleDetails? {
        return scheduleDao.getFullScheduleDetailsOnce(scheduleId)
    }

    suspend fun getScheduleName(scheduleId: Long): String {
        return scheduleDao.getScheduleByIdOnce(scheduleId)?.name ?: "Schedule #$scheduleId"
    }

    suspend fun getOrCreateDoseRecord(
        scheduleId: Long,
        scheduleMedicineId: Long,
        scheduleMedicineTimeId: Long,
        medicineName: String,
        period: String,
        scheduledAt: Long,
        foodInstruction: String
    ): DoseRecord {
        val existing = doseRecordDao.getDoseByTimeAndScheduledAt(scheduleMedicineTimeId, scheduledAt)
        if (existing != null) return existing

        val newRecord = DoseRecord(
            scheduleId = scheduleId,
            scheduleMedicineId = scheduleMedicineId,
            scheduleMedicineTimeId = scheduleMedicineTimeId,
            medicineName = medicineName,
            period = period,
            scheduledAt = scheduledAt,
            recordedAt = null,
            status = DoseRecord.STATUS_PENDING,
            isLate = false,
            foodInstruction = foodInstruction
        )
        val id = doseRecordDao.insertDoseRecord(newRecord)
        return if (id > 0) {
            newRecord.copy(id = id)
        } else {
            doseRecordDao.getDoseByTimeAndScheduledAt(scheduleMedicineTimeId, scheduledAt) ?: newRecord
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

        return doseRecordDao.getActiveScheduleDosesForDateRange(startTime, endOfToday).map { doses ->
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

            // Normalize start of schedule start date to start of day
            val scheduleStartCal = Calendar.getInstance().apply {
                timeInMillis = schedule.startDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (startOfDay < scheduleStartCal.timeInMillis) continue

            // Normalize end of schedule end date to end of day
            if (schedule.endDate != null) {
                val scheduleEndCal = Calendar.getInstance().apply {
                    timeInMillis = schedule.endDate
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                if (startOfDay > scheduleEndCal.timeInMillis) continue
            }

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

    /**
     * Centralized adherence calculation to guarantee 100% metric consistency across
     * Report Card, Detailed Report Screen, and PDF export.
     */
    fun computeScheduleAdherenceSummary(
        scheduleId: Long,
        scheduleName: String,
        doses: List<DoseRecord>,
        now: Long = System.currentTimeMillis()
    ): ScheduleAdherenceSummary {
        val pastOrDueDoses = doses.filter { it.scheduledAt <= now }
        val futureDoses = doses.filter { it.scheduledAt > now }

        val taken = pastOrDueDoses.count { it.status == DoseRecord.STATUS_TAKEN }
        val skipped = pastOrDueDoses.count { it.status == DoseRecord.STATUS_SKIPPED }
        val unconfirmed = pastOrDueDoses.count { it.status == DoseRecord.STATUS_UNCONFIRMED }
        val pending = pastOrDueDoses.count { it.status == DoseRecord.STATUS_PENDING } + futureDoses.size
        val late = pastOrDueDoses.count { it.isLate && it.status == DoseRecord.STATUS_TAKEN }

        return ScheduleAdherenceSummary(
            scheduleId = scheduleId,
            scheduleName = scheduleName,
            totalScheduled = doses.size,
            takenCount = taken,
            skippedCount = skipped,
            unconfirmedCount = unconfirmed,
            pendingCount = pending,
            lateCount = late
        )
    }

    /**
     * Emits a reactive list of schedule report card items for all schedules in Room.
     */
    fun getAllScheduleReportCards(): Flow<List<ScheduleReportCardItem>> {
        return combine(
            scheduleDao.getAllSchedules(),
            doseRecordDao.getAllDoses()
        ) { schedules, allDoses ->
            val now = System.currentTimeMillis()
            val dosesBySchedule = allDoses.groupBy { it.scheduleId }
            schedules.map { schedule ->
                val (start30, end30) = calculateDateRangeBounds(schedule, DateRangeType.LAST_30_DAYS)
                val doses = dosesBySchedule[schedule.id].orEmpty().filter { it.scheduledAt in start30..end30 }
                val summary = computeScheduleAdherenceSummary(schedule.id, schedule.name, doses, now)

                ScheduleReportCardItem(
                    scheduleId = schedule.id,
                    scheduleName = schedule.name,
                    scheduleStatus = schedule.status,
                    startDate = schedule.startDate,
                    endDate = schedule.endDate,
                    adherenceSummary = summary
                )
            }
        }
    }

    /**
     * Computes start and end timestamps (inclusive) for a schedule and date range type.
     */
    fun calculateDateRangeBounds(
        schedule: Schedule,
        rangeType: DateRangeType,
        customStart: Long? = null,
        customEnd: Long? = null
    ): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        return when (rangeType) {
            DateRangeType.LAST_7_DAYS -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                Pair(start, end)
            }
            DateRangeType.LAST_30_DAYS -> {
                cal.timeInMillis = now
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                cal.add(Calendar.DAY_OF_YEAR, -29)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                Pair(start, end)
            }
            DateRangeType.ENTIRE_PERIOD -> {
                cal.timeInMillis = schedule.startDate
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                val endMillis = if (schedule.endDate != null && schedule.endDate > 0) {
                    schedule.endDate
                } else {
                    maxOf(now, schedule.startDate + (30L * 24 * 60 * 60 * 1000L))
                }
                cal.timeInMillis = endMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateRangeType.CUSTOM -> {
                val s = customStart ?: (now - (7L * 24 * 60 * 60 * 1000L))
                val e = customEnd ?: now
                val validStart = minOf(s, e)
                val validEnd = maxOf(s, e)

                cal.timeInMillis = validStart
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.timeInMillis = validEnd
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
        }
    }

    /**
     * Returns a reactive Flow of the detailed report for an individual schedule.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getScheduleDetailedReport(
        scheduleId: Long,
        rangeType: DateRangeType,
        customStart: Long? = null,
        customEnd: Long? = null
    ): Flow<ScheduleDetailedReport?> {
        return scheduleDao.getScheduleById(scheduleId).flatMapLatest { schedule ->
            if (schedule == null) {
                flowOf(null)
            } else {
                val (startTime, endTime) = calculateDateRangeBounds(schedule, rangeType, customStart, customEnd)
                doseRecordDao.getDosesForScheduleAndDateRange(scheduleId, startTime, endTime).map { doses ->
                    val now = System.currentTimeMillis()
                    val summary = computeScheduleAdherenceSummary(schedule.id, schedule.name, doses, now)

                    val medicineSummaries = doses.groupBy { it.medicineName }.map { (medName, medDoses) ->
                        MedicineAdherenceSummary(
                            medicineName = medName,
                            totalScheduled = medDoses.size,
                            takenCount = medDoses.count { it.status == DoseRecord.STATUS_TAKEN && it.scheduledAt <= now },
                            skippedCount = medDoses.count { it.status == DoseRecord.STATUS_SKIPPED && it.scheduledAt <= now },
                            unconfirmedCount = medDoses.count { it.status == DoseRecord.STATUS_UNCONFIRMED && it.scheduledAt <= now },
                            lateCount = medDoses.count { it.isLate && it.status == DoseRecord.STATUS_TAKEN }
                        )
                    }

                    val hasFutureScheduledDoses = doses.any { it.scheduledAt > now }

                    ScheduleDetailedReport(
                        scheduleId = schedule.id,
                        scheduleName = schedule.name,
                        scheduleStatus = schedule.status,
                        startDate = schedule.startDate,
                        endDate = schedule.endDate,
                        physicianName = schedule.physicianName,
                        notes = schedule.notes,
                        dateRangeType = rangeType,
                        startTimestamp = startTime,
                        endTimestamp = endTime,
                        adherenceSummary = summary,
                        medicineSummaries = medicineSummaries,
                        doseRecords = doses,
                        hasFutureScheduledDoses = hasFutureScheduledDoses
                    )
                }
            }
        }
    }

    /**
     * Retrieves a one-shot snapshot of the detailed report for background operations (e.g. PDF generation).
     */
    suspend fun getScheduleDetailedReportOnce(
        scheduleId: Long,
        rangeType: DateRangeType,
        customStart: Long? = null,
        customEnd: Long? = null
    ): ScheduleDetailedReport? {
        val schedule = scheduleDao.getScheduleByIdOnce(scheduleId) ?: return null
        val (startTime, endTime) = calculateDateRangeBounds(schedule, rangeType, customStart, customEnd)
        val doses = doseRecordDao.getDosesForScheduleAndDateRangeOnce(scheduleId, startTime, endTime)

        val now = System.currentTimeMillis()
        val summary = computeScheduleAdherenceSummary(schedule.id, schedule.name, doses, now)

        val medicineSummaries = doses.groupBy { it.medicineName }.map { (medName, medDoses) ->
            MedicineAdherenceSummary(
                medicineName = medName,
                totalScheduled = medDoses.size,
                takenCount = medDoses.count { it.status == DoseRecord.STATUS_TAKEN && it.scheduledAt <= now },
                skippedCount = medDoses.count { it.status == DoseRecord.STATUS_SKIPPED && it.scheduledAt <= now },
                unconfirmedCount = medDoses.count { it.status == DoseRecord.STATUS_UNCONFIRMED && it.scheduledAt <= now },
                lateCount = medDoses.count { it.isLate && it.status == DoseRecord.STATUS_TAKEN }
            )
        }

        val hasFutureScheduledDoses = doses.any { it.scheduledAt > now }

        return ScheduleDetailedReport(
            scheduleId = schedule.id,
            scheduleName = schedule.name,
            scheduleStatus = schedule.status,
            startDate = schedule.startDate,
            endDate = schedule.endDate,
            physicianName = schedule.physicianName,
            notes = schedule.notes,
            dateRangeType = rangeType,
            startTimestamp = startTime,
            endTimestamp = endTime,
            adherenceSummary = summary,
            medicineSummaries = medicineSummaries,
            doseRecords = doses,
            hasFutureScheduledDoses = hasFutureScheduledDoses
        )
    }
}
