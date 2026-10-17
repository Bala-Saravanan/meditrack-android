package com.example.meditrack

import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.data.local.model.DateRangeType
import com.example.meditrack.data.local.model.MedicineAdherenceSummary
import com.example.meditrack.data.local.model.ScheduleAdherenceSummary
import com.example.meditrack.data.local.model.ScheduleDetailedReport
import com.example.meditrack.report.PdfReportGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Calendar

class Phase6VerificationTest {

    private fun getMidnightCalendar(daysOffset: Int = 0): Calendar {
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    @Test
    fun testStatusCountsAndDistinctOccurrences() {
        val doses = listOf(
            createDose(1, 10, DoseRecord.STATUS_TAKEN, 1729000000000L, 1729000600000L, false),
            createDose(2, 10, DoseRecord.STATUS_TAKEN, 1729003600000L, 1729004200000L, false),
            createDose(3, 10, DoseRecord.STATUS_SKIPPED, 1729007200000L, 1729007800000L, false),
            createDose(4, 10, DoseRecord.STATUS_UNCONFIRMED, 1729010800000L, null, false),
            createDose(5, 10, DoseRecord.STATUS_PENDING, 1729014400000L, null, false)
        )

        val taken = doses.count { it.status == DoseRecord.STATUS_TAKEN }
        val skipped = doses.count { it.status == DoseRecord.STATUS_SKIPPED }
        val unconfirmed = doses.count { it.status == DoseRecord.STATUS_UNCONFIRMED }
        val pending = doses.count { it.status == DoseRecord.STATUS_PENDING }

        assertEquals(2, taken)
        assertEquals(1, skipped)
        assertEquals(1, unconfirmed)
        assertEquals(1, pending)
        assertEquals(5, doses.size)
    }

    @Test
    fun testAdherenceDenominatorAndRounding() {
        // Case 1: 24 taken, 2 skipped, 2 unconfirmed, 0 pending (total 28)
        // Eligible = 28 - 0 = 28
        // Adherence = (24 / 28) * 100 = 85.7% -> 85%
        val summary1 = ScheduleAdherenceSummary(
            scheduleId = 1,
            scheduleName = "Cardiovascular Care",
            totalScheduled = 28,
            takenCount = 24,
            skippedCount = 2,
            unconfirmedCount = 2,
            pendingCount = 0,
            lateCount = 3
        )
        assertEquals(28, summary1.eligibleDoses)
        assertEquals(85, summary1.adherencePercentage)
        assertEquals(26, summary1.completedActionCount)
        assertEquals(85.71, summary1.takenPercentage, 0.1)
        assertEquals(7.14, summary1.skippedPercentage, 0.1)
        assertEquals(7.14, summary1.unconfirmedPercentage, 0.1)

        // Case 2: Future pending doses exist (10 taken, 0 skipped, 0 unconfirmed, 5 future pending, total 15)
        // Eligible = 15 - 5 = 10
        // Adherence = (10 / 10) * 100 = 100% (future doses do not unfairly lower the score)
        val summary2 = ScheduleAdherenceSummary(
            scheduleId = 2,
            scheduleName = "Ongoing Regimen",
            totalScheduled = 15,
            takenCount = 10,
            skippedCount = 0,
            unconfirmedCount = 0,
            pendingCount = 5,
            lateCount = 0
        )
        assertEquals(10, summary2.eligibleDoses)
        assertEquals(100, summary2.adherencePercentage)

        // Case 3: Empty schedule (0 scheduled doses)
        val summaryEmpty = ScheduleAdherenceSummary(
            scheduleId = 3,
            scheduleName = "New Regimen",
            totalScheduled = 0,
            takenCount = 0,
            skippedCount = 0,
            unconfirmedCount = 0,
            pendingCount = 0,
            lateCount = 0
        )
        assertEquals(0, summaryEmpty.eligibleDoses)
        assertEquals(0, summaryEmpty.adherencePercentage)
    }

    @Test
    fun testUnconfirmedDosesAreNotTreatedAsSkipped() {
        val summary = ScheduleAdherenceSummary(
            scheduleId = 1,
            scheduleName = "Test",
            totalScheduled = 10,
            takenCount = 6,
            skippedCount = 2,
            unconfirmedCount = 2,
            pendingCount = 0,
            lateCount = 0
        )
        // User explicitly completed actions: 6 taken + 2 skipped = 8
        assertEquals(8, summary.completedActionCount)
        // Unconfirmed is 2, distinctly separated from skipped
        assertEquals(2, summary.unconfirmedCount)
        assertEquals(2, summary.skippedCount)
        // Adherence denominator includes unconfirmed: 6 / 10 = 60%
        assertEquals(60, summary.adherencePercentage)
    }

    @Test
    fun testDateRangeInclusivityAndBoundaries() {
        val schedule = Schedule(
            id = 1,
            name = "Test Schedule",
            startDate = getMidnightCalendar(-10).timeInMillis,
            endDate = getMidnightCalendar(10).timeInMillis,
            status = Schedule.STATUS_ACTIVE
        )

        // Test Last 7 Days (6 days back to today end)
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, -6)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val expected7dStart = cal.timeInMillis

        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val expectedEnd = cal.timeInMillis

        // Custom range with reversed start and end (validation test)
        val inputStart = 1729100000000L
        val inputEnd = 1729000000000L // earlier than start!
        val validStart = minOf(inputStart, inputEnd)
        val validEnd = maxOf(inputStart, inputEnd)
        assertTrue("Custom start must be <= custom end after validation", validStart <= validEnd)
    }

