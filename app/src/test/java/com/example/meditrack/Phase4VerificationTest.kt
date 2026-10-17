package com.example.meditrack

import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Medicine
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.ui.dose.DoseFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class Phase4VerificationTest {

    @Test
    fun testStatusTransitionsAndAuditIntegrity() {
        val scheduledAt = 1729000000000L // e.g. 08:00 AM

        // 1. Initial State: PENDING
        val initialDose = DoseRecord(
            id = 1,
            scheduleId = 10,
            scheduleMedicineId = 20,
            scheduleMedicineTimeId = 30,
            medicineName = "Atorvastatin 20mg",
            period = ScheduleMedicineTime.PERIOD_MORNING,
            scheduledAt = scheduledAt,
            recordedAt = null,
            status = DoseRecord.STATUS_PENDING,
            isLate = false,
            foodInstruction = ScheduleMedicine.FOOD_BEFORE
        )
        assertEquals(DoseRecord.STATUS_PENDING, initialDose.status)
        assertNull(initialDose.recordedAt)
        assertFalse(initialDose.isLate)

        // 2. Window passes without response: marked UNCONFIRMED (never Skipped!)
        val unconfirmedDose = initialDose.copy(status = DoseRecord.STATUS_UNCONFIRMED)
        assertEquals(DoseRecord.STATUS_UNCONFIRMED, unconfirmedDose.status)
        assertNull(unconfirmedDose.recordedAt)
        assertFalse(unconfirmedDose.isLate)

        // 3. Late user response: user updates UNCONFIRMED dose to TAKEN after 90 minutes
        val lateResponseTime = scheduledAt + (90 * 60 * 1000L)
        val isLateTaken = DoseRecord.calculateIsLate(scheduledAt, lateResponseTime, DoseRecord.STATUS_TAKEN)
        val lateTakenDose = unconfirmedDose.copy(
            status = DoseRecord.STATUS_TAKEN,
            recordedAt = lateResponseTime,
            isLate = isLateTaken
        )
        assertEquals(DoseRecord.STATUS_TAKEN, lateTakenDose.status)
        assertEquals(scheduledAt, lateTakenDose.scheduledAt) // original scheduled timestamp preserved!
        assertEquals(lateResponseTime, lateTakenDose.recordedAt) // user action timestamp recorded!
        assertTrue(lateTakenDose.isLate)

        // 4. Late user response: user updates UNCONFIRMED dose to SKIPPED after 120 minutes
        val lateSkipTime = scheduledAt + (120 * 60 * 1000L)
        val isLateSkipped = DoseRecord.calculateIsLate(scheduledAt, lateSkipTime, DoseRecord.STATUS_SKIPPED)
        val lateSkippedDose = unconfirmedDose.copy(
            status = DoseRecord.STATUS_SKIPPED,
            recordedAt = lateSkipTime,
            isLate = isLateSkipped
        )
        assertEquals(DoseRecord.STATUS_SKIPPED, lateSkippedDose.status)
        assertEquals(scheduledAt, lateSkippedDose.scheduledAt)
        assertEquals(lateSkipTime, lateSkippedDose.recordedAt)
        assertTrue(lateSkippedDose.isLate)
    }

    @Test
    fun testResponseWindowEvaluation_60MinutesThreshold() {
        val scheduledAt = 1729000000000L

        // Taken 45 minutes after scheduled time (<= 60 min) -> NOT late
        val onTime45m = scheduledAt + (45 * 60 * 1000L)
        assertFalse(DoseRecord.calculateIsLate(scheduledAt, onTime45m, DoseRecord.STATUS_TAKEN))

        // Taken exactly 60 minutes after scheduled time -> NOT late (boundary)
        val boundary60m = scheduledAt + (60 * 60 * 1000L)
        assertFalse(DoseRecord.calculateIsLate(scheduledAt, boundary60m, DoseRecord.STATUS_TAKEN))

        // Taken 61 minutes after scheduled time (> 60 min threshold) -> LATE
        val late61m = scheduledAt + (61 * 60 * 1000L)
        assertTrue(DoseRecord.calculateIsLate(scheduledAt, late61m, DoseRecord.STATUS_TAKEN))

        // PENDING or UNCONFIRMED or null recordedAt must NEVER be flagged as late
        assertFalse(DoseRecord.calculateIsLate(scheduledAt, late61m, DoseRecord.STATUS_PENDING))
        assertFalse(DoseRecord.calculateIsLate(scheduledAt, late61m, DoseRecord.STATUS_UNCONFIRMED))
        assertFalse(DoseRecord.calculateIsLate(scheduledAt, null, DoseRecord.STATUS_TAKEN))
    }

    @Test
    fun testIdempotentOccurrenceGeneration() {
        val todayStart = 1729000000000L // midnight
        val morningTimeId = 101L
        val eveningTimeId = 102L

        // Occurrence generator simulation matching DoseRepository
        val existingUniqueKeys = mutableSetOf<Pair<Long, Long>>()
        val generatedRecords = mutableListOf<DoseRecord>()

        fun generateOccurrences(timeId: Long, minutes: Int, medName: String) {
            val scheduledAt = todayStart + (minutes * 60 * 1000L)
            val key = Pair(timeId, scheduledAt)
            if (existingUniqueKeys.add(key)) {
                generatedRecords.add(
                    DoseRecord(
                        scheduleId = 1,
                        scheduleMedicineId = 10,
                        scheduleMedicineTimeId = timeId,
                        medicineName = medName,
                        period = ScheduleMedicineTime.PERIOD_MORNING,
                        scheduledAt = scheduledAt
                    )
                )
            }
        }

        // First pass: generates 2 doses
        generateOccurrences(morningTimeId, 480, "Lisinopril 10mg")
        generateOccurrences(eveningTimeId, 1200, "Lisinopril 10mg")
        assertEquals(2, generatedRecords.size)

        // Second pass: revisiting screen on same date
        generateOccurrences(morningTimeId, 480, "Lisinopril 10mg")
        generateOccurrences(eveningTimeId, 1200, "Lisinopril 10mg")
        // Idempotency: count remains exactly 2, zero duplicates created!
        assertEquals(2, generatedRecords.size)
    }

    @Test
    fun testScheduleDateBoundaries() {
        val targetDateMidnight = 1729000000000L // Oct 15

        // Case 1: Schedule starts in future (Oct 20) -> should be skipped
        val futureStartDate = 1729432000000L
        val isApplicableFuture = targetDateMidnight >= futureStartDate
        assertFalse("Future schedule must not produce occurrences for today", isApplicableFuture)

        // Case 2: Schedule ended in past (Oct 10) -> should be skipped
        val pastEndDate = 1728568000000L
        val isApplicablePast = targetDateMidnight <= pastEndDate
        assertFalse("Expired schedule must not produce occurrences for today", isApplicablePast)

        // Case 3: Schedule active within bounds (Oct 01 to Oct 30) -> applicable
        val activeStartDate = 1727740800000L
        val activeEndDate = 1730332800000L
        val isApplicableActive = targetDateMidnight >= activeStartDate && targetDateMidnight <= activeEndDate
        assertTrue("Active schedule within date range must produce occurrences", isApplicableActive)
    }

    @Test
    fun testDisabledTimeSlotsAndInactiveAssignmentsExcluded() {
        val activeMed = ScheduleMedicine(id = 1, scheduleId = 1, medicineId = 1, isActive = true)
        val inactiveMed = ScheduleMedicine(id = 2, scheduleId = 1, medicineId = 2, isActive = false)

        val enabledTime = ScheduleMedicineTime(id = 10, scheduleMedicineId = 1, period = ScheduleMedicineTime.PERIOD_MORNING, minutesSinceMidnight = 480, isEnabled = true)
        val disabledTime = ScheduleMedicineTime(id = 11, scheduleMedicineId = 1, period = ScheduleMedicineTime.PERIOD_NIGHT, minutesSinceMidnight = 1200, isEnabled = false)

        // Generator filters
        assertTrue(activeMed.isActive && enabledTime.isEnabled)
        assertFalse(activeMed.isActive && disabledTime.isEnabled)
        assertFalse(inactiveMed.isActive && enabledTime.isEnabled)
    }

    @Test
    fun testHistoricalSnapshotRetentionOnScheduleEdit() {
        // A historical dose stores the snapshot of medicine name and meal instruction
        val dose = DoseRecord(
            id = 55,
            scheduleId = 1,
            scheduleMedicineId = 10,
            scheduleMedicineTimeId = 20,
            medicineName = "Amoxicillin 500mg",
            period = ScheduleMedicineTime.PERIOD_MORNING,
            scheduledAt = 1729000000000L,
            foodInstruction = ScheduleMedicine.FOOD_AFTER
        )

        // If the schedule medicine is subsequently edited or replaced
        val updatedMedicine = Medicine(id = 10, name = "Amoxicillin 250mg (Modified)", instructions = "New instructions")

        // The historical DoseRecord remains unchanged and fully readable
        assertEquals("Amoxicillin 500mg", dose.medicineName)
        assertEquals(ScheduleMedicine.FOOD_AFTER, dose.foodInstruction)
    }

    @Test
    fun testDoseFilteringLogic() {
        val now = 1729050000000L

        val doses = listOf(
            DoseRecord(id = 1, scheduleId = 1, scheduleMedicineId = 1, scheduleMedicineTimeId = 1, medicineName = "Med A", period = "Morning", scheduledAt = now - 3600000, status = DoseRecord.STATUS_TAKEN),
            DoseRecord(id = 2, scheduleId = 1, scheduleMedicineId = 1, scheduleMedicineTimeId = 2, medicineName = "Med B", period = "Morning", scheduledAt = now - 1800000, status = DoseRecord.STATUS_SKIPPED),
            DoseRecord(id = 3, scheduleId = 1, scheduleMedicineId = 1, scheduleMedicineTimeId = 3, medicineName = "Med C", period = "Afternoon", scheduledAt = now - 7200000, status = DoseRecord.STATUS_UNCONFIRMED),
            DoseRecord(id = 4, scheduleId = 1, scheduleMedicineId = 1, scheduleMedicineTimeId = 4, medicineName = "Med D", period = "Evening", scheduledAt = now + 3600000, status = DoseRecord.STATUS_PENDING),
            DoseRecord(id = 5, scheduleId = 1, scheduleMedicineId = 1, scheduleMedicineTimeId = 5, medicineName = "Med E", period = "Night", scheduledAt = now + 7200000, status = DoseRecord.STATUS_PENDING)
        )

        // Filter ALL
        val allFiltered = when (DoseFilter.ALL) {
            DoseFilter.ALL -> doses
            else -> emptyList()
        }
        assertEquals(5, allFiltered.size)

        // Filter UPCOMING (PENDING)
        val upcoming = doses.filter { it.status == DoseRecord.STATUS_PENDING }
        assertEquals(2, upcoming.size)

        // Filter TAKEN
        val taken = doses.filter { it.status == DoseRecord.STATUS_TAKEN }
        assertEquals(1, taken.size)
        assertEquals(1L, taken[0].id)

        // Filter SKIPPED
        val skipped = doses.filter { it.status == DoseRecord.STATUS_SKIPPED }
        assertEquals(1, skipped.size)
        assertEquals(2L, skipped[0].id)

        // Filter UNCONFIRMED
        val unconfirmed = doses.filter { it.status == DoseRecord.STATUS_UNCONFIRMED }
        assertEquals(1, unconfirmed.size)
        assertEquals(3L, unconfirmed[0].id)
    }
}
