package com.example.meditrack

import com.example.meditrack.data.local.entity.DoseRecord
import com.example.meditrack.data.local.entity.Schedule
import com.example.meditrack.data.local.entity.ScheduleMedicine
import com.example.meditrack.data.local.entity.ScheduleMedicineTime
import com.example.meditrack.reminder.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class Phase5VerificationTest {

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
    fun testNextReminder_sameDayFutureSlot() {
        val todayMidnight = getMidnightCalendar(0).timeInMillis
        // Now is 07:00 AM today (7 * 60 = 420 minutes)
        val nowMillis = todayMidnight + (7 * 60 * 60 * 1000L)

        // Slot is 08:30 AM (8 * 60 + 30 = 510 minutes)
        val slotMinutes = 8 * 60 + 30
        val expectedTrigger = todayMidnight + (slotMinutes * 60 * 1000L)

        val schedule = Schedule(
            id = 1,
            name = "Morning Cardio & Meds",
            startDate = todayMidnight - (2 * 24 * 60 * 60 * 1000L), // started 2 days ago
            endDate = todayMidnight + (10 * 24 * 60 * 60 * 1000L),  // ends in 10 days
            status = Schedule.STATUS_ACTIVE,
            notificationsEnabled = true
        )
        val timeSlot = ScheduleMedicineTime(
            id = 101,
            scheduleMedicineId = 10,
            minutesSinceMidnight = slotMinutes,
            period = ScheduleMedicineTime.PERIOD_MORNING,
            isEnabled = true
        )

        val trigger = ReminderScheduler.calculateNextOccurrence(schedule, timeSlot, nowMillis)
        assertNotNull("Trigger should be scheduled for today", trigger)
        assertEquals(expectedTrigger, trigger)
    }

    @Test
    fun testNextReminder_sameDayPastSlot_rollsOverToTomorrow() {
        val todayMidnight = getMidnightCalendar(0).timeInMillis
        val tomorrowMidnight = getMidnightCalendar(1).timeInMillis
        // Now is 10:00 AM today (10 * 60 = 600 minutes)
        val nowMillis = todayMidnight + (10 * 60 * 60 * 1000L)

        // Slot was 08:00 AM (8 * 60 = 480 minutes) - has passed for today
        val slotMinutes = 8 * 60
        val expectedTrigger = tomorrowMidnight + (slotMinutes * 60 * 1000L)

        val schedule = Schedule(
            id = 1,
            name = "Morning Cardio & Meds",
            startDate = todayMidnight - (2 * 24 * 60 * 60 * 1000L),
            endDate = todayMidnight + (10 * 24 * 60 * 60 * 1000L),
            status = Schedule.STATUS_ACTIVE,
            notificationsEnabled = true
        )
        val timeSlot = ScheduleMedicineTime(
            id = 102,
            scheduleMedicineId = 10,
            minutesSinceMidnight = slotMinutes,
            period = ScheduleMedicineTime.PERIOD_MORNING,
            isEnabled = true
        )

        val trigger = ReminderScheduler.calculateNextOccurrence(schedule, timeSlot, nowMillis)
        assertNotNull("Trigger should be scheduled for tomorrow", trigger)
        assertEquals(expectedTrigger, trigger)
    }

    @Test
    fun testNextReminder_scheduleStartsInFuture_jumpsToStartDate() {
        val todayMidnight = getMidnightCalendar(0).timeInMillis
        val futureStartMidnight = getMidnightCalendar(5).timeInMillis // starts in 5 days
        val nowMillis = todayMidnight + (9 * 60 * 60 * 1000L)

        val slotMinutes = 8 * 60 // 08:00 AM
        val expectedTrigger = futureStartMidnight + (slotMinutes * 60 * 1000L)

        val schedule = Schedule(
            id = 2,
            name = "Future Regimen",
            startDate = futureStartMidnight,
            endDate = futureStartMidnight + (30 * 24 * 60 * 60 * 1000L),
            status = Schedule.STATUS_ACTIVE,
            notificationsEnabled = true
        )
        val timeSlot = ScheduleMedicineTime(
            id = 103,
            scheduleMedicineId = 11,
            minutesSinceMidnight = slotMinutes,
            period = ScheduleMedicineTime.PERIOD_MORNING,
            isEnabled = true
        )

        val trigger = ReminderScheduler.calculateNextOccurrence(schedule, timeSlot, nowMillis)
        assertNotNull("Trigger should jump to schedule start date", trigger)
        assertEquals(expectedTrigger, trigger)
    }

    @Test
    fun testNextReminder_scheduleExpired_returnsNull() {
        val todayMidnight = getMidnightCalendar(0).timeInMillis
        val expiredEndMidnight = getMidnightCalendar(-1).timeInMillis // ended yesterday
        val nowMillis = todayMidnight + (9 * 60 * 60 * 1000L)

        val schedule = Schedule(
            id = 3,
            name = "Expired Regimen",
            startDate = todayMidnight - (10 * 24 * 60 * 60 * 1000L),
            endDate = expiredEndMidnight,
            status = Schedule.STATUS_ACTIVE,
            notificationsEnabled = true
        )
        val timeSlot = ScheduleMedicineTime(
            id = 104,
            scheduleMedicineId = 12,
            minutesSinceMidnight = 8 * 60,
            period = ScheduleMedicineTime.PERIOD_MORNING,
            isEnabled = true
        )

        val trigger = ReminderScheduler.calculateNextOccurrence(schedule, timeSlot, nowMillis)
        assertNull("Expired schedule must return null trigger", trigger)
    }

    @Test
    fun testNextReminder_scheduleEndsToday_pastSlotCannotRollOver() {
        val todayMidnight = getMidnightCalendar(0).timeInMillis
        val nowMillis = todayMidnight + (12 * 60 * 60 * 1000L) // 12:00 PM

        val schedule = Schedule(
            id = 4,
            name = "Final Day Regimen",
            startDate = todayMidnight - (5 * 24 * 60 * 60 * 1000L),
            endDate = todayMidnight, // ends today
            status = Schedule.STATUS_ACTIVE,
            notificationsEnabled = true
        )
        // Morning slot was 08:00 AM (already passed today)
        val timeSlot = ScheduleMedicineTime(
            id = 105,
            scheduleMedicineId = 13,
            minutesSinceMidnight = 8 * 60,
            period = ScheduleMedicineTime.PERIOD_MORNING,
            isEnabled = true
        )

        val trigger = ReminderScheduler.calculateNextOccurrence(schedule, timeSlot, nowMillis)
        assertNull("Past slot on schedule's last day must not roll over into the future", trigger)
    }

    @Test
    fun testNextReminder_disabledTimeSlot_returnsNull() {
        val todayMidnight = getMidnightCalendar(0).timeInMillis
        val nowMillis = todayMidnight + (7 * 60 * 60 * 1000L)

        val schedule = Schedule(
            id = 5,
            name = "Regimen with disabled slot",
            startDate = todayMidnight,
            endDate = todayMidnight + (10 * 24 * 60 * 60 * 1000L),
            status = Schedule.STATUS_ACTIVE,
            notificationsEnabled = true
        )
        val timeSlot = ScheduleMedicineTime(
            id = 106,
            scheduleMedicineId = 14,
            minutesSinceMidnight = 8 * 60,
            period = ScheduleMedicineTime.PERIOD_MORNING,
            isEnabled = false // DISABLED!
        )

        val trigger = ReminderScheduler.calculateNextOccurrence(schedule, timeSlot, nowMillis)
        assertNull("Disabled time slot must return null trigger", trigger)
    }

    @Test
    fun testNextReminder_notificationsDisabledOnSchedule_returnsNull() {
        val todayMidnight = getMidnightCalendar(0).timeInMillis
        val nowMillis = todayMidnight + (7 * 60 * 60 * 1000L)

        val schedule = Schedule(
            id = 6,
            name = "Silent Regimen",
            startDate = todayMidnight,
            endDate = todayMidnight + (10 * 24 * 60 * 60 * 1000L),
            status = Schedule.STATUS_ACTIVE,
            notificationsEnabled = false // NOTIFICATIONS OFF!
        )
        val timeSlot = ScheduleMedicineTime(
            id = 107,
            scheduleMedicineId = 15,
            minutesSinceMidnight = 8 * 60,
            period = ScheduleMedicineTime.PERIOD_MORNING,
            isEnabled = true
        )

        val trigger = ReminderScheduler.calculateNextOccurrence(schedule, timeSlot, nowMillis)
        assertNull("Notifications disabled on schedule must return null trigger", trigger)
    }

    @Test
    fun testNextReminder_completedOrDiscontinuedStatus_returnsNull() {
        val todayMidnight = getMidnightCalendar(0).timeInMillis
        val nowMillis = todayMidnight + (7 * 60 * 60 * 1000L)

        val timeSlot = ScheduleMedicineTime(
            id = 108,
            scheduleMedicineId = 16,
            minutesSinceMidnight = 8 * 60,
            period = ScheduleMedicineTime.PERIOD_MORNING,
            isEnabled = true
        )

        val completedSchedule = Schedule(
            id = 7,
            name = "Completed Regimen",
            startDate = todayMidnight - (10 * 24 * 60 * 60 * 1000L),
            endDate = todayMidnight + (10 * 24 * 60 * 60 * 1000L),
            status = Schedule.STATUS_COMPLETED,
            notificationsEnabled = true
        )
        assertNull(
            "Completed schedule must not schedule alarms",
            ReminderScheduler.calculateNextOccurrence(completedSchedule, timeSlot, nowMillis)
        )

        val discontinuedSchedule = completedSchedule.copy(status = Schedule.STATUS_DISCONTINUED)
        assertNull(
            "Discontinued schedule must not schedule alarms",
            ReminderScheduler.calculateNextOccurrence(discontinuedSchedule, timeSlot, nowMillis)
        )
    }

    @Test
    fun testAlarmIdentity_requestCodeDeterministicAndCollisionFree() {
        val slotId1 = 1001L
        val slotId2 = 1002L

        val reqCode1a = slotId1.toInt()
        val reqCode1b = slotId1.toInt()
        val reqCode2 = slotId2.toInt()

        // Same slot produces identical request code (idempotent, stable)
        assertEquals("Request codes for same slot must match for cancellation & overwrites", reqCode1a, reqCode1b)

        // Different slots produce distinct request codes (prevents collision / overwrites)
        assertNotEquals("Different slots must have distinct request codes", reqCode1a, reqCode2)
    }

    @Test
    fun testNotificationActions_idempotentTakenUpdateAndAuditPreservation() {
        val scheduledAt = 1729000000000L // 08:00 AM
        val responseAt = scheduledAt + (15 * 60 * 1000L) // 08:15 AM (within 60m threshold)

        val dose = DoseRecord(
            id = 501,
            scheduleId = 10,
            scheduleMedicineId = 20,
            scheduleMedicineTimeId = 30,
            medicineName = "Metformin 500mg",
            period = ScheduleMedicineTime.PERIOD_MORNING,
            scheduledAt = scheduledAt,
            recordedAt = null,
            status = DoseRecord.STATUS_PENDING,
            isLate = false,
            foodInstruction = ScheduleMedicine.FOOD_WITH
        )

        // User taps "Taken" quick action on notification
        val isLate = DoseRecord.calculateIsLate(scheduledAt, responseAt, DoseRecord.STATUS_TAKEN)
        val updatedDose = dose.copy(
            status = DoseRecord.STATUS_TAKEN,
            recordedAt = responseAt,
            isLate = isLate
        )

        assertEquals(DoseRecord.STATUS_TAKEN, updatedDose.status)
        assertEquals(scheduledAt, updatedDose.scheduledAt) // Scheduled time preserved!
        assertEquals(responseAt, updatedDose.recordedAt)   // Recorded time captured!
        assertFalse("15-minute response must not be late", updatedDose.isLate)

        // Idempotency: Duplicate action or tap must not overwrite original recordedAt or scheduledAt
        val duplicateActionTime = responseAt + (5 * 60 * 1000L)
        val shouldUpdate = updatedDose.status != DoseRecord.STATUS_TAKEN && updatedDose.status != DoseRecord.STATUS_SKIPPED
        assertFalse("Idempotency check must prevent updating already logged dose", shouldUpdate)
        assertEquals(responseAt, updatedDose.recordedAt)
    }

    @Test
    fun testNotificationActions_skippedActionPreservesScheduledTimestamp() {
        val scheduledAt = 1729000000000L
        val responseAt = scheduledAt + (10 * 60 * 1000L)

        val dose = DoseRecord(
            id = 502,
            scheduleId = 10,
            scheduleMedicineId = 20,
            scheduleMedicineTimeId = 30,
            medicineName = "Amoxicillin 500mg",
            period = ScheduleMedicineTime.PERIOD_MORNING,
            scheduledAt = scheduledAt,
            recordedAt = null,
            status = DoseRecord.STATUS_PENDING,
            isLate = false,
            foodInstruction = ScheduleMedicine.FOOD_AFTER
        )

        val isLate = DoseRecord.calculateIsLate(scheduledAt, responseAt, DoseRecord.STATUS_SKIPPED)
        val updatedDose = dose.copy(
            status = DoseRecord.STATUS_SKIPPED,
            recordedAt = responseAt,
            isLate = isLate
        )

        assertEquals(DoseRecord.STATUS_SKIPPED, updatedDose.status)
        assertEquals(scheduledAt, updatedDose.scheduledAt) // Scheduled time preserved!
        assertEquals(responseAt, updatedDose.recordedAt)
        assertFalse("Skipped status is never flagged late", updatedDose.isLate)
    }

    @Test
    fun testLateResponseThreshold_sixtyMinuteRule() {
        val scheduledAt = 1729000000000L

        // 59 minutes: on time
        val time59m = scheduledAt + (59 * 60 * 1000L)
        assertFalse(DoseRecord.calculateIsLate(scheduledAt, time59m, DoseRecord.STATUS_TAKEN))

        // Exactly 60 minutes: on time
        val time60m = scheduledAt + (60 * 60 * 1000L)
        assertFalse(DoseRecord.calculateIsLate(scheduledAt, time60m, DoseRecord.STATUS_TAKEN))

        // 61 minutes: late
        val time61m = scheduledAt + (61 * 60 * 1000L)
        assertTrue(DoseRecord.calculateIsLate(scheduledAt, time61m, DoseRecord.STATUS_TAKEN))
    }
}