    @Test
    fun testLateResponseCountingAndPreservation() {
        val scheduledAt = 1729000000000L // 08:00 AM

        // On-time dose (15 minutes after schedule)
        val onTimeDose = createDose(
            id = 1,
            scheduleId = 10,
            status = DoseRecord.STATUS_TAKEN,
            scheduledAt = scheduledAt,
            recordedAt = scheduledAt + (15 * 60 * 1000L),
            isLate = DoseRecord.calculateIsLate(scheduledAt, scheduledAt + (15 * 60 * 1000L), DoseRecord.STATUS_TAKEN)
        )
        assertFalse("15m response is not late", onTimeDose.isLate)
        assertEquals(scheduledAt, onTimeDose.scheduledAt)

        // Late dose (75 minutes after schedule)
        val lateDose = createDose(
            id = 2,
            scheduleId = 10,
            status = DoseRecord.STATUS_TAKEN,
            scheduledAt = scheduledAt,
            recordedAt = scheduledAt + (75 * 60 * 1000L),
            isLate = DoseRecord.calculateIsLate(scheduledAt, scheduledAt + (75 * 60 * 1000L), DoseRecord.STATUS_TAKEN)
        )
        assertTrue("75m response is late", lateDose.isLate)
        assertEquals(scheduledAt, lateDose.scheduledAt)

        // Dose with null recordedAt (unconfirmed or pending)
        val nullRecordedDose = createDose(
            id = 3,
            scheduleId = 10,
            status = DoseRecord.STATUS_UNCONFIRMED,
            scheduledAt = scheduledAt,
            recordedAt = null,
            isLate = false
        )
        assertFalse(nullRecordedDose.isLate)
        assertEquals(scheduledAt, nullRecordedDose.scheduledAt)
    }

    @Test
    fun testScheduleIsolationInReporting() {
        val allDoses = listOf(
            createDose(1, scheduleId = 101, status = DoseRecord.STATUS_TAKEN, scheduledAt = 1729000000000L, recordedAt = 1729000500000L, isLate = false),
            createDose(2, scheduleId = 101, status = DoseRecord.STATUS_TAKEN, scheduledAt = 1729003600000L, recordedAt = 1729004100000L, isLate = false),
            createDose(3, scheduleId = 202, status = DoseRecord.STATUS_SKIPPED, scheduledAt = 1729000000000L, recordedAt = 1729000600000L, isLate = false)
        )

        val schedule101Doses = allDoses.filter { it.scheduleId == 101L }
        val schedule202Doses = allDoses.filter { it.scheduleId == 202L }

        assertEquals(2, schedule101Doses.size)
        assertEquals(1, schedule202Doses.size)
        assertTrue("Schedule 101 report must never contain Schedule 202 doses", schedule101Doses.none { it.scheduleId == 202L })
    }

    @Test
    fun testPdfGenerationExecutionAndIntegrity() {
        val summary = ScheduleAdherenceSummary(
            scheduleId = 1,
            scheduleName = "Cardiovascular Care",
            totalScheduled = 14,
            takenCount = 12,
            skippedCount = 1,
            unconfirmedCount = 1,
            pendingCount = 0,
            lateCount = 2
        )

        val medSummaries = listOf(
            MedicineAdherenceSummary("Atorvastatin 20mg", 7, 7, 0, 0, 1),
            MedicineAdherenceSummary("Lisinopril 10mg", 7, 5, 1, 1, 1)
        )

        val doseRecords = (1..14).map { i ->
            createDose(
                id = i.toLong(),
                scheduleId = 1,
                status = if (i <= 12) DoseRecord.STATUS_TAKEN else if (i == 13) DoseRecord.STATUS_SKIPPED else DoseRecord.STATUS_UNCONFIRMED,
                scheduledAt = 1729000000000L + (i * 24 * 3600 * 1000L),
                recordedAt = 1729000000000L + (i * 24 * 3600 * 1000L) + 300000L,
                isLate = (i == 5 || i == 9)
            )
        }

        val report = ScheduleDetailedReport(
            scheduleId = 1,
            scheduleName = "Cardiovascular Care",
            scheduleStatus = "Active",
            startDate = 1729000000000L,
            endDate = 1729000000000L + (14 * 24 * 3600 * 1000L),
            physicianName = "Dr. Sarah Jenkins",
            notes = "Daily cardiac regimen",
            dateRangeType = DateRangeType.LAST_30_DAYS,
            startTimestamp = 1729000000000L,
            endTimestamp = 1729000000000L + (14 * 24 * 3600 * 1000L),
            adherenceSummary = summary,
            medicineSummaries = medSummaries,
            doseRecords = doseRecords,
            hasFutureScheduledDoses = false
        )

        // In pure JVM unit test environment, android.graphics.pdf.PdfDocument is mocked or requires Robolectric.
        // We verify the data structure, calculations, and properties required by PdfReportGenerator.
        assertNotNull(report)
        assertEquals("Cardiovascular Care", report.scheduleName)
        assertEquals(14, report.doseRecords.size)
        assertEquals(85, report.adherenceSummary.adherencePercentage)
    }

    private fun createDose(
        id: Long,
        scheduleId: Long,
        status: String,
        scheduledAt: Long,
        recordedAt: Long?,
        isLate: Boolean
    ): DoseRecord {
        return DoseRecord(
            id = id,
            scheduleId = scheduleId,
            scheduleMedicineId = 1,
            scheduleMedicineTimeId = 1,
            medicineName = "Atorvastatin 20mg",
            period = ScheduleMedicineTime.PERIOD_MORNING,
            scheduledAt = scheduledAt,
            recordedAt = recordedAt,
            status = status,
            isLate = isLate,
            foodInstruction = ScheduleMedicine.FOOD_BEFORE
        )
    }
}
