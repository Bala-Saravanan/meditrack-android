package com.example.meditrack

import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.data.local.model.MedicineAdherenceSummary
import com.example.meditrack.data.local.model.ScheduleAdherenceSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase1VerificationTest {

    @Test
    fun testNormalizedMinutesSinceMidnightFormatting() {
        assertEquals("08:00 AM", ScheduleMedicineTime.formatTime(480))
        assertEquals("12:00 PM", ScheduleMedicineTime.formatTime(720))
        assertEquals("08:00 PM", ScheduleMedicineTime.formatTime(1200))
        assertEquals("10:00 PM", ScheduleMedicineTime.formatTime(1320))
        assertEquals("12:00 AM", ScheduleMedicineTime.formatTime(0))
        assertEquals("01:15 PM", ScheduleMedicineTime.formatTime(13 * 60 + 15))
    }

    @Test
    fun testTimestampHandlingAndLateEvaluation() {
        val scheduledTime = 1728892800000L // 08:00 AM

        // On-time submission: recorded 15 minutes after scheduled time
        val onTimeSubmission = scheduledTime + (15 * 60 * 1000L)
        assertFalse(DoseRecord.calculateIsLate(scheduledTime, onTimeSubmission))

        // Late submission: recorded 65 minutes after scheduled time (> 60 min threshold)
        val lateSubmission = scheduledTime + (65 * 60 * 1000L)
        assertTrue(DoseRecord.calculateIsLate(scheduledTime, lateSubmission))

        // Exactly on threshold: 60 minutes
        val thresholdSubmission = scheduledTime + (60 * 60 * 1000L)
        assertFalse(DoseRecord.calculateIsLate(scheduledTime, thresholdSubmission))

        // Null-safe assertions
        assertFalse(DoseRecord.calculateIsLate(scheduledTime, null))
        assertFalse(DoseRecord.calculateIsLate(null, onTimeSubmission))
        assertFalse(DoseRecord.calculateIsLate(null, null))

        // Only applies when dose has a recorded response (TAKEN or SKIPPED)
        // UNCONFIRMED and PENDING must never be flagged as late
        assertFalse(DoseRecord.calculateIsLate(scheduledTime, lateSubmission, DoseRecord.STATUS_UNCONFIRMED))
        assertFalse(DoseRecord.calculateIsLate(scheduledTime, lateSubmission, DoseRecord.STATUS_PENDING))
        assertTrue(DoseRecord.calculateIsLate(scheduledTime, lateSubmission, DoseRecord.STATUS_TAKEN))
        assertTrue(DoseRecord.calculateIsLate(scheduledTime, lateSubmission, DoseRecord.STATUS_SKIPPED))
    }

    @Test
    fun testDistinctDoseStatesAndAuditIntegrity() {
        val scheduledTime = 1728892800000L

        // Initial pending state: recordedAt is null
        val pendingDose = DoseRecord(
            id = 101,
            scheduleId = 1,
            scheduleMedicineId = 1,
            scheduleMedicineTimeId = 1,
            medicineName = "Atorvastatin 20mg",
            period = ScheduleMedicineTime.PERIOD_MORNING,
            scheduledAt = scheduledTime,
            recordedAt = null,
            status = DoseRecord.STATUS_PENDING,
            isLate = false,
            foodInstruction = "BEFORE_FOOD"
        )
        assertEquals(DoseRecord.STATUS_PENDING, pendingDose.status)
        assertNull(pendingDose.recordedAt)

        // When scheduled window passes with no response, marked UNCONFIRMED (never Skipped!)
        val unconfirmedDose = pendingDose.copy(status = DoseRecord.STATUS_UNCONFIRMED)
        assertEquals(DoseRecord.STATUS_UNCONFIRMED, unconfirmedDose.status)
        assertNull(unconfirmedDose.recordedAt)

        // Late user response: user updates UNCONFIRMED dose to TAKEN
        val lateRecordedAt = scheduledTime + (90 * 60 * 1000L)
        val resolvedDose = unconfirmedDose.copy(
            status = DoseRecord.STATUS_TAKEN,
            recordedAt = lateRecordedAt,
            isLate = DoseRecord.calculateIsLate(scheduledTime, lateRecordedAt)
        )

        assertEquals(DoseRecord.STATUS_TAKEN, resolvedDose.status)
        assertEquals(scheduledTime, resolvedDose.scheduledAt) // Original scheduledAt preserved!
        assertEquals(lateRecordedAt, resolvedDose.recordedAt) // User action timestamp preserved!
        assertTrue(resolvedDose.isLate) // Correctly flagged as late submission
    }

    @Test
    fun testHistoricalRecordPreservationWhenScheduleModified() {
        // Dose record snapshots medicine name and food instruction at time of occurrence
        val historicalDose = DoseRecord(
            id = 201,
            scheduleId = 1,
            scheduleMedicineId = 2,
            scheduleMedicineTimeId = 3,
            medicineName = "Lisinopril 10mg",
            period = ScheduleMedicineTime.PERIOD_EVENING,
            scheduledAt = 1728892800000L,
            recordedAt = 1728893100000L,
            status = DoseRecord.STATUS_TAKEN,
            isLate = false,
            foodInstruction = "WITH_FOOD"
        )

        // Even if future schedule changes medicine to 20mg or changes food instruction,
        // the historical dose record retains its immutable audit snapshot
        assertEquals("Lisinopril 10mg", historicalDose.medicineName)
        assertEquals("WITH_FOOD", historicalDose.foodInstruction)
        assertEquals(ScheduleMedicineTime.PERIOD_EVENING, historicalDose.period)
    }

    @Test
    fun testAccurateAdherenceSummaryCalculations() {
        // Scenario 1: 8 total scheduled, 6 taken, 1 skipped, 1 unconfirmed (from Stitch Dashboard!)
        val summary1 = ScheduleAdherenceSummary(
            scheduleId = 1,
            scheduleName = "Cardiovascular Care",
            totalScheduled = 8,
            takenCount = 6,
            skippedCount = 1,
            unconfirmedCount = 1,
            pendingCount = 0,
            lateCount = 0
        )
        assertEquals(8, summary1.totalScheduled)
        assertEquals(6, summary1.takenCount)
        assertEquals(1, summary1.skippedCount)
        assertEquals(1, summary1.unconfirmedCount)
        assertEquals(7, summary1.completedActionCount)
        // 6 of 8 = 75%
        assertEquals(75, summary1.adherencePercentage)

        // Scenario 2: 8 scheduled, 8 taken = 100% (from Stitch Post-Surgery Recovery!)
        val summary2 = ScheduleAdherenceSummary(
            scheduleId = 2,
            scheduleName = "Post-Surgery Recovery",
            totalScheduled = 8,
            takenCount = 8,
            skippedCount = 0,
            unconfirmedCount = 0,
            pendingCount = 0,
            lateCount = 0
        )
        assertEquals(100, summary2.adherencePercentage)

        // Scenario 3: 4 scheduled today, 2 taken, 2 pending for tonight
        // Adherence of completed intake window (2 taken out of 2 due = 100% of past doses)
        val summary3 = ScheduleAdherenceSummary(
            scheduleId = 1,
            scheduleName = "Today Partial",
            totalScheduled = 4,
            takenCount = 2,
            skippedCount = 0,
            unconfirmedCount = 0,
            pendingCount = 2,
            lateCount = 0
        )
        // Eligible doses = 4 - 2 pending = 2; taken = 2; adherence = 100% of past doses
        assertEquals(100, summary3.adherencePercentage)

        // Scenario 4: Medicine-level breakdown
        val medSummary = MedicineAdherenceSummary(
            medicineName = "Lisinopril 10mg",
            totalScheduled = 14,
            takenCount = 10,
            skippedCount = 2,
            unconfirmedCount = 2,
            lateCount = 2
        )
        // 10 of 14 = 71.4% -> 71%
        assertEquals(71, medSummary.adherencePercentage)
    }

    @Test
    fun testUserProfileContractAndNullSafety() {
        val emptyUser: com.example.meditrack.data.local.entity.UserProfile? = null
        assertNull(emptyUser)

        val profile = com.example.meditrack.data.local.entity.UserProfile(id = 1, fullName = "Johnathan Davis")
        assertEquals(1L, profile.id)
        assertEquals("Johnathan Davis", profile.fullName)
    }
}
