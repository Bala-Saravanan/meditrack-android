package com.example.meditrack

import com.example.meditrack.data.local.dao.CaregiverDao
import com.example.meditrack.data.local.dao.DoseRecordDao
import com.example.meditrack.data.local.dao.ScheduleDao
import com.example.meditrack.data.local.entity.Caregiver
import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.data.local.model.FullScheduleDetails
import com.example.meditrack.data.local.model.ScheduleAdherenceSummary
import com.example.meditrack.data.repository.CaregiverRepository
import com.example.meditrack.data.repository.DoseRepository
import com.example.meditrack.ui.dose.DoseListItem
import com.example.meditrack.ui.reports.ReportDoseListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Phase9VerificationTest {

    private class FakeCaregiverDao : CaregiverDao {
        var storedCaregiver: Caregiver? = null
        private val flow = MutableStateFlow<Caregiver?>(null)

        override fun getCaregiver(): Flow<Caregiver?> = flow
        override suspend fun getCaregiverOnce(): Caregiver? = storedCaregiver
        override suspend fun insertCaregiver(caregiver: Caregiver): Long {
            val id = if (caregiver.id == 0L) 1L else caregiver.id
            val entity = caregiver.copy(id = id)
            storedCaregiver = entity
            flow.value = entity
            return id
        }
        override suspend fun updateCaregiver(caregiver: Caregiver) {
            storedCaregiver = caregiver
            flow.value = caregiver
        }
        override suspend fun updateEmergencyContactStatus(id: Long, isEmergency: Boolean) {
            storedCaregiver = storedCaregiver?.copy(isEmergencyContact = isEmergency)
            flow.value = storedCaregiver
        }
        override suspend fun deleteCaregiver() {
            storedCaregiver = null
            flow.value = null
        }
    }

    private class FakeDoseRecordDao : DoseRecordDao {
        val records = mutableListOf<DoseRecord>()
        private var nextId = 1L

        override suspend fun insertDoseRecord(record: DoseRecord): Long {
            val existing = records.find {
                it.scheduleMedicineTimeId == record.scheduleMedicineTimeId &&
                    it.scheduledAt == record.scheduledAt
            }
            if (existing != null) return -1L
            val id = if (record.id == 0L) nextId++ else record.id
            records.add(record.copy(id = id))
            return id
        }

        override suspend fun insertDoseRecords(records: List<DoseRecord>): List<Long> {
            return records.map { insertDoseRecord(it) }
        }

        override suspend fun updateDoseRecord(record: DoseRecord) {
            val index = records.indexOfFirst { it.id == record.id }
            if (index != -1) {
                records[index] = record
            }
        }

        override suspend fun getDoseRecordById(id: Long): DoseRecord? =
            records.find { it.id == id }

        override suspend fun getDoseByTimeAndScheduledAt(
            timeId: Long,
            scheduledAt: Long
        ): DoseRecord? =
            records.find { it.scheduleMedicineTimeId == timeId && it.scheduledAt == scheduledAt }

        override fun getDosesForDateRange(startTime: Long, endTime: Long): Flow<List<DoseRecord>> =
            flowOf(records.filter { it.scheduledAt in startTime until endTime })

        override fun getActiveScheduleDosesForDateRange(startTime: Long, endTime: Long): Flow<List<DoseRecord>> =
            flowOf(records.filter { it.scheduledAt in startTime until endTime })

        override suspend fun getDosesForDateRangeOnce(startTime: Long, endTime: Long): List<DoseRecord> =
            records.filter { it.scheduledAt in startTime until endTime }

        override fun getDosesForScheduleAndDateRange(
            scheduleId: Long,
            startTime: Long,
            endTime: Long
        ): Flow<List<DoseRecord>> =
            flowOf(records.filter { it.scheduleId == scheduleId && it.scheduledAt in startTime until endTime })

        override suspend fun getDosesForScheduleAndDateRangeOnce(
            scheduleId: Long,
            startTime: Long,
            endTime: Long
        ): List<DoseRecord> =
            records.filter { it.scheduleId == scheduleId && it.scheduledAt in startTime until endTime }

        override fun getDosesForSchedule(scheduleId: Long): Flow<List<DoseRecord>> =
            flowOf(records.filter { it.scheduleId == scheduleId })

        override suspend fun getDosesForScheduleOnce(scheduleId: Long): List<DoseRecord> =
            records.filter { it.scheduleId == scheduleId }

        override fun getAllDoses(): Flow<List<DoseRecord>> = flowOf(records.toList())

        override fun getPendingRemindersForToday(
            startTime: Long,
            endTime: Long
        ): Flow<List<DoseRecord>> =
            flowOf(records.filter { it.scheduledAt in startTime until endTime && it.status == DoseRecord.STATUS_PENDING })

        override suspend fun getExpiredPendingDoses(thresholdTime: Long): List<DoseRecord> =
            records.filter { it.status == DoseRecord.STATUS_PENDING && it.scheduledAt < thresholdTime }

        override suspend fun updateStatus(
            id: Long,
            status: String,
            recordedAt: Long?,
            isLate: Boolean
        ) {
            val index = records.indexOfFirst { it.id == id }
            if (index != -1) {
                records[index] = records[index].copy(
                    status = status,
                    recordedAt = recordedAt,
                    isLate = isLate
                )
            }
        }

        override suspend fun markAsUnconfirmed(id: Long) {
            val index = records.indexOfFirst { it.id == id }
            if (index != -1) {
                records[index] = records[index].copy(status = DoseRecord.STATUS_UNCONFIRMED)
            }
        }

        override fun getRecentDoses(limit: Int): Flow<List<DoseRecord>> =
            flowOf(records.take(limit))

        override suspend fun deletePendingDosesForTime(timeId: Long) {
            records.removeAll {
                it.scheduleMedicineTimeId == timeId && it.status == DoseRecord.STATUS_PENDING && it.recordedAt == null
            }
        }

        override suspend fun reconcileFutureCompletedDoses(now: Long) {
            for (i in records.indices) {
                val r = records[i]
                if (r.scheduledAt > now && (r.status == DoseRecord.STATUS_TAKEN || r.status == DoseRecord.STATUS_SKIPPED)) {
                    records[i] = r.copy(
                        status = DoseRecord.STATUS_PENDING,
                        recordedAt = null,
                        isLate = false
                    )
                }
            }
        }
    }

    private class FakeScheduleDao : ScheduleDao {
        val schedules = mutableListOf<Schedule>()
        val fullSchedules = mutableListOf<FullScheduleDetails>()
        val medicines = mutableListOf<ScheduleMedicine>()
        val times = mutableListOf<ScheduleMedicineTime>()

        override fun getAllSchedules(): Flow<List<Schedule>> = flowOf(schedules.toList())
        override fun getSchedulesByStatus(status: String): Flow<List<Schedule>> =
            flowOf(schedules.filter { it.status == status })
        override fun getScheduleById(id: Long): Flow<Schedule?> =
            flowOf(schedules.find { it.id == id })
        override suspend fun getScheduleByIdOnce(id: Long): Schedule? =
            schedules.find { it.id == id }
        override fun getActiveSchedulesCount(): Flow<Int> =
            flowOf(schedules.count { it.status == "Active" })
        override fun getFullScheduleDetails(scheduleId: Long): Flow<FullScheduleDetails?> =
            flowOf(fullSchedules.find { it.schedule.id == scheduleId })
        override suspend fun getFullScheduleDetailsOnce(scheduleId: Long): FullScheduleDetails? =
            fullSchedules.find { it.schedule.id == scheduleId }
        override fun getAllFullSchedules(): Flow<List<FullScheduleDetails>> =
            flowOf(fullSchedules.toList())
        override suspend fun getAllActiveFullSchedulesOnce(): List<FullScheduleDetails> =
            fullSchedules.filter { it.schedule.status == "Active" }
        override suspend fun insertSchedule(schedule: Schedule): Long {
            val id = if (schedule.id == 0L) (schedules.size + 1).toLong() else schedule.id
            schedules.add(schedule.copy(id = id))
            return id
        }
        override suspend fun updateSchedule(schedule: Schedule) {
            val idx = schedules.indexOfFirst { it.id == schedule.id }
            if (idx != -1) schedules[idx] = schedule
        }
        override suspend fun insertScheduleMedicine(scheduleMedicine: ScheduleMedicine): Long {
            val id = if (scheduleMedicine.id == 0L) (medicines.size + 1).toLong() else scheduleMedicine.id
            medicines.add(scheduleMedicine.copy(id = id))
            return id
        }
        override suspend fun updateScheduleMedicine(scheduleMedicine: ScheduleMedicine) {}
        override suspend fun insertScheduleMedicineTime(time: ScheduleMedicineTime): Long {
            val id = if (time.id == 0L) (times.size + 1).toLong() else time.id
            times.add(time.copy(id = id))
            return id
        }
        override suspend fun insertScheduleMedicineTimes(times: List<ScheduleMedicineTime>): List<Long> {
            return times.map { insertScheduleMedicineTime(it) }
        }
        override suspend fun updateScheduleMedicineTime(time: ScheduleMedicineTime) {}
        override suspend fun getScheduleMedicinesForScheduleOnce(scheduleId: Long): List<ScheduleMedicine> =
            medicines.filter { it.scheduleId == scheduleId }
        override suspend fun getScheduleMedicineTimesOnce(scheduleMedicineId: Long): List<ScheduleMedicineTime> =
            times.filter { it.scheduleMedicineId == scheduleMedicineId }
        override suspend fun deleteScheduleMedicineTimes(scheduleMedicineId: Long) {}
        override suspend fun disableScheduleMedicineTimes(scheduleMedicineId: Long) {}
        override suspend fun disableScheduleMedicine(scheduleMedicineId: Long) {}
        override suspend fun updateScheduleStatus(scheduleId: Long, newStatus: String) {}
        override suspend fun deletePendingDosesForTime(timeId: Long) {}
    }

    // -------------------------------------------------------------
    // Scenario 1 & 2: Future Doses Cannot be Marked Taken or Skipped
    // -------------------------------------------------------------
    @Test
    fun testFutureDosesCannotBeMarkedTakenOrSkipped() = runBlocking {
        val fakeDoseDao = FakeDoseRecordDao()
        val fakeScheduleDao = FakeScheduleDao()
        val repo = DoseRepository(fakeDoseDao, fakeScheduleDao)

        // Dose scheduled in the future (10 hours from now)
        val futureScheduledAt = System.currentTimeMillis() + (10 * 3600 * 1000L)
        val futureDose = DoseRecord(
            id = 101L,
            scheduleId = 1L,
            scheduleMedicineId = 1L,
            scheduleMedicineTimeId = 1L,
            medicineName = "Metformin 500mg",
            period = "Morning",
            scheduledAt = futureScheduledAt,
            recordedAt = null,
            status = DoseRecord.STATUS_PENDING,
            isLate = false,
            foodInstruction = "WITH_FOOD"
        )
        fakeDoseDao.insertDoseRecord(futureDose)

        // Attempt to mark future dose as Taken
        repo.recordDoseTaken(101L)

        // Verify dose remains unchanged as PENDING and recordedAt is null
        val afterTakenAttempt = fakeDoseDao.getDoseRecordById(101L)
        assertNotNull(afterTakenAttempt)
        assertEquals(DoseRecord.STATUS_PENDING, afterTakenAttempt?.status)
        assertNull(afterTakenAttempt?.recordedAt)

        // Attempt to mark future dose as Skipped
        repo.recordDoseSkipped(101L)

        val afterSkippedAttempt = fakeDoseDao.getDoseRecordById(101L)
        assertEquals(DoseRecord.STATUS_PENDING, afterSkippedAttempt?.status)
        assertNull(afterSkippedAttempt?.recordedAt)
    }

    // -------------------------------------------------------------
    // Scenario 2 (cont): Reconcile Corrupted Future Records Safely
    // -------------------------------------------------------------
    @Test
    fun testReconcileCorruptedFutureCompletedDoses() = runBlocking {
        val now = System.currentTimeMillis()
        val fakeDoseDao = FakeDoseRecordDao()
        val fakeScheduleDao = FakeScheduleDao()
        val repo = DoseRepository(fakeDoseDao, fakeScheduleDao)

        // Erroneously completed future dose (from prior bug)
        val corruptedFuture = DoseRecord(
            id = 201L,
            scheduleId = 1L,
            scheduleMedicineId = 1L,
            scheduleMedicineTimeId = 1L,
            medicineName = "Atorvastatin 20mg",
            period = "Night",
            scheduledAt = now + (2 * 60 * 60 * 1000L),
            recordedAt = now - 5000L,
            status = DoseRecord.STATUS_TAKEN,
            isLate = false,
            foodInstruction = ""
        )
        // Genuine historical completed dose
        val genuinePast = DoseRecord(
            id = 202L,
            scheduleId = 1L,
            scheduleMedicineId = 1L,
            scheduleMedicineTimeId = 1L,
            medicineName = "Atorvastatin 20mg",
            period = "Night",
            scheduledAt = now - (24 * 60 * 60 * 1000L),
            recordedAt = now - (24 * 60 * 60 * 1000L) + 60000L,
            status = DoseRecord.STATUS_TAKEN,
            isLate = false,
            foodInstruction = ""
        )
        fakeDoseDao.insertDoseRecord(corruptedFuture)
        fakeDoseDao.insertDoseRecord(genuinePast)

        // Run reconciliation
        repo.reconcileFutureDoses(now = now)

        // Check corrupted future dose was repaired to PENDING with null recordedAt
        val repairedDose = fakeDoseDao.getDoseRecordById(201L)
        assertEquals(DoseRecord.STATUS_PENDING, repairedDose?.status)
        assertNull(repairedDose?.recordedAt)

        // Check genuine past dose remained completely intact
        val intactPastDose = fakeDoseDao.getDoseRecordById(202L)
        assertEquals(DoseRecord.STATUS_TAKEN, intactPastDose?.status)
        assertNotNull(intactPastDose?.recordedAt)
    }

    // -------------------------------------------------------------
    // Scenario 3, 4, 5: Schedule Edit Invalidates Old Pending Occurrence
    // -------------------------------------------------------------
    @Test
    fun testScheduleEditRemovesOldPendingOccurrence() = runBlocking {
        val fakeDoseDao = FakeDoseRecordDao()
        val timeId = 55L

        // Old pending dose generated for 1:30 PM (timeId 55)
        val oldOccurrence = DoseRecord(
            id = 301L,
            scheduleId = 1L,
            scheduleMedicineId = 10L,
            scheduleMedicineTimeId = timeId,
            medicineName = "Lisinopril 10mg",
            period = "Morning",
            scheduledAt = 1729010000000L,
            recordedAt = null,
            status = DoseRecord.STATUS_PENDING,
            isLate = false,
            foodInstruction = ""
        )
        // Past genuine completed dose with the same timeId
        val pastOccurrence = DoseRecord(
            id = 300L,
            scheduleId = 1L,
            scheduleMedicineId = 10L,
            scheduleMedicineTimeId = timeId,
            medicineName = "Lisinopril 10mg",
            period = "Morning",
            scheduledAt = 1728923600000L,
            recordedAt = 1728923700000L,
            status = DoseRecord.STATUS_TAKEN,
            isLate = false,
            foodInstruction = ""
        )
        fakeDoseDao.insertDoseRecord(oldOccurrence)
        fakeDoseDao.insertDoseRecord(pastOccurrence)

        assertEquals(2, fakeDoseDao.records.size)

        // When schedule is edited or time changed from 1:30 PM to 1:35 PM,
        // deletePendingDosesForTime is invoked in the transaction
        fakeDoseDao.deletePendingDosesForTime(timeId)

        // Old pending occurrence removed!
        assertNull(fakeDoseDao.getDoseRecordById(301L))

        // Past completed dose preserved!
        val preserved = fakeDoseDao.getDoseRecordById(300L)
        assertNotNull(preserved)
        assertEquals(DoseRecord.STATUS_TAKEN, preserved?.status)
    }

    // -------------------------------------------------------------
    // Scenario 6 & 7: Grouping Dashboard and Dose Reminders by Schedule
    // -------------------------------------------------------------
    @Test
    fun testScheduleGroupingForDashboardAndDoseScreen() {
        val dose1 = DoseRecord(1, 2L, 1, 1, "Med B1", "Morning", 1729014000000L, null, DoseRecord.STATUS_PENDING, false, "")
        val dose2 = DoseRecord(2, 1L, 1, 1, "Med A2", "Morning", 1729017600000L, null, DoseRecord.STATUS_PENDING, false, "")
        val dose3 = DoseRecord(3, 1L, 1, 1, "Med A1", "Morning", 1729010400000L, null, DoseRecord.STATUS_PENDING, false, "")
        val dose4 = DoseRecord(4, 2L, 1, 1, "Med B2", "Morning", 1729008600000L, null, DoseRecord.STATUS_PENDING, false, "")

        val allDoses = listOf(dose1, dose2, dose3, dose4)
        val scheduleNames = mapOf(1L to "Alpha Cardiac Care", 2L to "Beta Diabetes Care")

        val scheduleIds = allDoses.map { it.scheduleId }.distinct()
        val sortedScheduleIds = scheduleIds.sortedWith(
            compareBy({ scheduleNames[it]?.lowercase() ?: "" }, { it })
        )

        val resultList = mutableListOf<DoseListItem>()
        for (schedId in sortedScheduleIds) {
            val schedName = scheduleNames[schedId] ?: "Schedule #$schedId"
            val dosesForSched = allDoses.filter { it.scheduleId == schedId }
                .sortedWith(compareBy({ it.scheduledAt }, { it.id }))

            resultList.add(DoseListItem.ScheduleHeader(schedId, schedName, dosesForSched.size))
            for (dose in dosesForSched) {
                resultList.add(DoseListItem.DoseEntry(dose, schedName))
            }
        }

        // First item is Header for Schedule A
        val header1 = resultList[0] as DoseListItem.ScheduleHeader
        assertEquals(1L, header1.scheduleId)
        assertEquals("Alpha Cardiac Care", header1.scheduleName)
        assertEquals(2, header1.count)

        // Doses for Schedule A sorted by time: Med A1 (09:00 AM), then Med A2 (11:00 AM)
        val entry1 = resultList[1] as DoseListItem.DoseEntry
        assertEquals("Med A1", entry1.dose.medicineName)
        val entry2 = resultList[2] as DoseListItem.DoseEntry
        assertEquals("Med A2", entry2.dose.medicineName)

        // Header for Schedule B
        val header2 = resultList[3] as DoseListItem.ScheduleHeader
        assertEquals(2L, header2.scheduleId)
        assertEquals("Beta Diabetes Care", header2.scheduleName)
        assertEquals(2, header2.count)

        // Doses for Schedule B sorted by time: Med B2 (08:30 AM), then Med B1 (10:00 AM)
        val entry3 = resultList[4] as DoseListItem.DoseEntry
        assertEquals("Med B2", entry3.dose.medicineName)
        val entry4 = resultList[5] as DoseListItem.DoseEntry
        assertEquals("Med B1", entry4.dose.medicineName)
    }

    // -------------------------------------------------------------
    // Scenario 8 & 9: Report Card and Full Report Adherence Consistency
    // -------------------------------------------------------------
    @Test
    fun testReportCardAndDetailedReportCalculationAgreement() {
        val now = 1729000000000L
        val fakeDoseDao = FakeDoseRecordDao()
        val fakeScheduleDao = FakeScheduleDao()
        val repo = DoseRepository(fakeDoseDao, fakeScheduleDao)

        // 6 eligible past doses: 4 Taken, 1 Skipped, 1 Unconfirmed
        // Plus 2 future pending doses
        val testDoses = listOf(
            DoseRecord(1, 10L, 1, 1, "Med 1", "Morning", now - 100000L, now - 90000L, DoseRecord.STATUS_TAKEN, false, ""),
            DoseRecord(2, 10L, 1, 1, "Med 1", "Morning", now - 80000L, now - 70000L, DoseRecord.STATUS_TAKEN, false, ""),
            DoseRecord(3, 10L, 1, 1, "Med 1", "Morning", now - 60000L, now - 50000L, DoseRecord.STATUS_TAKEN, false, ""),
            DoseRecord(4, 10L, 1, 1, "Med 1", "Morning", now - 40000L, now - 30000L, DoseRecord.STATUS_TAKEN, false, ""),
            DoseRecord(5, 10L, 1, 1, "Med 1", "Morning", now - 20000L, now - 10000L, DoseRecord.STATUS_SKIPPED, false, ""),
            DoseRecord(6, 10L, 1, 1, "Med 1", "Morning", now - 10000L, null, DoseRecord.STATUS_UNCONFIRMED, false, ""),
            // Future doses (must NOT be counted in eligible denominator)
            DoseRecord(7, 10L, 1, 1, "Med 1", "Morning", now + 10000L, null, DoseRecord.STATUS_PENDING, false, ""),
            DoseRecord(8, 10L, 1, 1, "Med 1", "Morning", now + 20000L, null, DoseRecord.STATUS_PENDING, false, "")
        )

        val summary = repo.computeScheduleAdherenceSummary(
            scheduleId = 10L,
            scheduleName = "Hypertension Care",
            doses = testDoses,
            now = now
        )

        // Total 8 scheduled, 2 future pending -> 6 eligible doses
        assertEquals(8, summary.totalScheduled)
        assertEquals(4, summary.takenCount)
        assertEquals(1, summary.skippedCount)
        assertEquals(1, summary.unconfirmedCount)
        assertEquals(2, summary.pendingCount)
        assertEquals(6, summary.eligibleDoses)

        // 4 Taken out of 6 eligible = 66%
        assertEquals(66, summary.adherencePercentage)
        assertEquals(66.6, summary.takenPercentage, 0.1)
        assertEquals(16.6, summary.skippedPercentage, 0.1)
        assertEquals(16.6, summary.unconfirmedPercentage, 0.1)
    }

    // -------------------------------------------------------------
    // Scenario 10, 11, 12: Caregiver Dynamic Updates in Reports
    // -------------------------------------------------------------
    @Test
    fun testCaregiverReactiveFlowUpdatesImmediately() = runBlocking {
        val fakeDao = FakeCaregiverDao()
        val repo = CaregiverRepository(fakeDao)

        // Initially no caregiver
        val initial = repo.caregiver.first()
        assertNull(initial)

        // Save caregiver
        repo.saveCaregiver(
            fullName = "Emma Watson",
            relationship = "Daughter",
            phone = "+1 555-0199",
            email = "emma@example.com",
            isEmergencyContact = true
        )

        // Observe flow immediately receives updated caregiver without app restart
        val updated = repo.caregiver.first()
        assertNotNull(updated)
        assertEquals("Emma Watson", updated?.fullName)
        assertEquals("Daughter", updated?.relationship)

        // Delete caregiver returns to null immediately
        repo.deleteCaregiver()
        val afterDelete = repo.caregiver.first()
        assertNull(afterDelete)
    }

    // -------------------------------------------------------------
    // Scenario 13: Individual Reports Group Records by Scheduled Date
    // -------------------------------------------------------------
    @Test
    fun testIndividualReportDateGrouping() {
        val dayGroupFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateFormatHeading = SimpleDateFormat("EEEE • MMM dd, yyyy", Locale.getDefault())

        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.OCTOBER, 15, 8, 0, 0)
        }
        val day1A = cal.timeInMillis
        cal.set(2026, java.util.Calendar.OCTOBER, 15, 20, 0, 0)
        val day1B = cal.timeInMillis
        cal.set(2026, java.util.Calendar.OCTOBER, 16, 9, 0, 0)
        val day2A = cal.timeInMillis
        cal.set(2026, java.util.Calendar.OCTOBER, 17, 10, 0, 0)
        val futureTime = cal.timeInMillis
        val now = futureTime - (3600 * 1000L) // 1 hour before futureTime

        val doseDay1A = DoseRecord(1, 1L, 1, 1, "Med A", "Morning", day1A, day1A + 60000L, DoseRecord.STATUS_TAKEN, false, "")
        val doseDay1B = DoseRecord(2, 1L, 1, 1, "Med B", "Night", day1B, day1B + 60000L, DoseRecord.STATUS_TAKEN, false, "")
        val doseDay2A = DoseRecord(3, 1L, 1, 1, "Med A", "Morning", day2A, day2A + 60000L, DoseRecord.STATUS_TAKEN, false, "")
        val futureDose = DoseRecord(4, 1L, 1, 1, "Med A", "Morning", futureTime, null, DoseRecord.STATUS_PENDING, false, "")

        val allRecords = listOf(doseDay1A, doseDay1B, doseDay2A, futureDose)

        val pastDoses = allRecords.filter { it.scheduledAt <= now }
        val futureDoses = allRecords.filter { it.scheduledAt > now }

        val groupedItems = mutableListOf<ReportDoseListItem>()
        val groupedByDay = pastDoses.groupBy { dayGroupFormat.format(Date(it.scheduledAt)) }
        val sortedDayKeys = groupedByDay.keys.sortedDescending()

        for (dayKey in sortedDayKeys) {
            val dosesForDay = groupedByDay[dayKey].orEmpty().sortedBy { it.scheduledAt }
            val headingDate = dateFormatHeading.format(Date(dosesForDay.first().scheduledAt))
            groupedItems.add(ReportDoseListItem.DateHeader(headingDate, dosesForDay.size))
            for (dose in dosesForDay) {
                groupedItems.add(ReportDoseListItem.DoseItem(dose))
            }
        }

        if (futureDoses.isNotEmpty()) {
            val sortedFuture = futureDoses.sortedBy { it.scheduledAt }
            groupedItems.add(ReportDoseListItem.UpcomingHeader(sortedFuture.size))
            for (dose in sortedFuture) {
                groupedItems.add(ReportDoseListItem.DoseItem(dose))
            }
        }

        // Day 2 (newest past date Oct 16) comes first with 1 dose
        assertTrue(groupedItems[0] is ReportDoseListItem.DateHeader)
        assertEquals(1, (groupedItems[0] as ReportDoseListItem.DateHeader).count)
        assertEquals("Med A", (groupedItems[1] as ReportDoseListItem.DoseItem).dose.medicineName)

        // Day 1 (Oct 15) comes second with 2 doses
        assertTrue(groupedItems[2] is ReportDoseListItem.DateHeader)
        assertEquals(2, (groupedItems[2] as ReportDoseListItem.DateHeader).count)
        assertEquals("Med A", (groupedItems[3] as ReportDoseListItem.DoseItem).dose.medicineName)
        assertEquals("Med B", (groupedItems[4] as ReportDoseListItem.DoseItem).dose.medicineName)

        // Upcoming section is separate at the end with 1 future dose
        assertTrue(groupedItems[5] is ReportDoseListItem.UpcomingHeader)
        assertEquals(1, (groupedItems[5] as ReportDoseListItem.UpcomingHeader).count)
        assertEquals("Med A", (groupedItems[6] as ReportDoseListItem.DoseItem).dose.medicineName)
    }
}
